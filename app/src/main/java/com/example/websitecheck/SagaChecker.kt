package com.example.websitecheck

import okhttp3.Request

class SagaChecker() : WebsiteChecker(
    "https://www.saga.hamburg/immobiliensuche?Kategorie=APARTMENT",
    "#APARTMENT",
    "Saga"
) {
    private val baseURL = "https://www.saga.hamburg"

    override fun processLink(link: String) {
        val fullLink = baseURL + link
        val directLink = fetchDirectLink(fullLink)
        if (directLink != null) {
            sendNotification(directLink)
        } else
            notifier.notify(name)
    }

    private fun fetchDirectLink(url: String): String? {
        val request = Request.Builder().url(url).build()
        val html = Fetcher.fetchHtml(request) ?: return null
        val element = getElementBySelector(html, "a[href^=https://tenant.immomio.com/apply/]")
        return element?.attr("href")
    }
}