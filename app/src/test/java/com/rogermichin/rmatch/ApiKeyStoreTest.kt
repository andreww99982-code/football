package com.rogermichin.rmatch

import com.google.common.truth.Truth.assertThat
import com.rogermichin.rmatch.data.ApiKeyStore
import com.rogermichin.rmatch.data.SecureValueStore
import org.junit.Test

class ApiKeyStoreTest {
    @Test
    fun saveReadDeleteAndMask() {
        val store = ApiKeyStore(secureValueStore = FakeStore())
        store.save("abcdef123456")
        assertThat(store.currentKey()).isEqualTo("abcdef123456")
        assertThat(store.mask()).isEqualTo("abc••••••456")
        store.delete()
        assertThat(store.currentKey()).isNull()
    }

    private class FakeStore : SecureValueStore {
        private var value: String? = null
        override fun read(): String? = value
        override fun write(value: String) { this.value = value }
        override fun clear() { value = null }
    }
}
