package com.example.patientvisits.data.remote

import com.example.patientvisits.data.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.token() }
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
            .build()
        return chain.proceed(request)
    }
}
