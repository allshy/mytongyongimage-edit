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

class ReplicateImageClient(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val downloader: ImageDownloader
) : ImageProviderClient {
    override suspend fun run(apiKey: String, request: ImageGenerationRequest): ImageGenerationResult =
        runCatching {
            val imageDataUri = request.inputImageUri?.let { context.uriToDataUri(it) }
            val maskDataUri = request.maskUri?.let { context.uriToDataUri(it) }
            val input = ProviderJson.replicateInput(request, imageDataUri, maskDataUri)
            submitAndPoll(apiKey, request.modelId, input)
        }.getOrElse {
            ImageGenerationResult(status = ImageJobStatus.Failed, error = it.message ?: "Replicate 请求失败")
        }

    private suspend fun submitAndPoll(apiKey: String, modelId: String, input: JSONObject): ImageGenerationResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("input", input)
                .toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.replicate.com/v1/models/$modelId/predictions")
                .header("Authorization", "Bearer $apiKey")
                .header("Prefer", "wait=5")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string().orEmpty()
            val code = response.code
            val successful = response.isSuccessful
            response.close()
            if (!successful) {
                return@withContext ImageGenerationResult(
                    status = ImageJobStatus.Failed,
                    error = parseReplicateError(raw, code)
                )
            }
            val submitted = JSONObject(raw)
            val rawStatus = submitted.optString("status")
            val status = ProviderStatus.fromReplicate(rawStatus)
            if (status == ImageJobStatus.Succeeded) return@withContext successFromPrediction(submitted)
            if (status == ImageJobStatus.Failed || status == ImageJobStatus.Canceled) {
                return@withContext ImageGenerationResult(
                    status = status,
                    error = submitted.optString("error").ifBlank { "Replicate 任务失败：$rawStatus" }
                )
            }
            val getUrl = submitted.optJSONObject("urls")?.optString("get").orEmpty()
            if (getUrl.isBlank()) {
                return@withContext ImageGenerationResult(
                    status = ImageJobStatus.Failed,
                    error = "Replicate 响应缺少轮询地址"
                )
            }
            poll(apiKey, getUrl)
        }

    private suspend fun poll(apiKey: String, getUrl: String): ImageGenerationResult {
        repeat(90) {
            delay(2_000)
            val request = Request.Builder()
                .url(getUrl)
                .header("Authorization", "Bearer $apiKey")
                .build()
            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string().orEmpty()
            val code = response.code
            val successful = response.isSuccessful
            response.close()
            if (!successful) {
                return ImageGenerationResult(
                    status = ImageJobStatus.Failed,
                    error = parseReplicateError(raw, code)
                )
            }
            val prediction = JSONObject(raw)
            val rawStatus = prediction.optString("status")
            val status = ProviderStatus.fromReplicate(rawStatus)
            if (status == ImageJobStatus.Succeeded) return successFromPrediction(prediction)
            if (status == ImageJobStatus.Failed || status == ImageJobStatus.Canceled) {
                return ImageGenerationResult(
                    status = status,
                    error = prediction.optString("error").ifBlank { "Replicate 任务失败：$rawStatus" }
                )
            }
        }
        return ImageGenerationResult(status = ImageJobStatus.Failed, error = "Replicate 任务超时")
    }

    private suspend fun successFromPrediction(prediction: JSONObject): ImageGenerationResult {
        val urls = ProviderJson.extractImageUrls(prediction)
        if (urls.isEmpty()) {
            return ImageGenerationResult(status = ImageJobStatus.Failed, error = "Replicate 响应中没有图片地址")
        }
        return ImageGenerationResult(
            status = ImageJobStatus.Succeeded,
            localImageUris = downloader.downloadAll(urls),
            remoteUrls = urls
        )
    }

    private fun parseReplicateError(raw: String, code: Int): String {
        val message = runCatching { JSONObject(raw).optString("detail") }.getOrNull()
        return message?.takeIf { it.isNotBlank() } ?: "Replicate 请求失败：HTTP $code"
    }
}
