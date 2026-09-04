package com.example.glitchkeyboard

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ToggleButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class GlitchInputMethodService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var glitchEngine: GlitchEngine
    private lateinit var languageJumper: LanguageJumper
    private var isShifted = false

    private val rows = listOf(
        "1234567890",
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    override fun onCreate() {
        super.onCreate()
        glitchEngine = GlitchEngine(this)
        languageJumper = LanguageJumper(serviceScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(4, 8, 4, 8)
        }
        rows.forEach { root.addView(buildRow(it)) }
        root.addView(buildBottomRow())
        return root
    }

    private fun buildRow(chars: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        chars.forEach { c -> row.addView(makeKey(c.toString()) { onCharKey(c) }) }
        return row
    }

    private fun buildBottomRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val shiftBtn = ToggleButton(this).apply {
            textOn = "⇧"; textOff = "⇧"; text = "⇧"
            setOnClickListener { isShifted = !isShifted }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(shiftBtn)

        val spaceBtn = makeKey("␣") { onSpaceKey() }
        (spaceBtn.layoutParams as LinearLayout.LayoutParams).weight = 4f
        row.addView(spaceBtn)

        row.addView(makeKey("⌫") { onDeleteKey() })
        row.addView(makeKey("⏎") { onEnterKey() })
        return row
    }

    private fun makeKey(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { action() }
        }

    private fun onCharKey(c: Char) {
        val ic = currentInputConnection ?: return
        val out = if (isShifted) c.uppercaseChar().toString() else c.toString()
        ic.commitText(out, 1)
        glitchEngine.maybeInjectGlitch(ic, GlitchEngine.Trigger.CHAR)
    }

    private fun onSpaceKey() {
        val ic = currentInputConnection ?: return
        ic.commitText(" ", 1)
        glitchEngine.maybeInjectGlitch(ic, GlitchEngine.Trigger.WORD_BOUNDARY)

        val prefs = Prefs.get(this)
        if (prefs.getBoolean(Prefs.KEY_LANG_JUMP, false)) {
            val freq = Prefs.frequency(this) / 100f
            languageJumper.maybeJump(ic, probability = 0.15f * freq)
        }
    }

    private fun onDeleteKey() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun onEnterKey() {
        currentInputConnection?.commitText("\n", 1)
    }
}
