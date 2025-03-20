package com.arpanrec.bastet.test.encryption.gpg

import com.arpanrec.bastet.encryption.gpg.GnuPG
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class GnuPGTest : Data() {
    private val encryption: GnuPG
    private val log = LoggerFactory.getLogger(GnuPGTest::class.java)

    init {
        val gnuPG = GnuPG()
        gnuPG.setPgpPrivateKeyFromArmoredString(armoredPrivateKey, privateKeyPassphrase)
        encryption = gnuPG
    }

    @Test
    fun testEncrypt() {
        val encryptedMessage = encryption.encrypt(message)
        log.info("Able to encrypt message: {}", encryptedMessage)
    }

    @Test
    fun testDecrypt() {
        val decryptedMessage = encryption.decrypt(armoredBcEncryptedMessage)
        log.info("Able to decrypt message: {}", decryptedMessage)
        assert(decryptedMessage == message) { "Decrypted message is not same as original message" }
    }

    @Test
    fun testEncryptAndDecrypt() {
        val encryptedMessage = encryption.encrypt(message)
        log.info("Encrypted message: {}", encryptedMessage)
        val decryptedMessage = encryption.decrypt(encryptedMessage)
        log.info("Decrypted message: {}", decryptedMessage)
        assert(decryptedMessage == message)
    }

    @Test
    fun testNewEncryptedMessage() {
        val newEncryptedMessage = encryption.encrypt(message)
        log.info("New encrypted message: {}", newEncryptedMessage)
        assert(newEncryptedMessage != armoredBcEncryptedMessage)
        { "New encrypted message is not same as old encrypted message" }
    }

    @Test
    fun testCliEncryptedMessage() {
        val decryptedMessage = encryption.decrypt(armoredCliEncryptedMessage)
        log.info("Decrypted CLI message: {}", decryptedMessage)
        assert(decryptedMessage == message)
    }
}
