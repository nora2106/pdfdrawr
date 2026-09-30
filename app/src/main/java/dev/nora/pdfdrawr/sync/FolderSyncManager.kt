package dev.nora.pdfdrawr.sync

import android.content.Context
import android.util.Log
import android.util.Xml
import dev.nora.pdfdrawr.storage.PreferenceStorage.getStoredNextcloudCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser

object FolderSyncManager {
    private val ns: String? = null

    // TODO specify folder via parameters
    suspend fun syncFolder(context: Context): List<WebDavFile> = withContext(Dispatchers.IO) {
        val fileList: MutableList<WebDavFile> = mutableListOf()
        val storedCredentials = getStoredNextcloudCredentials(context) ?: return@withContext fileList
        val url = storedCredentials.url
        val credential = Credentials.basic(storedCredentials.username, storedCredentials.password)

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
                    fileList.add(entry)
                }
            }
        }
        return@withContext fileList
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