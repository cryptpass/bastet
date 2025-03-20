package com.arpanrec.bastet.encryption

import com.arpanrec.bastet.exceptions.CaughtException
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory

class AES256CBC {
    private val log = LoggerFactory.getLogger(this.javaClass)

    init {
        log.info("Adding BouncyCastle provider, for PKCS7Padding.")
        Security.addProvider(BouncyCastleProvider())
    }

    val prefix = "enc:$:AES256CBC:$:"

    fun encrypt(data: String, ivB64KeyB64: String): String {
        val (secretKey, iv) = splitKeys(ivB64KeyB64)
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
        val cipherData = cipher.doFinal(data.toByteArray())
        val cipherDataB64 = Base64.getEncoder().encodeToString(cipherData)
        return "$prefix${cipherDataB64}"
    }

    fun decrypt(prefixCipherTextB64: String, ivB64KeyB64: String): String {
        val (secretKey, iv) = splitKeys(ivB64KeyB64)
        if (!prefixCipherTextB64.startsWith(prefix)) {
            throw CaughtException("Invalid encrypted text")
        }
        val cipherTextB64 = prefixCipherTextB64.substring(prefix.length)
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
        val plainText = cipher.doFinal(Base64.getDecoder().decode(cipherTextB64))
        return String(plainText)
    }

    private fun splitKeys(ivB64KeyB64: String): Pair<SecretKeySpec, IvParameterSpec> {
        val parts = ivB64KeyB64.split(":$:").toTypedArray()
        val keyB64 = parts[0]
        val ivB64 = parts[1]
        val iv = IvParameterSpec(Base64.getDecoder().decode(ivB64))
        val decodedKey: ByteArray = Base64.getDecoder().decode(keyB64)
        val secretKey = SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
        return Pair(secretKey, iv)
    }

    fun generateKey(): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256) // 256-bit key
        val key = keyGen.generateKey()
        val keyB64 = Base64.getEncoder().encodeToString(key.encoded)
        val randomBytes = ByteArray(16) // AES block size is 16 bytes
        SecureRandom().nextBytes(randomBytes)
        val iv = IvParameterSpec(randomBytes)
        val ivB64 = Base64.getEncoder().encodeToString(iv.iv)
        return "$keyB64:$:$ivB64"
    }
}