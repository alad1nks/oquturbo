package com.alad1nks.oquturbo.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview

@Composable
fun GameMenuItem(
    imageVector: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameMenuItemIcon(imageVector = imageVector)
            Spacer(modifier = Modifier.width(16.dp))
            GameMenuItemTitleAndSubtitle(
                title = title,
                subtitle = subtitle,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GameMenuItemIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun GameMenuItemTitleAndSubtitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(
    name = "Game menu item",
    widthDp = 390,
    heightDp = 120,
)
@ScreenshotPreview
@Composable
private fun GameMenuItemItemPreview() {
    OquTurboTheme {
        Surface {
            GameMenuItem(
                imageVector = Icons.Outlined.Timer,
                title = "Game Title",
                subtitle = "Game Subtitle",
                onClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(
    name = "Game menu items — matching height",
    widthDp = 720,
    heightDp = 220,
)
@ScreenshotPreview
@Composable
private fun GameMenuItemsMatchingHeightPreview() {
    OquTurboTheme {
        Surface {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GameMenuItem(
                    imageVector = Icons.Outlined.Timer,
                    title = "Short title",
                    subtitle = "Short subtitle",
                    onClick = {},
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                GameMenuItem(
                    imageVector = Icons.Outlined.Timer,
                    title = "A deliberately long game title",
                    subtitle = "A longer subtitle that wraps onto multiple lines at this width",
                    onClick = {},
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}
