package dev.nora.pdfdrawr.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import androidx.core.content.FileProvider
import dev.nora.pdfdrawr.storage.PreferenceStorage.getStoredNextcloudCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.io.File

object FolderSyncManager {
    private val ns: String? = null

    /**
     * Get list of files from specified folder.
     *
     * @param context Context
     * @param path Path of the specified folder
     * @return List of files
     */
    suspend fun syncFolder(context: Context, path: String?): List<WebDavFile> = withContext(Dispatchers.IO) {
        val fileList: MutableList<WebDavFile> = mutableListOf()
        val storedCredentials =
            getStoredNextcloudCredentials(context) ?: return@withContext fileList
        val url = storedCredentials.url
        val credential = Credentials.basic(storedCredentials.username, storedCredentials.password)

        val xmlBody = """<?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns" xmlns:nc="http://nextcloud.org/ns">
                <d:prop>
                    <d:getlastmodified/>
                    <d:getcontentlength/>
                    <d:getcontenttype/>
                    <d:resourcetype/>
                    <d:displayname/>
                    <d:getetag/>
                </d:prop>
            </d:propfind>
        """.trimIndent()

        val requestBody = xmlBody.toRequestBody("application/xml".toMediaType())
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url + path)
            .method("PROPFIND", requestBody)
            .header("Authorization", credential)
            .header("Depth", "1")
            .build()

        try {
            client.newCall(request).execute().use { res ->
                res.body.byteStream().use { inputStream ->
                    val parser: XmlPullParser = Xml.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(inputStream, null)   // null = Encoding automatisch erkennen
                    parser.nextTag()
                    val entries = WebDavXmlParser.readFeed(parser)
                    for (entry in entries) {
                        fileList.add(entry)
                    }
                }
            }
        }
        catch (e: Exception) {
            Log.d("DebugLog", e.toString())
        }
        return@withContext fileList
    }

    /**
     * Initiate file download
     *
     * @param context Context
     * @param fileName Name of the file
     * @param path Path to the parent directory
     */
    suspend fun getFile(context: Context, fileName: String?, path: String?): File {
        return WebDavSyncService.downloadFile(context, fileName, path)
    }

    // TODO currently unused
    fun getFileFromAssets(context: Context, fileName: String): File {
        val file = File(context.cacheDir, fileName)

        if (!file.exists()) {
            val outputStream = file.outputStream()
            val inputStream = context.assets.open(fileName)
            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()
        }

        return file
    }
}

data class WebDavFile(
    val path: String?,
    val lastModified: String?,
    val isDirectory: Boolean,
    val type: String?,
    val name: String?,
    val size: String?
)

