package me.m64diamondstar.security

import java.security.MessageDigest
import java.util.UUID

object ApiKeyUtil {

    /**
     * Generates a random 128-character API key.
     *
     * This method concatenates four UUIDs (without dashes) to create
     * a unique, hard-to-guess string. It is recommended to hash this
     * key using [hashKey] before storing it in a database for security.
     *
     * **Note:** Each call to this function produces a new, unique key.
     * Do not reuse keys across different users or applications.
     *
     * @return A 128-character string that can be used as an API key.
     */
    fun generateApiKey(): String {
        return (1..4)
            .joinToString("") { UUID.randomUUID().toString() }
            .replace("-", "")
    }

    /**
     * Hashes the provided API key using the SHA-256 algorithm.
     *
     * This method converts the input string into a secure, fixed-length
     * hexadecimal representation. It is recommended to use this before
     * storing API keys in a database.
     *
     * ### Example
     * ```kotlin
     * val apiKey = ApiKeyUtil.generateApiKey()
     * val hashedKey = ApiKeyUtil.hashKey(apiKey)
     * println(hashedKey) // prints a 64-character hex string
     * ```
     *
     * **Warning:** SHA-256 is a one-way hash. You cannot retrieve
     * the original API key from the hash.
     *
     * @param apiKey The plain-text API key to hash.
     * @return A 64-character hexadecimal string representing the SHA-256 hash.
     */
    fun hashKey(apiKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(apiKey.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

}