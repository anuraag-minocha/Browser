package com.kotlin.browser.application

import android.animation.Animator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.MailTo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Patterns
import android.util.SparseArray
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.jetbrains.anko.toast
import com.kotlin.browser.App
import com.kotlin.browser.R
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.net.URISyntaxException
import java.net.URL
import java.util.regex.Pattern


fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.isVisible() = visibility == View.VISIBLE

fun View.isGone() = visibility == View.GONE


fun <T : View> BottomSheetBehavior<T>.isCollapsed() = state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED


fun <T : View> BottomSheetBehavior<T>.collapsed() {
    state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
}


fun <T : View> BottomSheetBehavior<T>.isHidden() = state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN


fun <T : View> BottomSheetBehavior<T>.hidden() {
    state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
}


fun <T : View> BottomSheetBehavior<T>.isExpanded() = state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED


fun <T : View> BottomSheetBehavior<T>.expanded() {
    state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
}


fun View.show(animate: Boolean = true) {
    if (animate) {
        alpha = 0f
        visible()
        animate().apply {
            alpha = 1f
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            interpolator = LinearInterpolator()
            setListener(null)
            start()
        }

        return
    }

    alpha = 1f
    visible()
}

fun View.hide(animate: Boolean = false) {
    if (animate) {
        animate().apply {
            alpha = 0f
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            interpolator = LinearInterpolator()
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {}

                override fun onAnimationCancel(p0: Animator?) {}

                override fun onAnimationStart(p0: Animator?) {}

                override fun onAnimationEnd(p0: Animator?) {
                    gone()
                }

            })
            start()
        }

        return
    }

    gone()
}


fun View.showKeyboard() {
    post {
        requestFocus()
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.showSoftInput(this, 1)
    }
}


fun View.hideKeyboard() {
    post {
        clearFocus()
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(windowToken, 0)
    }
}


inline fun <T : View> T.visibleDo(todo: (T) -> Unit) {
    if (visibility == View.VISIBLE) {
        todo(this)
    }
}


fun <T : View> View.findViewOften(viewId: Int): T {
    val viewHolder: SparseArray<View> = tag as? SparseArray<View> ?: SparseArray()
    tag = viewHolder
    var childView: View? = viewHolder.get(viewId)
    if (null == childView) {
        childView = findViewById(viewId)
        viewHolder.put(viewId, childView)
    }
    return childView as T
}


fun Context.openIntentByDefault(url: String) {
    val intent = url.parseIntent()
    val component = intent.resolveActivity(packageManager)
    if (component == null) {
        toast(R.string.toast_no_handle_application)
        return
    }

    if (component.packageName == packageName) {
        startActivity(intent)
        return
    }

    invokeApp(component.packageName) {
        toast(R.string.toast_no_handle_application)
    }
}

inline fun <reified T : Activity> Context.go() {
    startActivity(Intent(this, T::class.java))
}

fun Context.defaultSharePreferences() = PreferenceManager.getDefaultSharedPreferences(this)


fun Context.versionCode() = packageManager
        .getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS).versionCode


fun Context.versionName() = packageManager
        .getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS).versionName


fun Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(intent, text))
}


fun Context.sendMailTo(mail: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        val parse = MailTo.parse(if (mail.startsWith(Protocol.MAIL_TO)) mail else Protocol.MAIL_TO + mail)
        putExtra(Intent.EXTRA_EMAIL, arrayOf(parse.to))
        putExtra(Intent.EXTRA_TEXT, parse.body)
        putExtra(Intent.EXTRA_SUBJECT, parse.subject)
        putExtra(Intent.EXTRA_CC, parse.cc)
        type = "message/rfc822"
    }

    supportIntent(intent) { startActivity(it) }
}


fun Context.callPhone(phone: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse(if (phone.startsWith(Protocol.TEL)) phone else Protocol.TEL + phone)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    supportIntent(intent) { startActivity(it) }
}


fun Context.copyToClipboard(text: String) {
    // Gets a handle to the clipboard service.
    val mClipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Creates a new text clip to put on the clipboard
    val clip = ClipData.newPlainText(null, text.trim())

    // Set the clipboard's primary clip.
    mClipboard.setPrimaryClip(clip)
    toast(resources.getString(R.string.toast_copy_url, text))
}


fun Context.dip2px(dip: Float) = (resources.displayMetrics.density * dip + 0.5f).toInt()


fun Context.sp2px(sp: Float) = (resources.displayMetrics.scaledDensity * sp + 0.5f).toInt()


inline fun <reified T : Activity> Activity.go4Result(requestCode: Int) {
    startActivityForResult(Intent(this, T::class.java), requestCode)
}


fun Activity.pickImage(requestCode: Int) {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.type = "image/*"
    startActivityForResult(intent, requestCode)
}


fun Activity.permission(vararg permissions: String, f: Func) {
    PermissionsManager.getInstance()
            .requestPermissionsIfNecessaryForResult(this, permissions,
                    object : PermissionsResultAction() {
                        override fun onGranted() {
                            f()
                        }

                        override fun onDenied(permission: String) {
                            toast(R.string.toast_storage_permission_denied)
                        }
                    })
}


fun String.toColorUrl(): SpannableStringBuilder {
    val color: Int
    val endIndex: Int
    when {
        URLUtil.isHttpUrl(this) -> {
            color = ContextCompat.getColor(App.instance, R.color.text_secondary)
            endIndex = 7
        }
        URLUtil.isHttpsUrl(this) -> {
            color = ContextCompat.getColor(App.instance, R.color.green)
            endIndex = 8
        }
        else -> {
            color = ContextCompat.getColor(App.instance, R.color.text_primary)
            endIndex = 0
        }
    }
    val builder = SpannableStringBuilder(this)
    if (endIndex > 0) {
        builder.setSpan(ForegroundColorSpan(color), 0, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return builder
}


fun String.isIntent(): Boolean {
    if (isEmpty()) return false
    return startsWith(Protocol.INTENT) || startsWith(Protocol.INTENT_OLD)
}


fun String.parseIntent(): Intent {
    var intent = Intent(Intent.ACTION_VIEW, Uri.parse(this))
    // Parse intent URI into Intent Object
    var flags = 0
    var isIntentUri = false
    if (startsWith(Protocol.INTENT)) {
        isIntentUri = true
        flags = Intent.URI_INTENT_SCHEME
    } else if (startsWith(Protocol.INTENT_OLD)) {
        isIntentUri = true
    }
    if (isIntentUri) {
        try {
            intent = Intent.parseUri(this, flags)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

    }
    return intent
}


val String.pattern
    get() = Pattern.compile("(?i)((?:http|https|file|ftp)://|(?:data|about|javascript|mailto):|(?:.*:.*@))(.*)")

fun String.isProtocolUrl() = pattern.matcher(this).matches()


fun String.isAppProtocolUrl() = Pattern.compile("[a-zA-z]+://[^\\s]*").matcher(this).matches()


fun String.isEmailTo() = isNotEmpty() && startsWith(Protocol.MAIL_TO)


fun String.isTel() = isNotEmpty() && startsWith(Protocol.TEL)


fun String.isWebUrl() = Patterns.WEB_URL.matcher(this).matches()


fun String.isBase64Url() = isNotEmpty() && startsWith(Protocol.BASE64)


fun String.host(): String {
    return try {
        URL(this).host
    } catch (e: Exception) {
        e.printStackTrace()
        this
    }
}


fun String.parseUrl(): String {
    val trim = trim()
    val matcher = pattern.matcher(trim)
    when {
        matcher.matches() -> {
            val group0 = matcher.group(0)
            println("group0: $group0")
            val group1 = matcher.group(1)
            println("group1: $group1")
            val group2 = matcher.group(2)
            println("group2: $group2")
            return trim.replace(" ", "%20")
        }
        trim.isWebUrl() -> return URLUtil.guessUrl(trim)
        else -> {
            val search = when (SP.searchEngine) {
                Type.SEARCH_GOOGLE -> WebUtil.SEARCH_ENGINE_GOOGLE
                Type.SEARCH_DUCKDUCKGO -> WebUtil.SEARCH_ENGINE_DUCKDUCKGO
                Type.SEARCH_BING -> WebUtil.SEARCH_ENGINE_BING
                Type.SEARCH_GITHUB -> WebUtil.SEARCH_ENGINE_GITHUB
                else -> ""
            }

            return URLUtil.composeSearchUrl(trim, search, "%s")
        }
    }
}


fun String.base64ToBitmap(): Bitmap? {
    if (isBase64Url()) {
        val byteArray = Base64.decode(split(",")[1], Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    return null
}


fun Long.formatSize(): String {
    val kiloByte = this / 1024
    if (kiloByte < 1) {
        return "0K"
    }

    val megaByte = kiloByte / 1024
    if (megaByte < 1) {
        val result1 = BigDecimal(kiloByte.toString())
        return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "K"
    }

    val gigaByte = megaByte / 1024
    if (gigaByte < 1) {
        val result2 = BigDecimal(megaByte.toString())
        return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "M"
    }

    val teraBytes = gigaByte / 1024
    if (teraBytes < 1) {
        val result3 = BigDecimal(gigaByte.toString())
        return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "GB"
    }
    val result4 = BigDecimal(teraBytes)
    return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB"
}


fun String.toMask(start: Int = 3, end: Int = 2, replace: String = "****"): String {
    if (length <= (start + 1)) return this
    return substring(0, start) + replace + substring(length - end, length)
}


fun View.toBitmap(w: Float, h: Float, scroll: Boolean = false): Bitmap {

    if (!isDrawingCacheEnabled) isDrawingCacheEnabled = true

    var left = left
    var top = top

    if (scroll) {
        left = scrollX
        top = scrollY
    }

    return Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.WHITE)

        Canvas(this).apply {
            val status = save()
            translate(-left.toFloat(), -top.toFloat())

            val scale = w / width
            scale(scale, scale, left.toFloat(), top.toFloat())

            draw(this)
            restoreToCount(status)

            val alphaPaint = Paint()
            alphaPaint.color = Color.TRANSPARENT

            drawRect(0f, 0f, 1f, h, alphaPaint)
            drawRect(w - 1f, 0f, w, h, alphaPaint)
            drawRect(0f, 0f, w, 1f, alphaPaint)
            drawRect(0f, h - 1f, w, h, alphaPaint)
            setBitmap(null)
        }
    }
}


fun Bitmap.save(name: String): File {
    val externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    var i = 0
    var file = File(externalStoragePublicDirectory, "$name.png")
    while (file.exists()) {
        file = File(externalStoragePublicDirectory, "$name.${i++}.png")
    }
    val fileOutputStream = FileOutputStream(file)
    compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
    fileOutputStream.flush()
    fileOutputStream.close()
    return file
}


fun Bitmap.scale(w: Float, h: Float): Bitmap {
    val temp = copy(config, true)
    val width = temp.width
    val height = temp.height
    val scaleX = w / width
    val scaleY = h / height
    val matrix = Matrix()
    matrix.postScale(scaleX, scaleY)
    val bitmap = Bitmap.createBitmap(temp, 0, 0, width, height, matrix, false)
    temp.recycle()
    return bitmap
}


fun File.mediaScan(context: Context) {
    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(this)))
}


inline fun supportM(todo: Func) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        todo()
    }
}


inline fun supportN(todo: Func) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        todo()
    }
}


inline fun Context.supportIntent(intent: Intent, todo: Func1<Intent>) {
    if (intent.resolveActivity(packageManager) != null) {
        todo(intent)
    } else {
        toast(R.string.toast_no_handle_application)
    }
}


fun Context.invokeApp(pkg: String, todo: Func) {
    val intent = packageManager.getLaunchIntentForPackage(pkg)
    if (intent != null) {
        startActivity(intent)
    } else {
        todo()
    }
}


fun <T : TextView> T.drawableLeft(resId: Int) {
    ContextCompat.getDrawable(context, resId)?.apply {
        setBounds(0, 0, minimumWidth, minimumHeight)
        setCompoundDrawables(this, null, null, null)
    }
}


fun <T : TextView> T.drawableRight(resId: Int) {
    ContextCompat.getDrawable(context, resId)?.apply {
        setBounds(0, 0, minimumWidth, minimumHeight)
        setCompoundDrawables(null, null, this, null)
    }
}


fun <T : TextView> T.drawableTop(resId: Int) {
    ContextCompat.getDrawable(context, resId)?.apply {
        setBounds(0, 0, minimumWidth, minimumHeight)
        setCompoundDrawables(null, this, null, null)
    }
}


fun <T : TextView> T.drawableBottom(resId: Int) {
    ContextCompat.getDrawable(context, resId)?.apply {
        setBounds(0, 0, minimumWidth, minimumHeight)
        setCompoundDrawables(null, null, null, this)
    }
}


fun <T : TextView> T.addTextWatcher(watcher: (text: String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            watcher(s?.toString() ?: "")
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    })
}