package com.sea.quickstart.helper

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences


object SpHelper {

    private val SP_NAME = "quickstart"

    fun put(context: Context, key: String, value: String) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(SP_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.commit()
    }

    fun get(context: Context, key: String): String {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(SP_NAME, MODE_PRIVATE)
        return sharedPreferences.getString(key, "").toString()
    }
}