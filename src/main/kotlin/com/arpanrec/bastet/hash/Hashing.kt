package com.arpanrec.bastet.hash

import com.arpanrec.bastet.exceptions.CaughtException
import org.springframework.security.crypto.password.PasswordEncoder

object Hashing : PasswordEncoder {

    private val sha256: Sha256 = Sha256()
    private val argon2: Argon2 = Argon2()

    override fun encode(rawPassword: CharSequence?): String? {
        if (rawPassword == null) {
            throw CaughtException("Password cannot be null")
        }
        return sha256.encode(rawPassword.toString())
    }

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
        if (rawPassword == null || encodedPassword == null) {
            throw CaughtException("Password or encoded password cannot be null")
        }
        return when {
            encodedPassword.startsWith(sha256.prefix) -> {
                sha256.matches(rawPassword.toString(), encodedPassword.toString())
            }

            encodedPassword.startsWith(argon2.prefix) -> {
                argon2.matches(rawPassword.toString(), encodedPassword)
            }

            else -> {
                throw CaughtException("Invalid hash format")
            }
        }
    }
}