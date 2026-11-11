package dev.nora.pdfdrawr.data.annotation

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import java.util.LinkedList

class DrawingCanvasState(private var toolState: ToolState) {
    val paths = mutableStateListOf<Path>()
    val redoStack = mutableListOf<Pair<Int, DrawnPath>>()
    val pathsPerPage = mutableStateMapOf<Int, List<DrawnPath>>()
    var canvasWidth = 0f
    val currentPage = mutableIntStateOf(-1)
    val currentPath = mutableStateOf<Path?>(null)
    val currentPathRef = mutableIntStateOf(1)
    private val lastOffset = mutableStateOf<Offset?>(null)

    /**
     * Draw single tap.
     *
     * @param offset Offset
     * @param pageIndex Index of current page
     */
    fun onTapGesture(offset: Offset, pageIndex: Int) {
        currentPath.value = Path()
        currentPage.intValue = pageIndex
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
                val drawnPath = DrawnPath(value,
                    PathStyle(toolState.color.toArgb().toLong(), toolState.strokeWidth)
                )
                pathsPerPage[currentPage.intValue] = (pathsPerPage[currentPage.intValue] ?: emptyList()) + drawnPath
                currentPage.intValue = -1
            }
        }
    }

    fun onDragStart(offset: Offset, page: Int, pageWidth: Float) {
        canvasWidth = pageWidth
        currentPage.intValue = page
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
                val drawnPath = DrawnPath(value,
                    PathStyle(toolState.color.toArgb().toLong(), toolState.strokeWidth)
                )
                pathsPerPage[currentPage.intValue] = (pathsPerPage[currentPage.intValue] ?: emptyList()) + drawnPath
            }
        }
    }

    fun onDragCancel() {
        currentPath.value = null
    }

    /**
     * Clear canvas.
     */
    fun clear() {
        paths.clear()
        pathsPerPage.clear()
        currentPath.value = null
    }

    /**
     * Draw saved paths on canvas.
     *
     * @param savedPaths List of paths.
     */
    fun redraw(savedPaths: Map<Int, List<DrawnPath>>?) {
        clear()
        savedPaths?.forEach { (page, paths) -> pathsPerPage[page] = paths }
    }

    /**
     * Delete most recent drawn path.
     */
    fun undo() {
        val page = currentPage.intValue
        val list = pathsPerPage[page] ?: return

        val removed = list.lastOrNull() ?: return
        redoStack.add(page to removed)
        pathsPerPage[page] = list.dropLast(1)
    }

    /**
     * Redraw last deleted drawn path.
     */
    fun redo() {
        val (page, path) = redoStack.removeLastOrNull() ?: return
        pathsPerPage[page] = (pathsPerPage[page] ?: emptyList()) + path
    }
}

data class DrawnPath(
    var path: Path,
    var style: PathStyle
)