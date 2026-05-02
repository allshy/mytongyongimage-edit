package com.personal.aiimageclient.data.network

import com.personal.aiimageclient.data.model.ImageGenerationRequest
import com.personal.aiimageclient.data.model.ImageJobStatus
import com.personal.aiimageclient.data.model.ImageOperation
import com.personal.aiimageclient.data.model.ImageProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderJsonTest {
    @Test
    fun openAiGenerateBuildsExpectedPayload() {
        val request = ImageGenerationRequest(
            provider = ImageProvider.OpenAI,
            operation = ImageOperation.TextToImage,
            modelId = "gpt-image-1",
            prompt = "一只透明玻璃茶杯",
            size = "1024x1024",
            count = 2
        )

        val json = ProviderJson.openAiGenerate(request)

        assertEquals("gpt-image-1", json.getString("model"))
        assertEquals("一只透明玻璃茶杯", json.getString("prompt"))
        assertEquals("1024x1024", json.getString("size"))
        assertEquals(2, json.getInt("n"))
    }

    @Test
    fun falInputMapsSizeAndImageReferences() {
        val request = ImageGenerationRequest(
            provider = ImageProvider.Fal,
            operation = ImageOperation.Inpaint,
            modelId = "fal-ai/gpt-image-1.5/edit",
            prompt = "替换背景",
            size = "1536x1024",
            seed = 42
        )

        val json = ProviderJson.falInput(request, "data:image/png;base64,a", "data:image/png;base64,b")

        assertEquals("landscape_3_2", json.getString("image_size"))
        assertEquals(42L, json.getLong("seed"))
        assertEquals("data:image/png;base64,a", json.getString("image_url"))
        assertEquals("data:image/png;base64,b", json.getString("mask_url"))
    }

    @Test
    fun extractsUrlsFromCommonProviderShapes() {
        val json = JSONObject()
            .put("images", JSONArray().put(JSONObject().put("url", "https://a.example/1.png")))
            .put("output", JSONArray().put("https://b.example/2.png"))

        val urls = ProviderJson.extractImageUrls(json)

        assertTrue("https://a.example/1.png" in urls)
        assertTrue("https://b.example/2.png" in urls)
    }

    @Test
    fun parsesProviderStatuses() {
        assertEquals(ImageJobStatus.Queued, ProviderStatus.fromFal("IN_QUEUE"))
        assertEquals(ImageJobStatus.Succeeded, ProviderStatus.fromFal("COMPLETED"))
        assertEquals(ImageJobStatus.Running, ProviderStatus.fromReplicate("processing"))
        assertEquals(ImageJobStatus.Canceled, ProviderStatus.fromReplicate("canceled"))
    }
}
