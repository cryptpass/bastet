package com.arpanrec.bastet.encryption.gpg

import com.arpanrec.bastet.encryption.gpg.PgpPainlessUtil.encryptString
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.util.Passphrase


class PainlessGnuPG {
    private var pgpSecretKeyRing: PGPSecretKeyRing? = null
    private var pgpSecretKeyRingPassphrase: String? = null

    fun setPgpPrivateKeyFromArmoredString(armoredPrivateKey: String, privateKeyPassphrase: String) {
        this.pgpSecretKeyRing = PGPainless.readKeyRing().secretKeyRing(armoredPrivateKey)
        this.pgpSecretKeyRingPassphrase = privateKeyPassphrase
    }

    fun encrypt(plainText: String): String {
        return encryptString(
            "this is plainText",
            pgpSecretKeyRing!!.getEncoded(),
            pgpSecretKeyRing!!.publicKey.getEncoded()
        )
    }

    fun decrypt(encryptedText: String): String {
        val decryptionStream = PGPainless.decryptAndOrVerify().onInputStream(encryptedText.byteInputStream())
            .withOptions(
                ConsumerOptions().addDecryptionKey(pgpSecretKeyRing!!).addDecryptionPassphrase(
                    Passphrase(
                        pgpSecretKeyRingPassphrase!!.toCharArray()
                    )
                )
            )

        decryptionStream.use {
            return String(it.readBytes())
        }

    }
}