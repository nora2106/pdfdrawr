package dev.nora.pdfdrawr.ui.fileview

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import dev.nora.pdfdrawr.viewmodel.DrawingCanvasViewModel
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.nora.pdfdrawr.data.pdf.AnnotationRepository
import dev.nora.pdfdrawr.viewmodel.PdfViewModel

// TODO currently only works on vertical screens
@Composable
fun PdfView(pdfFile: File) {
    val appContext = LocalContext.current.applicationContext
    val toolState = ToolState()
    val vm: DrawingCanvasViewModel = viewModel()
    vm.toolState = toolState
    val pdfVm: PdfViewModel = viewModel(factory = viewModelFactory {
        initializer { PdfViewModel(AnnotationRepository(appContext), vm) }
    })

    LaunchedEffect(pdfFile) {
        pdfVm.openFile(pdfFile)
    }

    Column() {
        DrawingToolbar(toolState, { pdfVm.saveData() }, { pdfVm.uploadAsPdf() } )
        PageColumn(vm, pdfVm, toolState)
    }
}

@Composable
fun PageColumn(vm: DrawingCanvasViewModel, pdfVm: PdfViewModel, toolState: ToolState) {
    LazyColumn(
        userScrollEnabled = !toolState.drawingEnabled
    ) {
        items(count = pdfVm.getPageCount()) { index -> PdfPage(vm, pdfVm, index, toolState) }
    }
}

@Composable
fun PdfPage(vm: DrawingCanvasViewModel, pdfVm: PdfViewModel, index: Int, toolState: ToolState) {
    val file = pdfVm.currentFile
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(index) {
        bitmap = pdfVm.renderPage(index)
    }

    // TODO add white background (transparent pdfs and placeholder)
    Box( Modifier
        .fillMaxWidth()
        //.background(Color.White)
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
        DrawingCanvas(vm, index, toolState)
    }
}

@Composable
fun DrawingCanvas(vm: DrawingCanvasViewModel, index: Int, ts: ToolState) {
    Canvas(
        modifier = Modifier.fillMaxSize()
            .background(Color.White.copy(alpha = 0.2f))
            .pointerInput(index, ts.drawingEnabled) {
                if(!ts.drawingEnabled) return@pointerInput
                detectTapGestures { vm.onTapGesture(it, index) }
            }
            .pointerInput(index, ts.drawingEnabled) {
                if(!ts.drawingEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { vm.onDragStart(it, index, size.width.toFloat()) },
                    onDragEnd = { vm.onDragEnd() },
                    onDragCancel = { vm.onDragCancel() },
                    onDrag = { _, amount -> vm.onDrag(amount) }
                )
            }
    ) {
        vm.pathsPerPage[index]?.forEach { drawPath(path = it.path, color = it.style.composeColor, style = Stroke(it.style.strokeWidth)) }

        if (vm.currentPath.value != null && vm.currentPathRef.intValue > 0 && vm.currentPage.intValue == index) {
            drawPath(path = vm.currentPath.value!!, color = ts.color, style = Stroke(ts.strokeWidth))
        }
    }
}
