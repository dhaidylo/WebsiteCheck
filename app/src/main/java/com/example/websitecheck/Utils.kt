package com.example.websitecheck

import android.util.Log
import java.security.MessageDigest

fun log(msg: String) {
    Log.d("WEBSITE-CHECK-SERVICE", msg)
}

fun hashContent(content: String): String {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val hashBytes = messageDigest.digest(content.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}