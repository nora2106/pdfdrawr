package dev.nora.pdfdrawr.ui.fileview

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.nora.pdfdrawr.pdf.DrawingCanvasViewModel
import dev.nora.pdfdrawr.pdf.PDFViewer
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel

// TODO currently only works on vertical screens
@Composable
fun PdfPage(pdfFile: File) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pdfFile) {
        bitmap = PDFViewer().getFirstPage(pdfFile)
    }
    bitmap?.let {
        DrawingCanvas()
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "PDF Seite",
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(bitmap!!.width.toFloat() / bitmap!!.height.toFloat()),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun DrawingCanvas() {
    val vm: DrawingCanvasViewModel = viewModel()
    Canvas(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { vm.onTapGesture(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { vm.onDragStart(it) },
                    onDragEnd = { vm.onDragEnd() },
                    onDragCancel = { vm.onDragCancel() },
                    onDrag = { _, amount -> vm.onDrag(amount) }
                )
            }
    ) {
        vm.paths.forEach { drawPath(path = it, color = Color.Black, style = Stroke(10f)) }

        if (vm.currentPath.value != null && vm.currentPathRef.intValue > 0) {
            drawPath(path = vm.currentPath.value!!, color = Color.Black, style = Stroke(10f))
        }
    }
}