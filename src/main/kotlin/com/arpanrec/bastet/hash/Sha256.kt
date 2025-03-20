package com.arpanrec.bastet.hash

import java.security.MessageDigest

class Sha256 {
    val prefix = "SHA256:$:"
    fun encode(rawData: String): String? {
        return prefix + hashString(rawData.toString())
    }

    private fun hashString(inputString: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(inputString.toByteArray())
        val hexString = StringBuilder()
        for (b in hash) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

    fun matches(rawData: String, prefixHexHash: String): Boolean {
        if (!prefixHexHash.startsWith(prefix)) {
            return false
        }

        val hexHash = prefixHexHash.substring(prefix.length)
        val rawDataHash = hashString(rawData.toString())
        return hexHash == rawDataHash
    }
}