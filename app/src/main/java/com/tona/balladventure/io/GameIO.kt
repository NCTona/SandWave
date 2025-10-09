package com.tona.balladventure.io

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object GameIO {
    private const val PREF_NAME = "balladventure_prefs"
    private const val KEY_HIGH_SCORES = "high_scores"
    private const val MAX_SCORES = 6

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /** Lấy danh sách điểm cao nhất (tối đa 6 phần tử) */
    fun getHighScores(context: Context): List<Long> {
        val json = getPrefs(context).getString(KEY_HIGH_SCORES, null) ?: return emptyList()
        val jsonArray = JSONArray(json)
        val scores = mutableListOf<Long>()
        for (i in 0 until jsonArray.length()) {
            scores.add(jsonArray.getLong(i))
        }
        return scores
    }

    /** Thêm điểm mới và cập nhật danh sách top 6 */
    fun saveHighScore(context: Context, score: Long) {
        val scores = getHighScores(context).toMutableList()
        scores.add(score)
        val sorted = scores.sortedDescending().take(MAX_SCORES)

        val jsonArray = JSONArray()
        for (s in sorted) {
            jsonArray.put(s)
        }

        getPrefs(context).edit()
            .putString(KEY_HIGH_SCORES, jsonArray.toString())
            .apply()
    }

    /** Lấy điểm cao nhất (top 1) */
    fun getHighestScore(context: Context): Long {
        return getHighScores(context).maxOrNull() ?: 0L
    }
}
