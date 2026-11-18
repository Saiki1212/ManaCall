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
import com.saiki1212.manacall.contacts.Contact
import com.saiki1212.manacall.contacts.ContactsRepository
import com.saiki1212.manacall.ui.ManaCall

class MainActivity : AppCompatActivity() {
    private var contactsGranted by mutableStateOf(false)
    private var permissionRequiresSettings by mutableStateOf(false)
    private var whatsappAvailable by mutableStateOf(false)
    private var settingsError by mutableStateOf(false)
    private var phoneMessage by mutableStateOf<Int?>(null)
    private var pendingCall: CallRequest? = null
    private var favorites by mutableStateOf(emptyMap<String, Boolean>())
    private val favoritePreferences by lazy { getSharedPreferences("favorites", MODE_PRIVATE) }
    private val callHandoff by lazy { CallHandoff(this) }

    private val contactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { updatePermission() }

    private val phonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val call = pendingCall
        pendingCall = null
        if (call != null) completeCall(call, granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
        favorites = favoritePreferences.all.mapNotNull { (key, value) ->
            (value as? Boolean)?.let { key to it }
        }.toMap()
        val repository = ContactsRepository(contentResolver)
        setContent {
            ManaCall(
                lifecycle = lifecycle,
                repository = repository,
                callHandoff = callHandoff,
                favorites = favorites,
                setFavorite = ::setFavorite,
                requestPhoneCall = { requestCall(CallRequest.Phone(it)) },
                requestWhatsAppCall = ::requestWhatsAppCall,
                phoneMessage = phoneMessage,
                clearPhoneMessage = { phoneMessage = null },
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

    private fun setFavorite(contact: Contact, favorite: Boolean) {
        val key = contact.key
        if (favorite == contact.favorite) {
            favoritePreferences.edit { remove(key) }
            favorites = favorites - key
        } else {
            favoritePreferences.edit { putBoolean(key, favorite) }
            favorites = favorites + (key to favorite)
        }
    }

    private fun requestWhatsAppCall(action: Intent?, digits: String) {
        if (action == null) {
            openWhatsAppChat(digits)
        } else {
            requestCall(CallRequest.WhatsApp(action, digits))
        }
    }

    private fun requestCall(call: CallRequest) {
        if (pendingCall != null) return
        if (callHandoff.hasPhonePermission()) {
            completeCall(call, granted = true)
        } else {
            pendingCall = call
            phonePermission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun completeCall(call: CallRequest, granted: Boolean) {
        when (call) {
            is CallRequest.Phone -> {
                if (!granted) {
                    phoneMessage = R.string.phone_permission_denied
                } else if (!callHandoff.callPhone(call.number)) {
                    phoneMessage = R.string.phone_unavailable
                }
            }
            is CallRequest.WhatsApp -> {
                if (!granted || !callHandoff.startWhatsAppAction(call.action)) openWhatsAppChat(call.digits)
            }
        }
    }

    private fun openWhatsAppChat(digits: String) {
        if (!callHandoff.openWhatsAppChat(digits)) {
            phoneMessage = if (callHandoff.hasWhatsApp()) {
                R.string.whatsapp_open_failed
            } else {
                R.string.whatsapp_unavailable
            }
        }
    }

    private sealed interface CallRequest {
        data class Phone(val number: String) : CallRequest
        data class WhatsApp(val action: Intent, val digits: String) : CallRequest
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
