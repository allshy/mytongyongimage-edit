package com.personal.aiimageclient.data.history

import com.personal.aiimageclient.data.model.ImageGenerationRequest
import com.personal.aiimageclient.data.model.ImageGenerationResult
import com.personal.aiimageclient.data.model.ImageJobStatus
import com.personal.aiimageclient.data.model.ImageOperation
import com.personal.aiimageclient.data.model.ImageProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryMapperTest {
    @Test
    fun mapsRequestAndResultIntoPersistableHistory() {
        val request = ImageGenerationRequest(
            provider = ImageProvider.Replicate,
            operation = ImageOperation.TextToImage,
            modelId = "black-forest-labs/flux-schnell",
            prompt = "城市夜景"
        )
        val result = ImageGenerationResult(
            status = ImageJobStatus.Succeeded,
            localImageUris = listOf("file:///one.png", "file:///two.png"),
            remoteUrls = listOf("https://example.com/one.png")
        )

        val entity = HistoryMapper.fromResult(request, result, nowMillis = 1234)

        assertEquals("Replicate", entity.provider)
        assertEquals("TextToImage", entity.operation)
        assertEquals("Succeeded", entity.status)
        assertEquals(listOf("file:///one.png", "file:///two.png"), entity.localUriList())
        assertEquals(1234, entity.createdAtMillis)
    }
}

