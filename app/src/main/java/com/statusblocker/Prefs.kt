package com.statusblocker

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun enabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    fun cover(c: Context) = sp(c).getBoolean("cover", true)
    fun setCover(c: Context, v: Boolean) = sp(c).edit().putBoolean("cover", v).apply()

    fun count(c: Context) = sp(c).getInt("count", 0)
    fun incrementCount(c: Context) = sp(c).edit().putInt("count", count(c) + 1).apply()
}
