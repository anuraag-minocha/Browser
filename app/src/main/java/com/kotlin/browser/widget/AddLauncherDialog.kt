package com.kotlin.browser.widget

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_add_to_launcher.*
import com.kotlin.browser.R
import com.kotlin.browser.application.Func
import com.kotlin.browser.application.Func1
import com.kotlin.browser.application.addTextWatcher


class AddLauncherDialog(context: Context) : Dialog(context, R.style.AppTheme_Dialog) {

    private var mNegativeBtn: TextView? = null
    private var mPositiveBtn: TextView? = null
    private var mLauncherIcon: ImageView? = null
    private var mSelectBtn: TextView? = null
    private var mLabelEdit: EditText? = null

    init {
        setContentView(R.layout.layout_add_to_launcher)

        initView()

        initEvent()
    }


    private var url: String = ""

    fun setUrl(url: String): AddLauncherDialog {
        this.url = url
        return this
    }

    fun getUrl() = url


    private var mLabelText: CharSequence? = null


    fun getTitle() = mLabelText?.toString() ?: ""

    fun setLabel(title: CharSequence?) = apply { mLabelText = title }


    override fun setTitle(titleId: Int) {
        mLabelText = context.resources.getString(titleId)
    }


    override fun setTitle(title: CharSequence?) {
        mLabelText = title
    }


    private var mLauncherBitmap: Bitmap? = null


    fun getIcon(): Bitmap = mLauncherBitmap
            ?: Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)


    fun setIcon(bitmap: Bitmap?) = apply {
        mLauncherBitmap = bitmap
        mLauncherBitmap?.let { mLauncherIcon?.setImageBitmap(it) }
    }

    private fun initView() {
        mLauncherIcon = icon
        mSelectBtn = select
        mLabelEdit = label
        mNegativeBtn = negativeBtn
        mPositiveBtn = positiveBtn
    }

    private fun initEvent() {
        mSelectBtn?.setOnClickListener {
            _selectListener?.invoke()
        }
        mLauncherIcon?.setOnClickListener {
            _selectListener?.invoke()
        }

        mNegativeBtn?.setOnClickListener {
            mNegativeListener?.invoke(this)
            dismiss()
        }

        mPositiveBtn?.setOnClickListener {
            mPositiveListener?.invoke(this)
            dismiss()
        }

        mLabelEdit?.addTextWatcher {
            mPositiveBtn?.isEnabled = it.isNotEmpty()
        }
    }


    private var _selectListener: Func? = null


    fun setOnSelectListener(todo: Func) = apply { _selectListener = todo }

    private var mPositiveListener: Func1<AddLauncherDialog>? = null
    private var mPositiveBtnText: String = "确定"
    fun setOnPositiveClickListener(text: String = "确定", todo: Func1<AddLauncherDialog>) = apply {
        mPositiveBtnText = text
        mPositiveListener = todo
    }

    private var mNegativeListener: Func1<AddLauncherDialog>? = null
    private var mNegativeBtnText: String = "取消"
    fun setOnNegativeClickListener(text: String = "取消", todo: Func1<AddLauncherDialog>) = apply {
        mNegativeBtnText = text
        mNegativeListener = todo
    }

    override fun show() {
        if (mLabelText != null) {
            mLabelEdit?.setText(mLabelText)
        }

        if (mLauncherBitmap != null) {
            mLauncherIcon?.setImageBitmap(mLauncherBitmap)
        }

        if (mNegativeBtnText.isNotEmpty()) {
            mNegativeBtn?.text = mNegativeBtnText
        }
        if (mPositiveBtnText.isNotEmpty()) {
            mPositiveBtn?.text = mPositiveBtnText
        }

        super.show()
    }
}