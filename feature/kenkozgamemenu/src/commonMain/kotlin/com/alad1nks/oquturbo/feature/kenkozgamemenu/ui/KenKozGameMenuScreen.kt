package com.alad1nks.oquturbo.feature.kenkozgamemenu.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KenKozGameMenuScreen(
    onModeClick: (KenKozGameMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(text = stringResource(AppResource.String.kenkoz_title))
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        )

        KenKozGameMenuContentColumn(onModeClick = onModeClick)
    }
}

@Preview
@Composable
private fun KenKozGameMenuScreenPreview() {
    Scaffold {
        KenKozGameMenuScreen(onModeClick = {})
    }
}
