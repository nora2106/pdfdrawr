package dev.nora.pdfdrawr.ui.fileview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.nora.pdfdrawr.data.annotation.ToolState
import dev.nora.pdfdrawr.data.annotation.ToolbarActions

@Composable
fun DrawingToolbar(toolState: ToolState, actions: ToolbarActions) {
    Column() {
        ButtonRow(toolState, actions)
        ColorSelector(toolState)
    }
}

@Composable
fun ButtonRow(toolState: ToolState, actions: ToolbarActions) {
    Row() {
//        Button(
//            onClick = { saveData() }
//        ) {
//            Text("Save")
//        }
        Button(
            onClick = { actions.onUpload }
        ) {
            Text("Upload")
        }
        Button(
            onClick = { actions.onUndo() }
        ) {
            Text("Undo")
        }
        Button(
            onClick = { actions.onRedo() }
        ) {
            Text("Redo")
        }
        FilledIconToggleButton(checked = toolState.drawingEnabled, enabled = true, onCheckedChange = { toolState.drawingEnabled = !toolState.drawingEnabled },
            content = { Icon(Icons.Rounded.RemoveRedEye, contentDescription = "Toggle View/Draw") })
    }
}

@Composable
fun ColorSelector(toolState: ToolState) {
    Row() {
//        Button(
//            onClick = { toolState.color = Color.Black }
//        ) {
//            Text("Black")
//        }
//        Button(
//            onClick = { toolState.color = Color.Red }
//        ) {
//            Text("Red")
//        }
    }
}