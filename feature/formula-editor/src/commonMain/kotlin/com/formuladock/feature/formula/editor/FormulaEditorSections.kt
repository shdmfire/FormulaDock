package com.formuladock.feature.formula.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.formuladock.core.designsystem.component.FdSectionCard
import com.formuladock.core.designsystem.theme.FdDimensions
import com.formuladock.core.designsystem.theme.FdAlphas
import com.formuladock.feature.formula.editor.FormulaEditorAction
import com.formuladock.core.domain.formula.editor.FormulaEditorSection
import com.formuladock.feature.formula.editor.FormulaEditorState
import com.formuladock.core.domain.formula.editor.FormulaPreviewState
import com.formuladock.core.formula.engine.FormulaExpressionNames

/**
 * 统一样式的 M3 Outlined 输入框组件
 */
@Composable
internal fun FormulaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        supportingText = errorText?.let { { Text(it) } },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(FdDimensions.CompactCorner),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun FormulaMetaDataSection(state: FormulaEditorState, onAction: (FormulaEditorAction) -> Unit) {
    FormulaSectionCard("Meta Data", state, FormulaEditorSection.MetaData, onAction) {
        Column(verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS)) {
            val titleError = state.validation.issues.firstOrNull { it.location.field == "title" }?.message
            FormulaTextField(
                value = state.draft.title,
                onValueChange = { onAction(FormulaEditorAction.UpdateTitle(it)) },
                label = "FORMULA TITLE",
                placeholder = "e.g. Cylinder Volume",
                isError = titleError != null,
                errorText = titleError
            )
            FormulaTextField(
                value = state.draft.description,
                onValueChange = { onAction(FormulaEditorAction.UpdateDescription(it)) },
                label = "DESCRIPTION",
                placeholder = "Description...",
            )
        }
    }
}

@Composable
internal fun FormulaInputsSection(state: FormulaEditorState, onAction: (FormulaEditorAction) -> Unit) {
    FormulaSectionCard("Inputs", state, FormulaEditorSection.Inputs, onAction) {
        Column(verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceM)) {
            state.draft.inputs.forEach { input ->
                key(input.rowId) {
                    val issues = state.validation.issuesForRow(input.rowId)
                    Column(verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS), modifier = Modifier.fillMaxWidth()) {
                            FormulaTextField(
                                value = input.key,
                                onValueChange = { onAction(FormulaEditorAction.UpdateInputKey(input.rowId, it)) },
                                label = "VAR NAME",
                                placeholder = "a",
                                isMonospace = true,
                                isError = issues.fieldMessage("key") != null,
                                errorText = issues.fieldMessage("key"),
                                modifier = Modifier.weight(1f)
                            )
                            FormulaTextField(
                                value = input.label,
                                onValueChange = { onAction(FormulaEditorAction.UpdateInputLabel(input.rowId, it)) },
                                label = "DISPLAY",
                                placeholder = "Label",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FormulaTextField(
                                value = input.defaultValue,
                                onValueChange = { onAction(FormulaEditorAction.UpdateInputDefaultValue(input.rowId, it)) },
                                label = "DEFAULT",
                                placeholder = "0",
                                isError = issues.fieldMessage("defaultValue") != null,
                                errorText = issues.fieldMessage("defaultValue"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1.2f)
                            )
                            FormulaTextField(
                                value = input.unit,
                                onValueChange = { onAction(FormulaEditorAction.UpdateInputUnit(input.rowId, it)) },
                                label = "UNIT",
                                placeholder = "m, kg",
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onAction(FormulaEditorAction.RemoveInput(input.rowId)) },
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .padding(top = FdDimensions.SpaceXs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
            DashedAddButton("Add Input Parameter") { onAction(FormulaEditorAction.AddInput) }
        }
    }
}

@Composable
internal fun FormulaConstantsSection(state: FormulaEditorState, onAction: (FormulaEditorAction) -> Unit) {
    FormulaSectionCard("Constants", state, FormulaEditorSection.Constants, onAction, titleColor = MaterialTheme.colorScheme.primary) {
        Column(verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceM)) {
            state.draft.constants.forEach { constant ->
                key(constant.rowId) {
                    val issues = state.validation.issuesForRow(constant.rowId)
                    Column(verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS), modifier = Modifier.fillMaxWidth()) {
                            FormulaTextField(
                                value = constant.key,
                                onValueChange = { onAction(FormulaEditorAction.UpdateConstantKey(constant.rowId, it)) },
                                label = "CONST NAME",
                                placeholder = "PI",
                                isMonospace = true,
                                isError = issues.fieldMessage("key") != null,
                                errorText = issues.fieldMessage("key"),
                                modifier = Modifier.weight(1f)
                            )
                            FormulaTextField(
                                value = constant.label,
                                onValueChange = { onAction(FormulaEditorAction.UpdateConstantLabel(constant.rowId, it)) },
                                label = "DISPLAY",
                                placeholder = "Label",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FormulaTextField(
                                value = constant.value,
                                onValueChange = { onAction(FormulaEditorAction.UpdateConstantValue(constant.rowId, it)) },
                                label = "VALUE",
                                placeholder = "0",
                                isError = issues.fieldMessage("value") != null,
                                errorText = issues.fieldMessage("value"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1.2f)
                            )
                            FormulaTextField(
                                value = constant.unit,
                                onValueChange = { onAction(FormulaEditorAction.UpdateConstantUnit(constant.rowId, it)) },
                                label = "UNIT",
                                placeholder = "unit",
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onAction(FormulaEditorAction.RemoveConstant(constant.rowId)) },
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .padding(top = FdDimensions.SpaceXs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
            DashedAddButton("Add Constant") { onAction(FormulaEditorAction.AddConstant) }
        }
    }
}

@Composable
internal fun FormulaOutputsSection(
    state: FormulaEditorState,
    onAction: (FormulaEditorAction) -> Unit,
    expressionHelper: FormulaExpressionHelperController,
) {
    FormulaSectionCard("Outputs", state, FormulaEditorSection.Outputs, onAction, titleColor = MaterialTheme.colorScheme.tertiary) {
        Column(verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceM)) {
            state.draft.outputs.forEach { output ->
                key(output.rowId) {
                    val issues = state.validation.issuesForRow(output.rowId)

                    Column(verticalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS), modifier = Modifier.fillMaxWidth()) {
                            FormulaTextField(
                                value = output.key,
                                onValueChange = { onAction(FormulaEditorAction.UpdateOutputKey(output.rowId, it)) },
                                label = "OUTPUT KEY",
                                placeholder = "result",
                                isMonospace = true,
                                isError = issues.fieldMessage("key") != null,
                                errorText = issues.fieldMessage("key"),
                                modifier = Modifier.weight(1f)
                            )
                            FormulaTextField(
                                value = output.label,
                                onValueChange = { onAction(FormulaEditorAction.UpdateOutputLabel(output.rowId, it)) },
                                label = "DISPLAY",
                                placeholder = "Result",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        FormulaExpressionTextField(
                            expression = output.expression,
                            onExpressionChange = { onAction(FormulaEditorAction.UpdateOutputExpression(output.rowId, it)) },
                            variables = state.expressionHelperNames(),
                            isError = issues.fieldMessage("expression") != null,
                            errorText = issues.fieldMessage("expression"),
                            expressionHelper = expressionHelper,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(FdDimensions.SpaceS),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FormulaTextField(
                                value = output.unit,
                                onValueChange = { onAction(FormulaEditorAction.UpdateOutputUnit(output.rowId, it)) },
                                label = "UNIT",
                                placeholder = "unit",
                                modifier = Modifier.weight(1f)
                            )
                            FormulaTextField(
                                value = output.precision,
                                onValueChange = { onAction(FormulaEditorAction.UpdateOutputPrecision(output.rowId, it)) },
                                label = "PRECISION",
                                placeholder = "2",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                isError = issues.fieldMessage("precision") != null,
                                errorText = issues.fieldMessage("precision")
                            )
                            IconButton(
                                onClick = { onAction(FormulaEditorAction.RemoveOutput(output.rowId)) },
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .padding(top = FdDimensions.SpaceXs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
            DashedAddButton("Add Output Formula") { onAction(FormulaEditorAction.AddOutput) }
        }
    }
}

@Composable
private fun FormulaExpressionTextField(
    expression: String,
    onExpressionChange: (String) -> Unit,
    variables: List<String>,
    isError: Boolean,
    errorText: String?,
    expressionHelper: FormulaExpressionHelperController,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(expression)) }
    var isFocused by remember { mutableStateOf(false) }

    val owner = remember { Any() }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(expression) {
        if (expression != fieldValue.text) {
            val cursor = fieldValue.selection.end.coerceIn(0, expression.length)
            fieldValue = fieldValue.copy(
                text = expression,
                selection = TextRange(cursor),
            )
        }
    }

    LaunchedEffect(expressionHelper, owner) {
        expressionHelper.onSettled.collect { settledOwner ->
            if (settledOwner == owner && isFocused) {
                bringIntoViewRequester.bringIntoView()
            }
        }
    }

    DisposableEffect(owner) {
        onDispose { expressionHelper.blur(owner) }
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { value ->
            fieldValue = value
            onExpressionChange(value.text)
        },
        label = { Text("EXPRESSION") },
        placeholder = {
            Text(
                text = "a + b",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    fontFamily = FontFamily.Monospace,
                ),
            )
        },
        singleLine = true,
        isError = isError,
        supportingText = errorText?.let { { Text(it) } },
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(FdDimensions.CompactCorner),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    expressionHelper.focus(
                        owner = owner,
                        variables = variables,
                        onInsertTemplate = { template ->
                            val newValue = fieldValue.insertExpressionTemplate(template)
                            fieldValue = newValue
                            onExpressionChange(newValue.text)
                            focusRequester.requestFocus()
                        },
                    )
                } else {
                    expressionHelper.blur(owner)
                }
            },
    )
}

private fun FormulaEditorState.expressionHelperNames(): List<String> {
    val inputNames = draft.inputs
        .map { it.key.trim() }
        .filter { it.isNotBlank() }
        .filter { FormulaExpressionNames.isValidIdentifier(it) }

    val constantNames = draft.constants
        .map { it.key.trim() }
        .filter { it.isNotBlank() }
        .filter { FormulaExpressionNames.isValidIdentifier(it) }

    return (inputNames + constantNames)
        .distinct()
        .ifEmpty { listOf("weight", "PI_2") }
}

@Composable
internal fun FormulaPreviewSection(state: FormulaEditorState, onAction: (FormulaEditorAction) -> Unit) {
    FormulaSectionCard("Preview", state, FormulaEditorSection.Preview, onAction) {
        when (val preview = state.preview) {
            FormulaPreviewState.Empty -> {
                Text(
                    text = "Fix validation errors or add default input values to preview.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            is FormulaPreviewState.Failure -> {
                Text(
                    text = preview.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            is FormulaPreviewState.Success -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    preview.outputs.forEach { output ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = output.label.ifBlank { output.key },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = output.formattedValue + (output.unit?.let { " $it" } ?: ""),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulaSectionCard(
    title: String,
    state: FormulaEditorState,
    section: FormulaEditorSection,
    onAction: (FormulaEditorAction) -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    val expanded = state.expandedSections.isExpanded(section)

    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    FdSectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAction(FormulaEditorAction.ToggleSection(section)) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = titleColor),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(rotationAngle),
                tint = titleColor
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(FdDimensions.SpaceS))
                content()
            }
        }
    }
}

/**
 * 精致的 M3 添加按钮替代原本的 DashedButton
 */
@Composable
private fun DashedAddButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(36.dp),
        shape = RoundedCornerShape(FdDimensions.CompactCorner),
        border = BorderStroke(FdDimensions.Hairline, MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.Container)),
        contentPadding = PaddingValues(vertical = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(FdDimensions.IconS)
        )
        Spacer(Modifier.width(FdDimensions.SpaceS))
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}
