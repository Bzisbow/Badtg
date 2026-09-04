package com.example.glitchkeyboard

import android.content.Context
import android.view.inputmethod.InputConnection
import kotlin.random.Random

/**
 * Генерирует "AI-hallucination"-подобные глюки в тексте: внезапные вставки
 * иероглифов, повторы токенов, "мусорные" служебные метки и Zalgo-диакритику.
 * Все типы артефактов и общая частота настраиваются через Prefs (SettingsActivity).
 */
class GlitchEngine(private val context: Context) {

    enum class Trigger { CHAR, WORD_BOUNDARY }

    // Базовые вероятности при частоте (frequency) = 100%
    private val baseCharProbability = 0.05f
    private val baseWordProbability = 0.30f

    private val cjkRange = 0x4E00..0x9FFF

    private val glitchTokens = listOf(
        " СЛОВНЫЙТОКЕН", "�", "▯▯", " [UNK]", " token_leak", " ⟨EOS⟩", " <|glitch|>"
    )

    fun maybeInjectGlitch(ic: InputConnection, trigger: Trigger) {
        val prefs = Prefs.get(context)
        if (!prefs.getBoolean(Prefs.KEY_MASTER, true)) return

        val freq = Prefs.frequency(context) / 100f
        val probability = when (trigger) {
            Trigger.CHAR -> baseCharProbability * freq
            Trigger.WORD_BOUNDARY -> baseWordProbability * freq
        }
        if (Random.nextFloat() > probability) return

        val enabledActions = buildList {
            if (prefs.getBoolean(Prefs.KEY_CJK, true)) add(0)
            if (prefs.getBoolean(Prefs.KEY_REPEAT, true)) add(1)
            if (prefs.getBoolean(Prefs.KEY_RANDOM_TOKEN, true)) add(2)
            if (prefs.getBoolean(Prefs.KEY_ZALGO, true)) add(3)
        }
        if (enabledActions.isEmpty()) return

        when (enabledActions.random()) {
            0 -> injectCjkBurst(ic)
            1 -> injectRepeatedToken(ic)
            2 -> injectRandomToken(ic)
            3 -> injectZalgoBurst(ic)
        }
    }

    private fun injectCjkBurst(ic: InputConnection) {
        val length = Random.nextInt(2, 6)
        val burst = buildString {
            repeat(length) { append(cjkRange.random().toChar()) }
        }
        ic.commitText(burst, 1)
    }

    private fun injectRepeatedToken(ic: InputConnection) {
        val before = ic.getTextBeforeCursor(20, 0)?.toString() ?: return
        val lastWord = before.trim().split(Regex("\\s+")).lastOrNull() ?: return
        if (lastWord.isBlank()) return
        ic.commitText(" $lastWord", 1)
    }

    private fun injectRandomToken(ic: InputConnection) {
        ic.commitText(glitchTokens.random(), 1)
    }

    private fun injectZalgoBurst(ic: InputConnection) {
        val combiningMarks = 0x0300..0x036F
        val marks = buildString {
            repeat(Random.nextInt(3, 8)) { appendCodePoint(combiningMarks.random()) }
        }
        ic.commitText(marks, 1)
    }
}
