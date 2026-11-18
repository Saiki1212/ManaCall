package com.saiki1212.manacall.calling

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import com.saiki1212.manacall.contacts.PhoneNumber
import java.util.Locale

internal class CallHandoff(private val context: Context) {
    fun openPhone(number: PhoneNumber): Boolean =
        launch(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number.value, null)))

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

    fun openWhatsApp(internationalDigits: String): Boolean {
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
        val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        val internationalNumber = Regex("\\+[1-9][0-9]{5,14}")
    }
}
