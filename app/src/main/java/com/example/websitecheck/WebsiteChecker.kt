package com.example.websitecheck

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.security.MessageDigest

class WebsiteChecker (
    private val url: String,
    private val selector: String,
    private val name: String
) {
    companion object{
        private val okHttpClient = OkHttpClient()
    }
    private val request = Request.Builder().url(url).build()
    private var hash: String? = null
    private lateinit var notifier: Notifier
    fun initialize(context: Context) {
        val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val intent: PendingIntent = PendingIntent.getActivity(context, 0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = Notification.Builder(context, Notifier.CHANNEL_ID)
            .setContentTitle("Website Check")
            .setContentText(name)
            .setContentIntent(intent)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
        notifier = Notifier(builder)
    }

    fun check() {
        val content = fetchContent()
        val currentHash = hashContent(content)
        if (hash == null) {
            hash = currentHash
            return
        }
        if (hash != currentHash) {
            hash = currentHash
            notifier.notify()
        }
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