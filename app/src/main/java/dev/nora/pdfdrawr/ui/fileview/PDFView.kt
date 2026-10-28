package dev.nora.pdfdrawr.ui.fileview

import android.graphics.Bitmap
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
import dev.nora.pdfdrawr.viewmodel.DrawingCanvasViewModel
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nora.pdfdrawr.viewmodel.PdfViewModel
import kotlinx.coroutines.launch

// TODO currently only works on vertical screens
@Composable
fun PdfView(pdfFile: File) {
    val scope = rememberCoroutineScope()
    var drawingEnabled by remember { mutableStateOf(true) }
    val vm: DrawingCanvasViewModel = viewModel()
    val pdfVm: PdfViewModel = viewModel()
    pdfVm.init(vm)

    LaunchedEffect(pdfFile) {
        pdfVm.openFile(pdfFile)
    }
    Column() {
        Row() {
            Button(
                onClick = {
                    scope.launch { pdfVm.saveData() } }
            ) {
                Text("Save")
            }
            Button(
                onClick = {
                    scope.launch { pdfVm.uploadAsPdf() } }
            ) {
                Text("Upload")
            }
            FilledIconToggleButton(checked = true, enabled = true, onCheckedChange = {
                drawingEnabled = !drawingEnabled

            }, content = {Icon(Icons.Rounded.RemoveRedEye, contentDescription = "Toggle View/Draw")})
        }

        PageColumn(vm, pdfVm, drawingEnabled)
    }
}

@Composable
fun PageColumn(vm: DrawingCanvasViewModel, pdfVm: PdfViewModel, drawingEnabled: Boolean) {
    LazyColumn(
        userScrollEnabled = !drawingEnabled
    ) {
        items(count = pdfVm.getPageCount()) { index -> PdfPage(vm, pdfVm, index, drawingEnabled) }
    }
}

@Composable
fun PdfPage(vm: DrawingCanvasViewModel, pdfVm: PdfViewModel, index: Int, drawingEnabled: Boolean) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(index) {
        bitmap = pdfVm.renderPage(index)
    }

    // TODO add white background (transparent pdfs and placeholder)
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

