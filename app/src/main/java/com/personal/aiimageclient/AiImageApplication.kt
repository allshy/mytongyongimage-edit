package com.personal.aiimageclient

import android.app.Application
import com.personal.aiimageclient.data.AppContainer

class AiImageApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

