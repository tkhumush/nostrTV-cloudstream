package com.lagradost.cloudstream3.ui.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.nostr.NostrConstants
import com.lagradost.cloudstream3.nostr.crypto.NostrKeyManager

class SettingsNostr : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_nostr, rootKey)

        // Enable/Disable Nostr
        findPreference<SwitchPreferenceCompat>("nostr_enabled")?.apply {
            isChecked = getKey(NostrConstants.NOSTR_ENABLED_KEY) ?: true
            setOnPreferenceChangeListener { _, newValue ->
                setKey(NostrConstants.NOSTR_ENABLED_KEY, newValue as Boolean)
                true
            }
        }

        // Display public key (read-only)
        findPreference<EditTextPreference>("nostr_public_key")?.apply {
            val pubkey = NostrKeyManager.getPublicKey(requireContext())
            summary = if (pubkey.isNullOrEmpty()) {
                "Not generated yet"
            } else {
                "${pubkey.take(16)}...${pubkey.takeLast(16)}"
            }
            isEnabled = false
        }

        // Relay list
        findPreference<EditTextPreference>("nostr_relays")?.apply {
            val savedRelays = getKey<String>(NostrConstants.NOSTR_RELAYS_KEY)
            val currentRelays = savedRelays ?: NostrConstants.DEFAULT_RELAYS.joinToString("\n")

            text = currentRelays
            summary = "${currentRelays.split("\n").size} relays configured"

            setOnPreferenceChangeListener { _, newValue ->
                val relayList = newValue as String
                setKey(NostrConstants.NOSTR_RELAYS_KEY, relayList)
                summary = "${relayList.split("\n").filter { it.isNotBlank() }.size} relays configured"
                true
            }
        }

        // Reset keypair
        findPreference<Preference>("reset_keypair")?.apply {
            setOnPreferenceClickListener {
                showResetKeypairDialog()
                true
            }
        }

        // Export private key
        findPreference<Preference>("export_private_key")?.apply {
            setOnPreferenceClickListener {
                showExportPrivateKeyDialog()
                true
            }
        }
    }

    private fun showResetKeypairDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset Nostr Keypair?")
            .setMessage("This will generate a new Nostr identity. Your old identity will be lost forever. Are you sure?")
            .setPositiveButton("Reset") { _, _ ->
                NostrKeyManager.deleteKeypair(requireContext())
                NostrKeyManager.generateAndStoreKeypair(requireContext())

                // Update public key display
                findPreference<EditTextPreference>("nostr_public_key")?.apply {
                    val pubkey = NostrKeyManager.getPublicKey(requireContext())
                    summary = if (pubkey != null) {
                        "${pubkey.take(16)}...${pubkey.takeLast(16)}"
                    } else {
                        "Not generated yet"
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExportPrivateKeyDialog() {
        val privateKey = NostrKeyManager.getPrivateKey(requireContext())

        if (privateKey == null) {
            android.widget.Toast.makeText(
                requireContext(),
                "No private key found",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Your Private Key")
            .setMessage("KEEP THIS SECRET!\n\nnsec: $privateKey\n\nAnyone with this key can impersonate you.")
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Nostr Private Key", privateKey)
                clipboard.setPrimaryClip(clip)

                android.widget.Toast.makeText(
                    requireContext(),
                    "Private key copied to clipboard",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
