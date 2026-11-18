package com.saiki1212.manacall.calling

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppCallTest {
    @Test
    fun acceptsTheSelectedPhoneAddress() {
        assertTrue(matchesWhatsAppRecipient("919876543210@s.whatsapp.net", null, "919876543210"))
        assertTrue(matchesWhatsAppRecipient("+919876543210", null, "919876543210"))
        assertFalse(matchesWhatsAppRecipient("919012345678@s.whatsapp.net", "+91 98765 43210", "919876543210"))
    }

    @Test
    fun acceptsLinkedAddressesOnlyWhenTheDisplayMatchesUnambiguously() {
        val linked = "123456789@lid"
        assertTrue(matchesWhatsAppRecipient(linked, "Video call +91 98765-43210", "919876543210"))
        assertFalse(matchesWhatsAppRecipient(linked, "Video call", "919876543210"))
        assertFalse(matchesWhatsAppRecipient(linked, "+91 98765 43210 or +91 90123 45678", "919876543210"))
    }
}
