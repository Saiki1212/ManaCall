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
import org.json.JSONArray

class MainActivity : AppCompatActivity() {
    private var contactsGranted by mutableStateOf(false)
    private var permissionRequiresSettings by mutableStateOf(false)
    private var settingsError by mutableStateOf(false)
    private var phoneMessage by mutableStateOf<Int?>(null)
    private var pendingCall: CallRequest? = null
    private var favorites by mutableStateOf(emptyMap<String, Boolean>())
    private var recentContacts by mutableStateOf(emptyList<String>())
    private val recentPreferences by lazy { getSharedPreferences("recents", MODE_PRIVATE) }
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
        recentContacts = readRecentContacts()
        val repository = ContactsRepository(contentResolver)
        setContent {
            ManaCall(
                lifecycle = lifecycle,
                repository = repository,
                callHandoff = callHandoff,
                favorites = favorites,
                setFavorite = ::setFavorite,
                recentContacts = recentContacts,
                requestPhoneCall = { key, number -> requestCall(CallRequest.Phone(key, number)) },
                requestWhatsAppVideo = { key, action -> requestCall(CallRequest.WhatsAppVideo(key, action)) },
                phoneMessage = phoneMessage,
                clearPhoneMessage = { phoneMessage = null },
                contactsGranted = contactsGranted,
                permissionRequiresSettings = permissionRequiresSettings,
                requestContacts = ::requestContacts,
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
        if (!granted) {
            phoneMessage = R.string.phone_permission_denied
            return
        }
        val started = when (call) {
            is CallRequest.Phone -> callHandoff.callPhone(call.number)
            is CallRequest.WhatsAppVideo -> callHandoff.startWhatsAppVideo(call.action)
        }
        if (started) {
            recentContacts = listOf(call.key) + recentContacts.filterNot { it == call.key }
            recentPreferences.edit { putString("order", JSONArray(recentContacts).toString()) }
        } else {
            phoneMessage = when (call) {
                is CallRequest.Phone -> R.string.phone_unavailable
                is CallRequest.WhatsAppVideo -> R.string.whatsapp_video_unavailable
            }
        }
    }

    private fun readRecentContacts(): List<String> {
        val saved = recentPreferences.getString("order", null) ?: return emptyList()
        return runCatching {
            val order = JSONArray(saved)
            List(order.length(), order::getString)
        }.getOrDefault(emptyList())
    }

    private sealed interface CallRequest {
        val key: String

        data class Phone(override val key: String, val number: String) : CallRequest
        data class WhatsAppVideo(override val key: String, val action: Intent) : CallRequest
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
