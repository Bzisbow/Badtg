package com.example.glitchkeyboard

import android.view.inputmethod.InputConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

/**
 * Переводит последнее набранное слово на случайный язык через бесплатный
 * MyMemory Translation API (без ключа, но с ограничением на число запросов
 * в сутки и неидеальным качеством перевода — это просто "эффект", а не
 * production-переводчик).
 *
 * ВАЖНО: перевод приходит асинхронно. Если пользователь продолжит печатать
 * до ответа сервера, замена может встать не в то место — это ожидаемый
 * компромисс для хобби-проекта.
 */
class LanguageJumper(private val scope: CoroutineScope) {

    private val targetLangs = listOf("en", "zh", "ja", "ko", "fr", "de", "es", "ar", "hi", "tr")
    private val sourceLang = "ru"

    fun maybeJump(ic: InputConnection, probability: Float) {
        if (Random.nextFloat() > probability) return

        val before = ic.getTextBeforeCursor(30, 0)?.toString() ?: return
        val lastWord = before.trim().split(Regex("\\s+")).lastOrNull()?.takeIf { it.length > 2 }
            ?: return
        val target = targetLangs.random()

        scope.launch {
            val translated = translate(lastWord, sourceLang, target)
            if (!translated.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    ic.deleteSurroundingText(lastWord.length, 0)
                    ic.commitText("$translated ", 1)
                }
            }
        }
    }

    private suspend fun translate(text: String, from: String, to: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val q = URLEncoder.encode(text, "UTF-8")
                val url = URL("https://api.mymemory.translated.net/get?q=$q&langpair=$from|$to")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                Regex("\"translatedText\":\"(.*?)\"").find(body)?.groupValues?.get(1)
            } catch (e: Exception) {
                null
            }
        }
}
