package com.tona.sandwave.io

import android.content.Context
import android.content.SharedPreferences

object GameIO {
    private const val PREF_NAME = "sandwave_prefs"
    private const val KEY_HIGH_SCORE = "high_score"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getHighScore(context: Context): Long {
        return getPrefs(context).getLong(KEY_HIGH_SCORE, 0)
    }

    fun saveHighScore(context: Context, score: Long) {
        val currentHigh = getHighScore(context)
        if (score >= currentHigh) {
            getPrefs(context).edit().putLong(KEY_HIGH_SCORE, score).apply()
        }
    }
}
