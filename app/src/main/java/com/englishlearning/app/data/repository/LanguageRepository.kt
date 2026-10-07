package com.englishlearning.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class AppLanguage { PT_BR, EN }

@Singleton
class LanguageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    fun getLanguage(): AppLanguage {
        val saved = prefs.getString(KEY_LANGUAGE, AppLanguage.PT_BR.name)
        return try { AppLanguage.valueOf(saved!!) } catch (e: Exception) { AppLanguage.PT_BR }
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, lang.name).apply()
    }

    companion object {
        const val KEY_LANGUAGE = "app_language"
    }
}
