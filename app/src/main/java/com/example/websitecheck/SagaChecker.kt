package com.example.websitecheck

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Request

class SagaChecker() : WebsiteChecker(
    "https://www.saga.hamburg/immobiliensuche?Kategorie=APARTMENT",
    "#APARTMENT",
    "Saga"
) {
    private lateinit var _context: Context
    private val baseURL = "https://www.saga.hamburg"
    private var linksSet = emptySet<String>()

    override fun initialize(context: Context) {
        super.initialize(context)
        _context = context
    }

    override fun run() {
        val content = Fetcher.fetchHtml(request) ?: return
        val selectedContent = getElementBySelector(content, selector) ?: return
        val links = getLinks(selectedContent)
        if (links.isEmpty())
            return
        if (linksSet.isNotEmpty()) {
            val newLinks = links.subtract(linksSet)
            runBlocking {
                newLinks.forEach { relativeLink ->
                    launch {
                        val fullLink = baseURL + relativeLink
                        val directLink = fetchDirectLink(fullLink)
                        if (directLink != null) {
                            sendNotification(directLink, _context)
                        }
                        else
                            notifier.notify(name)
                    }
                }
            }
        }
        linksSet = links
    }

    private fun fetchDirectLink(url: String): String? {
        val request = Request.Builder().url(url).build()
        val html = Fetcher.fetchHtml(request) ?: return null
        val element = getElementBySelector(html, "a[href^=https://tenant.immomio.com/apply/]")
        return element?.attr("href")
    }

    private fun sendNotification(link: String, context: Context) {
        val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        val intent: PendingIntent = PendingIntent.getActivity(context,
            0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notifier.notify(name, intent)
    }
}