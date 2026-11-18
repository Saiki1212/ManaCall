package com.saiki1212.manacall.contacts

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.SystemClock
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.telephony.PhoneNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

internal data class ContactsSnapshot(
    val contacts: List<Contact>,
    val photoRevision: Long,
)

internal class ContactsRepository(private val resolver: ContentResolver) {
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(ContactsContract.AUTHORITY_URI, true, observer)
        trySend(Unit)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate()

    suspend fun load(): ContactsSnapshot = withContext(Dispatchers.IO) {
        val contacts = readContacts()
        val numbers = if (contacts.isEmpty()) emptyMap() else readNumbers()
        ContactsSnapshot(
            contacts = contacts.map { (id, contact) ->
                contact.copy(numbers = numbers[id].orEmpty())
            },
            photoRevision = SystemClock.elapsedRealtimeNanos(),
        )
    }

    private fun readContacts(): Map<Long, Contact> {
        val columns = arrayOf(
            Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY,
            Contacts.PHOTO_URI, Contacts.PHOTO_THUMBNAIL_URI, Contacts.STARRED,
        )
        val cursor = checkNotNull(resolver.query(
            Contacts.CONTENT_URI, columns, null, null,
            "${Contacts.SORT_KEY_PRIMARY} ASC",
        ))
        return cursor.use {
            buildMap {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    put(id, Contact(
                        id = id,
                        key = it.getString(1)?.takeUnless(String::isBlank) ?: "id:$id",
                        name = it.getString(2)?.takeUnless(String::isBlank),
                        photo = it.getString(3)?.takeUnless(String::isBlank)?.let(Uri::parse),
                        thumbnail = it.getString(4)?.takeUnless(String::isBlank)?.let(Uri::parse),
                        favorite = it.getInt(5) != 0,
                        numbers = emptyList(),
                    ))
                }
            }
        }
    }

    private fun readNumbers(): Map<Long, List<PhoneNumber>> {
        val columns = arrayOf(
            Phone.CONTACT_ID, Phone.NUMBER, Phone.NORMALIZED_NUMBER, Phone.TYPE,
            Phone.LABEL, Phone.IS_SUPER_PRIMARY, Phone.IS_PRIMARY,
        )
        val cursor = checkNotNull(resolver.query(
            Phone.CONTENT_URI, columns, null, null, "${Phone._ID} ASC",
        ))
        val numbers = mutableMapOf<Long, MutableList<PhoneNumber>>()
        cursor.use {
            while (it.moveToNext()) {
                val value = it.getString(1)?.trim()?.takeIf { number -> number.any(Char::isDigit) }
                    ?: continue
                numbers.getOrPut(it.getLong(0), ::mutableListOf).add(PhoneNumber(
                    value = value,
                    normalized = it.getString(2)?.takeUnless(String::isBlank),
                    labelType = it.getInt(3),
                    customLabel = it.getString(4),
                    priority = 2 * it.getInt(5) + it.getInt(6),
                ))
            }
        }
        return numbers.mapValues { (_, entries) ->
            entries.sortedByDescending(PhoneNumber::priority).distinctBy(::numberKey)
        }
    }

    private fun numberKey(number: PhoneNumber): String {
        // Extensions and service characters can make two otherwise similar dial strings distinct.
        val stripped = PhoneNumberUtils.stripSeparators(number.value).orEmpty()
        return if (stripped.all { it.isDigit() || it == '+' }) {
            number.normalized ?: stripped
        } else {
            stripped
        }
    }
}
