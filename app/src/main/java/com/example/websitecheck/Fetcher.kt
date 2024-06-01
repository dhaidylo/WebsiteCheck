package com.example.websitecheck

import okhttp3.OkHttpClient
import okhttp3.Request

class Fetcher {
    companion object {
        private val okHttpClient = OkHttpClient()

        fun fetchHtml(request: Request): String? {
            return try {
                val response = okHttpClient.newCall(request).execute()
                response.body?.string()
            } catch (e: Exception) {
                log("Error fetching content: ${e.message}")
                null
            }
        }
    }
}