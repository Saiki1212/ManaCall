package com.saiki1212.manacall.contacts

import android.net.Uri

internal data class Contact(
    val key: String,
    val name: String?,
    val photo: Uri?,
    val thumbnail: Uri?,
    val favorite: Boolean,
    val numbers: List<PhoneNumber>,
) {
    val hasPhoto: Boolean get() = photo != null || thumbnail != null
}

internal data class PhoneNumber(
    val value: String,
    val normalized: String?,
    val labelType: Int,
    val customLabel: String?,
    val priority: Int,
)
