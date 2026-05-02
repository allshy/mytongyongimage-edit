package com.personal.aiimageclient.data

import android.content.Context
import androidx.room.Room
import com.personal.aiimageclient.data.history.HistoryDatabase
import com.personal.aiimageclient.data.history.HistoryRepository
import com.personal.aiimageclient.data.model.ModelCatalog
import com.personal.aiimageclient.data.network.FalImageClient
import com.personal.aiimageclient.data.network.ImageDownloader
import com.personal.aiimageclient.data.network.OpenAiImageClient
import com.personal.aiimageclient.data.network.ReplicateImageClient
import com.personal.aiimageclient.data.secure.ApiKeyStore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val apiKeyStore = ApiKeyStore(appContext)
    val modelCatalog = ModelCatalog.load(appContext)

    private val database = Room.databaseBuilder(
        appContext,
        HistoryDatabase::class.java,
        "ai_image_history.db"
    ).build()

    val historyRepository = HistoryRepository(database.historyDao())
    val downloader = ImageDownloader(appContext, httpClient)

    val openAiClient = OpenAiImageClient(appContext, httpClient, downloader)
    val falClient = FalImageClient(appContext, httpClient, downloader)
    val replicateClient = ReplicateImageClient(appContext, httpClient, downloader)
}
