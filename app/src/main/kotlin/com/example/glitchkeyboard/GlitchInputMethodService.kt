package com.example.glitchkeyboard

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class GlitchInputMethodService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var glitchEngine: GlitchEngine
    private lateinit var languageJumper: LanguageJumper
    private var isShifted = false
    private var currentLang = "ru"

    private val digitsRow = "1234567890"
    private val enRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val ruRows = listOf("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю")

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
            setBackgroundColor(Color.parseColor("#14151A"))
            setPadding(dp(6), dp(6), dp(6), dp(8))
        }
        root.addView(buildTopBar())
        root.addView(buildRow(digitsRow))
        val letterRows = if (currentLang == "ru") ruRows else enRows
        letterRows.forEach { root.addView(buildRow(it)) }
        root.addView(buildBottomRow())
        return root
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildTopBar(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(10))

            addView(TextView(this@GlitchInputMethodService).apply {
                text = "✨ Glitch Keyboard"
                setTextColor(Color.parseColor("#9BA0FF"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(this@GlitchInputMethodService).apply {
                text = "⚙"
                textSize = 18f
                setTextColor(Color.parseColor("#B7BAC8"))
                setPadding(dp(10), 0, dp(4), 0)
                setOnClickListener { openSettings() }
            })
        }

    private fun openSettings() {
        startActivity(
            Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    private fun buildRow(chars: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            chars.forEach { c -> addView(makeKey(displayChar(c)) { onCharKey(c) }) }
        }

    private fun displayChar(c: Char): String =
        if (isShifted) c.uppercaseChar().toString() else c.toString()

    private fun buildBottomRow(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )

            addView(makeKey("⇧") { isShifted = !isShifted; refreshKeyboard() })
            addView(makeKey("🌐") { toggleLanguage() })

            val label = if (currentLang == "ru") "Glitch · РУ" else "Glitch · EN"
            val spaceBtn = makeKey(label) { onSpaceKey() }.apply { textSize = 12f }
            (spaceBtn.layoutParams as LinearLayout.LayoutParams).weight = 4f
            addView(spaceBtn)

            addView(makeKey(",") { onCharKey(',') })
            addView(makeKey("⌫") { onDeleteKey() })
            addView(makeKey("⏎") { onEnterKey() })
        }

    private fun toggleLanguage() {
        currentLang = if (currentLang == "ru") "en" else "ru"
        refreshKeyboard()
    }

    private fun refreshKeyboard() {
        setInputView(onCreateInputView())
    }

    private fun keyBackground(): Drawable {
        val content = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(Color.parseColor("#23252E"))
        }
        val mask = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(Color.WHITE)
        }
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#5B5FEF")), content, mask)
    }

    private fun makeKey(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.parseColor("#ECEEF7"))
            background = keyBackground()
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
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
