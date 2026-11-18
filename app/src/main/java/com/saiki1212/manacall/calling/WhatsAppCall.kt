package com.saiki1212.manacall.calling

internal enum class WhatsAppCall(val mimeType: String) {
    Voice("vnd.android.cursor.item/vnd.com.whatsapp.voip.call"),
    Video("vnd.android.cursor.item/vnd.com.whatsapp.video.call"),
}

private val phoneAddress = Regex("\\+?([1-9][0-9]{5,14})(?:@s\\.whatsapp\\.net)?")
private val linkedAddress = Regex("[0-9]+@lid")
private val displayedPhone = Regex("\\+[1-9][0-9() .\\p{Zs}-]*[0-9]")

// A linked contact can have multiple numbers. Only hand off a row tied to the selected one.
internal fun matchesWhatsAppRecipient(address: String?, detail: String?, internationalDigits: String): Boolean {
    val recipient = address?.trim() ?: return false
    val phone = phoneAddress.matchEntire(recipient)?.groupValues?.get(1)
    if (phone != null) return phone == internationalDigits
    if (!linkedAddress.matches(recipient) || detail == null) return false
    val number = displayedPhone.findAll(detail).singleOrNull() ?: return false
    return number.value.filter { it in '0'..'9' } == internationalDigits
}
