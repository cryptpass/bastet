package com.arpanrec.bastet.test.hash

import com.arpanrec.bastet.hash.Argon2
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class Argon2Test {

    private val log = LoggerFactory.getLogger(Argon2Test::class.java)
    private val passwordEncoder: Argon2 = Argon2()
    private val password = "root1"
    private val hashedPassword =
        "ARGON2:$:r+js/iLqgq9nDcpuRFI1x20qzY7Z/2vhaqyy7e8sCXU=:$:++KQe/KplFhMyyhkPAn6NtttnTU/JdaIVN6J6YyzJkk="

    @Test
    fun testHashedPassword() {
        log.info("OLD Encoded Password: $hashedPassword")
        assert(passwordEncoder.matches(password, hashedPassword)) { "Old hashed password does not match" }
        log.info("Old encoded password matches")
    }

    @Test
    fun testNewHashedPassword() {
        val newEncodedPassword = passwordEncoder.encode(this.password)
        log.info("New Hashed Password: $newEncodedPassword")
        assert(passwordEncoder.matches(password, newEncodedPassword)) { "New encoded password does not match" }
        log.info("New hashed password matches")
    }

    @Test
    fun testNewHash() {
        val newHash = passwordEncoder.encode(this.password)
        log.info("New Hash: $newHash")
        assert(newHash != hashedPassword) { "New hash matches old hash" }
        log.info("New hash does not match old hash")
    }

    @Test
    fun wrongPasswordTest() {
        val newEncodedPassword = passwordEncoder.encode(password.toString() + "1")
        assert(!passwordEncoder.matches(password, newEncodedPassword)) { "Wrong password matches" }
        log.info("Wrong password does not match")
    }
}
