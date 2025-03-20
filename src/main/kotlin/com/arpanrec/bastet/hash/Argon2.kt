package com.arpanrec.bastet.hash

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import java.util.Base64

class Argon2 {

    val prefix = "ARGON2:$:"
    val saltLength = 44 // 32 bytes base64 encoded
    fun generateSalt(): ByteArray {
        val secureRandom = SecureRandom()
        val salt = ByteArray(32)
        secureRandom.nextBytes(salt)
        return salt
    }

    private var characters = "abcdefghijklmnopqrstuvwxyz"

    private fun hashString(inputString: String, salt: ByteArray): String {
        val random = SecureRandom()
        val randomIndex = random.nextInt(characters.length)
        val randomChar = characters[randomIndex]
        return hashString(inputString, salt, randomChar)
    }

    private fun hashString(inputString: String, salt: ByteArray, paper: Char): String {
        val inputStringWithPepper = inputString + paper
        val iterations = 2
        val memLimit = 66536
        val hashLength = 32
        val parallelism = 1
        val builder =
            Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations).withMemoryAsKB(memLimit).withParallelism(parallelism).withSalt(salt)

        val generate = Argon2BytesGenerator()
        generate.init(builder.build())
        val result = ByteArray(hashLength)
        generate.generateBytes(inputStringWithPepper.toByteArray(Charsets.UTF_8), result, 0, result.size)
        return Base64.getEncoder().encodeToString(result)
    }

    fun encode(rawPasswordChar: String): String {
        var salt = generateSalt()
        val rawPassword: String = rawPasswordChar.toString()
        val hashedPassword = hashString(rawPassword, salt)
        return "$prefix${Base64.getEncoder().encodeToString(salt)}:$:$hashedPassword"
    }

    fun matches(rawPasswordChar: String, prefixSaltB64HashedPassword: String): Boolean {
        if (!prefixSaltB64HashedPassword.startsWith(prefix)) {
            return false
        }
        val saltHashedPassword = prefixSaltB64HashedPassword.substring(prefix.length) // salt:$:hash
        val saltB64 = saltHashedPassword.substring(0, saltLength)
        val salt = Base64.getDecoder().decode(saltB64)
        val hashedPassword =
            saltHashedPassword.substring(saltLength + 3, saltHashedPassword.length) // 3 for :$: in between
        for (c: Char in characters) {
            val tryHashPassword = hashString(rawPasswordChar.toString(), salt, c)
            if (tryHashPassword == hashedPassword) {
                return true
            }
        }
        return false
    }
}
