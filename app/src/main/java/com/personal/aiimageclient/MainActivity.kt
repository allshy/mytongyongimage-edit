package com.personal.aiimageclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.aiimageclient.ui.AiImageApp
import com.personal.aiimageclient.ui.AppViewModel
import com.personal.aiimageclient.ui.AppViewModelFactory
import com.personal.aiimageclient.ui.theme.AIImageClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as AiImageApplication).container
        setContent {
            AIImageClientTheme {
                Surface {
                    val appViewModel: AppViewModel = viewModel(
                        factory = AppViewModelFactory(container)
                    )
                    AiImageApp(appViewModel)
                }
            }
        }
    }
}

