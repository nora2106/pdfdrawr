package dev.nora.pdfdrawr.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel

class DrawingCanvasViewModel: ViewModel() {
    val paths = mutableStateListOf<Path>()
    val pathsPerPage = mutableMapOf<Int, List<Path>>()
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
    fun onTapGesture(offset: Offset, pageIndex: Int, pageWidth: Float) {
        val p = Offset(offset.x / pageWidth, offset.y / pageWidth)

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
                pathsPerPage[currentPage.intValue] = (pathsPerPage[currentPage.intValue] ?: emptyList()) + value
                currentPage.intValue = -1
            }
        }
    }

    fun onDragStart(offset: Offset, page: Int, pageWidth: Float) {
        val p = Offset(offset.x / pageWidth, offset.y / pageWidth)
        canvasWidth = pageWidth
        currentPage.intValue = page
        currentPath.value = Path()
        currentPath.value?.moveTo(offset.x, offset.y)
        currentPathRef.intValue += 1
        lastOffset.value = offset
    }

    fun onDrag(offset: Offset, pageWidth: Float) {
        // TODO
        val p = Offset(offset.x / pageWidth, offset.y / pageWidth)

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
                pathsPerPage[currentPage.intValue] = (pathsPerPage[currentPage.intValue] ?: emptyList()) + value
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
    fun redraw(savedPaths: Map<Int, List<Path>>?) {
        clear()
        savedPaths?.forEach { (page, paths) -> pathsPerPage[page] = paths }
    }
}