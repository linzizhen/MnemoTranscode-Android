package com.mtc.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.mtc.app.di.RetrofitHolder
import com.mtc.app.ui.navigation.MtcNavHost
import com.mtc.app.ui.navigation.Screen
import com.mtc.app.ui.screens.auth.ServerConfigScreen
import com.mtc.app.ui.theme.MtcTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var retrofitHolder: RetrofitHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            // 会话级状态：配置完成前 false，完成后 true
            val (isConfigured, setConfigured) = remember { mutableStateOf(false) }

            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                MtcTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (isConfigured) {
                            true -> {
                                MtcNavHost(startDestination = Screen.Login.route)
                            }
                            else -> {
                                ServerConfigScreen(
                                    onServerConfigured = {
                                        val savedUrl = kotlinx.coroutines.runBlocking {
                                            com.mtc.app.data.remote.ServerManager.getServerUrlSync(this@MainActivity)
                                        }
                                        if (savedUrl != null) {
                                            retrofitHolder.notifyUrlChanged(savedUrl)
                                            setConfigured(true)
                                        }
                                    },
                                    onNavigateToLogin = {
                                        // 不允许跳过
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
