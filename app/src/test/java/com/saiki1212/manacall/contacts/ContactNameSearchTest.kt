package com.saiki1212.manacall.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactNameSearchTest {
    @Test
    fun ranksExactThenPrefixThenTypo() {
        val search = ContactNameSearch("Anjali")
        assertEquals(0, search.rank("ANJALI"))
        assertTrue(search.rank("Anjalina")!! < search.rank("Anjail")!!)
    }

    @Test
    fun acceptsRelatedNamesWithCommonTypos() {
        assertNotNull(ContactNameSearch("Anjail").rank("Anjali"))
        assertNotNull(ContactNameSearch("Anjli").rank("Anjali"))
        assertNotNull(ContactNameSearch("Anjalli").rank("Anjali"))
        assertNotNull(ContactNameSearch("Anjoli").rank("Anjali"))
    }

    @Test
    fun searchesAllNamePartsEvenWhenReordered() {
        assertNotNull(ContactNameSearch("rma anj").rank("Anjali Rama"))
        assertNull(ContactNameSearch("anj ravi").rank("Anjali Rama"))
    }

    @Test
    fun preservesTeluguMarks() {
        assertEquals(0, ContactNameSearch("అమ్మ").rank("అమ్మ"))
        assertNotNull(ContactNameSearch("అమమ").rank("అమ్మ"))
    }

    @Test
    fun avoidsUnrelatedResultsAndShortTypos() {
        assertNull(ContactNameSearch("Anjali").rank("Suresh"))
        assertNull(ContactNameSearch("ar").rank("Aj"))
        assertNull(ContactNameSearch("  ").rank("Amma"))
    }
}
