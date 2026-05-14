package com.ansangha.craxxjxbdbf.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Client-side AES-GCM using an Android Keystore AES key (non-extractable).
 *
 * Threat model (MVP): ciphertext is opaque to the server; TLS protects in transit.
 * A compromised server still cannot decrypt without the device keystore + app binary.
 * This is not zero-knowledge multi-device sync unless you export the key (not done here).
 */
object BackupAesGcm {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "habitpro_backup_aes_gcm_v1"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) {
            val e = ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return e.secretKey
        }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        kg.init(spec)
        return kg.generateKey()
    }

    fun encrypt(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return iv to ct
    }

    fun decrypt(iv: ByteArray, cipherBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), spec)
        return cipher.doFinal(cipherBytes)
    }

    fun encodeIvAndCiphertext(iv: ByteArray, cipherBytes: ByteArray): String {
        val bb = ByteBuffer.allocate(iv.size + cipherBytes.size)
        bb.put(iv)
        bb.put(cipherBytes)
        return Base64.encodeToString(bb.array(), Base64.NO_WRAP)
    }

    fun decodeIvAndCiphertext(b64: String): Pair<ByteArray, ByteArray> {
        val all = Base64.decode(b64, Base64.NO_WRAP)
        require(all.size >= IV_BYTES + 16)
        val iv = all.copyOfRange(0, IV_BYTES)
        val ct = all.copyOfRange(IV_BYTES, all.size)
        return iv to ct
    }
}
