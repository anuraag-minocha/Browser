package com.kotlin.browser

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.kotlin.browser.application.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class SettingsActivity : BaseActivity() {

    private lateinit var mFragment: SettingPreferenceFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableBackPress()

        val beginTransaction = fragmentManager.beginTransaction()
        mFragment = SettingPreferenceFragment()

        beginTransaction.replace(android.R.id.content, mFragment)
        beginTransaction.commit()
    }

    override fun onBackPressed() {
        if (!mFragment.isWaiting()) {
            super.onBackPressed()
        }
    }
}

class SettingPreferenceFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var isWaiting = false
    fun isWaiting() = isWaiting

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        setSearchSummary()
        super.onResume()
    }

    override fun onPause() {
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference?): Boolean {
       when(preference?.titleRes) {
           R.string.preference_title_clear_history -> Snackbar(R.string.toast_clear_history) {
               doAsync {
                   SQLHelper.clearAllRecord()
                   uiThread { toast(R.string.toast_clear_history_success) }
               }
           }
       }
        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

        when (key) {

            Key.SEARCH_ENGINE -> {
                setSearchSummary()
            }
        }
    }


    private fun setSearchSummary() {
        findPreference(getString(R.string.preference_key_search_engine_id)).summary = resources
                .getStringArray(R.array.preference_entries_search_engine_id)[SP.searchEngine.toInt()]
    }

    private fun toast(msg: Int) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    private fun Snackbar(msg: Int, confirm: Func) {
        Snackbar.make(view!!, msg, Snackbar.LENGTH_LONG)
                .setAction(R.string.dialog_button_confirm) {
                    confirm()
                }.show()
    }

}