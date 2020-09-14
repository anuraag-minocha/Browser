package com.kotlin.browser.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.jetbrains.anko.toast
import com.kotlin.browser.R

class ShortcutAddedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val name = intent?.getStringExtra(Type.EXTRA_SHORTCUT_BROADCAST) ?: ""
            it.toast(it.getString(R.string.toast_add_to_launcher_successful, name))
        }
    }
}