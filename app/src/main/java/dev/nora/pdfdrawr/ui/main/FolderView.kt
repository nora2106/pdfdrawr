package dev.nora.pdfdrawr.ui.main

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import dev.nora.pdfdrawr.sync.FolderSyncManager.syncFolder
import dev.nora.pdfdrawr.sync.WebDavFile
import dev.nora.pdfdrawr.ui.theme.PDFDrawrTheme
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nora.pdfdrawr.helpers.convertFileSize
import dev.nora.pdfdrawr.helpers.trimModifiedDate
import dev.nora.pdfdrawr.storage.PreferenceStorage.getNextcloudUsername

// TODO reload wenn nextcloud verbindung sich ändert
@Composable
fun FolderView() {
    val context = LocalContext.current
    val username by getNextcloudUsername(context).collectAsStateWithLifecycle(initialValue = "")
    var selectedPath by remember(username) { mutableStateOf<String?>(null) }
    var previousPath by remember{mutableStateOf<String?>(null)}

    var files by remember { mutableStateOf<List<WebDavFile>>(emptyList()) }

    LaunchedEffect(username) {
        if (selectedPath == null && username.isNotBlank()) {
            selectedPath = "/remote.php/dav/files/$username"
        }
    }

    LaunchedEffect(selectedPath) {
        if(username.isBlank()) return@LaunchedEffect
        files = syncFolder(context, selectedPath)
    }
    Button(
        onClick = {selectedPath = previousPath}
    ) {
        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back")
    }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        for (file in files) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if(file.isDirectory) FolderElement(file, onFolderClick = {
                    clickedPath ->
                    previousPath = selectedPath
                    selectedPath = clickedPath
                })
                else FileElement(file)
            }
        }
    }
}


@Composable
fun FileElement(file: WebDavFile) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        if (file.type?.equals("application/pdf") == true) Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF")

        Column(
        ) {
            file.name?.let { Text(it) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                Text(trimModifiedDate(file.lastModified))
                Text("•")
                Text(convertFileSize(file.size))
            }
        }
    }
}

@Composable
fun FolderElement(file: WebDavFile,  onFolderClick: (String) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clickable(
                onClick = {
                    file.path?.let { onFolderClick(it) }},
                interactionSource = interactionSource,
            ),
        ) {
        Icon(Icons.Rounded.Folder, contentDescription = "Folder")

        Column(
        ) {
            file.name?.let { Text(it) }
            Text(trimModifiedDate(file.lastModified))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FolderViewPreview() {
    PDFDrawrTheme {
        FolderView()
    }
}
