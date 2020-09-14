package com.kotlin.browser

data class Pin(
        var _id: Int = 0,
        var title: String,
        val url: String,
        val time: String = System.currentTimeMillis().toString(),
        val visit: Int = 0
)

data class Record(
        val title: String,
        val url: String,
        val time: String,
        val visit: Int = 0
)