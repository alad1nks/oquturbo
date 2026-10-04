package com.alad1nks.oquturbo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alad1nks.oquturbo.shared.reminders.PracticeReminderActivity

class MainActivity : PracticeReminderActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.auto(
                    lightScrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                ),
        )
        super.onCreate(savedInstanceState)

        setContent {
            ReminderApp()
        }
    }
}
