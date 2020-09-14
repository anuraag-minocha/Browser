package com.kotlin.browser

import android.app.Application
import android.os.Message
import com.kotlin.browser.application.AdBlock
import kotlin.properties.Delegates

class App : Application() {

    companion object {
        var instance: App by Delegates.notNull()
        var MESSAGE: Message? = null
            get() {
                val msg = field
                field = null
                return msg
            }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        AdBlock.init(this)
    }
}