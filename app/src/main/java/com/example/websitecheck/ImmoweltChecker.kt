package com.example.websitecheck

import android.content.Context
import org.jsoup.nodes.Element
import java.util.regex.Pattern

class ImmoweltChecker(context: Context) : WebsiteChecker(
    context,
    "https://www.immowelt.de/suche/hamburg/wohnungen/mieten?ama=55&ami=30&d=true&pma=600&r=20&sd=DESC&sf=TIMESTAMP&sp=1",
    ".SearchList-22b2e",
    "Immowelt"
) {
    override fun processLink(link: Element) {
        val url = link.attr("href")
        if (!urlsSet.contains(url)) {
            urlsSet.add(url)

            // Check if it has "Neu" label
            if (link.select(".badge-ca1b3").isEmpty()) return

            val text = link.text()
            when {
                text.contains("TAUSCHWOHNUNG", true) -> return
                text.contains("Vermietungshotline der SAGA Unternehmensgruppe") -> {
                    val directUrl = fetchSagaUrl(url)
                    sendNotification(directUrl ?: url)
                }
                else -> sendNotification(url)
            }
        }
    }

    private fun fetchSagaUrl(url: String) : String? {
        val html = Fetcher.fetchHtml(url)
        return extractSagaUrl(html.toString())
    }

    private fun extractSagaUrl(text: String): String? {
        val pattern = Pattern.compile("(https://rdr\\.immomio\\.com/\\w+)")
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
}