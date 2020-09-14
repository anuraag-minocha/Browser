package com.kotlin.browser.application

import com.kotlin.browser.App
import com.kotlin.browser.R


object SP {
    private val ctx = App.instance


    var adBlock: Boolean by ctx.preference(Key.AD, false)


    var canScreenshot: Boolean by ctx.preference(Key.SCREENSHOT, false)


    var isShowKeyboard: Boolean by ctx.preference(Key.KEYBOARD, false)


    var pinsReverse: Boolean by ctx.preference(Key.PINS_REVERSE, false)


    var omniboxCtrl: Boolean by ctx.preference(Key.OMNIBOX_CTRL, false)


    var omniboxFixed: Boolean by ctx.preference(Key.OMNIBOX_FIXED, true)


    var vibrate: Boolean by ctx.preference(Key.VIBRATE, false)


    var searchEngine: String by ctx.preference(Key.SEARCH_ENGINE, "0")


    var UA: String by ctx.preference(Key.UA, "")


    var enableUA: Boolean by ctx.preference(Key.ENABLE_UA, false)


    val isEnableMultipleWindows: Boolean by ctx.preference(Key.MULTIPLE_WINDOW, true)


    var isOpenInBackground: Boolean by ctx.preference(Key.OPEN_IN_BG, false)


    var isFirstInstall: Boolean by ctx.preference(Key.FIRST_INSTALL, true)

    var hasFingerprintManager: Boolean by ctx.preference(Key.FINGERPRINT, false)

    var textZoom: Int by ctx.preference(Key.TEXT_ZOOM, 100)

    var minimumFontSize: Int by ctx.preference(Key.TEXT_MINIMUM_SIZE, 1)
}

object Key {
    private val ctx = App.instance

    val AD: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_adblock)
    }

    val SCREENSHOT: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_screenshot)
    }

    val KEYBOARD: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_show_soft_keyboard)
    }

    val PINS_REVERSE: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_homepage_reverse)
    }

    val OMNIBOX_CTRL: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_omnibox_control)
    }

    val OMNIBOX_FIXED: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_omnibox_fixed)
    }

    val VIBRATE: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_back_vibrate)
    }

    val UA: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_custom_user_agent)
    }

    val ENABLE_UA: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_enable_user_agent)
    }

    val OPEN_IN_BG: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_open_in_background)
    }

    val SEARCH_ENGINE: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_search_engine_id)
    }

    val MULTIPLE_WINDOW: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_enable_multiple_windows)
    }

    val FIRST_INSTALL: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_first_install)
    }

    val FINGERPRINT: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_fingerprint)
    }

    val TEXT_ZOOM: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_text_zoom)
    }

    val TEXT_MINIMUM_SIZE: String by lazy(LazyThreadSafetyMode.NONE) {
        ctx.resources.getString(R.string.preference_key_text_minimum_size)
    }
}