package com.anthropic.claudemonitor.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.anthropic.claudemonitor.R
import com.anthropic.claudemonitor.data.api.ApiClientFactory
import com.anthropic.claudemonitor.data.api.Result
import com.anthropic.claudemonitor.data.api.UsageRepository
import com.anthropic.claudemonitor.util.Prefs
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("plan_tier")?.setOnPreferenceChangeListener { _, _ ->
            // Clear custom limit when tier changes
            findPreference<EditTextPreference>("plan_limit")?.text = ""
            true
        }

        findPreference<Preference>("test_connection")?.setOnPreferenceClickListener {
            testConnection()
            true
        }
    }

    private fun testConnection() {
        val ctx = requireContext()
        lifecycleScope.launch {
            val repo = UsageRepository(ApiClientFactory.create(Prefs.serverUrl(ctx)))
            when (val result = repo.checkHealth()) {
                is Result.Success ->
                    Toast.makeText(ctx, "Connected!", Toast.LENGTH_SHORT).show()
                is Result.Error   ->
                    Toast.makeText(ctx, "Failed: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
