package com.arpanrec.bastet.encryption.gpg

import com.arpanrec.bastet.exceptions.CaughtException
import com.arpanrec.bastet.utils.FileUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Date
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.CompressionAlgorithmTags
import org.bouncycastle.bcpg.PublicKeyPacket
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPCompressedData
import org.bouncycastle.openpgp.PGPCompressedDataGenerator
import org.bouncycastle.openpgp.PGPEncryptedData
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator
import org.bouncycastle.openpgp.PGPEncryptedDataList
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPLiteralDataGenerator
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPOnePassSignatureList
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder
import org.slf4j.LoggerFactory
import java.security.NoSuchProviderException
import java.security.Security

class GnuPG {
    private val log = LoggerFactory.getLogger(this.javaClass)

    init {
        log.info("Adding BouncyCastle provider.")
        Security.addProvider(BouncyCastleProvider())
    }

    private var pgpPrivateKey: PGPPrivateKey? = null
    private var encryptedDataGenerator: PGPEncryptedDataGenerator? = null

    fun setPgpPrivateKeyFromArmoredString(armoredPrivateKey: String, privateKeyPassphrase: String) {
        log.info("Loading GPG armored private key.")
        if (this.pgpPrivateKey != null) {
            throw CaughtException("Private key already loaded.")
        }
        this.pgpPrivateKey = this.loadGpgPrivateKeyFromArmoredString(armoredPrivateKey, privateKeyPassphrase)
        this.setEncryptedDataGeneratorFromArmoredString(pgpPrivateKey!!)
    }

    private fun setEncryptedDataGeneratorFromArmoredString(pgpPrivateKey: PGPPrivateKey) {
        log.info("Loading GPG armored public key.")
        val gpgPublicKey = this.loadGpgPublicKeyFromArmoredString(pgpPrivateKey)
        log.info("Creating encrypted data generator.")
        this.encryptedDataGenerator = this.createEncryptedDataGenerator(gpgPublicKey)
    }

    private fun loadGpgPublicKeyFromArmoredString(pgpPrivateKey: PGPPrivateKey): PGPPublicKey {
        val publicKeyPacket: PublicKeyPacket? = pgpPrivateKey.publicKeyPacket
        return try {
            PGPPublicKey(publicKeyPacket, BcKeyFingerprintCalculator())
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Error converting PublicKeyPacket to PGPPublicKey", e)
        }
    }

    private fun createEncryptedDataGenerator(gpgPublicKey: PGPPublicKey): PGPEncryptedDataGenerator {
        val encryptedDataGenerator = PGPEncryptedDataGenerator(
            JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256).setWithIntegrityPacket(true)
                .setSecureRandom(SecureRandom()).setProvider(BouncyCastleProvider.PROVIDER_NAME)
        )
        encryptedDataGenerator.addMethod(BcPublicKeyKeyEncryptionMethodGenerator(gpgPublicKey))
        log.info("Encrypted data generator created with AES-256.")
        return encryptedDataGenerator
    }

    fun encrypt(plainText: String): String {
        val clearTextDataByteOutputStream = ByteArrayOutputStream()
        val gpgCompressedDataGenerator = PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP)
        val gpgLiteralDataGenerator = PGPLiteralDataGenerator()
        val pOut: OutputStream = gpgLiteralDataGenerator.open(
            gpgCompressedDataGenerator.open(clearTextDataByteOutputStream),
            PGPLiteralData.TEXT,
            GnuPG::class.java.canonicalName,
            plainText.length.toLong(),
            Date()
        )
        pOut.write(plainText.toByteArray(StandardCharsets.US_ASCII))
        pOut.close()
        gpgCompressedDataGenerator.close()

        val encryptedOut = ByteArrayOutputStream()
        val out: OutputStream = ArmoredOutputStream(encryptedOut)
        val encryptedOutStream = encryptedDataGenerator!!.open(
            out, clearTextDataByteOutputStream.toByteArray().size.toLong()
        )
        encryptedOutStream.write(clearTextDataByteOutputStream.toByteArray())
        encryptedOutStream.close()
        out.close()
        val encryptedData = encryptedOut.toString(StandardCharsets.US_ASCII)
        log.trace("Clear Text: {}, Encrypted data: {}", plainText, encryptedData)
        return encryptedData
    }

    private fun loadGpgPrivateKeyFromArmoredString(
        armoredPrivateKey: String, privateKeyPassphrase: String?
    ): PGPPrivateKey {

        val armoredPrivateKeyString: String = FileUtils.fileOrString(armoredPrivateKey)

        val privateKeyPassphraseString: String = privateKeyPassphrase?.let { FileUtils.fileOrString(it) }.toString()

        val armoredPrivateKeyInputStreamStream: InputStream = ArmoredInputStream(
            ByteArrayInputStream(armoredPrivateKeyString.toByteArray(StandardCharsets.US_ASCII))
        )

        val pgpSec = PGPSecretKeyRingCollection(
            PGPUtil.getDecoderStream(armoredPrivateKeyInputStreamStream), JcaKeyFingerprintCalculator()
        )
        var pgpPrivateKey: PGPPrivateKey? = null
        val keyRingIter = pgpSec.keyRings
        while (keyRingIter.hasNext()) {
            val keyRing = keyRingIter.next()
            val keyIter = keyRing.secretKeys
            while (keyIter.hasNext()) {
                val key = keyIter.next()
                if (key.isSigningKey) continue
                val privateKey = key.extractPrivateKey(
                    JcePBESecretKeyDecryptorBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(privateKeyPassphraseString.toCharArray())
                )
                if (privateKey != null) {
                    pgpPrivateKey = privateKey
                    break
                }
            }
            if (pgpPrivateKey != null) {
                break
            }
        }

        requireNotNull(pgpPrivateKey) { "No private key found." }
        log.info("Private key loaded.")
        return pgpPrivateKey
    }

    fun decrypt(encryptedText: String): String {

        val encryptedDataStream: InputStream = ArmoredInputStream(
            ByteArrayInputStream(
                encryptedText.toByteArray(StandardCharsets.US_ASCII)
            )
        )

        var publicKeyEncryptedData: PGPPublicKeyEncryptedData? = null

        val pgpObjectFactory = PGPObjectFactory(
            PGPUtil.getDecoderStream(encryptedDataStream), JcaKeyFingerprintCalculator()
        )
        val o = pgpObjectFactory.nextObject()

        val encryptedDataList = o as? PGPEncryptedDataList ?: pgpObjectFactory.nextObject() as PGPEncryptedDataList

        val it = encryptedDataList.encryptedDataObjects
        while (it.hasNext()) {
            val data = it.next()

            if (data is PGPPublicKeyEncryptedData) {
                publicKeyEncryptedData = data
                break
            }
        }

        requireNotNull(publicKeyEncryptedData) { "No encrypted data found." }

        val clear = publicKeyEncryptedData.getDataStream(
            JcePublicKeyDataDecryptorFactoryBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(this.pgpPrivateKey)
        )
        val plainFact = PGPObjectFactory(clear, JcaKeyFingerprintCalculator())
        var message: Any = plainFact.nextObject()

        if (message is PGPCompressedData) {
            val pgpFact = PGPObjectFactory(message.dataStream, JcaKeyFingerprintCalculator())
            message = pgpFact.nextObject()
        }

        when (message) {
            is PGPLiteralData -> {
                val unc: InputStream = message.inputStream
                try {
                    ByteArrayOutputStream().use { out ->
                        var ch: Int
                        while ((unc.read().also { ch = it }) >= 0) {
                            out.write(ch)
                        }
                        val decryptedData = out.toString()
                        log.trace("Decrypting data: {}, Decrypted data: {}", encryptedText, decryptedData)
                        return decryptedData
                    }
                } catch (e: Exception) {
                    throw PGPException("Failed to decrypt message", e)
                }
            }

            is PGPOnePassSignatureList -> {
                throw CaughtException(
                    "Encrypted message contains a signed message - not literal data."
                )
            }

            else -> {
                throw CaughtException("Message is not a simple encrypted file - type unknown.")
            }
        }
    }
}
