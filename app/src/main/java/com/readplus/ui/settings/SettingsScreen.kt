package com.readplus.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readplus.data.preferences.ThemeMode
import com.readplus.data.preferences.ThemeStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.export(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.import(it) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 进度区
            AnimatedVisibility(visible = state.working) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = buildProgressText(state),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.progressTotal > 0) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // ---------- 外观 ----------
            SectionHeader("外观")

            ListItem(
                headlineContent = { Text("主题风格") },
                supportingContent = {
                    Text(
                        when (state.themeStyle) {
                            ThemeStyle.MATERIAL -> "Material 3 · Tonal Spot · Expressive"
                            ThemeStyle.MIUIX -> "Miuix（HyperOS 风格）"
                        }
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Palette, contentDescription = null)
                }
            )

            ThemeRadioGroup(
                selected = state.themeStyle,
                onSelect = viewModel::setThemeStyle
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text("明暗模式") },
                supportingContent = {
                    Text(
                        when (state.themeMode) {
                            ThemeMode.SYSTEM -> "跟随系统"
                            ThemeMode.LIGHT -> "浅色"
                            ThemeMode.DARK -> "深色"
                        }
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Brightness6, contentDescription = null)
                }
            )

            ThemeModeRadioGroup(
                selected = state.themeMode,
                onSelect = viewModel::setThemeMode
            )

            HorizontalDivider()

            // ---------- 数据 ----------
            SectionHeader("数据")

            ListItem(
                headlineContent = { Text("完整导出（含媒体文件）") },
                supportingContent = {
                    Text("将漫画、视频、分类、封面和媒体文件全部打包为 ZIP，体积可能较大")
                },
                leadingContent = {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                },
                modifier = Modifier.clickable(enabled = !state.working) {
                    val name = "readplus_backup_${System.currentTimeMillis()}.zip"
                    exportLauncher.launch(name)
                }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("完整导入") },
                supportingContent = {
                    Text("从 ZIP 备份恢复全部数据（含媒体文件），将覆盖现有全部数据")
                },
                leadingContent = {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                },
                modifier = Modifier.clickable(enabled = !state.working) {
                    importLauncher.launch(arrayOf("application/zip", "*/*"))
                }
            )

            Spacer(Modifier.height(24.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "备份包含：漫画 ZIP、视频文件、封面缩略图、分类、阅读进度。\n文件较大，请确保存储空间充足。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ThemeRadioGroup(
    selected: ThemeStyle,
    onSelect: (ThemeStyle) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        ThemeOption(
            label = "Material 3",
            description = "Tonal Spot 调色板 + 2025 Expressive 色彩规范",
            selected = selected == ThemeStyle.MATERIAL,
            onClick = { onSelect(ThemeStyle.MATERIAL) }
        )
        ThemeOption(
            label = "Miuix",
            description = "HyperOS / MIUI 风格，大圆角扁平卡片",
            selected = selected == ThemeStyle.MIUIX,
            onClick = { onSelect(ThemeStyle.MIUIX) }
        )
    }
}

@Composable
private fun ThemeModeRadioGroup(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        ThemeOption(
            label = "跟随系统",
            description = "系统深色时使用深色，否则使用浅色",
            selected = selected == ThemeMode.SYSTEM,
            onClick = { onSelect(ThemeMode.SYSTEM) }
        )
        ThemeOption(
            label = "浅色",
            description = "始终使用浅色主题",
            selected = selected == ThemeMode.LIGHT,
            onClick = { onSelect(ThemeMode.LIGHT) }
        )
        ThemeOption(
            label = "深色",
            description = "始终使用深色主题",
            selected = selected == ThemeMode.DARK,
            onClick = { onSelect(ThemeMode.DARK) }
        )
    }
}

@Composable
private fun ThemeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.width(4.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildProgressText(state: SettingsUiState): String {
    return if (state.progressTotal > 0) {
        "${state.progressPhase}  ${state.progressCurrent}/${state.progressTotal}"
    } else {
        state.progressPhase.ifBlank { "处理中..." }
    }
}