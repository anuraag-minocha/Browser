package com.kotlin.browser.application

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Message
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat


class FingerprintService : AccessibilityService() {

    private val TAG = "FingerprintService-->"
    private val ACTION_RECENT = 0
    private val ACTION_SCREEN_SPLIT = 1

    companion object {
        var isForeground = false
    }

    override fun onInterrupt() {
        L.i(TAG, "onInterrupt")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        L.i(TAG, "event: $event")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        initFingerprintManager()
        registerFingerprint()

        L.i(TAG, "Service started")
    }

    override fun onDestroy() {
        super.onDestroy()
        L.i(TAG, "Service destroy")
    }

    private lateinit var mFingerprintManager: FingerprintManager
    private lateinit var mCancellationSignal: CancellationSignal


    @SuppressLint("InlinedApi")
    private fun initFingerprintManager() {

        checkFingerprintManager()


        if (SP.hasFingerprintManager) {
            mFingerprintManager = getSystemService(Application.FINGERPRINT_SERVICE) as FingerprintManager
            L.i(TAG, "init fingerprint manager")

            initHandler()

        } else {
            L.i(TAG, "have no fingerprint manager")
        }
    }


    @SuppressLint("NewApi")
    private fun registerFingerprint() {

        if (!SP.hasFingerprintManager) {
            L.w(TAG, "have no fingerprint manager")
            return
        }

        if (!hadFingerprintPermission()) {
            L.w(TAG, "have no permission")
            return
        }

        if (mFingerprintManager.hasEnrolledFingerprints()) {
            L.i(TAG, "Fingerprint has enrolled")
            mCancellationSignal = CancellationSignal()
            mFingerprintManager.authenticate(null, mCancellationSignal, 0, _authenticationCallback, null)
        } else {
            L.w(TAG, "Fingerprint have no enrolled")
        }
    }


    @SuppressLint("InlinedApi")
    private fun hadFingerprintPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED
    }


    private fun checkFingerprintManager() {
//        if (!SP.isFirstInstall) return

        try {

            Class.forName("android.hardware.fingerprint.FingerprintManager")
            L.i(TAG, "exist fingerprintManager")
            SP.hasFingerprintManager = true
        } catch (e: Exception) {
            e.printStackTrace()
            SP.hasFingerprintManager = false
        }
    }


    private val _authenticationCallback = object : FingerprintManager.AuthenticationCallback() {

        private val TAG = "AuthenticationCallback-->"

        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
            super.onAuthenticationError(errMsgId, errString)
            L.w(TAG, "onAuthenticationError: $errString")
            mHandler.sendEmptyMessageDelayed(0, DELAY)
            cancelSignal()
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpMsgId, helpString)
            L.i(TAG, "onAuthenticationHelp: $helpString")

            doAction(ACTION_RECENT)
        }

        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            L.i(TAG, "onAuthenticationSucceeded: ${result.toString()}")
            mHandler.sendEmptyMessageDelayed(0, DELAY)

            doAction(ACTION_SCREEN_SPLIT)
        }


        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            L.w(TAG, "onAuthenticationFailed")

            doAction(ACTION_SCREEN_SPLIT)
        }
    }


    @SuppressLint("NewApi")
    private fun cancelSignal() {
        if (mFingerprintManager.isHardwareDetected) {
            mCancellationSignal.cancel()
        }
    }

    private val DELAY = 10000L // 10s


    private lateinit var mHandler: Handler

    private fun initHandler() {
        mHandler = object : Handler() {
            @SuppressLint("NewApi")
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                L.i("Handler-->", "restart fingerprint module")
                mFingerprintManager.authenticate(null, mCancellationSignal, 0, _authenticationCallback, mHandler)
            }
        }
    }


    private fun doAction(action: Int) {

        if (!isForeground) return

        when (action) {
            ACTION_RECENT -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

            ACTION_SCREEN_SPLIT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                } else {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                }
            }
        }
    }

}