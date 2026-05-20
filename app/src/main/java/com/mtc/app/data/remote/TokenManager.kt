package com.mtc.app.data.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mtc_prefs")

object TokenManager {
    private val TOKEN_KEY = stringPreferencesKey("access_token")
    private var cachedToken: String? = null

    suspend fun saveToken(context: Context, token: String) {
        cachedToken = token
        context.dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[TOKEN_KEY] = token
            }
        }
    }

    suspend fun getToken(context: Context): String? {
        return cachedToken ?: context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }.first().also { cachedToken = it }
    }

    fun getTokenSync(context: Context): String? {
        if (cachedToken == null) {
            runBlocking {
                cachedToken = getToken(context)
            }
        }
        return cachedToken
    }

    suspend fun clearToken(context: Context) {
        cachedToken = null
        context.dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                remove(TOKEN_KEY)
            }
        }
    }

    fun invalidateCache() {
        cachedToken = null
    }
}

/**
 * OkHttp 认证拦截器，自动添加 Token
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = TokenManager.getTokenSync(context)
        return if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
