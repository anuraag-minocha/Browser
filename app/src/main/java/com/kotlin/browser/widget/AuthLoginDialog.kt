package com.kotlin.browser.widget

import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_auth_login.*
import com.kotlin.browser.R
import com.kotlin.browser.application.Func1
import com.kotlin.browser.application.visible


class AuthLoginDialog(context: Context) : Dialog(context, R.style.AppTheme_Dialog) {

    init {
        setContentView(R.layout.layout_auth_login)

        initView()

        initEvent()
    }


    private var mTitleText: CharSequence? = null


    private lateinit var mTitle: TextView


    private lateinit var mUserNameEdit: EditText


    private lateinit var mPasswordEdit: EditText


    private lateinit var mLoginBtn: Button


    private lateinit var mCancelBtn: Button

    private fun initView() {
        mTitle = title
        mUserNameEdit = username
        mPasswordEdit = password
        mLoginBtn = loginBtn
        mCancelBtn = cancelBtn
    }


    override fun setTitle(titleId: Int) {
        mTitleText = context.resources.getString(titleId)
    }


    override fun setTitle(title: CharSequence?) {
        mTitleText = title
    }


    fun getUserName(): String = mUserNameEdit.text.toString().trim()


    fun getPassword(): String = mPasswordEdit.text.toString().trim()

    private var _method: Func1<AuthLoginDialog>? = null
    private fun initEvent() {

        setCancelable(false)

        //set download image option click even
        mLoginBtn.setOnClickListener {
            if (validate()) {
                _method = mPositiveListener
                dismiss()
            }
        }

        //set copyToClipboard url option click even
        mCancelBtn.setOnClickListener {
            _method = mNegativeListener
            dismiss()
        }

        //set dismiss listener
        setOnDismissListener {
            _method?.invoke(this)
            _method = null
        }
    }


    private fun validate(): Boolean {
        val name = mUserNameEdit.text.toString().trim()
        if (name.isEmpty()) {
            mUserNameEdit.apply {
                error = hint
                requestFocus()
            }

            return false
        }

        val pass = mPasswordEdit.text.toString().trim()
        if (pass.isEmpty()) {
            mPasswordEdit.apply {
                error = hint
                requestFocus()
            }
            return false
        }

        return true
    }

    override fun show() {
        mTitleText?.let {
            if (it.isNotEmpty()) {
                mTitle.text = mTitleText
                mTitle.visible()
            }
        }

        super.show()
    }

    private var mPositiveListener: Func1<AuthLoginDialog>? = null
    fun setOnPositiveClickListener(todo: Func1<AuthLoginDialog>?) = apply { mPositiveListener = todo }

    private var mNegativeListener: Func1<AuthLoginDialog>? = null
    fun setOnNegativeClickListener(todo: Func1<AuthLoginDialog>?) = apply { mNegativeListener = todo }
}
