package com.readplus.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

data class AppSettings(
    val themeStyle: ThemeStyle = ThemeStyle.MATERIAL
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_THEME_STYLE = stringPreferencesKey("theme_style")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeStyle = prefs[KEY_THEME_STYLE]
                ?.let { runCatching { ThemeStyle.valueOf(it) }.getOrNull() }
                ?: ThemeStyle.MATERIAL
        )
    }

    suspend fun setThemeStyle(style: ThemeStyle) {
        context.dataStore.edit { it[KEY_THEME_STYLE] = style.name }
    }
}