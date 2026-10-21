package com.example.myapplication

import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHashTest {
    @Test
    fun passwordHashVerifiesOnlyTheRightSecretAndSalt() {
        val salt = ByteArray(16) { it.toByte() }
        val saved = deriveSecretHash("LoopiPass123", salt, iterations = 10_000)

        assertTrue(MessageDigest.isEqual(saved, deriveSecretHash("LoopiPass123", salt, iterations = 10_000)))
        assertFalse(MessageDigest.isEqual(saved, deriveSecretHash("wrong-password", salt, iterations = 10_000)))
        assertFalse(MessageDigest.isEqual(saved, deriveSecretHash("LoopiPass123", ByteArray(16) { (it + 1).toByte() }, iterations = 10_000)))
    }
}
