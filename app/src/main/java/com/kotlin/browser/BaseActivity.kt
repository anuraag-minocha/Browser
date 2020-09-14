package com.kotlin.browser

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.anthonycr.grant.PermissionsManager
import com.kotlin.browser.application.Protocol
import com.kotlin.browser.application.ShortcutAddedReceiver
import com.kotlin.browser.application.Type
import com.kotlin.browser.application.supportN


abstract class BaseActivity : AppCompatActivity() {

    open lateinit var TAG: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TAG = "${this::class.java.simpleName}-->"
    }


    protected val EXTRA_TARGET_URL = "EXTRA_TARGET_URL"


    protected val EXTRA_PRIVATE = "EXTRA_PRIVATE"


    protected val EXTRA_TASK_ID = "EXTRA_TASK_ID"


    fun enableBackPress() = supportActionBar?.setDisplayHomeAsUpEnabled(true)


    fun createLauncherShortcut(url: String, name: String, bitmap: Bitmap) {

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {


            val shortcutInfoIntent = Intent(this, RouteActivity::class.java).apply {
                action = Intent.ACTION_VIEW //action必须设置，不然报错
                data = Uri.parse(Protocol.SHORTCUT + url)
            }


            val info = ShortcutInfoCompat.Builder(this, url)
                    .setIcon(IconCompat.createWithBitmap(bitmap))
                    .setShortLabel(name)
                    .setIntent(shortcutInfoIntent)
                    .build()


            val broadcastIntent = Intent(this, ShortcutAddedReceiver::class.java).apply {
                action = Intent.ACTION_CREATE_SHORTCUT
                putExtra(Type.EXTRA_SHORTCUT_BROADCAST, name)
            }


            val shortcutCallbackIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            ShortcutManagerCompat.requestPinShortcut(this, info, shortcutCallbackIntent.intentSender)
        }
    }


    fun openUrl(url: String, private: Boolean = false, taskId: Int = 0) {
        startActivity(new(url, private, taskId))
    }


    fun openUrlOverviewScreen(url: String, private: Boolean = false, taskId: Int = 0) {
        startActivity(new(url, private, taskId), ActivityOptions.makeTaskLaunchBehind().toBundle())
    }

    /**
     * Returns an new [Intent] to start [PageActivity] as a new document in
     * overview menu.
     *
     * To start a new document task [Intent.FLAG_ACTIVITY_NEW_DOCUMENT] must be used. The
     * system will search through existing tasks for one whose Intent matches the Intent component
     * name and the Intent data. If it finds one then that task will be brought to the front and the
     * new Intent will be passed to onNewIntent().
     *
     * Activities launched with the NEW_DOCUMENT flag must be created with launchMode="standard".
     */
    private fun new(url: String, private: Boolean = false, taskId: Int = 0): Intent {
        return Intent(this, PageActivity::class.java).apply {
            putExtra(EXTRA_TARGET_URL, url)
            putExtra(EXTRA_PRIVATE, private)
            putExtra(EXTRA_TASK_ID, taskId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

            /*
            When {@link Intent#FLAG_ACTIVITY_NEW_DOCUMENT} is used with {@link Intent#FLAG_ACTIVITY_MULTIPLE_TASK}
            the system will always create a new task with the target activity as the root. This allows the same
            document to be opened in more than one task.
            */
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            supportN {
                //addFlags(CodedOutputStream.DEFAULT_BUFFER_SIZE); //4096
                addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
        }
    }

    private var mAlertDialog: AlertDialog.Builder? = null
    fun dialogBuilder(): AlertDialog.Builder {
        if (mAlertDialog == null) {
            mAlertDialog = AlertDialog.Builder(this)
        }

        return mAlertDialog!!
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults)
    }
}