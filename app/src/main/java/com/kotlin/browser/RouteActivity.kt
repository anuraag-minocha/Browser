package com.kotlin.browser

import android.app.ActivityManager
import android.app.ActivityManager.AppTask
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kotlin.browser.application.*

class RouteActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.apply {


            when(action){
                Intent.ACTION_MAIN -> {
                    val appTasks: List<AppTask> = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).appTasks
                    if (appTasks.size > 1) {
                        appTasks[1].moveToFront()
                        finish()
                        return
                    }
                }


                Intent.ACTION_WEB_SEARCH -> {
                    openUrl(getStringExtra(SearchManager.QUERY)!!)
                    finish()
                    return
                }
            }


            dataString?.apply {


                if (isEmpty()) return

                if (this == Protocol.PRIVATE_TAB){
                    openUrl("", true)
                    finish()
                    return
                }


                if (startsWith(Protocol.SHORTCUT)){
                    openUrl(substring(Protocol.SHORTCUT.length, length))
                    finish()
                    return
                }


                if (isWebUrl()){
                    supportM { overridePendingTransition(0, 0) }
                    openUrl(this)
                    finish()
                    return
                }


                openUrl(this)
                finish()
                return
            }

            L.d(TAG, "intent data string is null or empty")
        }


        L.d(TAG, "intent is null")
        go<PageActivity>()
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        L.d(TAG, "onNewIntent")
    }
}