package com.personal.aiimageclient.data.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

suspend fun Context.uriToDataUri(uriString: String): String = withContext(Dispatchers.IO) {
    val uri = Uri.parse(uriString)
    val mime = contentResolver.getType(uri) ?: "image/png"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("无法读取图片")
    "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
}

suspend fun Context.uriToTempFile(uriString: String, prefix: String): File = withContext(Dispatchers.IO) {
    val uri = Uri.parse(uriString)
    val mime = contentResolver.getType(uri) ?: "image/png"
    val extension = when {
        mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
        mime.contains("webp") -> "webp"
        else -> "png"
    }
    val file = File(cacheDir, "$prefix-${UUID.randomUUID()}.$extension")
    contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    } ?: error("无法读取图片")
    file
}

