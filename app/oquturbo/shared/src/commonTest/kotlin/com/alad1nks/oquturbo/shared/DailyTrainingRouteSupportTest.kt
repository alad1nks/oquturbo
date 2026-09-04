package com.alad1nks.oquturbo.shared

import com.alad1nks.oquturbo.core.data.model.GameId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyTrainingRouteSupportTest {
    @Test
    fun rotationMatchRouteIsRejected() {
        assertFalse(GameId.RotationMatch.isDailyTrainingGameSupported())
    }

    @Test
    fun existingDailyTrainingRoutesRemainSupported() {
        assertTrue(GameId.NumberSprint.isDailyTrainingGameSupported())
        assertTrue(GameId.WideEye.isDailyTrainingGameSupported())
        assertTrue(GameId.DontTap.isDailyTrainingGameSupported())
    }
}
