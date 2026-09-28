package com.alad1nks.oquturbo.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboLayout
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview

@Composable
private fun DesignGallery(dark: Boolean) {
    OquTurboTheme(darkTheme = dark) {
        Column(
            Modifier.appBackground().padding(OquTurboLayout.pageGutter),
            verticalArrangement = Arrangement.spacedBy(OquTurboLayout.gap),
        ) {
            PageHeader("Training", "Shared surfaces and actions")
            AppCard(Modifier.fillMaxWidth()) { Text("Neutral page card", Modifier.padding(OquTurboLayout.cardInset)) }
            AppCard(
                Modifier.fillMaxWidth(),
                compact = true,
            ) { Text("Compact card", Modifier.padding(OquTurboLayout.gap)) }
            AppCard(Modifier.fillMaxWidth(), tone = AppCardTone.Primary) {
                Text("Primary training", Modifier.padding(OquTurboLayout.cardInset))
            }
            AppCard(Modifier.fillMaxWidth(), tone = AppCardTone.Secondary) {
                Text("Secondary progress", Modifier.padding(OquTurboLayout.cardInset))
            }
            AppCard(Modifier.fillMaxWidth(), tone = AppCardTone.Subdued) {
                Text("Supporting information", Modifier.padding(OquTurboLayout.cardInset))
            }
            GameMenuItem(Icons.Outlined.Timer, "Number Sprint", "Remember a sequence of numbers", {})
            GameStagePanel(GameStage.Ready) {
                Text("Ready to train")
                Text("Remember the target and select the matching answer.")
                Button({}, Modifier.fillMaxWidth().heightIn(min = OquTurboLayout.actionMinHeight)) { Text("Start") }
            }
            GameStagePanel(GameStage.Paused) {
                Text("Game paused")
                Button({}, Modifier.fillMaxWidth().heightIn(min = OquTurboLayout.actionMinHeight)) { Text("Resume") }
            }
            GameResultCard("24 correct answers", "Your best: 28", Modifier.fillMaxWidth())
        }
    }
}

@Preview(widthDp = 390, heightDp = 1200)
@ScreenshotPreview
@Composable
private fun DesignGalleryLightPreview() = DesignGallery(false)

@Preview(widthDp = 390, heightDp = 1200)
@ScreenshotPreview
@Composable
private fun DesignGalleryDarkPreview() = DesignGallery(true)

@Preview(widthDp = 320, heightDp = 1800, fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun DesignGalleryCompactLargeTextPreview() = DesignGallery(false)

@Composable
private fun LongHeader(scoreLabel: String, recordLabel: String) {
    OquTurboTheme {
        Box(Modifier.fillMaxSize().appBackground()) {
            GameHeader(
                scoreLabel,
                "2147483647",
                recordLabel,
                "2147483647",
                Modifier.padding(OquTurboLayout.pageGutter),
                leadingContent = { AppBackButton({}) },
            )
        }
    }
}

@Preview(widthDp = 320, heightDp = 450, fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun GameHeaderLongEnglishPreview() = LongHeader("Score", "Personal record")

@Preview(widthDp = 320, heightDp = 450, fontScale = 1.5f, locale = "ru")
@ScreenshotPreview
@Composable
private fun GameHeaderLongRussianPreview() = LongHeader("Счёт", "Личный рекорд")

@Preview(widthDp = 320, heightDp = 450, fontScale = 1.5f, locale = "kk")
@ScreenshotPreview
@Composable
private fun GameHeaderLongKazakhPreview() = LongHeader("Ұпай", "Жеке рекорд")

@Preview(widthDp = 320, heightDp = 650, fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun GameOverlayDarkCompactPreview() {
    OquTurboTheme(darkTheme = true) {
        GameStateOverlay("Ready to train", "Remember the sequence and enter the answer.", Icons.Default.PlayArrow, {})
    }
}
