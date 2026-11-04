package com.studyhub.app.util

import java.net.URI
object ApiEndpointPolicy {
    fun allowed(url: String, debug: Boolean): Boolean = try {
        val uri = URI(url)
        val host = uri.host.orEmpty().lowercase()
        val local = host == "localhost" || host == "127.0.0.1" || host.startsWith("10.") || host.startsWith("192.168.") ||
            (host.startsWith("172.") && (host.split('.').getOrNull(1)?.toIntOrNull() ?: 0) in 16..31)
        host.isNotBlank() && host != "example.com" && uri.userInfo == null && uri.query == null && uri.fragment == null &&
            (uri.scheme == "https" || (debug && uri.scheme == "http" && local &&
                (host == "localhost" || host.split('.').let { it.size == 4 && it.all { part -> part.toIntOrNull()?.let { number -> number in 0..255 } == true } })))
    } catch (_: Exception) { false }
}
