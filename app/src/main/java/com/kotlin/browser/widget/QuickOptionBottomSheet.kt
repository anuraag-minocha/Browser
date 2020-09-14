package com.kotlin.browser.widget

import android.content.Context
import android.widget.Switch
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kotlin.browser.R
import com.kotlin.browser.application.*
import kotlinx.android.synthetic.main.layout_quick_option.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread


class QuickOptionDialog(context: Context) : BottomSheetDialog(context, R.style.AppTheme_BottomSheetDialog) {

    init {
        setContentView(R.layout.layout_quick_option)

        initView()

        initEvent()
    }


    private lateinit var mOptionSwitch: Switch


    private lateinit var mNewTab: TextView


    private lateinit var mNewPrivateTab: TextView


    private lateinit var mDownloadImg: TextView


    private lateinit var mExtractQRImg: TextView


    private lateinit var mCopyUrl: TextView


    private lateinit var mShareUrl: TextView

    private fun initView() {
        mOptionSwitch = optionSwitch
        mNewTab = newTab
        mNewPrivateTab = privateTab
        mDownloadImg = downloadImg
        mExtractQRImg = decodeQRImg
        mCopyUrl = copyUrl
        mShareUrl = shareUrl
    }

    private var _method: ((String) -> Unit)? = null
        get() {
            val m = field
            field = null
            return m
        }

    private fun initEvent() {

        //set switch event
        mOptionSwitch.isChecked = SP.isOpenInBackground
        mOptionSwitch.setOnCheckedChangeListener { _, isChecked ->
            SP.isOpenInBackground = isChecked
            context?.toast(if (isChecked) R.string.options_background else R.string.options_foreground)
        }

        //set new tab option click even
        mNewTab.setOnClickListener {
            baseCallback?.run { _method = quickNewTab }
            dismiss()
        }

        //set new private tab option click even
        mNewPrivateTab.setOnClickListener {
            baseCallback?.run { _method = quickNewPrivateTab }
            dismiss()
        }

        //set download image option click even
        mDownloadImg.setOnClickListener {
            baseCallback?.run { _method = quickDownloadImg }
            dismiss()
        }

        //set extract QR code option click event
        mExtractQRImg.setOnClickListener {
            baseCallback?.quickExtractQR?.invoke(mExtractRes)
            dismiss()
        }

        //set copyToClipboard url option click even
        mCopyUrl.setOnClickListener {
            baseCallback?.run { _method = quickCopyUrl }
            dismiss()
        }

        //set share url option click even
        mShareUrl.setOnClickListener {
            baseCallback?.run { _method = quickShareUrl }
            dismiss()
        }

        //set dismiss listener
        setOnDismissListener { _method?.invoke(_url) }
    }

    private var _url = ""


    fun setUrl(url: String) = apply {
        _url = url.trim()
        parseUrl(url)
    }

    private var isQRImage = false
    private var mExtractRes = ""
    private fun parseUrl(url: String) {
        doAsync {
            val res = QR.decodeUrl(url)
            if (res == null) {
                isQRImage = false
                mExtractRes = ""
                uiThread { mExtractQRImg.gone() }
            } else {
                isQRImage = true
                mExtractRes = res
                uiThread { mExtractQRImg.visible() }
            }
        }
    }


    fun isImageUrl(bool: Boolean) = apply {
        if (bool) {
            mDownloadImg.visible()
        } else {
            mDownloadImg.gone()
        }
    }

    private var baseCallback: QuickCallbackWrap? = null


    fun setQuickListener(init: (QuickCallbackWrap.() -> Unit)) = apply {
        baseCallback = QuickCallbackWrap()
        baseCallback!!.context = context
        baseCallback!!.init() // 执行闭包，完成数据填充
    }
}

class QuickCallbackWrap {

    internal lateinit var context: Context

    var quickNewTab: Func1<String> = {}
    var quickNewPrivateTab: Func1<String> = {}
    var quickDownloadImg: Func1<String> = {}
    var quickExtractQR: Func1<String> = {}
    var quickCopyUrl: Func1<String> = {
        context.copyToClipboard(it)
    }
    var quickShareUrl: Func1<String> = {
        context.shareText(it)
    }


}
