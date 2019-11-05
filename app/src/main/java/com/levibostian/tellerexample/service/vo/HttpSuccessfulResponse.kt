package com.levibostian.tellerexample.service.vo

import okhttp3.Headers

data class HttpSuccessfulResponse<RESPONSE: Any>(val statusCode: Int, val responseBody: RESPONSE?, val responseHeaders: com.levibostian.tellerexample.service.vo.Headers)

data class Headers(private val rawHeaders: Headers) {
    fun get(key: String): String? = rawHeaders.get(key)
}