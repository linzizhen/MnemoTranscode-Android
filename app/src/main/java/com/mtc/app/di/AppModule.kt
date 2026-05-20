package com.mtc.app.di

import android.content.Context
import androidx.room.Room
import com.mtc.app.BuildConfig
import com.mtc.app.data.local.*
import com.mtc.app.data.remote.AuthInterceptor
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.data.remote.ServerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit 单例持有 context 以便动态获取 baseUrl
    @Provides
    @Singleton
    fun provideRetrofitHolder(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): RetrofitHolder {
        return RetrofitHolder(context, okHttpClient)
    }
}

/**
 * Retrofit 持有器，支持动态更新 baseUrl
 * 优化：只在 URL 真正改变时才重建 Retrofit，避免每次同步阻塞 DataStore
 */
class RetrofitHolder(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    @Synchronized
    fun getRetrofit(): Retrofit {
        // 缓存命中：直接返回
        if (retrofit != null) {
            return retrofit!!
        }

        // 首次初始化或缓存失效：从 DataStore 读取并重建
        val baseUrl: String = kotlinx.coroutines.runBlocking {
            ServerManager.getServerUrlSync(context)
        } ?: ServerManager.DEFAULT_SERVER_URL

        currentBaseUrl = baseUrl
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit!!
    }

    // 外部通知 URL 变化时调用（如用户在设置页改了 IP）
    fun notifyUrlChanged(newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (formatted != currentBaseUrl) {
            synchronized(this) {
                retrofit = null
                currentBaseUrl = formatted
            }
        }
    }

    fun getBaseUrl(): String {
        return currentBaseUrl ?: kotlinx.coroutines.runBlocking {
            ServerManager.getServerUrlSync(context)
        } ?: "http://26.240.64.54:8000/"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MtcDatabase {
        return Room.databaseBuilder(
            context,
            MtcDatabase::class.java,
            "mtc_database"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideArchiveDao(database: MtcDatabase): ArchiveDao {
        return database.archiveDao()
    }

    @Provides
    @Singleton
    fun provideMemberDao(database: MtcDatabase): MemberDao {
        return database.memberDao()
    }

    @Provides
    @Singleton
    fun provideMemoryDao(database: MtcDatabase): MemoryDao {
        return database.memoryDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMtcApiService(retrofitHolder: RetrofitHolder): MtcApiService {
        return retrofitHolder.getRetrofit().create(MtcApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: MtcApiService,
        @ApplicationContext context: Context
    ): com.mtc.app.domain.repository.AuthRepository {
        return com.mtc.app.data.repository.AuthRepositoryImpl(apiService, context)
    }

    @Provides
    @Singleton
    fun provideArchiveRepository(
        apiService: MtcApiService,
        archiveDao: ArchiveDao,
        memberDao: MemberDao,
        memoryDao: MemoryDao
    ): com.mtc.app.domain.repository.ArchiveRepository {
        return com.mtc.app.data.repository.ArchiveRepositoryImpl(apiService, archiveDao, memberDao, memoryDao)
    }

    @Provides
    @Singleton
    fun provideMemoryRepository(
        apiService: MtcApiService,
        memoryDao: MemoryDao
    ): com.mtc.app.domain.repository.MemoryRepository {
        return com.mtc.app.data.repository.MemoryRepositoryImpl(apiService, memoryDao)
    }

    @Provides
    @Singleton
    fun provideDialogueRepository(
        apiService: MtcApiService,
        @ApplicationContext context: android.content.Context
    ): com.mtc.app.domain.repository.DialogueRepository {
        return com.mtc.app.data.repository.DialogueRepositoryImpl(apiService, context)
    }

    @Provides
    @Singleton
    fun provideStorybookRepository(
        apiService: MtcApiService
    ): com.mtc.app.domain.repository.StorybookRepository {
        return com.mtc.app.data.repository.StorybookRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideCapsuleRepository(
        apiService: MtcApiService
    ): com.mtc.app.domain.repository.CapsuleRepository {
        return com.mtc.app.data.repository.CapsuleRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideMediaRepository(
        apiService: MtcApiService
    ): com.mtc.app.domain.repository.MediaRepository {
        return com.mtc.app.data.repository.MediaRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun providePreferencesRepository(
        apiService: MtcApiService,
        @ApplicationContext context: android.content.Context
    ): com.mtc.app.domain.repository.PreferencesRepository {
        return com.mtc.app.data.repository.PreferencesRepositoryImpl(apiService, context)
    }
}
