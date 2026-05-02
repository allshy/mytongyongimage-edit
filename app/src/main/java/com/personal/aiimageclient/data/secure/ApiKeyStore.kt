package com.personal.aiimageclient.data.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.personal.aiimageclient.data.model.ImageProvider

class ApiKeyStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "api_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun get(provider: ImageProvider): String =
        prefs.getString(provider.name, "").orEmpty()

    fun set(provider: ImageProvider, value: String) {
        prefs.edit().putString(provider.name, value.trim()).apply()
    }

    fun has(provider: ImageProvider): Boolean = get(provider).isNotBlank()
}

