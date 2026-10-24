package dev.nora.pdfdrawr.viewmodel

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nora.pdfdrawr.data.pdf.PDFViewer
import kotlinx.coroutines.launch
import java.io.File

class PdfViewModel : ViewModel() {
    var viewer: PDFViewer? = null

    fun openFile(file: File) = viewModelScope.launch {
        val input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(input)
        viewer = PDFViewer(renderer)

    }

    fun save() {

    }

    override fun onCleared() {
        // save and close renderer
    }
}