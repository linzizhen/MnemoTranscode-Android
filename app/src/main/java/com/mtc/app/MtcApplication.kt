package com.mtc.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "mtc_prefs")

@HiltAndroidApp
class MtcApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(this))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .crossfade(true)
            .crossfade(300)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .networkCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .okHttpClient(okHttpClient)
            .respectCacheHeaders(false)
            .build()
    }
}

private class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = getTokenSync()
        return if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }

    private fun getTokenSync(): String? {
        return try {
            runBlocking {
                context.dataStore.data.map { it[androidx.datastore.preferences.core.stringPreferencesKey("access_token")] }.first()
            }
        } catch (_: Exception) {
            null
        }
    }
}
