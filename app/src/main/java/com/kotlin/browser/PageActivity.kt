package com.kotlin.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.security.KeyChain
import android.text.*
import android.text.style.StyleSpan
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.kotlin.browser.application.*
import com.kotlin.browser.widget.*
import kotlinx.android.synthetic.main.activity_page.*
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import kotlinx.android.synthetic.main.content_bottom_sheet.view.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.IOException


class PageActivity : BaseActivity(), PageView.Delegate, SharedPreferences.OnSharedPreferenceChangeListener {

    var mCurrentMode = Type.MODE_DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SP.canScreenshot) {
            WebView.enableSlowWholeDocumentDraw()
        }

        setContentView(R.layout.activity_page)
        setSupportActionBar(toolbar)

        checkExtra()

        initEvent()

        initInputBox()
        initRecordRecycler()
    }

    /**
     * If [Intent.FLAG_ACTIVITY_MULTIPLE_TASK] has not been used this Activity
     * will be reused.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        L.i(TAG, "onNewIntent")
        // It is good pratice to remove a document from the overview stack if not needed anymore.
//        finishAndRemoveTask()
    }


    private var isPrivate: Boolean = false


    private var mTargetUrl: String? = null

    private var mParentTaskId: Int = 0


    private fun checkExtra() {
        mParentTaskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
        isPrivate = intent.getBooleanExtra(EXTRA_PRIVATE, false)
        mPageView.setupViewMode(isPrivate)

        mTargetUrl = intent.getStringExtra(EXTRA_TARGET_URL)
        mTargetUrl?.let { if (it.isNotEmpty()) loadPage(it) }

        App.MESSAGE?.let { loadPage(it) }
    }


    private var mPinsRecycler: RecyclerView? = null

    private lateinit var mPinsLayoutManager: LinearLayoutManager
    private val mPins: ArrayList<Pin> = ArrayList()


    private fun setPinReversePadding() {
        val padding = dip2px(10f)
        if (SP.pinsReverse) {
            mPinsRecycler?.setPadding(0, 0, 0, padding)
        } else {
            mPinsRecycler?.setPadding(0, padding, 0, 0)
        }
    }


    private var mCurrentEditorPosition: Int = 0

    private var mRecordRecycler: RecyclerView? = null

    private lateinit var mRecordsAdapter: RecordsAdapter

    private val mRecords: ArrayList<Record> = ArrayList()

    private fun initRecordRecycler() {
        if (mRecordRecycler == null) {
            mRecordRecycler = mRecordsStub.inflate() as RecyclerView
        }

        mRecordRecycler?.let {
            mRecordsAdapter = RecordsAdapter(this, mRecords)
            it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
            it.setHasFixedSize(true)
            it.itemAnimator = DefaultItemAnimator()
            mRecordsAdapter.setOnClickListener { _, position ->
                //加载网址
                loadPage(mRecords[position].url)
            }
            it.adapter = mRecordsAdapter
        }
    }

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<*>
    private fun initEvent() {
        // Bottom Sheet content layout views
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)

        // Capturing the callbacks for bottom sheet
        mBottomSheetBehavior.setBottomSheetCallback(mBottomSheetCallback)

        mMenuOptionWidget.setMenuOptionListener(mMenuOptionListener)

        mPageView.setPageViewDelegate(this)
        setInputBoxNestScroll()

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)

        registerNetworkChangeBroadcastReceiver()

        registerVibrator()
    }

    private var mVibrator: Vibrator? = null
    private fun registerVibrator() {
        if (!SP.vibrate) {
            mVibrator = null
            return
        }
        mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun vibrate() {
        mVibrator?.apply {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // This ignores all exceptions to stay compatible with pre-O implementations.
                    vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrate(50)
                }
            } catch (iae: IllegalArgumentException) {
                L.e(TAG, "Failed to create VibrationEffect")
                iae.printStackTrace()
            }
        }
    }


    private fun setInputBoxNestScroll() {

        mPageView.apply {
            if (SP.omniboxFixed) {
                mBottomSheetBehavior.isHideable = false
                val margin = resources.getDimension(R.dimen.action_bar_size_48).toInt()
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        .apply { setMargins(0, 0, 0, margin) }
                setScrollChangeListener(null)
            } else {

                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        .apply { setMargins(0, 0, 0, 0) }


                setScrollChangeListener { _, dy ->

                    if (dy > 0) {
                        mBottomSheetBehavior.let {
                            if (it.isCollapsed()) {
                                mBottomSheetBehavior.isHideable = true
                                it.hidden()
                            }
                        }
                    } else if (dy < 0) {
                        mBottomSheetBehavior.let {
                            if (it.isHidden()) {
                                it.collapsed()
                            }
                        }
                    }
                }
            }
        }
    }


    private var mNetworkBroadcastReceiver: BroadcastReceiver? = null

    private fun registerNetworkChangeBroadcastReceiver() {
        mNetworkBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                    val networkInfo = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
                    val available = networkInfo != null && networkInfo.isAvailable
                    mPageView.setNetworkAvailable(available)
                }
            }
        }

        registerReceiver(mNetworkBroadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }


    private fun initInputBox() = with(mInputBox) {
        addTextChangedListener(mTextWatcher)

        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == KeyEvent.KEYCODE_ENDCALL) {
                val url = mInputBox.text.toString().trim()
                if (url.isEmpty()) {
                    toast("Please enter text")
                    return@setOnEditorActionListener true
                }

                loadPage(url)
            }
            return@setOnEditorActionListener true
        }

        if (isPrivate) {
            drawableLeft(R.drawable.ic_action_private)
            setPadding(-dip2px(5f), paddingTop, paddingRight, paddingBottom)
        }

        if (SP.isShowKeyboard) {
            showKeyboard()
        }

        //todo[✔] 地址栏控制
        setOmniboxControlListener()
    }


    private fun setOmniboxControlListener() {
        if (!SP.omniboxCtrl) {
            mInputBox.setOnTouchListener(null)
            return
        }

        mInputBox.setOnTouchListener(SwipeToBoundListener(toolbar, object : SwipeToBoundListener.BoundCallback {
            private val keyListener = mInputBox.keyListener
            override fun canSwipe(): Boolean = mBottomSheetBehavior.isCollapsed() && mCurrentMode == Type.MODE_WEB

            override fun onSwipe() {
                mInputBox.keyListener = null
                mInputBox.isFocusable = false
                mInputBox.isFocusableInTouchMode = false
                mInputBox.clearFocus()
            }

            override fun onBound(canSwitch: Boolean, left: Boolean) {
                mInputBox.keyListener = keyListener
                mInputBox.isFocusable = true
                mInputBox.isFocusableInTouchMode = true
                mInputBox.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                mInputBox.clearFocus()

                if (!canSwitch) return
                val msg: String
                if (left) {
                    if (mPageView.canGoBackOrForward(+1)) {
                        msg = mPageView.getBackOrForwardHistoryItem(+1).title
                        mPageView.goForward()
                    } else {
                        msg = getString(R.string.toast_msg_last_history)
                    }
                } else {
                    if (mPageView.canGoBackOrForward(-1)) {
                        msg = mPageView.getBackOrForwardHistoryItem(-1).title
                        mPageView.goBack()
                    } else {
                        restUiAndStatus()
                        return
                    }
                }
                toast(msg)
            }

        }))
    }


    private fun loadPage(msg: Message) {
        val transport = msg.obj
        when (transport) {
            is WebView.WebViewTransport -> {
                transport.webView = mPageView
                msg.sendToTarget()
            }
        }
    }


    private fun loadPage(url: String) {
        goneRecordRecycler()
        mInputBox.hideKeyboard()
        mPageView.loadUrl(url)
    }


    private fun showRecordRecycler() {
        mRecordRecycler?.visible()
    }


    private fun goneRecordRecycler() {
        mRecordRecycler?.gone()
    }


    private val mTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val input = s?.apply { toString().trim() } ?: ""


            if (mCurrentMode == Type.MODE_PIN_EDIT) {
                mConfirmMenu?.isEnabled = input.isNotEmpty()
                return
            }


            if (input.isEmpty()) {
                goneRecordRecycler()
                return
            }


            showRecordRecycler()
            searchRecord(input)

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }


    private fun searchRecord(input: CharSequence) {

        doAsync {
            val records = SQLHelper.searchRecord(input.toString())

            val oldList = ArrayList<Record>(mRecords)

            mRecords.clear()
            mRecords.addAll(records)

            compareRecordListDiff(oldList, mRecords).dispatchUpdatesTo(mRecordsAdapter)
        }
    }


    private fun compareRecordListDiff(oldList: List<Record>, newList: List<Record>): DiffUtil.DiffResult {
        return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldList[oldItemPosition].url == newList[newItemPosition].url

            override fun getOldListSize(): Int = oldList.size

            override fun getNewListSize(): Int = newList.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldRecord = oldList[oldItemPosition]
                val newRecord = newList[newItemPosition]
                return oldRecord.title == newRecord.title && oldRecord.time == newRecord.time
            }

        })
    }


    private val mBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {

                BottomSheetBehavior.STATE_EXPANDED -> afterBottomSheetExpanded()


                BottomSheetBehavior.STATE_COLLAPSED -> afterBottomSheetCollapsed()

                BottomSheetBehavior.STATE_DRAGGING -> mBottomSheetBehavior.isHideable = false
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val alpha = slideOffset * 0.3f
            mMaskView.alpha = if (alpha < 0) 0f else alpha
        }
    }


    private val mMaskTouchListener = View.OnTouchListener { v, _ ->
        if (v.id == R.id.mMaskView) {
            closeBottomSheet()
            return@OnTouchListener true
        }

        return@OnTouchListener false
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.preference_key_homepage_reverse) -> {
                mPinsLayoutManager.stackFromEnd = SP.pinsReverse
                setPinReversePadding()
            }

            getString(R.string.preference_key_omnibox_fixed) -> setInputBoxNestScroll()


            getString(R.string.preference_key_omnibox_control) -> setOmniboxControlListener()


            getString(R.string.preference_key_back_vibrate) -> registerVibrator()
        }
    }

    override fun onReceivedWebThemeColor(str: String) {

        L.d(TAG, "onReceivedWebThemeColor: $str")
        setStatusBarColor(str)
    }

    private fun setStatusBarColor(color: String) {
        if (isPrivate) return
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            try {
                statusBarColor = if (color.isNotEmpty()) Color.parseColor(color) else Color.BLACK
            } catch (e: Exception) {
                statusBarColor = Color.BLACK
                e.printStackTrace()
            }
        }
    }

    override fun onFormResubmission(dontResend: Message, resend: Message) {

        dialogBuilder().setCancelable(false)
                .setMessage(R.string.dialog_message_form_resubmission)
                .setPositiveButton(R.string.dialog_button_resend) { dialog, _ ->
                    resend.sendToTarget()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_cancel) { dialog, _ ->
                    dontResend.sendToTarget()
                    dialog.dismiss()
                }.create().show()
    }


    override fun onReceivedClientCertRequest(request: ClientCertRequest) {

        KeyChain.choosePrivateKeyAlias(this, { alias ->
            alias?.let {
                if (it.isEmpty()) request.cancel()
                else proceedClientCertRequest(request, it)
            }
        }, request.keyTypes,
                request.principals,
                request.host,
                request.port,
                null)
    }


    private fun proceedClientCertRequest(request: ClientCertRequest, alias: String) {
        doAsync {
            try {
                request.proceed(KeyChain.getPrivateKey(this@PageActivity, alias),
                        KeyChain.getCertificateChain(this@PageActivity, alias))
            } catch (e: Exception) {
                e.printStackTrace()
                request.ignore()
            }
        }
    }

    /**
     * @see https://blog.csdn.net/Crazy_zihao/article/details/51557425
     */
    override fun onReceivedSslError(handler: SslErrorHandler, error: SslError) {

        val host = error.url.host()

        val builder = SpannableStringBuilder().apply {
            val prefix = getString(R.string.dialog_message_ssl_error_prefix)
            append(prefix)
            append(host)
            setSpan(StyleSpan(android.graphics.Typeface.BOLD), prefix.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(getString(R.string.dialog_message_ssl_error_middle))
            append(getSslErrorMsg(error))
        }

        dialogBuilder().setCancelable(false)
                .setMessage(builder)
                .setPositiveButton(R.string.dialog_button_cancel) { dialog, _ ->
                    handler.cancel()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_process) { dialog, _ ->
                    handler.proceed()
                    dialog.dismiss()
                }.create().show()
    }


    private fun getSslErrorMsg(sslError: SslError): String {
        return when (sslError.primaryError) {
            SslError.SSL_NOTYETVALID -> resources.getString(R.string.ssl_error_notyetvalid)
            SslError.SSL_EXPIRED -> resources.getString(R.string.ssl_error_expired)
            SslError.SSL_IDMISMATCH -> resources.getString(R.string.ssl_error_idmismatch)
            SslError.SSL_UNTRUSTED -> resources.getString(R.string.ssl_error_untrusted)
            SslError.SSL_DATE_INVALID -> resources.getString(R.string.ssl_error_date_invalid)
            else -> resources.getString(R.string.ssl_error_invalid)
        }
    }

    override fun onReceivedHttpAuthRequest(handler: HttpAuthHandler, host: String, realm: String) {

        AuthLoginDialog(this).apply {
            setTitle(host)
            setOnPositiveClickListener { handler.proceed(getUserName(), getPassword()) }
            setOnNegativeClickListener { handler.cancel() }
        }.show()
    }

    override fun onPageStarted(url: String, title: String, icon: Bitmap?) {

        //set Web Mode
        mCurrentMode = Type.MODE_WEB

        mInputBox.hideKeyboard()
        mInputBox.text = url.toColorUrl()

        //only show pageView
        mPageView.onResume()
        mPageView.visible()
        goneRecordRecycler()
        mPinsRecycler?.gone()

        //show progress
        mProgress.visible()
//        mProgress.progress = 0

        //set menu item
        showMenu(stop = true)
    }

    override fun onPageFinished(url: String, title: String, icon: Bitmap?) {
        mProgress.gone()
        mMenuOptionWidget.showMoreMenu()

        showMenu(refresh = true)
    }

    override fun onProgressChanged(progress: Int) {
        mProgress.apply {
            if (isGone()) visible()
            this.progress = progress

            if (progress >= 80) gone()
        }
    }

    override fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long) {

        permission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {

            var fileName = Download.parseFileName(url, contentDisposition, mimetype)

            if (fileName.length > 30) {
                fileName = fileName.toMask(10, 10)
            }

            Snackbar.make(mInputBox, getString(R.string.toast_download_confirm, fileName, contentLength.formatSize()), Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.dialog_button_confirm) {
                        Download.inBrowser(this, url, contentDisposition, mimetype)
                    }.show()
        }
    }

    private var mQuickOptionDialog: QuickOptionDialog? = null
    private val mImageType = arrayOf(WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
            WebView.HitTestResult.IMAGE_TYPE
    )

    override fun onWebViewLongPress(url: String, type: Int) {

        if (mQuickOptionDialog == null) {
            mQuickOptionDialog = QuickOptionDialog(this)
                    .setQuickListener {
                        quickNewTab = {
                            if (SP.isOpenInBackground)
                                openUrlOverviewScreen(it, taskId = taskId)
                            else openUrl(it, taskId = taskId)
                        }

                        quickNewPrivateTab = {
                            if (SP.isOpenInBackground)
                                openUrlOverviewScreen(it, true, taskId)
                            else openUrl(it, true, taskId)
                        }

                        quickDownloadImg = {
                            permission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                                if (it.isEmpty()) return@permission
                                startDownloadImg(it)
                            }
                        }

                        quickExtractQR = {
                            toast(it)
                            if (SP.isOpenInBackground)
                                openUrlOverviewScreen(it, taskId = taskId)
                            else openUrl(it, taskId = taskId)
                        }
                    }
        }

        mQuickOptionDialog!!.setUrl(url).isImageUrl(type in mImageType).show()
    }


    private fun startDownloadImg(it: String) {
        if (it.isBase64Url()) {
            doAsync {
                it.base64ToBitmap()?.apply {
                    val file = save(System.currentTimeMillis().toString())
                    file.mediaScan(this@PageActivity)
                    uiThread { toast(getString(R.string.toast_save_to_path, file.absolutePath)) }
                    return@doAsync
                }

                uiThread { toast(R.string.toast_download_failed) }
            }
            return
        }

        Download.inBackground(this, it, WebUtil.HEADER_CONTENT_DISPOSITION, WebUtil.MIME_TYPE_IMAGE)
    }

    private var mCustomView: View? = null
    private var mOriginalSystemUiVisibility: Int = 0
    private var mOriginalOrientation: Int = 0
    private var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {

        mCustomView?.let {
            onHideCustomView()
            return
        }

        // 1. Stash the current state
        mCustomView = view
        mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
        mOriginalOrientation = requestedOrientation

        // 2. Stash the custom view callback
        mCustomViewCallback = callback

        // 3. Add the custom view to the view hierarchy
//        val decor = window.decorView as FrameLayout
//        val decor = find<FrameLayout>(android.R.id.content)
        mCustomViewContainer.visible()
        mCustomViewContainer.addView(mCustomView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))


        // 4. Change the state of the window
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    @SuppressLint("WrongConstant")
    override fun onHideCustomView() {

//        val decor = window.decorView as FrameLayout
//        val decor = find<FrameLayout>(android.R.id.content)
        mCustomViewContainer.removeView(mCustomView)
        mCustomViewContainer.gone()
        mCustomView = null

        // 2. Restore the state to it's original form
        window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
        requestedOrientation = mOriginalOrientation

        // 3. Call the custom view callback
        mCustomViewCallback?.onCustomViewHidden()
        mCustomViewCallback = null
    }

    override fun onPermissionRequest(request: PermissionRequest) {

        val permission = arrayOfNulls<String>(0)
        request.resources.forEach {
            when (it) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> permission[permission.size] = Manifest.permission.CAMERA
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> permission[permission.size] = Manifest.permission.RECORD_AUDIO
                else -> L.i(TAG, "request other permission: $it")
            }
        }

        if (permission.isNotEmpty()) {
            PermissionsManager.getInstance()
                    .requestPermissionsIfNecessaryForResult(this, permission,
                            object : PermissionsResultAction() {
                                override fun onGranted() {
                                    request.grant(permission)
                                }

                                override fun onDenied(permission: String) {
                                    request.deny()
                                    grantVideoAndAudioPermissionFail(permission)
                                }
                            })
        }
    }


    private fun grantVideoAndAudioPermissionFail(permission: String) {
        when (permission) {
            Manifest.permission.CAMERA -> toast(R.string.toast_camera_permission_denied)
            Manifest.permission.RECORD_AUDIO -> toast(R.string.toast_record_audio_permission_denied)
        }
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {

    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {

        permission(Manifest.permission.ACCESS_FINE_LOCATION) {
            showRequestLocationPermissionDialog(origin, callback)
        }
    }

    override fun onGeolocationPermissionsHidePrompt() {

    }


    private fun showRequestLocationPermissionDialog(origin: String, callback: GeolocationPermissions.Callback) {

        val builder = SpannableStringBuilder().apply {
            val prefix = getString(R.string.dialog_message_allow_location_prefix)
            append(prefix)
            append(origin)
            setSpan(StyleSpan(android.graphics.Typeface.BOLD), prefix.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(getString(R.string.dialog_message_allow_location_suffix))
        }

        dialogBuilder().setCancelable(true)
                .setMessage(builder)
                .setPositiveButton(R.string.dialog_button_allow) { dialog, _ ->
                    callback.invoke(origin, true, true)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_deny) { dialog, _ ->
                    callback.invoke(origin, false, false)
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.dialog_button_block) { dialog, _ ->
                    callback.invoke(origin, false, true)
                    dialog.dismiss()
                }.setOnCancelListener {
                    callback.invoke(origin, false, false)
                }.create().show()
    }

    override fun onReceivedTitle(url: String, title: String) {
        //todo[✔] 处理接收到的网站标题
        setAppTaskDescription(title, null)
    }


    override fun onReceivedIcon(url: String, title: String, icon: Bitmap?) {

//        setAppTaskDescription(title, icon)
    }


    override fun onReceivedWebConfig(title: String, icon: Bitmap?, color: String) {

        L.d(TAG, "onReceivedWebConfig $title : $color | ${icon != null}")
        setStatusBarColor(color)
        setAppTaskDescription(title, icon, color)
    }


    override fun onReceivedTouchIconUrl(url: String, precomposed: Boolean) {

    }

    private var mAppName: String? = null
    private var mAppIcon: Bitmap? = null
    private fun defaultThemeColor(): Int = if (isPrivate) Color.BLACK else Color.WHITE


    private fun setAppTaskDescription(title: String, icon: Bitmap?, color: String = "") {
        var label = title
        var bitmap = icon
        var color2 = defaultThemeColor()

        if (title.isEmpty()) {
            if (mAppName == null) mAppName = getString(R.string.app_name)
            label = mAppName!!
        }

        if (bitmap == null) {
            if (mAppIcon == null) mAppIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            bitmap = mAppIcon
        }

        if (color.isNotEmpty() && !isPrivate) {
            color2 = Color.parseColor(color)
        }

        setTaskDescription(ActivityManager.TaskDescription(label, bitmap, color2))
    }


    private var mFileChooserCallback: ValueCallback<Array<Uri>>? = null


    override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>,
                                   fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        mFileChooserCallback = filePathCallback
        startActivityForResult(fileChooserParams.createIntent(), Type.CODE_CHOOSE_FILE)
        return true
    }

    override fun onJsAlert(url: String, message: String, result: JsResult): Boolean {

//        jsResponseDialog(url, message, result)
        longToast(message)
        result.confirm()
        return true
    }

    override fun onJsPrompt(url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {

        dialogBuilder().setTitle(url.host())
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_button_confirm) { dialog, _ ->
                    result.confirm(defaultValue)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_cancel) { dialog, _ ->
                    result.cancel()
                    dialog.dismiss()
                }.create().show()
        return true
    }

    override fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {

        App.MESSAGE = resultMsg
        openUrl("", isPrivate, taskId)
        return true
    }

    override fun onCloseWindow() {

        finishAndRemoveTask()
    }

    override fun onJsConfirm(url: String, message: String, result: JsResult): Boolean {

        jsResponseDialog(url, message, result)
        return true
    }

    override fun onJsBeforeUnload(url: String, message: String, result: JsResult): Boolean {

        jsResponseDialog(url, message, result)
        return true
    }


    private fun afterBottomSheetExpanded() {
        mInputBox.isFocusable = false
        mInputBox.isFocusableInTouchMode = false
        mInputBox.clearFocus()
        mMaskView.setOnTouchListener(mMaskTouchListener)
        mInputBox.hideKeyboard()
    }


    private fun afterBottomSheetCollapsed() {
        mInputBox.isFocusable = true
        mInputBox.isFocusableInTouchMode = true
        mInputBox.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        mInputBox.clearFocus()
        mMaskView.setOnTouchListener(null)

        _closedTodo?.invoke()
    }


    private var _closedTodo: Func? = null
        get() {
            val m = field
            field = null
            return m
        }

    private fun closeBottomSheet(todo: Func? = null) {
        _closedTodo = todo
        mBottomSheetBehavior.collapsed()
    }


    private val mMenuOptionListener = object : MenuOptionListener {
        override fun onDesktopCheckedChanged(check: Boolean) {
            mPageView.setUserAgent(if (check) Type.UA_DESKTOP else Type.UA_DEFAULT)
            closeBottomSheet()
        }

        override fun onCustomUACheckedChanged(check: Boolean) {
            mPageView.setUserAgent(if (check) Type.UA_CUSTOM else Type.UA_DEFAULT)
            closeBottomSheet()
        }

        override fun onScreenshotsClick() {
            closeBottomSheet {
                permission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    captureScreenshot2Bitmap()
                }
            }
        }

        override fun onShareUrlClick() {
            closeBottomSheet { shareText(mInputBox.text.toString()) }
        }

        override fun onNewTabClick() {
            closeBottomSheet { openUrl("", taskId = taskId) }
        }

        override fun onPrivateTabClick() {
            closeBottomSheet { openUrl("", true, taskId) }
        }

        override fun onPinToHome() {
        }

        override fun addToLauncher() {
            closeBottomSheet { createLauncherIcon() }
        }

        override fun onSettingsClick() {
            closeBottomSheet { go<SettingsActivity>() }
        }
    }


    private var mAddLauncherDialog: AddLauncherDialog? = null


    private fun createLauncherIcon() {
        if (mAddLauncherDialog == null) {
            mAddLauncherDialog = AddLauncherDialog(this)
                    .setOnPositiveClickListener {
                        createLauncherShortcut(it.getUrl(), it.getTitle(), it.getIcon())
                    }
                    .setOnSelectListener {
                        pickImage(Type.CODE_GET_IMAGE)
                    }
        }


        mAddLauncherDialog!!
                .setUrl(mPageView.url!!)
                .setIcon(mPageView.favicon)
                .setLabel(mPageView.title)
                .show()
    }


    private var mRefreshMenu: MenuItem? = null
    private var mStopMenu: MenuItem? = null
    private var mConfirmMenu: MenuItem? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_page, menu)
        mRefreshMenu = menu.findItem(R.id.refresh)
        mStopMenu = menu.findItem(R.id.stop)
        mConfirmMenu = menu.findItem(R.id.confirm)

        return true
    }


    private fun showMenu(confirm: Boolean = false, stop: Boolean = false, refresh: Boolean = false) {
        mConfirmMenu?.isVisible = confirm
        mRefreshMenu?.isVisible = refresh
        mStopMenu?.isVisible = stop
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {

                if (mCurrentMode == Type.MODE_WEB) {
                    mPageView.reload()
                }
            }
            R.id.stop -> mPageView.stopLoading()
        }
        return true
    }


    private fun restUiAndStatus() {
        mCurrentMode = Type.MODE_DEFAULT

        //rest optionMenu
        showMenu(refresh = true)

        //rest inputBox
        mInputBox.apply {
            setHint(R.string.hint_input_normal)
            clearFocus()
            setText("")
            hideKeyboard()
        }

        mPageView?.apply {
            onBackPressed()
            clearHistory()
        }

        mPinsRecycler?.visible()
        mRecordRecycler?.visibleDo { it.gone() }
        mProgress?.visibleDo { it.gone() }
    }

    override fun onResume() {
        mPageView.onResume()
        FingerprintService.isForeground = true
        super.onResume()
    }

    override fun onPause() {
        mPageView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        //todo[BUG] 退出应用后，内存并没有得到释放
        mPageView.destroy()
        mInputBox.removeTextChangedListener(mTextWatcher)
        mMenuOptionWidget.setMenuOptionListener(null)
        unregisterReceiver(mNetworkBroadcastReceiver)
        FingerprintService.isForeground = false
        super.onDestroy()
    }

    override fun onBackPressed() {


        if (mCurrentMode == Type.MODE_PIN_EDIT) {
            restUiAndStatus()
            return
        }


        mRecordRecycler?.visibleDo {
            it.gone()
            it.hideKeyboard()
            return
        }


        if (mBottomSheetBehavior.isExpanded()) {
            closeBottomSheet()
            return
        }

        if (mBottomSheetBehavior.isHidden()) {
            mBottomSheetBehavior.collapsed()
        }


        if (mPageView.canGoBack()) {
            mPageView.goBack()
            return
        }

        if (mPageView.isVisible()) {
            mCurrentMode = Type.MODE_DEFAULT
            mPageView.onBackPressed()
            mPinsRecycler?.visible()
            mMenuOptionWidget.hideMoreMenu()
            mInputBox.setText("")
            return
        }


        listAppTask()

//        super.onBackPressed()
    }


    private var mLoadingView: View? = null

    private fun showLoading() {
        if (mLoadingView == null) {
            mLoadingView = mLoadingStub.inflate()
        }

        mLoadingView?.show()
    }


    private fun hideLoading() {
        mLoadingView?.hide()
    }


    private fun captureScreenshot2Bitmap() {
        showLoading()

        mPageView.apply {
            try {
                val saveName = title

                doAsync {
                    val bitmap = toBitmap(getContentWidth().toFloat(), contentHeight.toFloat())
                    val file = bitmap.save(saveName!!)
                    file.mediaScan(this@PageActivity)

                    uiThread {
                        hideLoading()
                        toast(getString(R.string.toast_save_to_path, file.absolutePath))
                    }
                }

            } catch (e: Exception) {
                hideLoading()
                toast(R.string.toast_screenshot_failed)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == Type.CODE_CHOOSE_FILE) {
            mFileChooserCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            return
        }


        if (requestCode == Type.CODE_GET_IMAGE) {
            data?.let {
                try {
                    var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)

                    if (bitmap.width > 192 || bitmap.height > 192) {
                        bitmap = bitmap.scale(192f, 192f)
                    }

                    mAddLauncherDialog?.setIcon(bitmap)

                } catch (e: IOException) {
                    e.printStackTrace()
                    toast(R.string.toast_select_icon_failed)
                }
            }
        }
    }


    private fun jsResponseDialog(url: String, msg: String, jsResult: JsResult) {
        dialogBuilder().setTitle(url.host())
                .setCancelable(false)
                .setMessage(msg)
                .setPositiveButton(R.string.dialog_button_confirm) { dialog, _ ->
                    jsResult.confirm()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_button_cancel) { dialog, _ ->
                    jsResult.cancel()
                    dialog.dismiss()
                }.create().show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event)
        }


        supportN {
            CountDown.with { handleKeyLongPress() }.start()
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            CountDown.cancle()
        }

        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        L.d(TAG, "onKeyLongPress")

        if (handleKeyLongPress()) return true

        return super.onKeyLongPress(keyCode, event)
    }


    private fun handleKeyLongPress(): Boolean {
        if (mCurrentMode == Type.MODE_WEB) {
            //todo[✔] 长按返回时震动
            vibrate()
            restUiAndStatus()
            return true
        }

        return false
    }

    private object CountDown : CountDownTimer(500L, 500L) {

        override fun onFinish() {
            _todo?.invoke()
        }

        override fun onTick(millisUntilFinished: Long) {}

        private var _todo: Func? = null
        fun with(f: Func): CountDown {
            _todo = f
            return this
        }

        fun cancle() {
            _todo = null
        }
    }

    private var mActivityManager: ActivityManager? = null


    private fun listAppTask() {
        if (mActivityManager == null) {
            mActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        }

        mActivityManager!!.appTasks.forEach {
            L.i(TAG, "listAppTask taskInfo id: " + it.taskInfo.id)
            if (it.taskInfo.id == mParentTaskId) {
                finishAndRemoveTask()
                it.moveToFront()
                return
            }
        }

        finishAndRemoveTask()
        super.onBackPressed()
    }
}