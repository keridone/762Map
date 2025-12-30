package com.example.a762map.ui.profile

import android.content.Context

class SessionManager(context: Context) {
    private val sp = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = sp.getLong("uid", -1L) > 0
    fun getUserId(): Long = sp.getLong("uid", -1L)

    fun saveLogin(userId: Long) {
        sp.edit().putLong("uid", userId).apply()
    }

    fun logout() {
        sp.edit().remove("uid").apply()
    }
}
