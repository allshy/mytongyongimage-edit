package com.personal.aiimageclient.data.model

import android.content.Context
import org.json.JSONArray

object ModelCatalog {
    val defaultModels = listOf(
        ImageModelOption(
            id = "gpt-image-1",
            label = "GPT Image 1",
            provider = ImageProvider.OpenAI,
            operations = setOf(ImageOperation.TextToImage, ImageOperation.ImageEdit, ImageOperation.Inpaint),
            supportsMask = true
        ),
        ImageModelOption(
            id = "dall-e-3",
            label = "DALL-E 3",
            provider = ImageProvider.OpenAI,
            operations = setOf(ImageOperation.TextToImage)
        ),
        ImageModelOption(
            id = "fal-ai/flux/dev",
            label = "Flux Dev",
            provider = ImageProvider.Fal,
            operations = setOf(ImageOperation.TextToImage, ImageOperation.ImageEdit)
        ),
        ImageModelOption(
            id = "fal-ai/gpt-image-1.5/edit",
            label = "GPT Image 1.5 Edit on fal",
            provider = ImageProvider.Fal,
            operations = setOf(ImageOperation.ImageEdit, ImageOperation.Inpaint),
            supportsMask = true
        ),
        ImageModelOption(
            id = "fal-ai/realesrgan",
            label = "RealESRGAN Upscale",
            provider = ImageProvider.Fal,
            operations = setOf(ImageOperation.Upscale)
        ),
        ImageModelOption(
            id = "black-forest-labs/flux-schnell",
            label = "Flux Schnell",
            provider = ImageProvider.Replicate,
            operations = setOf(ImageOperation.TextToImage)
        ),
        ImageModelOption(
            id = "stability-ai/stable-diffusion",
            label = "Stable Diffusion",
            provider = ImageProvider.Replicate,
            operations = setOf(ImageOperation.TextToImage, ImageOperation.ImageEdit)
        )
    )

    val models: List<ImageModelOption>
        get() = defaultModels

    fun load(context: Context): List<ImageModelOption> = runCatching {
        val raw = context.assets.open("model_catalog.json").bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val provider = ImageProvider.valueOf(item.getString("provider"))
                val operationsJson = item.getJSONArray("operations")
                val operations = buildSet {
                    for (operationIndex in 0 until operationsJson.length()) {
                        add(ImageOperation.valueOf(operationsJson.getString(operationIndex)))
                    }
                }
                add(
                    ImageModelOption(
                        id = item.getString("id"),
                        label = item.optString("label", item.getString("id")),
                        provider = provider,
                        operations = operations,
                        defaultSize = item.optString("defaultSize", "1024x1024"),
                        supportsMask = item.optBoolean("supportsMask", false)
                    )
                )
            }
        }.ifEmpty { defaultModels }
    }.getOrElse { defaultModels }

    fun forProvider(
        models: List<ImageModelOption>,
        provider: ImageProvider,
        operation: ImageOperation
    ): List<ImageModelOption> = models.filter { it.provider == provider && operation in it.operations }

    fun forProvider(provider: ImageProvider, operation: ImageOperation): List<ImageModelOption> =
        forProvider(defaultModels, provider, operation)
}
