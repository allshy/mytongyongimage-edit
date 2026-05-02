package com.personal.aiimageclient.data.network

import com.personal.aiimageclient.data.model.ImageGenerationRequest
import com.personal.aiimageclient.data.model.ImageGenerationResult

interface ImageProviderClient {
    suspend fun run(apiKey: String, request: ImageGenerationRequest): ImageGenerationResult
}

