package com.readplus.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.readplus.domain.model.ReadingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "readplus_settings")

enum class ThemeStyle {
    /** Material 3，Tonal Spot 调色板 + 2025 Expressive 色彩规范 */
    MATERIAL,
    /** Miuix（HyperOS/MIUI 风格） */
    MIUIX
}

enum class ThemeMode {
    /** 跟随系统深色/浅色 */
    SYSTEM,
    /** 始终浅色 */
    LIGHT,
    /** 始终深色 */
    DARK
}

data class AppSettings(
    val themeStyle: ThemeStyle = ThemeStyle.MATERIAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** 漫画阅读模式，记忆用户上一次选择 */
    val readingMode: ReadingMode = ReadingMode.LEFT_TO_RIGHT
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_THEME_STYLE = stringPreferencesKey("theme_style")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_READING_MODE = stringPreferencesKey("reading_mode")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeStyle = prefs[KEY_THEME_STYLE]
                ?.let { runCatching { ThemeStyle.valueOf(it) }.getOrNull() }
                ?: ThemeStyle.MATERIAL,
            themeMode = prefs[KEY_THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            readingMode = prefs[KEY_READING_MODE]
                ?.let { runCatching { ReadingMode.valueOf(it) }.getOrNull() }
                ?: ReadingMode.LEFT_TO_RIGHT
        )
    }

    suspend fun setThemeStyle(style: ThemeStyle) {
        context.dataStore.edit { it[KEY_THEME_STYLE] = style.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setReadingMode(mode: ReadingMode) {
        context.dataStore.edit { it[KEY_READING_MODE] = mode.name }
    }
}