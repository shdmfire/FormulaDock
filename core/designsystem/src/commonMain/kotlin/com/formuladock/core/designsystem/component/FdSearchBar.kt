package com.formuladock.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.formuladock.core.designsystem.theme.FdAlphas
import com.formuladock.core.designsystem.theme.FdDimensions

@Composable
fun FdSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    clearContentDescription: String,
    filterContentDescription: String? = null,
    onFilterClick: (() -> Unit)? = null,
    hasActiveFilters: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FdDimensions.SearchBarHeight)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = FdAlphas.Container),
                shape = RoundedCornerShape(FdDimensions.SearchBarCorner)
            )
            .border(
                width = FdDimensions.Hairline,
                color = MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.Border),
                shape = RoundedCornerShape(FdDimensions.SearchBarCorner)
            )
            .padding(horizontal = FdDimensions.SpaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.MutedContent),
            modifier = Modifier.size(FdDimensions.IconM)
        )
        Spacer(modifier = Modifier.width(FdDimensions.SpaceS))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.MutedContent),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(FdDimensions.IconButtonS)) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = clearContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FdAlphas.SecondaryContent),
                    modifier = Modifier.size(FdDimensions.IconS)
                )
            }
        }
        if (onFilterClick != null) {
            Spacer(
                modifier = Modifier
                    .padding(horizontal = FdDimensions.SpaceS)
                    .height(FdDimensions.IconM)
                    .width(FdDimensions.Hairline)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = FdAlphas.Divider))
            )
            IconButton(onClick = onFilterClick, modifier = Modifier.size(FdDimensions.IconButtonS)) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = filterContentDescription,
                    tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(FdDimensions.IconM)
                )
            }
        }
    }
}
