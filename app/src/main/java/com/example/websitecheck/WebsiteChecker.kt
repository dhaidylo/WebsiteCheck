package com.example.websitecheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.security.MessageDigest

class WebsiteChecker (
    url: String,
    private val selector: String,
    private val okHttpClient: OkHttpClient,
    val name: String
) {
    private val request = Request.Builder().url(url).build()
    private var hash: String? = null

    suspend fun check(): Boolean {
        val currentHash = hashContent(fetchContent())
        val result = hash != null && currentHash != hash
        hash = currentHash
        return result
    }

    private suspend fun fetchContent(): String = withContext(Dispatchers.IO) {
        try {
            val response = okHttpClient.newCall(request).execute()
            val websiteContent = response.body?.string() ?: ""
            fetchContentBySelector(websiteContent, selector)
        } catch (e: Exception) {
            log("Error fetching content: ${e.message}")
            "" // Возвращаем пустую строку в случае ошибки
        }
    }

    private fun fetchContentBySelector(html: String, selector: String): String {
        val document = Jsoup.parse(html)
        val divElement = document.select(selector).firstOrNull()
        return divElement?.html() ?: ""
    }

    private fun hashContent(content: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}