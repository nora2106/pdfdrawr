package dev.nora.pdfdrawr.ui.fileview

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.nora.pdfdrawr.data.pdf.AnnotationRepository
import dev.nora.pdfdrawr.data.pdf.DrawingCanvasState
import dev.nora.pdfdrawr.viewmodel.PdfViewModel

@Composable
fun PdfView(pdfFile: File) {
    val appContext = LocalContext.current.applicationContext
    val pdfVm: PdfViewModel = viewModel(factory = viewModelFactory {
        initializer { PdfViewModel(AnnotationRepository(appContext)) }
    })

    LaunchedEffect(pdfFile) {
        pdfVm.openFile(pdfFile)
    }

    Column() {
        DrawingToolbar(pdfVm.toolState, { pdfVm.saveData() }, { pdfVm.uploadAsPdf() } )
        PageColumn(pdfVm)
    }
}

@Composable
fun PageColumn(pdfVm: PdfViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth
        LazyColumn(
            userScrollEnabled = !pdfVm.toolState.drawingEnabled
        ) {
            items(count = pdfVm.getPageCount()) { index -> PdfPage(pdfVm, index, width) }
        }
    }
}

@Composable
fun PdfPage(pdfVm: PdfViewModel, index: Int, pageWidth: Int) {
    val file = pdfVm.currentFile
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(index) {
        bitmap = pdfVm.renderPage(index, pageWidth)
    }

    Box( Modifier
        .fillMaxWidth()
        .aspectRatio(pdfVm.ratio(index))
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "PDF Seite",
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(bitmap!!.width.toFloat() / bitmap!!.height.toFloat()),
                contentScale = ContentScale.Fit
            )
        }
        DrawingCanvas(pdfVm, index)
    }
}

@Composable
fun DrawingCanvas(pdfVm: PdfViewModel, index: Int) {
    val ts: ToolState = pdfVm.toolState
    val cv: DrawingCanvasState = pdfVm.canvas
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.2f))
            .pointerInput(index, ts.drawingEnabled) {
                if (!ts.drawingEnabled) return@pointerInput
                detectTapGestures { cv.onTapGesture(it, index) }
            }
            .pointerInput(index, ts.drawingEnabled) {
                if (!ts.drawingEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { cv.onDragStart(it, index, size.width.toFloat()) },
                    onDragEnd = { cv.onDragEnd() },
                    onDragCancel = { cv.onDragCancel() },
                    onDrag = { _, amount -> cv.onDrag(amount) }
                )
            }
    ) {
        cv.pathsPerPage[index]?.forEach { drawPath(path = it.path, color = it.style.composeColor, style = Stroke(it.style.strokeWidth)) }

        if (cv.currentPath.value != null && cv.currentPathRef.intValue > 0 && cv.currentPage.intValue == index) {
            drawPath(path = cv.currentPath.value!!, color = ts.color, style = Stroke(ts.strokeWidth))
        }
    }
}
