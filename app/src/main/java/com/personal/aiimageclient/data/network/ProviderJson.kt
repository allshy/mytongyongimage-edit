package com.personal.aiimageclient.data.network

import com.personal.aiimageclient.data.model.ImageGenerationRequest
import org.json.JSONArray
import org.json.JSONObject

object ProviderJson {
    fun openAiGenerate(request: ImageGenerationRequest): JSONObject = JSONObject().apply {
        put("model", request.modelId)
        put("prompt", request.prompt)
        put("size", request.size)
        put("n", if (request.modelId == "dall-e-3") 1 else request.count.coerceIn(1, 4))
        if (request.quality != "auto") put("quality", request.quality)
    }

    fun falInput(
        request: ImageGenerationRequest,
        imageDataUri: String? = null,
        maskDataUri: String? = null
    ): JSONObject = JSONObject().apply {
        put("prompt", request.prompt)
        put("image_size", request.sizeToFalSize())
        put("num_images", request.count.coerceIn(1, 4))
        request.seed?.let { put("seed", it) }
        imageDataUri?.let { put("image_url", it) }
        maskDataUri?.let { put("mask_url", it) }
        request.extraParams.forEach { (key, value) -> put(key, value) }
    }

    fun replicateInput(
        request: ImageGenerationRequest,
        imageDataUri: String? = null,
        maskDataUri: String? = null
    ): JSONObject = JSONObject().apply {
        put("prompt", request.prompt)
        put("num_outputs", request.count.coerceIn(1, 4))
        put("aspect_ratio", request.sizeToAspectRatio())
        request.seed?.let { put("seed", it) }
        imageDataUri?.let { put("image", it) }
        maskDataUri?.let { put("mask", it) }
        request.extraParams.forEach { (key, value) -> put(key, value) }
    }

    fun extractImageUrls(json: JSONObject): List<String> {
        val urls = mutableListOf<String>()
        val images = json.optJSONArray("images")
        if (images != null) {
            for (index in 0 until images.length()) {
                val item = images.opt(index)
                when (item) {
                    is String -> urls += item
                    is JSONObject -> item.optString("url").takeIf { it.isNotBlank() }?.let(urls::add)
                }
            }
        }

        val output = json.opt("output")
        when (output) {
            is String -> if (output.startsWith("http")) urls += output
            is JSONArray -> {
                for (index in 0 until output.length()) {
                    val item = output.opt(index)
                    when (item) {
                        is String -> if (item.startsWith("http")) urls += item
                        is JSONObject -> item.optString("url").takeIf { it.isNotBlank() }?.let(urls::add)
                    }
                }
            }
            is JSONObject -> output.optString("url").takeIf { it.isNotBlank() }?.let(urls::add)
        }
        return urls.distinct()
    }
}

private fun ImageGenerationRequest.sizeToFalSize(): String = when (size) {
    "1024x1536" -> "portrait_2_3"
    "1536x1024" -> "landscape_3_2"
    "1024x768" -> "landscape_4_3"
    "768x1024" -> "portrait_4_3"
    else -> "square_hd"
}

private fun ImageGenerationRequest.sizeToAspectRatio(): String = when (size) {
    "1024x1536" -> "2:3"
    "1536x1024" -> "3:2"
    "1024x768" -> "4:3"
    "768x1024" -> "3:4"
    else -> "1:1"
}
