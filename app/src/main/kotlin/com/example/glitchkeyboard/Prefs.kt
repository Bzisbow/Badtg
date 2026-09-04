package com.example.glitchkeyboard

import android.content.Context
import android.content.SharedPreferences

/** Централизованный доступ к настройкам клавиатуры */
object Prefs {
    private const val NAME = "glitch_prefs"

    const val KEY_MASTER = "master_enabled"
    const val KEY_CJK = "cjk_enabled"
    const val KEY_REPEAT = "repeat_enabled"
    const val KEY_RANDOM_TOKEN = "random_token_enabled"
    const val KEY_ZALGO = "zalgo_enabled"
    const val KEY_LANG_JUMP = "lang_jump_enabled"
    const val KEY_FREQUENCY = "frequency" // 0..100

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun frequency(context: Context): Int =
        get(context).getInt(KEY_FREQUENCY, 20)
}
