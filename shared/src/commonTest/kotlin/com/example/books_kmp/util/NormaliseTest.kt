package com.example.books_kmp.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NormaliseTest {
    @Test
    fun `strips latin diacritics`() {
        assertEquals("cafe", normalise("café"))
        assertEquals("naive", normalise("naïve"))
    }

    @Test
    fun `strips hebrew niqqud outside the combining diacritical marks block`() {
        assertEquals(normalise("שלום"), normalise("שָׁלוֹם"))
    }

    @Test
    fun `strips arabic harakat outside the combining diacritical marks block`() {
        assertEquals(normalise("محمد"), normalise("مُحَمَّد"))
    }

    @Test
    fun `strips punctuation`() {
        assertEquals("hello world", normalise("hello, world!"))
    }
}
