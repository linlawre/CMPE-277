package com.example.personal_secretary

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

object ThemeList {

    enum class Theme(@DrawableRes val backgroundRes: Int) {
        DEFAULT(0),
        BOBA(R.drawable.boba_background),
        CAPYBARA(R.drawable.capybara_background),
        DRAGON(R.drawable.dragon_background),
        DUCK(R.drawable.duck_background);

        companion object {
            fun fromName(name: String): Theme {
                return values().find { it.name == name } ?: DEFAULT
            }
        }
    }

    var currentTheme by mutableStateOf(Theme.DEFAULT)

    fun getBackground(): Int = currentTheme.backgroundRes

    fun loadTheme(context: Context, userId: String, onComplete: (() -> Unit)? = null) {
        val repository = ThemeRepository(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val themeName = repository.getUserTheme(userId) ?: "DEFAULT"
            currentTheme = Theme.fromName(themeName)
            onComplete?.let { it() }
        }
    }

    fun saveTheme(context: Context, userId: String, theme: Theme, onComplete: (() -> Unit)? = null) {
        val repository = ThemeRepository(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.saveUserTheme(userId, theme.name)
            currentTheme = theme
            onComplete?.let { it() }
        }
    }
}
