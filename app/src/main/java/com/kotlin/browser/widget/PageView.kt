package com.kotlin.browser.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.View
import android.webkit.*
import android.webkit.WebChromeClient.FileChooserParams
import androidx.annotation.RequiresApi
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import com.kotlin.browser.App
import com.kotlin.browser.R
import com.kotlin.browser.application.*
import java.io.ByteArrayInputStream
import java.net.URL
import java.util.*


class PageView : WebView, PageViewClient.Delegate, PageChromeClient.Delegate,
        SharedPreferences.OnSharedPreferenceChangeListener, DownloadListener {

    private val TAG = "PageView-->"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    interface Delegate {
        fun onReceivedWebThemeColor(str: String)

        fun onProgressChanged(progress: Int)

        fun onFormResubmission(dontResend: Message, resend: Message)

        fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)

        fun onReceivedClientCertRequest(request: ClientCertRequest)

        fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String)

        fun onPermissionRequest(request: PermissionRequest)

        fun onReceivedSslError(handler: SslErrorHandler, error: SslError)

        fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback)

        fun onReceivedTitle(url: String, title: String)

        fun onReceivedIcon(url: String, title: String, icon: Bitmap?)

        fun onReceivedWebConfig(title: String, icon: Bitmap?, color: String)

        fun onPageStarted(url: String, title: String, icon: Bitmap?)

        fun onPageFinished(url: String, title: String, icon: Bitmap?)

        fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long)

        fun onReceivedTouchIconUrl(url: String, precomposed: Boolean)

        fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean

        fun onJsAlert(url: String, message: String, result: JsResult): Boolean

        fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean

        fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean

        fun onPermissionRequestCanceled(request: PermissionRequest)

        fun onWebViewLongPress(url: String, type: Int)

        fun onJsConfirm(url: String, message: String, result: JsResult): Boolean

        fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean

        fun onCloseWindow()

        fun onHideCustomView()

        fun onGeolocationPermissionsHidePrompt()
    }

    private var userAgent: String? = null
    private var mPrivateFlag: Boolean = false
    private var isAdBlock: Boolean = false

    init {
        initWebView()
        initWebSetting()
        settingMultipleWindow()
        initUserAgent()
        setWebContentsDebuggingEnabled(true)
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this)
    }

    private fun initUserAgent() {
        if (SP.enableUA) {
            val ua = SP.UA
            if (ua.isNotEmpty()) settings.userAgentString = ua
            else settings.userAgentString = WebUtil.UA_DESKTOP
        }
    }

    override fun onResume() {
        resumeTimers()
        super.onResume()
    }

    override fun onPause() {
        pauseTimers()
        super.onPause()
    }

    override fun destroy() {
        stopLoading()
        onPause()
        clearHistory()
        removeAllViews()
        destroyDrawingCache()
        super.destroy()
    }

    fun onBackPressed() {
        stopLoading()
        onPause()
        gone()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun initWebView() {
        isDrawingCacheEnabled = true
//        drawingCacheBackgroundColor = ContextCompat.getColor(context, R.color.white)
        drawingCacheQuality = DRAWING_CACHE_QUALITY_HIGH
        setWillNotCacheDrawing(false)
        setWebContentsDebuggingEnabled(true)
        isSaveEnabled = true
        isFocusable = true
        isFocusableInTouchMode = true
        isScrollbarFadingEnabled = true
        overScrollMode = OVER_SCROLL_ALWAYS

        webViewClient = PageViewClient(context, this)
        webChromeClient = PageChromeClient(this)
        setDownloadListener(this)

        // AppRTC requires third party cookies to work
        CookieManager.getInstance().acceptThirdPartyCookies(this)

        //@see http://www.cnblogs.com/classloader/p/5302784.html
        setOnLongClickListener {
            val result = hitTestResult
            return@setOnLongClickListener when (result.type) {
                HitTestResult.EDIT_TEXT_TYPE -> {
//                    result.extra?.let { _delegate?.onWebViewLongPress(it, result.type) }
                    false
                }
                HitTestResult.PHONE_TYPE -> {
                    context!!.callPhone(result.extra!!)
                    true
                }
                HitTestResult.EMAIL_TYPE -> {
                    context!!.sendMailTo(result.extra!!)
                    true
                }
                HitTestResult.GEO_TYPE -> {
                    false
                }
                HitTestResult.SRC_ANCHOR_TYPE -> {
                    result.extra?.let { _delegate?.onWebViewLongPress(it, result.type) }
                    false
                }
                HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    result.extra?.let { _delegate?.onWebViewLongPress(it, result.type) }
                    false
                }
                HitTestResult.IMAGE_TYPE -> {
                    result.extra?.let { _delegate?.onWebViewLongPress(it, result.type) }
                    false
                }
                else -> {
                    result.extra?.let { _delegate?.onWebViewLongPress(it, result.type) }
                    false
                }
            }
        }

        isAdBlock = SP.adBlock
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebSetting() = with(settings) {
        allowContentAccess = true

        allowFileAccess = true
        allowFileAccessFromFileURLs = true
        allowUniversalAccessFromFileURLs = true

        setAppCacheEnabled(true)
        setAppCachePath(context.cacheDir.toString())
        cacheMode = WebSettings.LOAD_DEFAULT
        databaseEnabled = true
        domStorageEnabled = true
        saveFormData = true
        setSupportZoom(true)
        useWideViewPort = true
        builtInZoomControls = true
        displayZoomControls = false

        textZoom = SP.textZoom
        minimumFontSize = SP.minimumFontSize

        javaScriptEnabled = true
        javaScriptCanOpenWindowsAutomatically = SP.isEnableMultipleWindows

        setSupportMultipleWindows(SP.isEnableMultipleWindows)

        blockNetworkImage = false
        blockNetworkLoads = false
        setGeolocationEnabled(true)
        defaultTextEncodingName = WebUtil.URL_ENCODE
        layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING

        loadWithOverviewMode = true
        loadsImagesAutomatically = true

        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        userAgent = userAgentString

        mediaPlaybackRequiresUserGesture = true

        pluginState = WebSettings.PluginState.ON
    }

    private fun settingMultipleWindow() {
        val enableMultipleWindows = SP.isEnableMultipleWindows
        settings.javaScriptCanOpenWindowsAutomatically = enableMultipleWindows
        settings.setSupportMultipleWindows(enableMultipleWindows)
    }

    override fun loadUrl(url: String) {
        if (!shouldOverrideUrlLoading(url)) {
            stopLoading()
            super.loadUrl(url.parseUrl())
        }
    }


    fun setupViewMode(isPrivate: Boolean) {
        mPrivateFlag = isPrivate

        if (mPrivateFlag) createNoTracePage()

        with(settings) {
            setAppCacheEnabled(!mPrivateFlag)
            cacheMode = if (mPrivateFlag) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            databaseEnabled = !mPrivateFlag

            domStorageEnabled = !mPrivateFlag
            saveFormData = !mPrivateFlag
            setGeolocationEnabled(!mPrivateFlag)
        }
    }

    private fun createNoTracePage() {
        loadUrl(Protocol.ABOUT_BLANK)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        _scrollListener?.invoke(l - oldl, t - oldt)
    }


    private var _scrollListener: ((dx: Int, dy: Int) -> Unit)? = null


    fun setScrollChangeListener(todo: ((dx: Int, dy: Int) -> Unit)?) {
        _scrollListener = todo
    }


    fun setUserAgent(type: Int) {
        when (type) {
            Type.UA_DESKTOP -> settings.userAgentString = WebUtil.UA_DESKTOP
            Type.UA_CUSTOM -> settings.userAgentString = SP.UA
            else -> settings.userAgentString = userAgent
        }
        reload()
    }


    fun getBackOrForwardHistoryItem(steps: Int): WebHistoryItem {
        val history = copyBackForwardList()
        return history.getItemAtIndex(history.currentIndex + steps)
    }


    fun getContentWidth() = computeHorizontalScrollRange()


    override fun getContentHeight() = computeVerticalScrollRange()


    private var _delegate: Delegate? = null

    fun setPageViewDelegate(delegate: Delegate) {
        _delegate = delegate
    }

    override fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long) {
        L.e(TAG, "onDownloadStart \n url: $url \n agent: $userAgent \n contentDisposition: $contentDisposition \n type: $mimetype \n contentLength: $contentLength")
        _delegate?.onDownloadStart(url, userAgent, contentDisposition, mimetype, contentLength)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        L.e(TAG, "onSharedPreferenceChanged")
        when (key) {

            resources.getString(R.string.preference_key_text_zoom) -> {
                settings.textZoom = SP.textZoom
            }


            resources.getString(R.string.preference_key_text_minimum_size) -> {
                settings.minimumFontSize = SP.minimumFontSize
            }
            resources.getString(R.string.preference_key_adblock) -> {
                //set ad block
                isAdBlock = SP.adBlock
            }
            resources.getString(R.string.preference_key_enable_multiple_windows) -> {
                settings.setSupportMultipleWindows(SP.isEnableMultipleWindows)
            }
        }
    }

    override fun onFormResubmission(dontResend: Message, resend: Message) {
        L.e(TAG, "onFormResubmission")
        _delegate?.onFormResubmission(dontResend, resend)
    }

    override fun onReceivedClientCertRequest(request: ClientCertRequest) {
        L.e(TAG, "onReceivedClientCertRequest")
        _delegate?.onReceivedClientCertRequest(request)
    }

    override fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String) {
        L.e(TAG, "onReceivedHttpAuthRequest")
        _delegate?.onReceivedHttpAuthRequest(handler, host, realm)
    }

    override fun onReceivedSslError(handler: SslErrorHandler, error: SslError) {
        L.e(TAG, "onReceivedSslError: $error")
        _delegate?.onReceivedSslError(handler, error)
    }

    override fun onPageStarted(url: String, title: String, icon: Bitmap?) {
        L.e(TAG, "onPageStarted $url : $title | ${icon != null}")
        _delegate?.onPageStarted(url, title, icon)
    }

    override fun onPageFinished(url: String, title: String, icon: Bitmap?) {
        L.e(TAG, "onPageFinished $url : $title | ${icon != null}")
        _delegate?.onPageFinished(url, title, icon)
    }

    override fun shouldOverrideUrlLoading(url: String): Boolean {
        L.e(TAG, "shouldOverrideUrlLoading $url")

        try {
            return when {
                url.isIntent() -> {
                    context.openIntentByDefault(url)
                    true
                }
                url.isEmailTo() -> {
                    context.sendMailTo(url)
                    true
                }
                url.isTel() -> {
                    context.callPhone(url)
                    true
                }
                url.isProtocolUrl() -> false
                url.isAppProtocolUrl() -> {
                    context.openIntentByDefault(url)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }


    override fun doUpdateVisitedHistory(url: String, isReload: Boolean) {
        L.e(TAG, "doUpdateVisitedHistory $url isReload $isReload")
        if (mPrivateFlag)
            clearHistory()
        else if (!isReload) {
            SQLHelper.saveOrUpdateRecord(title!!, url)
        }
    }

    override fun onReceivedError(url: String, code: Int, desc: String) {
        L.e(TAG, "onReceivedError $url & code: $code & desc: $desc")
    }


    override fun shouldInterceptRequest(url: String): WebResourceResponse? {
        if (isAdBlock && AdBlock.isAd(url)) {
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(byteArrayOf()))
        }

        return null
    }

    private fun evaluateScript() {
        evaluateJavascript(Evaluate.SCRIPT) {
            L.d(TAG, "evaluate JSON: $it")

            doAsync {
                val res = Evaluate.parseResult(it)

                //handle manifest.json
                res.manifest.apply {
                    if (isEmpty()) return@apply

                    val json = URL(this).readText()

                    Evaluate.parseManifest(json).apply {
                        if (themeColor.isNotEmpty()) {
                            uiThread { _delegate?.onReceivedWebThemeColor(themeColor) }
                        }

                        //handle website icons
                        sortIcons(icons)

                        var bitmap: Bitmap? = null
                        if (icons.size > 0) {
                            bitmap = BitmapFactory.decodeStream(URL(icons[0].src).openStream())
                        }
                        uiThread { _delegate?.onReceivedWebConfig(title!!, bitmap, themeColor) }
                    }
                }

                //handle theme color
                if (res.themeColor.isNotEmpty()) {
                    uiThread { _delegate?.onReceivedWebThemeColor(res.themeColor) }
                }

                //handle website favicon
                sortIcons(res.icons)

                var bitmap: Bitmap? = null
                if (res.icons.size > 0) {
                    bitmap = BitmapFactory.decodeStream(URL(res.icons[0].src).openStream())
                }
                uiThread { _delegate?.onReceivedWebConfig(title!!, bitmap, res.themeColor) }
            }
        }
    }


    private fun sortIcons(icons: ArrayList<Evaluate.Icon>) {
        icons.forEach { L.i(TAG, "before size: ${it.sizes}") }
        val newList = icons.filter { Evaluate.parseSize(it.sizes) in 72..192 }
                .sortedByDescending { it.sizes }

        icons.clear()
        icons.addAll(newList)
        icons.forEach { L.i(TAG, "after size: ${it.sizes}") }
    }

    override fun onCloseWindow() {
        L.e(TAG, "onCloseWindow")
        _delegate?.onCloseWindow()
    }

    override fun onProgressChanged(progress: Int) {
        L.e(TAG, "onProgressChanged $progress")
        _delegate?.onProgressChanged(progress)
    }

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        L.e(TAG, "onShowCustomView")
        _delegate?.onShowCustomView(view, callback)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        L.e(TAG, "onPermissionRequest")
        _delegate?.onPermissionRequest(request)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        L.e(TAG, "onGeolocationPermissionsShowPrompt origin: $origin")
        _delegate?.onGeolocationPermissionsShowPrompt(origin, callback)
    }

    override fun onReceivedTitle(url: String, title: String) {
        L.e(TAG, "onReceivedTitle $url : $title")
        _delegate?.onReceivedTitle(url, title)
        evaluateScript()
    }

    override fun onReceivedIcon(url: String, title: String, icon: Bitmap?) {
        L.e(TAG, "onReceivedIcon $url : $title | ${icon != null}")
        _delegate?.onReceivedIcon(url, title, icon)
    }

    override fun onReceivedTouchIconUrl(url: String, precomposed: Boolean) {
        L.e(TAG, "onReceivedTouchIconUrl $url : precomposed $precomposed")
        _delegate?.onReceivedTouchIconUrl(url, precomposed)
    }

    override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                                   fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        L.e(TAG, "onShowFileChooser")
        return _delegate?.onShowFileChooser(filePathCallback, fileChooserParams) ?: false
    }

    override fun onJsAlert(url: String, message: String, result: JsResult): Boolean {
        L.e(TAG, "onJsAlert: $url")
        return _delegate?.onJsAlert(url, message, result) ?: false
    }

    override fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        L.e(TAG, "onJsPrompt: $url")
        return _delegate?.onJsPrompt(url, message, defaultValue, result) ?: false
    }

    override fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        L.e(TAG, "onCreateWindow")
        return _delegate?.onCreateWindow(isDialog, isUserGesture, resultMsg) ?: false
    }

    override fun onHideCustomView() {
        L.e(TAG, "onHideCustomView")
        _delegate?.onHideCustomView()
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        L.e(TAG, "onPermissionRequestCanceled")
        _delegate?.onPermissionRequestCanceled(request)
    }

    override fun onJsConfirm(url: String, message: String, result: JsResult): Boolean {
        L.e(TAG, "onJsConfirm: $url")
        return _delegate?.onJsConfirm(url, message, result) ?: false
    }

    override fun onGeolocationPermissionsHidePrompt() {
        L.e(TAG, "onGeolocationPermissionsHidePrompt")
        _delegate?.onGeolocationPermissionsHidePrompt()
    }

    override fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean {
        L.e(TAG, "onJsBeforeUnload")
        return _delegate?.onJsBeforeUnload(url, message, result) ?: false
    }
}


class PageViewClient(val context: Context, val delegate: Delegate?) : WebViewClient() {

    interface Delegate {
        fun onFormResubmission(dontResend: Message, resend: Message)

        fun onReceivedClientCertRequest(request: ClientCertRequest)

        fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String)

        fun onReceivedSslError(handler: SslErrorHandler, error: SslError)

        fun onPageStarted(url: String, title: String, icon: Bitmap?)

        fun onPageFinished(url: String, title: String, icon: Bitmap?)

        fun shouldOverrideUrlLoading(url: String): Boolean = false

        fun doUpdateVisitedHistory(url: String, isReload: Boolean)

        fun onReceivedError(url: String, code: Int, desc: String)

        fun shouldInterceptRequest(url: String): WebResourceResponse?
    }


    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return delegate?.shouldOverrideUrlLoading(url)
                ?: super.shouldOverrideUrlLoading(view, url)
    }

    /**
     * @targetSdk >= 24
     */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return delegate?.shouldOverrideUrlLoading(request.url.toString())
                ?: super.shouldOverrideUrlLoading(view, request)
    }


    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        delegate?.onPageStarted(url, view.title!!, favicon)
                ?: super.onPageStarted(view, url, favicon)
    }


    override fun onPageFinished(view: WebView?, url: String) {
        delegate?.onPageFinished(url, view?.title ?: "", view?.favicon)
                ?: super.onPageFinished(view, url)
    }


    override fun onPageCommitVisible(view: WebView, url: String) {
        delegate?.onPageFinished(url, view.title!!, view.favicon)
                ?: super.onPageCommitVisible(view, url)
    }


    override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
        delegate?.onReceivedHttpAuthRequest(handler, host, realm)
                ?: super.onReceivedHttpAuthRequest(view, handler, host, realm)
    }


    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        delegate?.onFormResubmission(dontResend, resend)
                ?: super.onFormResubmission(view, dontResend, resend)
    }


    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        delegate?.onReceivedClientCertRequest(request)
                ?: super.onReceivedClientCertRequest(view, request)
    }


    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        delegate?.onReceivedSslError(handler, error)
                ?: super.onReceivedSslError(view, handler, error)
    }


    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        delegate?.doUpdateVisitedHistory(url, isReload)
                ?: super.doUpdateVisitedHistory(view, url, isReload)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        delegate?.onReceivedError(request.url.toString(), error.errorCode, error.description.toString())
                ?: super.onReceivedError(view, request, error)
    }


    override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
        delegate?.onReceivedError(failingUrl, errorCode, description)
                ?: super.onReceivedError(view, errorCode, description, failingUrl)
    }


    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        delegate?.onReceivedError(view.url!!, errorResponse.statusCode, errorResponse.reasonPhrase)
                ?: super.onReceivedHttpError(view, request, errorResponse)
    }


    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return delegate?.shouldInterceptRequest(request.url.toString())
                ?: super.shouldInterceptRequest(view, request)
    }


    override fun shouldInterceptRequest(view: WebView?, url: String): WebResourceResponse? {
        return delegate?.shouldInterceptRequest(url) ?: super.shouldInterceptRequest(view, url)
    }
}


class PageChromeClient(val delegate: Delegate) : WebChromeClient() {

    interface Delegate {
        fun onProgressChanged(progress: Int)

        fun onReceivedTitle(url: String, title: String) {}

        fun onReceivedIcon(url: String, title: String, icon: Bitmap?)

        fun onReceivedTouchIconUrl(url: String, precomposed: Boolean)

        fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean

        fun onJsAlert(url: String, message: String, result: JsResult): Boolean

        fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean

        fun onJsConfirm(url: String, message: String, result: JsResult): Boolean

        fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean

        fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean

        fun onCloseWindow()

        fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)

        fun onHideCustomView()

        fun onPermissionRequest(request: PermissionRequest)

        fun onPermissionRequestCanceled(request: PermissionRequest)

        fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback)

        fun onGeolocationPermissionsHidePrompt()
    }


    override fun getDefaultVideoPoster(): Bitmap {
        return BitmapFactory.decodeResource(App.instance.resources, R.drawable.poster)
    }


    override fun onProgressChanged(view: WebView, newProgress: Int) {
        delegate.onProgressChanged(newProgress)
    }


    override fun onReceivedTitle(view: WebView, title: String) {
        delegate.onReceivedTitle(view.url!!, title)
    }


    override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
        delegate.onReceivedIcon(view.url!!, view.title!!, icon)
    }


    override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
        delegate.onReceivedTouchIconUrl(url, precomposed)
    }


    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        return delegate.onCreateWindow(isDialog, isUserGesture, resultMsg)
    }


    override fun onCloseWindow(window: WebView) {
        delegate.onCloseWindow()
    }


    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        delegate.onShowCustomView(view, callback)
    }


    override fun onShowCustomView(view: View, requestedOrientation: Int, callback: CustomViewCallback) {
        delegate.onShowCustomView(view, callback)
    }


    override fun onHideCustomView() {
        delegate.onHideCustomView()
    }


    override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return delegate.onJsBeforeUnload(url, message, result)
    }


    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return delegate.onJsAlert(url, message, result)
    }


    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return delegate.onJsConfirm(url, message, result)
    }


    override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        return delegate.onJsPrompt(url, message, defaultValue, result)
    }


    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        delegate.onGeolocationPermissionsShowPrompt(origin, callback)
    }


    override fun onGeolocationPermissionsHidePrompt() {
        delegate.onGeolocationPermissionsHidePrompt()
    }


    override fun onPermissionRequest(request: PermissionRequest) {
        delegate.onPermissionRequest(request)
    }


    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        delegate.onPermissionRequestCanceled(request)
    }


    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
        return delegate.onShowFileChooser(filePathCallback, fileChooserParams)
    }

}