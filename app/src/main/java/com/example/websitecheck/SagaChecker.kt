package com.example.websitecheck

import org.jsoup.nodes.Element

class SagaChecker() : WebsiteChecker(
    "https://www.saga.hamburg/immobiliensuche?Kategorie=APARTMENT",
    "#APARTMENT",
    "Saga"
) {
    private val _baseURL = "https://www.saga.hamburg"

    override fun processLink(link: Element) {
        val url = link.attr("href")
        if (!urlsSet.contains(url)) {
            val fullUrl = _baseURL + url
            val directUrl = fetchDirectUrl(fullUrl)
            if (directUrl != null) {
                sendNotification(directUrl)
            } else {
                sendNotification(fullUrl)
            }
            urlsSet.add(url)
        }
    }

    private fun fetchDirectUrl(url: String): String? {
        val html = Fetcher.fetchHtml(url) ?: return null
        val element = getElementBySelector(html, "a[href^=https://tenant.immomio.com/apply/]")
        return element?.attr("href")
    }
}