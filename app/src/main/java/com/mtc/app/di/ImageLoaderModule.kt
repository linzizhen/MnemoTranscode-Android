package com.mtc.app.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/** 持有 auth-aware ImageLoader 的包装类 */
class AuthImageLoaderEntry @Inject constructor(
    val imageLoader: ImageLoader
)

/**
 * 为 Coil 提供带认证拦截器的 ImageLoader。
 * 用于加载需要身份验证的图片（如成员头像、媒体库图片）。
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideAuthImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .crossfade(300)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
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

    @Provides
    @Singleton
    fun provideAuthImageLoaderEntry(
        imageLoader: ImageLoader
    ): AuthImageLoaderEntry = AuthImageLoaderEntry(imageLoader)
}
