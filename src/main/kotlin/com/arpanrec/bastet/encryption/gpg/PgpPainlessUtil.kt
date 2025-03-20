package com.arpanrec.bastet.encryption.gpg

import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.util.io.Streams
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.ProducerOptions
import org.pgpainless.encryption_signing.SigningOptions
import org.pgpainless.key.protection.UnprotectedKeysProtector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Wrapper Utility class for PgPainless library, in this utility we only focus on the below 4 implementation
 *
 *  * decryptString
 *  * decryptFile
 *  * encryptString
 *  * encryptFile
 *
 *
 * exact implementation logic and document comments for the methods will be in the method documentation
 * @author marlan
 */
object PgpPainlessUtil {
    /**
     * Encrypt the given string with the provided public and private key
     * @param plainText : message to be encrypted
     * @param ourKey : private key of the sender
     * @param receiversKey : public key of the receiver
     * @return String : cypher text of the message
     * @throws PgpUtilException : For any exception will throw this with sufficient message detail
     */
    @Throws(PgpUtilException::class)
    fun encryptString(plainText: String, ourKey: ByteArray, receiversKey: ByteArray): String {
        if (plainText.isEmpty()) throw PgpUtilException("Message should be present to encrypt")
        val inputStream: InputStream = ByteArrayInputStream(plainText.toByteArray())
        val outputStream: OutputStream = ByteArrayOutputStream()

        encryptFile(inputStream, outputStream, ourKey, receiversKey)

        return outputStream.toString()
    }


    /**
     * Decrypt the string provided
     * @param cypherTextMessage : cypher text message
     * @param ourKey : private key
     * @param sendersPublicKey : public key of the sender
     * @return String :plain text
     * @throws PgpUtilException
     */
    @Throws(PgpUtilException::class)
    fun decryptstring(cypherTextMessage: String, ourKey: ByteArray, sendersPublicKey: ByteArray): String {
        if (cypherTextMessage.isEmpty()) throw PgpUtilException("Message should be present to encrypt")

        val inputStream: InputStream = ByteArrayInputStream(cypherTextMessage.toByteArray())
        val outputStream: OutputStream = ByteArrayOutputStream()

        decryptFile(inputStream, outputStream, ourKey, sendersPublicKey)

        return outputStream.toString()
    }

    /**
     * Encrypt provided input stream and return Output stream of the encrypted data
     * @param inputStream : plain file/text
     * @param ourKey : private key (our key)
     * @param receiversPublicKey : public key of the receiver
     * @throws PgpUtilException
     */
    @Throws(PgpUtilException::class)
    fun encryptFile(
        inputStream: InputStream, outputStream: OutputStream,
        ourKey: ByteArray, receiversPublicKey: ByteArray
    ) {

        val keys = getKeys(ourKey, receiversPublicKey)

        val encryptionOptions = EncryptionOptions.get().addRecipient(keys.receiverKey)
        val options: ProducerOptions?

        try {
            // Sign and encrypt
            options = ProducerOptions.signAndEncrypt(
                encryptionOptions,
                SigningOptions.get().addDetachedSignature(UnprotectedKeysProtector(), keys.secretKey)
            )
        } catch (e: PGPException) {
            throw PgpUtilException(e.message)
        }

        encrypt(options, inputStream, outputStream)
    }

    /**
     * Decrypt encrypted file with given keys
     * @param inputStream : Input Stream of the encrypted file
     * @param outputStream : Output Stream for the decrypted file
     * @param ourKey : private key (our key)
     * @param sendersPublicKey : public key of the sender
     * @throws PgpUtilException
     */
    @Throws(PgpUtilException::class)
    fun decryptFile(
        inputStream: InputStream, outputStream: OutputStream,
        ourKey: ByteArray, sendersPublicKey: ByteArray
    ) {
        val keys = getKeys(ourKey, sendersPublicKey)

        val options = ConsumerOptions.get()
            .addVerificationCert(keys.receiverKey) // add a verification cert for signature verification
            .addDecryptionKey(keys.secretKey)

        decrypt(inputStream, outputStream, options)
    }


    @Throws(PgpUtilException::class)
    private fun decrypt(
        inputStream: InputStream, outputStream: OutputStream,
        options: ConsumerOptions
    ) {
        try {
            val consumerStream = PGPainless.decryptAndOrVerify()
                .onInputStream(inputStream)
                .withOptions(options)

            Streams.pipeAll(consumerStream, outputStream)
            consumerStream.close() // important!

            // The result will contain metadata of the message
            val result = consumerStream.metadata
        } catch (e: PGPException) {
            throw PgpUtilException(e.message + " on decrypting message")
        } catch (e: IOException) {
            throw PgpUtilException(e.message + " on decrypting message")
        }
    }

    @Throws(PgpUtilException::class)
    private fun encrypt(options: ProducerOptions?, inputStream: InputStream, outputStream: OutputStream) {
        try {
            PGPainless.encryptAndOrSign()
                .onOutputStream(outputStream)
                .withOptions(options).use { encryptionStream ->
                    Streams.pipeAll(inputStream, encryptionStream)
                }
        } catch (e: PGPException) {
            throw PgpUtilException(e.message + "In encrypting the message")
        } catch (e: IOException) {
            throw PgpUtilException(e.message + "In encrypting the message")
        }
    }


    @Throws(PgpUtilException::class)
    private fun getKeys(privateKey: ByteArray, publicKey: ByteArray): Keys {
        validateKeysNotNull(privateKey, publicKey)

        val secretKey: PGPSecretKeyRing?
        val receiverKey: PGPPublicKeyRing?

        try {
            // get secret key
            secretKey = PGPainless.readKeyRing().secretKeyRing(privateKey)
            receiverKey = PGPainless.readKeyRing().publicKeyRing(publicKey)
        } catch (e: IOException) {
            throw PgpUtilException("Failed to get PgpKeys")
        }

        if (secretKey == null) throw PgpUtilException("Invalid Private key")
        if (receiverKey == null) throw PgpUtilException("Invalid Public key")

        return Keys(secretKey, receiverKey)
    }

    @Throws(PgpUtilException::class)
    private fun validateKeysNotNull(privateKey: ByteArray, publicKey: ByteArray) {
    }

    private class Keys(val secretKey: PGPSecretKeyRing, val receiverKey: PGPPublicKeyRing)
}