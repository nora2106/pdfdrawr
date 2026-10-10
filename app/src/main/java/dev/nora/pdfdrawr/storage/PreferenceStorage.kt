package dev.nora.pdfdrawr.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object PreferenceStorage {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    val usernameData = stringPreferencesKey("nextcloud_username")
    val passwordData = stringPreferencesKey("nextcloud_password")
    val urlData = stringPreferencesKey("nextcloud_url")

    /**
     * Store login data in DataStore
     *
     * @param context The Context
     * @param url URL String
     * @param username Username String
     * @param password Password String
     */
    suspend fun storeLoginData(context: Context, url: String, username: String, password: String) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[usernameData] = username
                preferences[urlData] = url
                preferences[passwordData] = encryptPassword(password)
            }
        }
    }

    suspend fun getStoredNextcloudCredentials(context: Context): LoginCredentials? {
        val url = getNextcloudUrl(context).first()
        val username = getNextcloudUsername(context).first()
        val password = getNextcloudPassword(context).first()

        if (url.isBlank() || username.isBlank() || password.isBlank()) {
            return null
        }

        return LoginCredentials(url, username, password)
    }

    fun getNextcloudUsername(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences -> preferences[usernameData] ?: "" }
    }

    fun getNextcloudUrl(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences -> preferences[urlData] ?: "" }
    }

    fun getNextcloudPassword(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences -> preferences[passwordData] ?: "" }
    }

    private fun encryptPassword(password: String): String {
        // TODO add encryption
        return password
    }

    data class LoginCredentials(
        val url: String,
        val username: String,
        val password: String
    )
}