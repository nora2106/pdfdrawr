package dev.nora.pdfdrawr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import dev.nora.pdfdrawr.ui.theme.PDFDrawrTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.nora.pdfdrawr.ui.fileview.DrawingCanvas
import dev.nora.pdfdrawr.ui.main.FolderView
import dev.nora.pdfdrawr.ui.settings.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PDFDrawrTheme {
                Content()
            }
        }
    }
}

@Composable
fun Content() {
    var showSettings by remember{mutableStateOf(false)}

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if(showSettings) {
            Button(
                onClick = { showSettings = false }
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close Settings")
            }
            LoginScreen()
        }
        else {
            Button(
                onClick = { showSettings = true }
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Open Settings")
            }
            FolderView()
        }
    }
}
