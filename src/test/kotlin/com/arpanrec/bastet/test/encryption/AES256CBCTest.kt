package com.arpanrec.bastet.test.encryption

import com.arpanrec.bastet.encryption.AES256CBC
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class AES256CBCTest {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val encryption: AES256CBC = AES256CBC()

    private val data = "Hello, World!"
    private val cipherText = "enc:$:AES256CBC:$:yQp5HF92QfpV/jdmPIDYJQ=="
    val keyB64 = "5jcK7IMk3+QbNLikFRl3Zwgl9xagKD87s5dT2UqaSR4=:$:5jcK7IMk3+QbNLikFRl3Zw==" //gitleaks:allow

    @Test
    fun testEncrypt() {
        log.info("Encrypting plain text")
        val newCipherText = encryption.encrypt(data, keyB64)
        log.info("Cipher text: $newCipherText")
        assert(cipherText == newCipherText) { "Cipher text does not match" }
    }

    @Test
    fun testDecrypt() {
        log.info("Decrypting cipher text")
        val decryptedText = encryption.decrypt(cipherText, keyB64)
        log.info("Decrypted text: $decryptedText")
        assert(decryptedText == data) { "Decrypted text does not match" }
    }
}
