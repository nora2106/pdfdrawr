package dev.nora.pdfdrawr.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import dev.nora.pdfdrawr.storage.PreferenceStorage.storeLoginData
import dev.nora.pdfdrawr.sync.WebDavSyncService.testWebdavConnection
import dev.nora.pdfdrawr.ui.theme.PDFDrawrTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        var username by remember { mutableStateOf("") }
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") }
        )

        var password by remember { mutableStateOf("") }
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") }
        )

        var webdavUrl by remember { mutableStateOf("") }
        TextField(
            value = webdavUrl,
            onValueChange = { webdavUrl = it },
            label = { Text("Nextcloud URL") }
        )
        var statusMessage by remember { mutableStateOf<String?>("") }

        Button(onClick = {
            if (webdavUrl.isBlank() || username.isBlank() || password.isBlank()) {
                statusMessage = "Bitte alle Felder ausfüllen"
                return@Button
            }
            scope.launch {
                val connectionSuccess = testWebdavConnection(webdavUrl, username, password)
                if(connectionSuccess) {
                    statusMessage = "Verbindung erfolgreich"
                    storeLoginData(context, webdavUrl, username, password)
                }
                else {
                    statusMessage = "Verbindung fehlgeschlagen"
                }
            }
        }) {
            Text("Connect")
        }
        statusMessage?.let { Text(it) }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    PDFDrawrTheme {
        LoginScreen()
    }
}