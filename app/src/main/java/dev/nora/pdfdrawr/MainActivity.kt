package dev.nora.pdfdrawr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.nora.pdfdrawr.ui.theme.PDFDrawrTheme
import kotlinx.coroutines.launch
import dev.nora.pdfdrawr.sync.WebDavSyncService.testWebdavConnection
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import dev.nora.pdfdrawr.storage.PreferenceStorage.storeLoginData
import dev.nora.pdfdrawr.sync.FolderSyncManager.syncFolder
import dev.nora.pdfdrawr.sync.WebDavFile

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PDFDrawrTheme {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LoginScreen()
                    FolderView()
                }
            }
        }
    }
}

// TODO reload wenn nextcloud verbindung sich ändert
@Composable
fun FolderView() {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<WebDavFile>>(emptyList()) }

    LaunchedEffect(Unit) {
        files = syncFolder(context)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (file in files) {
            Column(
            ) {
                Text(file.path ?: "Unbekannt")
                Text(file.lastModified ?: "")
                Text(file.fileType ?: "Folder")
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    //val test by getNextcloudUsername(context).collectAsStateWithLifecycle(initialValue = 0)

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
            label = { Text("WebDAV URL") }
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