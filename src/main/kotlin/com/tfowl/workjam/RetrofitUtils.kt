package com.tfowl.workjam

import okhttp3.Interceptor
import okhttp3.Response

internal class HeadersInterceptor(vararg headerStrings: String) : Interceptor {
    private val headers = headerStrings.map {
        val (key, value) = it.split(':', limit = 2)
        key.trim() to value.trim()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        for ((key, value) in headers)
            builder.addHeader(key, value)
        return chain.proceed(builder.build())
    }
}