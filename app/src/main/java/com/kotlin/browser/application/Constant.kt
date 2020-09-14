package com.kotlin.browser.application


object Type {
    const val MODE_DEFAULT  = 0x000
    const val MODE_PIN_EDIT = 0x001
    const val MODE_WEB      = 0x002

    const val CODE_CHOOSE_FILE  = 0x003
    const val CODE_GET_IMAGE    = 0x004

    const val UA_DEFAULT    = 0
    const val UA_DESKTOP    = 1
    const val UA_CUSTOM     = 2

    const val SEARCH_GOOGLE = "0"       //Google
    const val SEARCH_DUCKDUCKGO = "1"   //DuckDuckGo
    const val SEARCH_BING = "2"         //Bing
    const val SEARCH_GITHUB = "3"       //Github

    const val EXTRA_SHORTCUT_BROADCAST = "shortcutName"
}