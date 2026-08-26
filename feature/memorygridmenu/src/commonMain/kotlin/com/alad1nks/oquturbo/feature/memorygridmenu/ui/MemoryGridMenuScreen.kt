package com.alad1nks.oquturbo.feature.memorygridmenu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppTopBar
import com.alad1nks.oquturbo.core.ui.component.GameMenuItem
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoryGridMenuScreen(
    onModeClick: (MemoryGridGameMode) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Box(modifier.fillMaxSize().appBackground().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        Column(Modifier.align(Alignment.TopCenter).fillMaxSize()) {
            AppTopBar(
                title = stringResource(AppResource.String.memory_grid_title),
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior,
            )
            LazyColumn(
                modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 760.dp).fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    PageHeader(
                        title = stringResource(AppResource.String.memory_grid_menu_title),
                        subtitle = stringResource(AppResource.String.memory_grid_menu_subtitle),
                    )
                }
                item {
                    GameMenuItem(
                        imageVector = Icons.Outlined.GridView,
                        title = stringResource(AppResource.String.memory_grid_route_title),
                        subtitle = stringResource(AppResource.String.memory_grid_route_subtitle),
                        onClick = { onModeClick(MemoryGridGameMode.Route) },
                    )
                }
                item {
                    GameMenuItem(
                        imageVector = Icons.Outlined.GridView,
                        title = stringResource(AppResource.String.memory_grid_reverse_title),
                        subtitle = stringResource(AppResource.String.memory_grid_reverse_subtitle),
                        onClick = { onModeClick(MemoryGridGameMode.Reverse) },
                    )
                }
                item {
                    GameMenuItem(
                        imageVector = Icons.Outlined.GridView,
                        title = stringResource(AppResource.String.memory_grid_flash_title),
                        subtitle = stringResource(AppResource.String.memory_grid_flash_subtitle),
                        onClick = { onModeClick(MemoryGridGameMode.Flash) },
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun MemoryGridMenuPreview() {
    OquTurboTheme { MemoryGridMenuScreen({}, {}) }
}
