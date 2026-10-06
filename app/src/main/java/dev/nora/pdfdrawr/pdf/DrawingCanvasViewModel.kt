package dev.nora.pdfdrawr.pdf

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel

class DrawingCanvasViewModel: ViewModel() {
    val paths = mutableStateListOf<Path>()
    val currentPath = mutableStateOf<Path?>(null)
    val currentPathRef = mutableIntStateOf(1)
    private val lastOffset = mutableStateOf<Offset?>(null)

    fun onTapGesture(offset: Offset) {
        currentPath.value = Path()

        currentPath.value?.moveTo(offset.x, offset.y)
        currentPath.value?.addRect(
            Rect(
                offset.x - 0.5f,
                offset.y - 0.5f,
                offset.x + 0.5f,
                offset.y + 0.5f
            )
        )

        currentPath.value.let { value ->
            if (value != null) {
                paths.add(value)
            }
        }
    }

    fun onDragStart(offset: Offset) {
        currentPath.value = Path()
        currentPath.value?.moveTo(offset.x, offset.y)
        currentPathRef.intValue += 1
        lastOffset.value = offset
    }

    fun onDrag(offset: Offset) {
        if (lastOffset.value != null) {
            val newOffset = Offset(
                lastOffset.value!!.x + offset.x,
                lastOffset.value!!.y + offset.y
            )
            currentPath.value?.lineTo(newOffset.x, newOffset.y)
            currentPathRef.intValue += 1
            lastOffset.value = newOffset
        }
    }

    fun onDragEnd() {
        currentPath.value.let { value ->
            if (value != null) {
                paths.add(value)
                currentPath.value = null
                currentPathRef.intValue = 0
            }
        }
    }

    fun onDragCancel() {
        currentPath.value = null
    }
}