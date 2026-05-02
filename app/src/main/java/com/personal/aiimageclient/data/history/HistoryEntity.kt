package com.personal.aiimageclient.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.personal.aiimageclient.data.model.ImageGenerationRequest
import com.personal.aiimageclient.data.model.ImageGenerationResult
import com.personal.aiimageclient.data.model.ImageJobStatus
import com.personal.aiimageclient.data.model.ImageOperation
import com.personal.aiimageclient.data.model.ImageProvider

@Entity(tableName = "generation_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val provider: String,
    val modelId: String,
    val operation: String,
    val prompt: String,
    val status: String,
    val localUris: String,
    val remoteUrls: String,
    val error: String?,
    val createdAtMillis: Long
)

fun HistoryEntity.localUriList(): List<String> =
    localUris.split("\n").filter { it.isNotBlank() }

object HistoryMapper {
    fun fromResult(
        request: ImageGenerationRequest,
        result: ImageGenerationResult,
        nowMillis: Long = System.currentTimeMillis()
    ): HistoryEntity = HistoryEntity(
        provider = request.provider.name,
        modelId = request.modelId,
        operation = request.operation.name,
        prompt = request.prompt,
        status = result.status.name,
        localUris = result.localImageUris.joinToString("\n"),
        remoteUrls = result.remoteUrls.joinToString("\n"),
        error = result.error,
        createdAtMillis = nowMillis
    )

    fun providerOf(entity: HistoryEntity): ImageProvider? =
        runCatching { ImageProvider.valueOf(entity.provider) }.getOrNull()

    fun operationOf(entity: HistoryEntity): ImageOperation? =
        runCatching { ImageOperation.valueOf(entity.operation) }.getOrNull()

    fun statusOf(entity: HistoryEntity): ImageJobStatus? =
        runCatching { ImageJobStatus.valueOf(entity.status) }.getOrNull()
}

