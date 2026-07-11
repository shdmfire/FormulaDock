package com.formuladock.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.formuladock.core.designsystem.theme.FdAlphas
import com.formuladock.core.designsystem.theme.FdDimensions

@Composable
fun FdSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FdDimensions.CardCorner),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = FdAlphas.Container),
        border = BorderStroke(
            FdDimensions.Hairline,
            MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.Border)
        ),
        content = {
            Column(
                modifier = Modifier.padding(
                    horizontal = FdDimensions.SpaceL,
                    vertical = FdDimensions.SpaceS
                ),
                content = content
            )
        }
    )
}

@Composable
fun FdListItemCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(FdDimensions.CardCorner))
            .border(FdDimensions.Hairline, MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.Border), RoundedCornerShape(FdDimensions.CardCorner))
            .clickable(onClick = onClick)
            .padding(FdDimensions.SpaceL),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun FdSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = FdSectionCard(modifier, content)

@Composable
fun FdSettingsOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = FdDimensions.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}
