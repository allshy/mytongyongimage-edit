package com.personal.aiimageclient.data.network

import android.content.Context
import com.personal.aiimageclient.data.model.ImageGenerationRequest
import com.personal.aiimageclient.data.model.ImageGenerationResult
import com.personal.aiimageclient.data.model.ImageJobStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class FalImageClient(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val downloader: ImageDownloader
) : ImageProviderClient {
    override suspend fun run(apiKey: String, request: ImageGenerationRequest): ImageGenerationResult =
        runCatching {
            val imageDataUri = request.inputImageUri?.let { context.uriToDataUri(it) }
            val maskDataUri = request.maskUri?.let { context.uriToDataUri(it) }
            val input = ProviderJson.falInput(request, imageDataUri, maskDataUri)
            submitAndPoll(apiKey, request.modelId, input)
        }.getOrElse {
            ImageGenerationResult(status = ImageJobStatus.Failed, error = it.message ?: "fal.ai 请求失败")
        }

    private suspend fun submitAndPoll(apiKey: String, modelId: String, input: JSONObject): ImageGenerationResult =
        withContext(Dispatchers.IO) {
            val submitBody = input.toString()
                .toRequestBody("application/json".toMediaType())
            val submitRequest = Request.Builder()
                .url("https://queue.fal.run/$modelId")
                .header("Authorization", "Key $apiKey")
                .post(submitBody)
                .build()

            val response = httpClient.newCall(submitRequest).execute()
            val raw = response.body?.string().orEmpty()
            val code = response.code
            val successful = response.isSuccessful
            response.close()
            if (!successful) {
                return@withContext ImageGenerationResult(
                    status = ImageJobStatus.Failed,
                    error = parseFalError(raw, code)
                )
            }
            val submitted = JSONObject(raw)
            val immediateUrls = ProviderJson.extractImageUrls(submitted)
            if (immediateUrls.isNotEmpty()) {
                return@withContext successFromUrls(immediateUrls)
            }

            val statusUrl = submitted.optString("status_url")
            val responseUrl = submitted.optString("response_url")
            if (statusUrl.isBlank() || responseUrl.isBlank()) {
                return@withContext ImageGenerationResult(
                    status = ImageJobStatus.Failed,
                    error = "fal.ai 响应缺少队列地址"
                )
            }
            poll(apiKey, statusUrl, responseUrl)
        }

    private suspend fun poll(apiKey: String, statusUrl: String, responseUrl: String): ImageGenerationResult {
        repeat(90) {
            delay(2_000)
            val statusRequest = Request.Builder()
                .url(statusUrl)
                .header("Authorization", "Key $apiKey")
                .build()
            val response = httpClient.newCall(statusRequest).execute()
            val raw = response.body?.string().orEmpty()
            val code = response.code
            val successful = response.isSuccessful
            response.close()
            if (!successful) {
                return ImageGenerationResult(
                    status = ImageJobStatus.Failed,
                    error = parseFalError(raw, code)
                )
            }
            val rawStatus = JSONObject(raw).optString("status")
            val status = ProviderStatus.fromFal(rawStatus)
            if (status == ImageJobStatus.Succeeded) return fetchResult(apiKey, responseUrl)
            if (status == ImageJobStatus.Failed || status == ImageJobStatus.Canceled) {
                return ImageGenerationResult(status = status, error = "fal.ai 任务失败：$rawStatus")
            }
        }
        return ImageGenerationResult(status = ImageJobStatus.Failed, error = "fal.ai 任务超时")
    }

    private suspend fun fetchResult(apiKey: String, responseUrl: String): ImageGenerationResult {
        val resultRequest = Request.Builder()
            .url(responseUrl)
            .header("Authorization", "Key $apiKey")
            .build()
        val response = httpClient.newCall(resultRequest).execute()
        val raw = response.body?.string().orEmpty()
        val code = response.code
        val successful = response.isSuccessful
        response.close()
        if (!successful) {
            return ImageGenerationResult(status = ImageJobStatus.Failed, error = parseFalError(raw, code))
        }
        return successFromUrls(ProviderJson.extractImageUrls(JSONObject(raw)))
    }

    private suspend fun successFromUrls(urls: List<String>): ImageGenerationResult {
        if (urls.isEmpty()) {
            return ImageGenerationResult(status = ImageJobStatus.Failed, error = "fal.ai 响应中没有图片地址")
        }
        val localUris = downloader.downloadAll(urls)
        return ImageGenerationResult(
            status = ImageJobStatus.Succeeded,
            localImageUris = localUris,
            remoteUrls = urls
        )
    }

    private fun parseFalError(raw: String, code: Int): String {
        val message = runCatching { JSONObject(raw).optString("detail") }.getOrNull()
        return message?.takeIf { it.isNotBlank() } ?: "fal.ai 请求失败：HTTP $code"
    }
}
