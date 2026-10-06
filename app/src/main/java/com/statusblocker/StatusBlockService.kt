package com.statusblocker

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView

/**
 * Detects WhatsApp status two ways:
 *  1. Viewer: the status player is its own Activity (StatusPlaybackActivity). Its launch arrives as a
 *     TYPE_WINDOW_STATE_CHANGED event with the class name, so it's caught instantly with no logcat.
 *     Covers the ringed avatar, the Updates tab, contact info, and any other entry path.
 *  2. Updates tab (tap OR swipe): finds the updates_list RecyclerView and checks isVisibleToUser plus
 *     its on-screen (clipped) width. The retained off-screen fragment is still in the tree, but it is
 *     not visible, so it no longer causes false fires on the Chats tab.
 */
class StatusBlockService : AccessibilityService() {

    companion object {
        val WA_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

        // Resource IDs seen in uiautomator dumps. If WhatsApp renames them, change these.
        private const val ID_UPDATES_LIST = "updates_list"
        private const val ID_PLAYBACK_PROGRESS = "playback_progress"
        private const val VIEWER_CLASS_HINT = "StatusPlayback"

        private const val SCAN_DELAY_MS = 120L       // coalesce bursts of content events
        private const val DEBOUNCE_MS = 900L         // one Back per this window
        private const val COVER_MS = 700L            // how long the black cover stays up
        private const val MIN_VISIBLE_FRACTION = 0.5f // Updates page must fill half the width
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastBlockAt = 0L
    private var scanPending = false
    private var cover: View? = null
    private val rect = Rect()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !Prefs.enabled(this)) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in WA_PACKAGES) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.className?.toString()?.contains(VIEWER_CLASS_HINT) == true
        ) {
            block()
            return
        }
        scheduleScan()
    }

    override fun onInterrupt() {}

    private fun scheduleScan() {
        if (scanPending) return
        scanPending = true
        handler.postDelayed({
            scanPending = false
            scan()
        }, SCAN_DELAY_MS)
    }

    private fun scan() {
        if (!Prefs.enabled(this)) return
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return
        if (pkg !in WA_PACKAGES) return
        if (viewerShowing(root, pkg) || updatesTabShowing(root, pkg)) block()
    }

    private fun viewerShowing(root: AccessibilityNodeInfo, pkg: String): Boolean =
        root.findAccessibilityNodeInfosByViewId("$pkg:id/$ID_PLAYBACK_PROGRESS")
            .any { it.isVisibleToUser }

    private fun updatesTabShowing(root: AccessibilityNodeInfo, pkg: String): Boolean {
        val screenW = resources.displayMetrics.widthPixels
        return root.findAccessibilityNodeInfosByViewId("$pkg:id/$ID_UPDATES_LIST").any { node ->
            if (!node.isVisibleToUser) return@any false
            node.getBoundsInScreen(rect)
            rect.width() >= screenW * MIN_VISIBLE_FRACTION
        }
    }

    private fun block() {
        val now = SystemClock.uptimeMillis()
        if (now - lastBlockAt < DEBOUNCE_MS) return
        lastBlockAt = now

        if (Prefs.cover(this)) showCover()
        performGlobalAction(GLOBAL_ACTION_BACK)
        Prefs.incrementCount(this)

        // Re-check after the debounce window in case Back didn't fully leave status.
        handler.postDelayed({ scan() }, DEBOUNCE_MS + 50)
    }

    private fun showCover() {
        if (cover != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val v = TextView(this).apply {
            text = "Status blocked"
            setTextColor(Color.GRAY)
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        try {
            wm.addView(v, lp)
            cover = v
            handler.postDelayed({ removeCover() }, COVER_MS)
        } catch (_: Exception) {
        }
    }

    private fun removeCover() {
        val v = cover ?: return
        cover = null
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        removeCover()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
