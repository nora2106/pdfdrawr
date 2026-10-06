package dev.nora.pdfdrawr.sync

import android.util.Log
import dev.nora.pdfdrawr.storage.PreferenceStorage.getStoredNextcloudCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import android.content.Context
import okio.IOException

object WebDavSyncService {
    // try to connect to nextcloud instance
    suspend fun testWebdavConnection(url: String, username: String, password: String): Boolean = withContext(Dispatchers.IO) {
            val credential = Credentials.basic(username, password)
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .build()

            try {
                client.newCall(request).execute().use { res ->
                    res.code == 200 || res.code == 207
                }
            } catch (e: Exception) {
                Log.d("DebugLog", "Verbindung fehlgeschlagen: $e")
                false
            }
        }

    suspend fun downloadFile(context: Context, fileName: String?, path: String?): File = withContext(Dispatchers.IO){
        if(fileName == null) throw IllegalArgumentException("File hat keinen Namen")

        val storedCredentials = getStoredNextcloudCredentials(context) ?: throw IllegalStateException("Keine Zugangsdaten")
        val url = storedCredentials.url

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url + path)
            .header("Authorization", Credentials.basic(storedCredentials.username, storedCredentials.password))
            .build()

        val file = File(context.filesDir, fileName)

        client.newCall(request).execute().use { res ->
            if(!res.isSuccessful) throw IOException("Download fehlgeschlagen: ${res.code}")
            res.body.byteStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return@withContext file
    }
}