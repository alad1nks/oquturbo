package com.alad1nks.oquturbo.feature.rotationmatch.ui

import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchAnswer
import com.alad1nks.oquturbo.resources.AppResource
import kotlin.test.Test
import kotlin.test.assertEquals

class RotationMatchAnswerResourceTest {
    @Test
    fun resultExplanationMapsEveryCorrectAnswerDirectly() {
        assertEquals(
            AppResource.String.rotation_match_result_match_explanation,
            RotationMatchAnswer.Match.resultExplanationResource(),
        )
        assertEquals(
            AppResource.String.rotation_match_result_different_explanation,
            RotationMatchAnswer.Different.resultExplanationResource(),
        )
    }
}
