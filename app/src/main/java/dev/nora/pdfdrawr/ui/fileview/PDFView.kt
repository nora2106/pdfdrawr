package dev.nora.pdfdrawr.ui.fileview

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import dev.nora.pdfdrawr.viewmodel.DrawingCanvasViewModel
import dev.nora.pdfdrawr.data.pdf.PDFViewer
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO currently only works on vertical screens
@Composable
fun PdfView(pdfFile: File) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfViewer by remember { mutableStateOf<PDFViewer?>(null) }
    var drawingEnabled by remember { mutableStateOf(true) }
    val vm: DrawingCanvasViewModel = viewModel()

    LaunchedEffect(pdfFile) {
        // create renderer for file
        val input = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(input)
        val viewer = PDFViewer(renderer)

        // clear canvas and save/redraw saved data
        vm.clear()
        val saved = viewer.getSavedData(context, pdfFile.name)
        if(saved != null) vm.redraw(saved)

        pdfViewer = viewer

        // close renderer and save after pdfFile has changed
        try {
            awaitCancellation()
        } finally {
            pdfViewer = null

            withContext(NonCancellable + Dispatchers.IO) {
                viewer.saveCanvasData(vm.pathsPerPage, context, pdfFile.name)
                viewer.close()
            }
        }
    }
    Column() {
        Row() {
            Button(
                onClick = {
                    scope.launch { pdfViewer?.saveCanvasData(vm.pathsPerPage, context, pdfFile.name) } }
            ) {
                Text("Save")
            }
            Button(
                onClick = {
                    scope.launch { pdfViewer?.saveToPdf(context, pdfFile, vm.pathsPerPage, vm.canvasWidth) } }
            ) {
                Text("Upload")
            }
            FilledIconToggleButton(checked = true, enabled = true, onCheckedChange = {
                drawingEnabled = !drawingEnabled

            }, content = {Icon(Icons.Rounded.RemoveRedEye, contentDescription = "Toggle View/Draw")})
        }

        pdfViewer?.let{
            PageColumn(vm, pdfViewer!!, drawingEnabled)
        }
    }
}

@Composable
fun PageColumn(vm: DrawingCanvasViewModel, pdfViewer: PDFViewer, drawingEnabled: Boolean) {
    LazyColumn(
        userScrollEnabled = !drawingEnabled
    ) {
        items(count = pdfViewer.getPageCount()) { index -> PdfPage(vm, pdfViewer, index, drawingEnabled) }
    }
}

@Composable
fun PdfPage(vm: DrawingCanvasViewModel, pdfViewer: PDFViewer, index: Int, drawingEnabled: Boolean) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(index) {
        bitmap = pdfViewer.renderSinglePage(index)
    }

    // TODO add white background (transparent pdfs and placeholder)
    Box( Modifier
        .fillMaxWidth()
        .aspectRatio(pdfViewer.ratio(index))
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
        DrawingCanvas(vm, index, drawingEnabled)
    }
}

@Composable
fun DrawingCanvas(vm: DrawingCanvasViewModel, index: Int, drawingEnabled: Boolean) {
    Canvas(
        modifier = Modifier.fillMaxSize()
            .background(Color.White.copy(alpha = 0.2f))
            .pointerInput(index, drawingEnabled) {
                if(!drawingEnabled) return@pointerInput
                detectTapGestures { vm.onTapGesture(it, index, size.width.toFloat()) }
            }
            .pointerInput(index, drawingEnabled) {
                if(!drawingEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { vm.onDragStart(it, index, size.width.toFloat()) },
                    onDragEnd = { vm.onDragEnd() },
                    onDragCancel = { vm.onDragCancel() },
                    onDrag = { _, amount -> vm.onDrag(amount, size.width.toFloat()) }
                )
            }
    ) {
        vm.pathsPerPage[index]?.forEach { drawPath(path = it, color = Color.Red, style = Stroke(10f)) }

        if (vm.currentPath.value != null && vm.currentPathRef.intValue > 0 && vm.currentPage.intValue == index) {
            drawPath(path = vm.currentPath.value!!, color = Color.Red, style = Stroke(10f))
        }
    }
}

