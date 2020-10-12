package com.tfowl.googlapi.okhttp

import com.google.api.client.http.HttpTransport
import com.google.api.client.http.LowLevelHttpRequest
import com.google.api.client.http.LowLevelHttpResponse
import com.google.api.client.util.StreamingContent
import okhttp3.*
import okio.BufferedSink
import java.io.InputStream
import java.util.concurrent.TimeUnit

internal class StreamingContentRequestBody(
    private val content: StreamingContent,
    private val contentType: MediaType,
    private val length: Long
) : RequestBody() {
    override fun contentType(): MediaType = contentType
    override fun contentLength(): Long = length

    override fun writeTo(sink: BufferedSink) {
        content.writeTo(sink.outputStream())
    }
}

internal class Timeouts(val connect: Int, val read: Int)

internal class TimeoutInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val timeouts = request.tag(Timeouts::class.java) ?: return chain.proceed(request)

        return chain
            .withConnectTimeout(timeouts.connect, TimeUnit.MILLISECONDS)
            .withReadTimeout(timeouts.read, TimeUnit.MILLISECONDS)
            .proceed(request)
    }
}

internal class OkHttpLowLevelHttpRequest(
    private val client: OkHttpClient,
    private val requestBuilder: Request.Builder,
    private val method: String
) : LowLevelHttpRequest() {

    override fun addHeader(name: String, value: String) {
        requestBuilder.addHeader(name, value)
    }

    override fun setTimeout(connectTimeout: Int, readTimeout: Int) {
        requestBuilder.tag(Timeouts::class.java, Timeouts(connectTimeout, readTimeout))
    }

    override fun execute(): LowLevelHttpResponse {
        contentEncoding?.let { requestBuilder.addHeader("Content-Encoding", it) }

        requestBuilder.method(method, streamingContent?.let { content ->
            StreamingContentRequestBody(
                content,
                MediaType.get(contentType),
                contentLength
            )
        })

        return OkHttpLowLevelHttpResponse(client.newCall(requestBuilder.build()).execute())
    }
}

internal class OkHttpLowLevelHttpResponse(private val response: Response) : LowLevelHttpResponse() {
    private val body = response.body()

    override fun getContent(): InputStream? = body?.byteStream()

    override fun getContentEncoding(): String? = response.header("Content-Encoding")

    override fun getContentLength(): Long = body?.contentLength() ?: 0

    override fun getContentType(): String? = body?.contentType()?.toString()

    override fun getStatusLine(): String = response.message()

    override fun getStatusCode(): Int = response.code()

    override fun getReasonPhrase(): String? = response.message()

    override fun getHeaderCount(): Int = response.headers().size()

    override fun getHeaderName(index: Int): String = response.headers().name(index)

    override fun getHeaderValue(index: Int): String = response.headers().value(index)

    override fun disconnect() {
        response.close()
    }
}

class OkHttpTransport(defaults: OkHttpClient = OkHttpClient()) : HttpTransport() {
    private val client = defaults.newBuilder().addInterceptor(TimeoutInterceptor()).build()

    override fun buildRequest(method: String, url: String): LowLevelHttpRequest {
        return OkHttpLowLevelHttpRequest(client, Request.Builder().url(url), method)
    }
}