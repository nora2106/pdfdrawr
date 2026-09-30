package dev.nora.pdfdrawr.sync

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser

object WebDavSyncService {
    private val ns: String? = null

    // try to connect to nextcloud instance
    suspend fun testWebdavConnection(url: String, username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val credential = Credentials.basic(username, password);
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", credential)
            .build()

        try {
            client.newCall(request).execute().use { res ->
                Log.d("DebugLog", "Status: ${res.code}")
                res.code == 200 || res.code == 207
            }
        } catch (e: Exception) {
            Log.d("DebugLog", "Verbindung fehlgeschlagen: $e")
            false
        }

    }

    suspend fun getWebdavFolder() = withContext(Dispatchers.IO) {
        val url = "";
        val credential = Credentials.basic("n0ra", "xx");

        val xmlBody = """<?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns" xmlns:nc="http://nextcloud.org/ns">
                <d:prop>
                    <d:getlastmodified/>
                    <d:getcontentlength/>
                    <d:getcontenttype/>
                    <oc:permissions/>
                    <d:resourcetype/>
                    <d:getetag/>
                </d:prop>
            </d:propfind>
        """.trimIndent()

        val requestBody = xmlBody.toRequestBody("application/xml".toMediaType())
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", requestBody)
            .header("Authorization", credential)
            .header("Depth", "1")
            .build()

        client.newCall(request).execute().use { res ->
            res.body.byteStream().use { inputStream ->
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, null)   // null = Encoding automatisch erkennen
                parser.nextTag()
                val entries = readFeed(parser)
                for(entry in entries) {
                    entry.path?.let { Log.d("DebugLog", it) }
                }
            }
        }
    }

    fun readFeed(parser: XmlPullParser): List<WebDavFile> {
        val entries = mutableListOf<WebDavFile>()

        parser.require(XmlPullParser.START_TAG, ns, "d:multistatus")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (parser.name == "d:response") {
                entries.add(readEntry(parser))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    private fun readEntry(parser: XmlPullParser): WebDavFile {
        parser.require(XmlPullParser.START_TAG, ns, "d:response")
        var path: String? = null
        var lastModified: String? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "d:href" -> path = readPath(parser)
                "d:getlastmodified" -> lastModified = readLastModified(parser)
                else -> skip(parser)
            }
        }
        return WebDavFile(path, lastModified)
    }

    private fun readPath(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "d:href")
        val summary = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "d:href")
        return summary
    }

    private fun readLastModified(parser: XmlPullParser): String  {
        parser.require(XmlPullParser.START_TAG, ns, "d:getlastmodified")
        val summary = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "d:getlastmodified")
        return summary
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}

data class WebDavFile(
    val path: String?,
    val lastModified: String?
)