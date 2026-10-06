package com.statusblocker

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var countText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        statusText = TextView(this).apply { textSize = 18f }

        val openA11y = Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        val openInfo = Button(this).apply {
            text = "Open App info (allow restricted settings)"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        val enabledSwitch = Switch(this).apply {
            text = "Block WhatsApp status"
            textSize = 16f
            isChecked = Prefs.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this@MainActivity, on) }
        }

        val coverSwitch = Switch(this).apply {
            text = "Black out screen while blocking"
            textSize = 16f
            isChecked = Prefs.cover(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Prefs.setCover(this@MainActivity, on) }
        }

        countText = TextView(this)

        val help = TextView(this).apply {
            text = "Setup:\n" +
                "1. Tap Open Accessibility settings and turn on Status Blocker.\n" +
                "2. If it's greyed out as a restricted setting (Android 13+), tap Open App info, " +
                "use the ⋮ menu → Allow restricted settings, then try step 1 again.\n" +
                "3. In Settings → Apps → Status Blocker → Battery, choose Unrestricted.\n\n" +
                "Works with WhatsApp and WhatsApp Business. No Shizuku or ADB needed."
        }

        listOf(statusText, openA11y, openInfo, enabledSwitch, coverSwitch, countText, help).forEach {
            col.addView(it, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = pad / 2 })
        }

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        statusText.text = if (serviceEnabled()) "✅ Service is on"
        else "⚠️ Service is off. Enable Status Blocker in Accessibility settings."
        countText.text = "Blocked ${Prefs.count(this)} times"
    }

    private fun serviceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, StatusBlockService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}
