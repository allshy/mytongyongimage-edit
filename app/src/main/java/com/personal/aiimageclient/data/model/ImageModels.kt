package com.personal.aiimageclient.data.model

enum class ImageProvider(val label: String) {
    OpenAI("OpenAI"),
    Fal("fal.ai"),
    Replicate("Replicate")
}

enum class ImageOperation(val label: String) {
    TextToImage("文生图"),
    ImageEdit("参考图编辑"),
    Inpaint("局部重绘"),
    Upscale("放大")
}

enum class ImageJobStatus(val label: String) {
    Queued("排队中"),
    Running("生成中"),
    Succeeded("已完成"),
    Failed("失败"),
    Canceled("已取消")
}

data class ImageModelOption(
    val id: String,
    val label: String,
    val provider: ImageProvider,
    val operations: Set<ImageOperation>,
    val defaultSize: String = "1024x1024",
    val supportsMask: Boolean = false,
    val replicateUseModelEndpoint: Boolean = true
)

data class ImageGenerationRequest(
    val provider: ImageProvider,
    val operation: ImageOperation,
    val modelId: String,
    val prompt: String,
    val inputImageUri: String? = null,
    val maskUri: String? = null,
    val size: String = "1024x1024",
    val count: Int = 1,
    val seed: Long? = null,
    val quality: String = "auto",
    val extraParams: Map<String, String> = emptyMap()
)

data class ImageGenerationResult(
    val status: ImageJobStatus,
    val localImageUris: List<String> = emptyList(),
    val remoteUrls: List<String> = emptyList(),
    val revisedPrompt: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val error: String? = null
)

