package com.anthropic.claudemonitor.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.anthropic.claudemonitor.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
