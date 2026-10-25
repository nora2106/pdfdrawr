package dev.nora.pdfdrawr.data.sync

import android.content.Context
import android.util.Log
import dev.nora.pdfdrawr.data.storage.PreferenceStorage.getStoredNextcloudCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import java.io.File

object WebDavRepository {
    private lateinit var appContext: Context
    private val client = OkHttpClient()

    fun init(context: Context) {
        appContext = context.applicationContext
    }
    /**
     * Try to connect to Nextcloud instance
     *
     * @param url Server URL
     * @param username Nextcloud username
     * @param password Nextcloud password
     * @return if connection was successful
     */
    suspend fun testWebdavConnection(url: String, username: String, password: String): Boolean = withContext(Dispatchers.IO) {
            val credential = Credentials.basic(username, password)
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

    /**
     * Download specific file, throw error if it wasn't found
     *
     * @param fileName Name of the file
     * @param path Path to the parent directory
     * @return the file
     */
    suspend fun downloadFile(fileName: String?, path: String?): File = withContext(Dispatchers.IO){
        if(fileName == null) throw IllegalArgumentException("File hat keinen Namen")

        val storedCredentials = getStoredNextcloudCredentials(appContext) ?: throw IllegalStateException("Keine Zugangsdaten")
        val url = storedCredentials.url

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url + path)
            .header("Authorization", Credentials.basic(storedCredentials.username, storedCredentials.password))
            .build()

        val file = File(appContext.filesDir, fileName)

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

    /**
     * Upload (and replace if exists) specific file.
     *
     * @param file File to be uploaded
     * @param path Path to the parent directory
     */
    suspend fun uploadFile(file: File, path: String?) = withContext(Dispatchers.IO){
        val storedCredentials = getStoredNextcloudCredentials(appContext) ?: throw IllegalStateException("Keine Zugangsdaten")
        val baseUrl = storedCredentials.url
        val client = OkHttpClient()

        val request: Request = Request.Builder()
            .url("$baseUrl/remote.php/dav/files/${storedCredentials.username}/$path/${file.name}")
            .header("Authorization", Credentials.basic(storedCredentials.username, storedCredentials.password))
            .put(file.asRequestBody("application/pdf".toMediaType()))
            .build()

        client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) throw IOException("Upload fehlgeschlagen: ${res.code}")
        }
    }
}