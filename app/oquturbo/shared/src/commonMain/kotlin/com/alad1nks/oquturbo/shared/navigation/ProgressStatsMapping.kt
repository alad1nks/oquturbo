package com.alad1nks.oquturbo.shared.navigation

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode

internal fun GameId.toProgressStatsGame(): StatsGame =
    when (this) {
        GameId.NumberSprint -> StatsGame.NumberSprint
        GameId.WideEye -> StatsGame.WideEye
        GameId.DontTap -> StatsGame.DontTap
        GameId.MemoryGrid -> StatsGame.MemoryGrid
        GameId.WordFlow -> StatsGame.WordFlow
        GameId.DualFocus -> StatsGame.DualFocus
        GameId.RotationMatch -> StatsGame.RotationMatch
        GameId.NumberTrail -> StatsGame.NumberTrail
        GameId.SymbolCount -> StatsGame.SymbolCount
        GameId.RuleSwitch -> StatsGame.RuleSwitch
    }

internal fun GameModeId.toProgressStatsMode(): StatsMode =
    when (this) {
        GameModeId.NumberSprintClassic -> StatsMode.Classic
        GameModeId.NumberSprintBinary -> StatsMode.Binary
        GameModeId.NumberSprintCustom -> StatsMode.Custom
        GameModeId.WideEyeCharacters -> StatsMode.Characters
        GameModeId.WideEyeWords -> StatsMode.Words
        GameModeId.WideEyeFindDifference -> StatsMode.FindDifference
        GameModeId.WideEyeWideLine -> StatsMode.WideLine
        GameModeId.DontTapCategories -> StatsMode.Categories
        GameModeId.DontTapLetter -> StatsMode.Letter
        GameModeId.DontTapWordLength -> StatsMode.WordLength
        GameModeId.DontTapTextColor -> StatsMode.TextColor
        GameModeId.DontTapTrueFalse -> StatsMode.TrueFalse
        GameModeId.DontTapMath -> StatsMode.Math
        GameModeId.DontTapSpeedReading -> StatsMode.SpeedReading
        GameModeId.MemoryGridRoute -> StatsMode.Route
        GameModeId.MemoryGridReverse -> StatsMode.Reverse
        GameModeId.MemoryGridFlash -> StatsMode.Flash
        GameModeId.WordFlowContext -> StatsMode.Context
        GameModeId.DualFocusMatch -> StatsMode.Match
        GameModeId.RotationMatchRotation -> StatsMode.Rotation
        GameModeId.NumberTrailAscending -> StatsMode.Ascending
        GameModeId.SymbolCountCount -> StatsMode.Count
        GameModeId.RuleSwitchSwitch -> StatsMode.Switch
    }
