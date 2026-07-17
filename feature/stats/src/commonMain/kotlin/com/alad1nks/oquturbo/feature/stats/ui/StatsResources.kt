package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.alad1nks.oquturbo.feature.stats.model.ActivityStatus
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsSkill
import com.alad1nks.oquturbo.feature.stats.model.StatsTrend
import com.alad1nks.oquturbo.feature.stats.model.StatsWeekday
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource

internal fun StatsGame.titleResource(): StringResource =
    when (this) {
        StatsGame.NumberSprint -> AppResource.String.remember_number_title
        StatsGame.WideEye -> AppResource.String.kenkoz_title
        StatsGame.DontTap -> AppResource.String.baspa_title
    }

internal fun StatsGame.icon(): ImageVector =
    when (this) {
        StatsGame.NumberSprint -> Icons.Filled.Bolt
        StatsGame.WideEye -> Icons.Filled.Visibility
        StatsGame.DontTap -> Icons.Filled.Block
    }

internal fun StatsMode.titleResource(): StringResource =
    when (this) {
        StatsMode.Classic -> AppResource.String.remember_number_menu_item_classic_title
        StatsMode.Binary -> AppResource.String.remember_number_menu_item_binary_title
        StatsMode.Custom -> AppResource.String.remember_number_menu_item_custom_title
        StatsMode.Characters -> AppResource.String.kenkoz_game_menu_item_characters_title
        StatsMode.Words -> AppResource.String.kenkoz_game_menu_item_words_title
        StatsMode.FindDifference -> AppResource.String.kenkoz_game_menu_item_find_difference_title
        StatsMode.WideLine -> AppResource.String.kenkoz_game_menu_item_wide_line_title
        StatsMode.Categories -> AppResource.String.baspa_game_menu_categories_title
        StatsMode.Letter -> AppResource.String.baspa_game_menu_letter_title
        StatsMode.WordLength -> AppResource.String.baspa_game_menu_word_length_title
        StatsMode.TextColor -> AppResource.String.baspa_game_menu_text_color_title
        StatsMode.TrueFalse -> AppResource.String.baspa_game_menu_true_false_title
        StatsMode.Math -> AppResource.String.baspa_game_menu_math_title
        StatsMode.SpeedReading -> AppResource.String.baspa_game_menu_speed_reading_title
    }

internal fun StatsPeriod.titleResource(): StringResource =
    when (this) {
        StatsPeriod.SevenDays -> AppResource.String.stats_period_7_days
        StatsPeriod.ThirtyDays -> AppResource.String.stats_period_30_days
        StatsPeriod.AllTime -> AppResource.String.stats_period_all_time
    }

internal fun StatsWeekday.titleResource(): StringResource =
    when (this) {
        StatsWeekday.Monday -> AppResource.String.stats_weekday_mon
        StatsWeekday.Tuesday -> AppResource.String.stats_weekday_tue
        StatsWeekday.Wednesday -> AppResource.String.stats_weekday_wed
        StatsWeekday.Thursday -> AppResource.String.stats_weekday_thu
        StatsWeekday.Friday -> AppResource.String.stats_weekday_fri
        StatsWeekday.Saturday -> AppResource.String.stats_weekday_sat
        StatsWeekday.Sunday -> AppResource.String.stats_weekday_sun
    }

internal fun ActivityStatus.titleResource(): StringResource =
    when (this) {
        ActivityStatus.DailyComplete -> AppResource.String.stats_activity_completed
        ActivityStatus.DailyPartial -> AppResource.String.stats_activity_started
        ActivityStatus.GamesOnly -> AppResource.String.stats_activity_games_only
        ActivityStatus.None -> AppResource.String.stats_activity_none
    }

internal fun StatsSkill.titleResource(): StringResource =
    when (this) {
        StatsSkill.Memory -> AppResource.String.stats_skill_memory
        StatsSkill.Attention -> AppResource.String.stats_skill_attention
        StatsSkill.Reaction -> AppResource.String.stats_skill_reaction
        StatsSkill.PeripheralVision -> AppResource.String.stats_skill_peripheral_vision
        StatsSkill.RecognitionSpeed -> AppResource.String.stats_skill_recognition_speed
    }

internal fun StatsTrend.titleResource(): StringResource =
    when (this) {
        StatsTrend.Growing -> AppResource.String.stats_trend_growing
        StatsTrend.Declining -> AppResource.String.stats_trend_declining
        StatsTrend.Stable -> AppResource.String.stats_trend_stable
        StatsTrend.NotEnoughData -> AppResource.String.stats_skill_not_enough
    }
