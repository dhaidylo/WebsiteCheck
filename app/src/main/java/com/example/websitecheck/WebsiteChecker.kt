package com.example.websitecheck

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Request

open class WebsiteChecker (
    private val url: String,
    private val selector: String,
    protected val name: String,
) {
    private lateinit var _context: Context
    private val request = Request.Builder().url(url).build()
    private var linksSet = hashSetOf<String>()
    protected lateinit var notifier: INotifier

    open fun initialize(serviceContext: Context) {
        _context = serviceContext
        val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val intent: PendingIntent = PendingIntent.getActivity(_context,
            0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notifier = Notifier(_context, intent)
    }

    open fun run() {
        val content = Fetcher.fetchHtml(request) ?: return
        val selectedContent = getElementBySelector(content, selector) ?: return
        val links = getLinks(selectedContent)
        if (links.isEmpty())
            return
        if (linksSet.isEmpty())
            linksSet = links
        else {
            val newLinks = links.subtract(linksSet)
            runBlocking {
                newLinks.forEach { link ->
                    launch {
                        processLink(link)
                        linksSet.add(link)
                    }
                }
            }
        }
    }

    protected open fun processLink(link: String) {
        sendNotification(link)
    }

    protected fun sendNotification(link: String) {
        val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        val intent: PendingIntent = PendingIntent.getActivity(_context,
            0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notifier.notify(name, intent)
    }
}