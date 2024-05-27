package com.example.websitecheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.security.MessageDigest

class WebsiteChecker (
    val url: String,
    private val selector: String,
    val name: String
) {
    companion object{
        private val okHttpClient = OkHttpClient()
    }

    private val request = Request.Builder().url(url).build()
    private var hash: String? = null

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            hash = hashContent(fetchContent())
        }
    }
    fun check(): Boolean {
        val content = fetchContent()
        val currentHash = hashContent(content)
        if (hash != currentHash) {
            hash = currentHash
            return true
        }
        return false
    }

    private fun fetchContent(): String {
        return try {
            val response = okHttpClient.newCall(request).execute()
            val websiteContent = response.body?.string() ?: ""
            fetchContentBySelector(websiteContent, selector)
        } catch (e: Exception) {
            log("Error fetching content: ${e.message}")
            ""
        }
    }

    private fun fetchContentBySelector(html: String, selector: String): String {
        val document = Jsoup.parse(html)
        val divElement = document.select(selector).firstOrNull()
        return divElement?.text() ?: ""
    }

    private fun hashContent(content: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}