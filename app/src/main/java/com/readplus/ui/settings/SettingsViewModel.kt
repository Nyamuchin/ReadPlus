package com.readplus.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplus.data.backup.BackupManager
import com.readplus.data.backup.ProgressCallback
import com.readplus.data.preferences.PreferencesManager
import com.readplus.data.preferences.ThemeMode
import com.readplus.data.preferences.ThemeStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val working: Boolean = false,
    val message: String? = null,
    val progressPhase: String = "",
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val themeStyle: ThemeStyle = ThemeStyle.MATERIAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val progress: Float
        get() = if (progressTotal > 0) progressCurrent.toFloat() / progressTotal else 0f
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val progressCallback = ProgressCallback { phase, current, total ->
        _state.update {
            it.copy(
                progressPhase = phase,
                progressCurrent = current,
                progressTotal = total
            )
        }
    }

    init {
        // 持续同步偏好到 UI 状态
        viewModelScope.launch {
            preferencesManager.settings.collect { prefs ->
                _state.update {
                    it.copy(
                        themeStyle = prefs.themeStyle,
                        themeMode = prefs.themeMode
                    )
                }
            }
        }
    }

    fun setThemeStyle(style: ThemeStyle) = viewModelScope.launch {
        preferencesManager.setThemeStyle(style)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        preferencesManager.setThemeMode(mode)
    }

    fun export(uri: Uri) = viewModelScope.launch {
        _state.update {
            it.copy(working = true, message = null, progressPhase = "准备中...")
        }
        backupManager.export(uri, progressCallback)
            .onSuccess {
                _state.update {
                    it.copy(working = false, message = "导出成功")
                }
            }
            .onFailure {
                _state.update {
                    it.copy(
                        working = false,
                        message = "导出失败：${it.message ?: "未知错误"}"
                    )
                }
            }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        _state.update {
            it.copy(working = true, message = null, progressPhase = "准备中...")
        }
        backupManager.import(uri, progressCallback)
            .onSuccess {
                _state.update {
                    it.copy(working = false, message = "导入成功")
                }
            }
            .onFailure {
                _state.update {
                    it.copy(
                        working = false,
                        message = "导入失败：${it.message ?: "未知错误"}"
                    )
                }
            }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}