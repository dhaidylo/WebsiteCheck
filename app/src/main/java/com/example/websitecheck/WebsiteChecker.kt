package com.example.websitecheck

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

open class WebsiteChecker (
    private val _url: String,
    private val _selector: String,
    private val _name: String,
) {
    private lateinit var _context: Context
    private val _request = Request.Builder().url(_url).build()
    private lateinit var _notifier: INotifier

    protected lateinit var urlsSet: HashSet<String>

    open fun initialize(context: Context) {
        _context = context
        val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(_url))
        val intent: PendingIntent = PendingIntent.getActivity(_context,
            0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        _notifier = Notifier(_context, intent)
        urlsSet = getLinks()?.map { it.attr("href") }?.toHashSet() ?: HashSet()
    }

    fun run() {
        val links = getLinks() ?: return
        if (links.isEmpty()) return

        for (link in links) {
            processLink(link)
        }
    }

    protected open fun processLink(link: Element) {
        val url = link.attr("href")
        if (!urlsSet.contains(url)) {
            sendNotification(url)
            urlsSet.add(url)
        }
    }

    private fun getLinks(): Elements? {
        val content = Fetcher.fetchHtml(_request) ?: return null
        val element = getElementBySelector(content, _selector) ?: return null
        return element.select("a[href]")
    }

    protected fun getElementBySelector(html: String, selector: String): Element? {
        val document = Jsoup.parse(html)
        return document.select(selector).firstOrNull()
    }

    protected fun sendNotification(url: String) {
        val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val intent: PendingIntent = PendingIntent.getActivity(_context,
            0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        _notifier.notify(_name, intent)
    }
}