package com.kotlin.browser.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import com.kotlin.browser.application.*
import kotlinx.android.synthetic.main.layout_page_menu.view.*


class MenuOptionLayout : NestedScrollView, SharedPreferences.OnSharedPreferenceChangeListener {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var mListener: MenuOptionListener? = null

    fun setMenuOptionListener(listener: MenuOptionListener?) {
        mListener = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.defaultSharePreferences().registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.defaultSharePreferences().unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Key.UA) {
            initUserAgent()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()


        mScreenshotMenu.setOnClickListener {
            mListener?.onScreenshotsClick()
        }


        mShareUrlMenu.setOnClickListener {
            mListener?.onShareUrlClick()
        }


        mDesktopSwitch.setOnCheckedChangeListener { _, isChecked ->
            SP.enableUA = isChecked
            mListener?.onDesktopCheckedChanged(isChecked)
        }


        mCustomUASwitch.setOnCheckedChangeListener { _, isChecked ->
            SP.enableUA = isChecked
            mListener?.onCustomUACheckedChanged(isChecked)
        }


        mNewTabMenu.setOnClickListener {
            mListener?.onNewTabClick()
        }


        mNewPrivateTabMenu.setOnClickListener {
            mListener?.onPrivateTabClick()
        }


        mPin2HomeMenu.setOnClickListener {
            mListener?.onPinToHome()
        }


        mAdd2LauncherMenu.setOnClickListener {
            mListener?.addToLauncher()
        }


        mSettingsMenu.setOnClickListener {
            mListener?.onSettingsClick()
        }

        initUserAgent()
    }

    private fun initUserAgent() {
        if (SP.UA.isEmpty()) {
            mDesktopSwitch.visible()
            mCustomUASwitch.gone()
            mDesktopSwitch.isChecked = SP.enableUA
        } else {
            mDesktopSwitch.gone()
            mCustomUASwitch.visible()
            mCustomUASwitch.isChecked = SP.enableUA
        }
    }


    fun showMoreMenu() {
        mShareUrlMenu.visible()
    }


    fun hideMoreMenu() {
        mShareUrlMenu.gone()
    }
}

interface MenuOptionListener {
    fun onDesktopCheckedChanged(check: Boolean)

    fun onCustomUACheckedChanged(check: Boolean)

    fun onScreenshotsClick()

    fun onShareUrlClick()

    fun onNewTabClick()

    fun onPrivateTabClick()

    fun onPinToHome()

    fun addToLauncher()

    fun onSettingsClick()
}