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
}