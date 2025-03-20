package com.arpanrec.bastet.encryption

import com.arpanrec.bastet.exceptions.CaughtException

class Encryptor {

    private final val aes256cbc = AES256CBC()

    fun generateKey(): String {
        return aes256cbc.generateKey()
    }

    fun encrypt(data: String, key: String): String {
        return aes256cbc.encrypt(data, key)
    }

    @Throws(CaughtException::class)
    fun decrypt(encryptedData: String, key: String): String {
        when {
            encryptedData.startsWith(aes256cbc.prefix) -> {
                return aes256cbc.decrypt(encryptedData, key)
            }

            else -> {
                throw CaughtException("Invalid encrypted data")
            }
        }
    }
}