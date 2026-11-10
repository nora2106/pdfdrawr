package dev.nora.pdfdrawr.data.annotation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class ToolState {
    var drawingEnabled by mutableStateOf(true)
    var color by mutableStateOf(Color.Black)
    var strokeWidth by mutableFloatStateOf(10f)
    var selectedTool by mutableStateOf(Tool.PEN)
}

class ToolbarActions(
    val onSave: () -> Unit,
    val onUpload: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
)

enum class Tool {
    PEN, ERASER, MARKER
}