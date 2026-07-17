package com.alad1nks.oquturbo.feature.stats.di

import com.alad1nks.oquturbo.feature.stats.data.StatsDataSource
import com.alad1nks.oquturbo.feature.stats.demo.DemoStatsDataSource
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.ui.StatsGameDetailViewModel
import com.alad1nks.oquturbo.feature.stats.ui.StatsModeDetailViewModel
import com.alad1nks.oquturbo.feature.stats.ui.StatsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val StatsModule =
    module {
        singleOf(::DemoStatsDataSource).bind<StatsDataSource>()
        viewModel { StatsViewModel(dataSource = get()) }
        viewModel { (game: StatsGame, period: StatsPeriod) ->
            StatsGameDetailViewModel(game = game, period = period, dataSource = get())
        }
        viewModel { (game: StatsGame, mode: StatsMode, period: StatsPeriod) ->
            StatsModeDetailViewModel(
                game = game,
                mode = mode,
                period = period,
                dataSource = get(),
            )
        }
    }
