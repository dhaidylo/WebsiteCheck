package com.example.websitecheck

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class SagaChecker(okHttpClient: OkHttpClient) : WebsiteChecker(
    "https://www.saga.hamburg/immobiliensuche?Kategorie=APARTMENT",
    "#APARTMENT",
    "Saga",
    okHttpClient
) {
    private val baseURL = "https://www.saga.hamburg"
    private var linksSet = emptySet<String>()

    override fun check() {
        val content = fetchContent(baseRequest) ?: return
        val selectedContent = fetchContentBySelector(content, selector) ?: return
        val links = fetchLinks(selectedContent)
        if (links.isEmpty())
            return
        if (linksSet.isNotEmpty()) {
            val newLinks = links.subtract(linksSet)
            runBlocking {
                newLinks.forEach { relativeLink ->
                    launch {
                        val fullLink = baseURL + relativeLink
                        val directLink = fetchDirectLink(fullLink)
                        if (directLink != null)
                            notifier.notify(directLink)
                        else
                            notifier.notify(fullLink)
                    }
                }
            }
        }
        linksSet = links
    }

    private fun fetchLinks(element: Element): Set<String> {
        val links = element.select("a[href]")
        return links.map { it.attr("href") }.toSet()
    }

    private fun fetchDirectLink(url: String): String? {
        val request = Request.Builder().url(url).build()
        val content = fetchContent(request) ?: return null
        val document = Jsoup.parse(content)
        return document.select("a[href^=https://tenant.immomio.com/apply/]").firstOrNull()?.attr("href")
    }
}