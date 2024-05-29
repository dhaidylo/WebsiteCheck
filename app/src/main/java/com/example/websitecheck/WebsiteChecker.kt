package com.example.websitecheck

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.security.MessageDigest

open class WebsiteChecker (
    protected val url: String,
    protected val selector: String,
    protected val name: String,
    protected val okHttpClient: OkHttpClient
) {
    protected val baseRequest = Request.Builder().url(url).build()
    private var hash: String? = null
    protected lateinit var notifier: Notifier

    fun initialize(context: Context) {
        notifier = Notifier(context, name, url)
    }

    open fun check() {
        val content = fetchContent(baseRequest) ?: return
        val selectedContent = fetchContentBySelector(content, selector) ?: return
        val text = selectedContent.text()
        val currentHash = hashContent(text)
        if (hash == null) {
            hash = currentHash
            return
        }
        if (hash != currentHash) {
            hash = currentHash
            notifier.notify()
        }
    }

    protected fun fetchContent(request: Request): String? {
        return try {
            val response = okHttpClient.newCall(request).execute()
            response.body?.string()
            //fetchContentBySelector(websiteContent, selector)
        } catch (e: Exception) {
            log("Error fetching content: ${e.message}")
            null
        }
    }

    protected fun fetchContentBySelector(html: String, selector: String): Element? {
        val document = Jsoup.parse(html)
        return document.select(selector).firstOrNull()
    }

    protected fun hashContent(content: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}