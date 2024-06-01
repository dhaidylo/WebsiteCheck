package com.example.websitecheck

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.Request

open class WebsiteChecker (
    protected val url: String,
    protected val selector: String,
    protected val name: String,
) {
    protected val request = Request.Builder().url(url).build()
    private var hash: String? = null
    protected lateinit var notifier: INotifier

    open fun initialize(context: Context) {
        val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val intent: PendingIntent = PendingIntent.getActivity(context,
            0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notifier = Notifier(context, intent)
    }

    open fun run() {
        val content = Fetcher.fetchHtml(request) ?: return
        val element = getElementBySelector(content, selector) ?: return
        val text = element.text()
        val currentHash = hashContent(text)
        if (hash == null) {
            hash = currentHash
            return
        }
        if (hash != currentHash) {
            hash = currentHash
            notifier?.notify(name)
        }
    }
}