package com.example.websitecheck

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

fun getElementBySelector(html: String, selector: String): Element? {
    val document = Jsoup.parse(html)
    return document.select(selector).firstOrNull()
}

fun getLinks(element: Element): Set<String> {
    val links = element.select("a[href]")
    return links.map { it.attr("href") }.toSet()
}