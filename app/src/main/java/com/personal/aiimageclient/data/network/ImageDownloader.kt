package com.personal.aiimageclient.data.network

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.UUID

class ImageDownloader(
    private val context: Context,
    private val httpClient: OkHttpClient
) {
    suspend fun downloadAll(urls: List<String>): List<String> = withContext(Dispatchers.IO) {
        urls.map { download(it) }
    }

    suspend fun saveBytes(bytes: ByteArray, extension: String = "png"): String = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "generated").apply { mkdirs() }
        val file = File(outputDir, "${UUID.randomUUID()}.$extension")
        file.writeBytes(bytes)
        Uri.fromFile(file).toString()
    }

    private fun download(url: String): String {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("图片下载失败：HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("图片下载失败：响应为空")
            val contentType = body.contentType()?.subtype ?: "png"
            val extension = when {
                contentType.contains("jpeg") || contentType.contains("jpg") -> "jpg"
                contentType.contains("webp") -> "webp"
                else -> "png"
            }
            val outputDir = File(context.filesDir, "generated").apply { mkdirs() }
            val file = File(outputDir, "${UUID.randomUUID()}.$extension")
            file.outputStream().use { body.byteStream().copyTo(it) }
            return Uri.fromFile(file).toString()
        }
    }
}
