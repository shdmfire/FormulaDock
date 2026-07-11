package com.formuladock.feature.formula.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal fun TextFieldValue.insertExpressionTemplate(template: String): TextFieldValue {
    val markerIndex = template.indexOf('|')
    val insertedText = template.replace("|", "")

    val selectionStart = minOf(selection.start, selection.end)
    val selectionEnd = maxOf(selection.start, selection.end)

    val newText = StringBuilder(text)
        .replace(selectionStart, selectionEnd, insertedText)
        .toString()

    val newCursorPosition = selectionStart +
            if (markerIndex >= 0) markerIndex else insertedText.length

    return copy(
        text = newText,
        selection = TextRange(newCursorPosition),
    )
}

internal data class ExpressionHelperItem(
    val label: String,
    val template: String,
    val highlighted: Boolean = false,
)

/**
 * Coordinates the single shared [ExpressionHelperBar] across every expression field in the
 * editor, and drives the one animation that reserves space for it (IME + helper bar height).
 *
 * Ownership of the bar transfers purely through explicit [focus]/[blur] calls made from
 * focus-change callbacks — never inferred from recomposition — so there's no ambiguity about
 * which field is "active" when focus moves quickly between fields.
 *
 * [onSettled] emits the owner that should now scroll itself into view, exactly when the
 * reserved-space animation has reached its target. Nothing here guesses an animation duration.
 */
@Stable
internal class FormulaExpressionHelperController(
    private val scope: CoroutineScope,
    private val visualGapPx: Int = 0,
) {
    private val reservedSpace = Animatable(0f)
    private var settleJob: Job? = null
    private val settledEvents = MutableSharedFlow<Any>(extraBufferCapacity = 1)

    var activeOwner by mutableStateOf<Any?>(null)
        private set
    var variables by mutableStateOf<List<String>>(emptyList())
        private set
    var helperHeightPx by mutableIntStateOf(0)
        private set
    var imeBottomPx by mutableIntStateOf(0)
        private set

    private var insertHandler: ((String) -> Unit)? = null

    val isVisible: Boolean
        get() = activeOwner != null && (imeBottomPx > 0 || isDesktop)

    /** Current animated bottom-space reservation, in pixels. */
    val reservedBottomSpacePx: Float
        get() = reservedSpace.value

    val onSettled: SharedFlow<Any> = settledEvents

    fun focus(owner: Any, variables: List<String>, onInsertTemplate: (String) -> Unit) {
        activeOwner = owner
        this.variables = variables
        insertHandler = onInsertTemplate
        scheduleSettle()
    }

    fun blur(owner: Any) {
        if (activeOwner != owner) return
        activeOwner = null
        insertHandler = null
        variables = emptyList()
        scheduleSettle()
    }

    fun insert(template: String) {
        insertHandler?.invoke(template)
    }

    fun reportHelperHeightPx(heightPx: Int) {
        if (helperHeightPx == heightPx) return
        helperHeightPx = heightPx
        scheduleSettle()
    }

    fun reportImeBottomPx(bottomPx: Int) {
        if (imeBottomPx == bottomPx) return
        imeBottomPx = bottomPx
        scheduleSettle()
    }

    private fun scheduleSettle() {
        val target = if (isVisible) (helperHeightPx + visualGapPx).toFloat() else 0f
        settleJob?.cancel()
        settleJob = scope.launch {
            if (reservedSpace.value != target) {
                reservedSpace.animateTo(target, animationSpec = tween(220))
            }
            if (isVisible) activeOwner?.let { settledEvents.emit(it) }
        }
    }
}

/**
 * @param visualGap Cosmetic breathing room between the focused field and the top of the helper
 * bar. Baked into the same animation as the bar's own reveal, so it never pops in separately.
 */
@Composable
internal fun rememberFormulaExpressionHelperController(
    visualGap: Dp = 0.dp,
): FormulaExpressionHelperController {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val visualGapPx = with(density) { visualGap.roundToPx() }
    return remember(visualGapPx) { FormulaExpressionHelperController(scope, visualGapPx) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FormulaExpressionHelperOverlay(
    controller: FormulaExpressionHelperController,
    modifier: Modifier = Modifier,
) {
    if (!controller.isVisible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ExpressionHelperBar(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    controller.reportHelperHeightPx(coordinates.size.height)
                },
            variables = controller.variables,
            onInsertClick = controller::insert,
        )
    }
}

@Composable
internal fun ExpressionHelperBar(
    modifier: Modifier = Modifier,
    variables: List<String>,
    onInsertClick: (String) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(true) }

    val arithmeticItems = remember {
        listOf(
            ExpressionHelperItem("+", " + "),
            ExpressionHelperItem("-", " - "),
            ExpressionHelperItem("*", " * "),
            ExpressionHelperItem("/", " / "),
            ExpressionHelperItem("(", "("),
            ExpressionHelperItem(")", ")"),
            ExpressionHelperItem(">", " > "),
            ExpressionHelperItem("<", " < "),
            ExpressionHelperItem("==", " == "),
            ExpressionHelperItem("PI", "PI"),
            ExpressionHelperItem("E", "E"),
            ExpressionHelperItem("min", "min(|, )"),
            ExpressionHelperItem("max", "max(|, )"),
            ExpressionHelperItem("floor", "floor(|)"),
            ExpressionHelperItem("ceil", "ceil(|)"),
            ExpressionHelperItem("round", "round(|)"),
            ExpressionHelperItem("pow", "pow(|, )"),
            ExpressionHelperItem("sqrt", "sqrt(|)"),
            ExpressionHelperItem("abs", "abs(|)"),
            ExpressionHelperItem("sin", "sin(|)"),
            ExpressionHelperItem("cos", "cos(|)"),
            ExpressionHelperItem("tan", "tan(|)"),
            ExpressionHelperItem("log", "log(|, 10)"),
            ExpressionHelperItem("+ %", "pctAdd(|, )", highlighted = true),
            ExpressionHelperItem("- %", "pctSub(|, )", highlighted = true),
            ExpressionHelperItem("% Of", "pctOf(|, )", highlighted = true),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141416))
            .border(width = 1.dp, color = Color(0xFF27272A))
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Expression Helper",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E93),
                    fontFamily = FontFamily.Monospace,
                ),
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(16.dp),
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                arithmeticItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF222224), RoundedCornerShape(4.dp))
                            .then(
                                if (item.highlighted) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = Color(0xFFEAB308),
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onInsertClick(item.template) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.label,
                            color = if (item.highlighted) Color(0xFFEAB308) else Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                variables.forEach { variable ->
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(4.dp))
                            .clickable { onInsertClick(variable) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = variable,
                            color = Color(0xFFA1A1AA),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
