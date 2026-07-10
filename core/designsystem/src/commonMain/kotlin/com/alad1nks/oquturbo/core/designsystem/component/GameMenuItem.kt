package com.alad1nks.oquturbo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameMenuItem(
    imageVector: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape = shape)
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 32.dp,
                ),
    ) {
        GameMenuItemIconTitleAndSubtitleRow(
            imageVector = imageVector,
            title = title,
            subtitle = subtitle,
        )
    }
}

@Composable
private fun GameMenuItemIconTitleAndSubtitleRow(
    imageVector: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(16.dp))

        GameMenuItemItemTitleAndSubtitleColumn(
            title = title,
            subtitle = subtitle,
        )
    }
}

@Composable
private fun GameMenuItemItemTitleAndSubtitleColumn(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
        )
    }
}

@Preview
@Composable
private fun GameMenuItemItemPreview() {
    MaterialTheme {
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
