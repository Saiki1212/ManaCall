package com.saiki1212.manacall

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.saiki1212.manacall.calling.CallHandoff
import com.saiki1212.manacall.contacts.ContactsRepository
import com.saiki1212.manacall.ui.ManaCall

class MainActivity : AppCompatActivity() {
    private var contactsGranted by mutableStateOf(false)
    private var permissionRequiresSettings by mutableStateOf(false)
    private var whatsappAvailable by mutableStateOf(false)
    private var settingsError by mutableStateOf(false)
    private val callHandoff by lazy { CallHandoff(this) }

    private val contactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { updatePermission() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
        val repository = ContactsRepository(contentResolver)
        setContent {
            ManaCall(
                lifecycle = lifecycle,
                repository = repository,
                callHandoff = callHandoff,
                contactsGranted = contactsGranted,
                permissionRequiresSettings = permissionRequiresSettings,
                requestContacts = ::requestContacts,
                whatsappAvailable = whatsappAvailable,
                setLanguage = { tag ->
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                },
                settingsError = settingsError,
                clearSettingsError = { settingsError = false },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermission()
        whatsappAvailable = callHandoff.hasWhatsApp()
    }

    private fun updatePermission() {
        contactsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        permissionRequiresSettings = !contactsGranted &&
            getPreferences(MODE_PRIVATE).getBoolean("contacts_asked", false) &&
            !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
    }

    private fun requestContacts() {
        if (permissionRequiresSettings) {
            val settingsIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            )
            try {
                startActivity(settingsIntent)
            } catch (_: ActivityNotFoundException) {
                settingsError = true
            }
        } else {
            getPreferences(MODE_PRIVATE).edit { putBoolean("contacts_asked", true) }
            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }
}
