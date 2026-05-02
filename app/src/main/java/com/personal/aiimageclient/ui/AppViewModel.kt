package com.personal.aiimageclient.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.aiimageclient.data.AppContainer
import com.personal.aiimageclient.data.history.HistoryEntity
import com.personal.aiimageclient.data.history.HistoryMapper
import com.personal.aiimageclient.data.model.ImageGenerationRequest
import com.personal.aiimageclient.data.model.ImageJobStatus
import com.personal.aiimageclient.data.model.ImageModelOption
import com.personal.aiimageclient.data.model.ImageOperation
import com.personal.aiimageclient.data.model.ImageProvider
import com.personal.aiimageclient.data.model.ModelCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val provider: ImageProvider = ImageProvider.OpenAI,
    val operation: ImageOperation = ImageOperation.TextToImage,
    val modelId: String = "gpt-image-1",
    val prompt: String = "",
    val size: String = "1024x1024",
    val count: Int = 1,
    val seed: String = "",
    val quality: String = "auto",
    val inputImageUri: String? = null,
    val maskUri: String? = null,
    val isRunning: Boolean = false,
    val status: String = "准备就绪",
    val error: String? = null,
    val resultUris: List<String> = emptyList(),
    val openAiKey: String = "",
    val falKey: String = "",
    val replicateKey: String = "",
    val modelCatalog: List<ImageModelOption> = ModelCatalog.models
) {
    val availableModels: List<ImageModelOption>
        get() = ModelCatalog.forProvider(modelCatalog, provider, operation)
}

class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState(modelCatalog = container.modelCatalog))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<HistoryEntity>> = container.historyRepository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        _uiState.update {
            it.copy(
                openAiKey = container.apiKeyStore.get(ImageProvider.OpenAI),
                falKey = container.apiKeyStore.get(ImageProvider.Fal),
                replicateKey = container.apiKeyStore.get(ImageProvider.Replicate)
            )
        }
    }

    fun setProvider(provider: ImageProvider) {
        _uiState.update { state ->
            val model = ModelCatalog.forProvider(state.modelCatalog, provider, state.operation).firstOrNull()
                ?: ModelCatalog.forProvider(state.modelCatalog, provider, ImageOperation.TextToImage).first()
            val operation = if (state.operation in model.operations) state.operation else model.operations.first()
            state.copy(provider = provider, operation = operation, modelId = model.id)
        }
    }

    fun setOperation(operation: ImageOperation) {
        _uiState.update { state ->
            val model = ModelCatalog.forProvider(state.modelCatalog, state.provider, operation).firstOrNull()
                ?: ModelCatalog.forProvider(state.modelCatalog, state.provider, ImageOperation.TextToImage).first()
            state.copy(operation = if (operation in model.operations) operation else ImageOperation.TextToImage, modelId = model.id)
        }
    }

    fun setModel(modelId: String) = _uiState.update { it.copy(modelId = modelId) }
    fun setPrompt(prompt: String) = _uiState.update { it.copy(prompt = prompt) }
    fun setSize(size: String) = _uiState.update { it.copy(size = size) }
    fun setCount(count: Int) = _uiState.update { it.copy(count = count.coerceIn(1, 4)) }
    fun setSeed(seed: String) = _uiState.update { it.copy(seed = seed.filter { char -> char.isDigit() }.take(12)) }
    fun setQuality(quality: String) = _uiState.update { it.copy(quality = quality) }
    fun setInputImage(uri: String?) = _uiState.update { it.copy(inputImageUri = uri, resultUris = emptyList(), error = null) }
    fun setMask(uri: String?) = _uiState.update { it.copy(maskUri = uri) }

    fun updateKeys(openAi: String, fal: String, replicate: String) {
        container.apiKeyStore.set(ImageProvider.OpenAI, openAi)
        container.apiKeyStore.set(ImageProvider.Fal, fal)
        container.apiKeyStore.set(ImageProvider.Replicate, replicate)
        _uiState.update {
            it.copy(openAiKey = openAi.trim(), falKey = fal.trim(), replicateKey = replicate.trim(), status = "密钥已保存")
        }
    }

    fun clearHistory() {
        viewModelScope.launch { container.historyRepository.clear() }
    }

    fun runGeneration() {
        val state = uiState.value
        val key = keyFor(state.provider, state)
        if (key.isBlank()) {
            _uiState.update { it.copy(error = "请先在设置中填写 ${state.provider.label} API Key") }
            return
        }
        if (state.prompt.isBlank() && state.operation != ImageOperation.Upscale) {
            _uiState.update { it.copy(error = "请先输入提示词") }
            return
        }
        if (state.operation != ImageOperation.TextToImage && state.inputImageUri == null) {
            _uiState.update { it.copy(error = "请先选择要编辑的图片") }
            return
        }

        val request = ImageGenerationRequest(
            provider = state.provider,
            operation = state.operation,
            modelId = state.modelId,
            prompt = state.prompt,
            inputImageUri = state.inputImageUri,
            maskUri = state.maskUri,
            size = state.size,
            count = state.count,
            seed = state.seed.toLongOrNull(),
            quality = state.quality
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, status = ImageJobStatus.Running.label, error = null) }
            val client = when (request.provider) {
                ImageProvider.OpenAI -> container.openAiClient
                ImageProvider.Fal -> container.falClient
                ImageProvider.Replicate -> container.replicateClient
            }
            val result = client.run(key, request)
            container.historyRepository.save(HistoryMapper.fromResult(request, result))
            _uiState.update {
                it.copy(
                    isRunning = false,
                    status = result.status.label,
                    error = result.error,
                    resultUris = result.localImageUris
                )
            }
        }
    }

    private fun keyFor(provider: ImageProvider, state: AppUiState): String = when (provider) {
        ImageProvider.OpenAI -> state.openAiKey
        ImageProvider.Fal -> state.falKey
        ImageProvider.Replicate -> state.replicateKey
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(container) as T
    }
}
