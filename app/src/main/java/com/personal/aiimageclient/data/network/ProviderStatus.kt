package com.personal.aiimageclient.data.network

import com.personal.aiimageclient.data.model.ImageJobStatus

object ProviderStatus {
    fun fromFal(status: String): ImageJobStatus = when (status.uppercase()) {
        "IN_QUEUE", "QUEUED" -> ImageJobStatus.Queued
        "IN_PROGRESS" -> ImageJobStatus.Running
        "COMPLETED" -> ImageJobStatus.Succeeded
        "FAILED" -> ImageJobStatus.Failed
        "CANCELLED", "CANCELED" -> ImageJobStatus.Canceled
        else -> ImageJobStatus.Running
    }

    fun fromReplicate(status: String): ImageJobStatus = when (status.lowercase()) {
        "queued" -> ImageJobStatus.Queued
        "starting", "processing" -> ImageJobStatus.Running
        "succeeded", "successful" -> ImageJobStatus.Succeeded
        "failed" -> ImageJobStatus.Failed
        "canceled", "cancelled" -> ImageJobStatus.Canceled
        else -> ImageJobStatus.Running
    }
}
