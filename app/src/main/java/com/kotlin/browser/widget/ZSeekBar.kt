package com.kotlin.browser.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import com.kotlin.browser.R

class ZSeekBar : SeekBar {

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        getAttrs(attr)
    }

    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr) {
        getAttrs(attr)
    }

    private var mStep: Int = 1
    private var mMin: Int = 0
    private var _progress: Int = 1

    init {
        super.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                _progress = progress * mStep + mMin
                _listener?.invoke(_progress)
                mSeekChangeListener?.onProgressChanged(seekBar, _progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mSeekChangeListener?.onStartTrackingTouch(seekBar)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mSeekChangeListener?.onStopTrackingTouch(seekBar)
            }
        })
    }

    private fun getAttrs(attr: AttributeSet) {
        context.obtainStyledAttributes(attr, R.styleable.ZSeekBar).apply {
            mStep = getInteger(R.styleable.ZSeekBar_step, 1)
            mMin = getInteger(R.styleable.ZSeekBar_min, 0)
            recycle()
        }
    }

    private var mSeekChangeListener: OnSeekBarChangeListener? = null
    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        mSeekChangeListener = l
    }

    private var _listener: ((Int) -> Unit)? = null
    fun setOnSeekBarChangeListener(f: (Int) -> Unit) {
        _listener = f
    }

    fun getStep() = mStep


    fun setStep(step: Int) {
        mStep = step
    }

    fun min() = mMin


    fun min(min: Int){
        mMin = min
    }


    fun progress() = _progress


    fun progress(p: Int){
        progress = (p - mMin) / mStep
    }
}