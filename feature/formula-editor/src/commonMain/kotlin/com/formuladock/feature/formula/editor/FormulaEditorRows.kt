package com.formuladock.feature.formula.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formuladock.core.domain.formula.editor.FormulaValidationIssue

@Composable
internal fun LabelText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        ),
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
internal fun FormulaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                ),
            )
        },
        singleLine = true,
        isError = isError,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Black.copy(alpha = 0.28f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.28f),
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
    )
}

@Composable
internal fun FieldErrorText(message: String?) {
    if (message != null) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun DeleteIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Text("×", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
internal fun DashedButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .dashedBorder(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), shape)
            .background(Color.Black.copy(alpha = 0.1f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

internal fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    shape: Shape,
    on: Float = 8f,
    off: Float = 8f,
): Modifier = drawWithContent {
    drawContent()
    drawOutline(
        outline = shape.createOutline(size, layoutDirection, this),
        color = color,
        style = Stroke(width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(on, off), 0f)),
    )
}

@Composable
internal fun TwoColumnRow(content: @Composable RowScopeMarker.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        RowScopeMarker(this).content()
    }
}

internal class RowScopeMarker(private val rowScope: androidx.compose.foundation.layout.RowScope) {
    @Composable
    fun Cell(content: @Composable () -> Unit) {
        with(rowScope) { Column(modifier = Modifier.weight(1f)) { content() } }
    }
}

internal fun List<FormulaValidationIssue>.fieldMessage(field: String): String? =
    firstOrNull { it.location.field == field }?.message
