package com.rmatch.football

import com.rmatch.football.core.security.ApiKeyValidator
import com.rmatch.football.core.security.InMemoryApiKeyStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyStoreTest {

    private val sampleKey = "abcdef0123456789abcdef0123456789"

    @Test
    fun `storage keeps, reports and clears the key`() {
        val storage = InMemoryApiKeyStorage()
        assertFalse(storage.hasKey())
        assertNull(storage.getKey())

        storage.saveKey(sampleKey)
        assertTrue(storage.hasKey())
        assertEquals(sampleKey, storage.getKey())

        storage.clearKey()
        assertFalse(storage.hasKey())
        assertNull(storage.getKey())
    }

    @Test
    fun `blank keys are never stored`() {
        val storage = InMemoryApiKeyStorage()
        storage.saveKey("   ")
        assertFalse(storage.hasKey())
    }

    @Test
    fun `validator accepts plausible keys only`() {
        assertTrue(ApiKeyValidator.isPlausible(sampleKey))
        assertFalse(ApiKeyValidator.isPlausible(null))
        assertFalse(ApiKeyValidator.isPlausible(""))
        assertFalse(ApiKeyValidator.isPlausible("short"))
        assertFalse(ApiKeyValidator.isPlausible("abcdef0123456789abcdef-123456789"))
        assertFalse(ApiKeyValidator.isPlausible("a".repeat(ApiKeyValidator.MAX_LENGTH + 1)))
    }

    @Test
    fun `mask never reveals the secret`() {
        val masked = ApiKeyValidator.mask(sampleKey)
        assertFalse(masked.contains(sampleKey))
        assertTrue(masked.contains("•"))
        assertEquals("—", ApiKeyValidator.mask(null))
        assertEquals("—", ApiKeyValidator.mask(""))
    }
}
