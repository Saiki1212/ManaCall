package com.saiki1212.manacall.calling

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.saiki1212.manacall.contacts.PhoneNumber
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class CallHandoff(private val context: Context) {
    fun hasPhonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    fun callPhone(number: String): Boolean {
        if (!hasPhonePermission()) return false
        return launch(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)))
    }

    fun hasWhatsApp(): Boolean = whatsappPackages.any(::isEnabled)

    fun whatsappNumber(number: PhoneNumber): String? {
        val normalized = number.normalized
        if (normalized != null && internationalNumber.matches(normalized)) {
            return normalized.drop(1)
        }

        val dialString = PhoneNumberUtils.stripSeparators(number.value).orEmpty()
        if (internationalNumber.matches(dialString)) return dialString.drop(1)
        if (dialString.isEmpty() || !dialString.all { it in '0'..'9' }) return null

        val telephony = context.getSystemService(TelephonyManager::class.java)
        val regions = listOf(
            runCatching { telephony?.simCountryIso }.getOrNull(),
            runCatching { telephony?.networkCountryIso }.getOrNull(),
            Resources.getSystem().configuration.locales[0]?.country,
        ).filterNotNull().map { it.uppercase(Locale.ROOT) }.filter { it.length == 2 }

        // Only use the first available region. Trying more regions can select a different person.
        val region = regions.firstOrNull() ?: return null
        return PhoneNumberUtils.formatNumberToE164(dialString, region)
            ?.takeIf(internationalNumber::matches)?.drop(1)
    }

    suspend fun findWhatsAppAction(
        contactId: Long,
        internationalDigits: String,
        call: WhatsAppCall,
    ): Intent? = withContext(Dispatchers.IO) {
        if (!isEnabled(whatsapp)) return@withContext null
        try {
            val selection = "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} = ? AND ${RawContacts.ACCOUNT_TYPE} = ?"
            val cursor = context.contentResolver.query(
                Data.CONTENT_URI,
                arrayOf(Data._ID, Data.DATA1, Data.DATA3),
                selection,
                arrayOf(contactId.toString(), call.mimeType, whatsapp),
                null,
            )
            cursor?.use {
                while (it.moveToNext()) {
                    if (matchesWhatsAppRecipient(it.getString(1), it.getString(2), internationalDigits)) {
                        val uri = ContentUris.withAppendedId(Data.CONTENT_URI, it.getLong(0))
                        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, call.mimeType).setPackage(whatsapp)
                        return@use intent.takeIf { action -> action.resolveActivity(context.packageManager) != null }
                    }
                }
                null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            // Some device contact providers do not expose third-party call rows.
            null
        } catch (_: SQLiteException) {
            null
        }
    }

    fun startWhatsAppAction(action: Intent): Boolean = launch(action)

    fun openWhatsAppChat(internationalDigits: String): Boolean {
        val uri = Uri.Builder().scheme("https").authority("wa.me")
            .appendPath(internationalDigits).build()
        return whatsappPackages.any { app ->
            isEnabled(app) && launch(Intent(Intent.ACTION_VIEW, uri).setPackage(app))
        }
    }

    @Suppress("DEPRECATION")
    private fun isEnabled(packageName: String): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private companion object {
        const val whatsapp = "com.whatsapp"
        val whatsappPackages = listOf(whatsapp, "com.whatsapp.w4b")
        val internationalNumber = Regex("\\+[1-9][0-9]{5,14}")
    }
}
