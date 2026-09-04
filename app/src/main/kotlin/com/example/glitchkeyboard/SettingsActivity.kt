package com.example.glitchkeyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.core.content.edit

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = Prefs.get(this)

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
        }
        root.addView(container)
        setContentView(root)

        fun title(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 20f
            setPadding(0, 32, 0, 12)
        }

        fun switchRow(label: String, key: String, default: Boolean): Switch {
            val row = Switch(this).apply {
                text = label
                setTextColor(Color.WHITE)
                isChecked = prefs.getBoolean(key, default)
                setPadding(0, 12, 0, 12)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit { putBoolean(key, checked) }
                }
            }
            container.addView(row)
            return row
        }

        container.addView(title("Glitch Keyboard — настройки"))

        val enableBtn = Button(this).apply {
            text = "Включить клавиатуру в системе"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }
        container.addView(enableBtn)

        container.addView(title("Типы артефактов"))
        switchRow("Мастер-переключатель (вкл/выкл всё)", Prefs.KEY_MASTER, true)
        switchRow("CJK-вспышки (иероглифы)", Prefs.KEY_CJK, true)
        switchRow("Повтор последнего слова", Prefs.KEY_REPEAT, true)
        switchRow("Мусорные токены ([UNK], <EOS> и т.п.)", Prefs.KEY_RANDOM_TOKEN, true)
        switchRow("Zalgo-диакритика", Prefs.KEY_ZALGO, true)
        switchRow("Прыжки между языками (перевод фрагментов, нужен интернет)", Prefs.KEY_LANG_JUMP, false)

        container.addView(title("Частота глюков"))
        val freqLabel = TextView(this).apply {
            setTextColor(Color.LTGRAY)
        }
        fun updateFreqLabel(v: Int) {
            freqLabel.text = "Текущий уровень: $v%"
        }
        updateFreqLabel(prefs.getInt(Prefs.KEY_FREQUENCY, 20))
        container.addView(freqLabel)

        val seekBar = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt(Prefs.KEY_FREQUENCY, 20)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    updateFreqLabel(value)
                    prefs.edit { putInt(Prefs.KEY_FREQUENCY, value) }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        container.addView(seekBar)

        val note = TextView(this).apply {
            text = "После включения клавиатуры выбери её как способ ввода: " +
                    "долгое нажатие на поле ввода → выбрать клавиатуру → Glitch Keyboard."
            setTextColor(Color.GRAY)
            setPadding(0, 32, 0, 0)
        }
        container.addView(note)
    }
}
