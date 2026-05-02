package com.personal.aiimageclient.data.network

import android.content.Context
import android.util.Base64
import com.personal.aiimageclient.data.model.ImageGenerationRequest
import com.personal.aiimageclient.data.model.ImageGenerationResult
import com.personal.aiimageclient.data.model.ImageJobStatus
import com.personal.aiimageclient.data.model.ImageOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class OpenAiImageClient(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val downloader: ImageDownloader
) : ImageProviderClient {
    override suspend fun run(apiKey: String, request: ImageGenerationRequest): ImageGenerationResult =
        runCatching {
            when (request.operation) {
                ImageOperation.TextToImage -> generate(apiKey, request)
                ImageOperation.ImageEdit, ImageOperation.Inpaint -> edit(apiKey, request)
                ImageOperation.Upscale -> ImageGenerationResult(
                    status = ImageJobStatus.Failed,
                    error = "OpenAI Provider 首版未实现放大"
                )
            }
        }.getOrElse {
            ImageGenerationResult(status = ImageJobStatus.Failed, error = it.message ?: "OpenAI 请求失败")
        }

    private suspend fun generate(apiKey: String, request: ImageGenerationRequest): ImageGenerationResult =
        withContext(Dispatchers.IO) {
            val body = ProviderJson.openAiGenerate(request)
                .toString()
                .toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url("https://api.openai.com/v1/images/generations")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            executeOpenAi(httpRequest)
        }

    private suspend fun edit(apiKey: String, request: ImageGenerationRequest): ImageGenerationResult =
        withContext(Dispatchers.IO) {
            val inputUri = request.inputImageUri ?: error("请先选择要编辑的图片")
            val imageFile = context.uriToTempFile(inputUri, "openai-image")
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", request.modelId)
                .addFormDataPart("prompt", request.prompt)
                .addFormDataPart("size", request.size)
                .addFormDataPart("n", request.count.coerceIn(1, 4).toString())
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/png".toMediaType()))

            request.maskUri?.let {
                val maskFile = context.uriToTempFile(it, "openai-mask")
                builder.addFormDataPart("mask", maskFile.name, maskFile.asRequestBody("image/png".toMediaType()))
            }

            val httpRequest = Request.Builder()
                .url("https://api.openai.com/v1/images/edits")
                .header("Authorization", "Bearer $apiKey")
                .post(builder.build())
                .build()
            executeOpenAi(httpRequest)
        }

    private suspend fun executeOpenAi(httpRequest: Request): ImageGenerationResult {
        val response = httpClient.newCall(httpRequest).execute()
        val raw = response.body?.string().orEmpty()
        val code = response.code
        val successful = response.isSuccessful
        response.close()

        if (!successful) {
            return ImageGenerationResult(status = ImageJobStatus.Failed, error = parseError(raw, code))
        }

        val json = JSONObject(raw)
        val data = json.optJSONArray("data") ?: return ImageGenerationResult(
            status = ImageJobStatus.Failed,
            error = "OpenAI 响应中没有图片数据"
        )
        val remoteUrls = mutableListOf<String>()
        val localUris = mutableListOf<String>()
        var revisedPrompt: String? = null
        for (index in 0 until data.length()) {
            val item = data.getJSONObject(index)
            if (item.has("revised_prompt")) revisedPrompt = item.optString("revised_prompt")
            item.optString("b64_json").takeIf { it.isNotBlank() }?.let {
                localUris += downloader.saveBytes(Base64.decode(it, Base64.DEFAULT))
            }
            item.optString("url").takeIf { it.isNotBlank() }?.let(remoteUrls::add)
        }
        localUris += downloader.downloadAll(remoteUrls)
        if (localUris.isEmpty()) {
            return ImageGenerationResult(status = ImageJobStatus.Failed, error = "OpenAI 响应中没有可保存的图片")
        }
        return ImageGenerationResult(
            status = ImageJobStatus.Succeeded,
            localImageUris = localUris,
            remoteUrls = remoteUrls,
            revisedPrompt = revisedPrompt
        )
    }

    private fun parseError(raw: String, code: Int): String {
        val message = runCatching {
            JSONObject(raw).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return message?.takeIf { it.isNotBlank() } ?: "OpenAI 请求失败：HTTP $code"
    }
}
