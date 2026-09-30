package dev.nora.pdfdrawr.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object PreferenceStorage {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    val usernameData = stringPreferencesKey("nextcloud_username")
    val passwordData = stringPreferencesKey("nextcloud_password")
    val urlData = stringPreferencesKey("nextcloud_url")

    suspend fun storeLoginData(context: Context, url: String, username: String, password: String) {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[usernameData] = username
                preferences[urlData] = url
                preferences[passwordData] = encryptPassword(password)
            }
        }
    }

    fun getNextcloudUsername(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences -> preferences[usernameData] ?: "" }
    }

    fun getNextcloudUrl(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences -> preferences[urlData] ?: "" }
    }

    fun getNextcloudPassword(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences -> preferences[passwordData] ?: "" }
    }

    private fun encryptPassword(password: String): String {
        // TODO add encryption
        return password
    }
}