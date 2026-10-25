package dev.nora.pdfdrawr.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nora.pdfdrawr.data.pdf.AnnotationRepository
import dev.nora.pdfdrawr.data.pdf.PDFViewer
import dev.nora.pdfdrawr.data.pdf.SaveablePath
import dev.nora.pdfdrawr.data.sync.WebDavRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewModel(app: Application) : ViewModel() {
    var viewer by mutableStateOf<PDFViewer?>(null)
        private set
    private var currentFile: File? = null
    private val annotationRep = AnnotationRepository(app.applicationContext)


    fun openFile(file: File) = viewModelScope.launch {
        if (file == currentFile) return@launch

        val input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(input)
        renderer.let { viewer = PDFViewer(it) }
    }

    fun closeCurrentFile() {
        //saveData()
        viewer?.close()
    }

    suspend fun renderPage(index: Int): Bitmap? = viewer?.renderSinglePage(index)
    fun ratio(index: Int): Float = viewer?.ratio(index) ?: 1f
    fun getPageCount() = viewer?.getPageCount() ?: 0

    // TODO make independent of parameters after refactoring DrawingCanvasViewModel
    /**
     * Save canvas data as a list of serializable path objects.
     *
     * @param pathsPerPage Map containing all paths per page
     */
    suspend fun saveData(pathsPerPage: MutableMap<Int, List<Path>>) = withContext(Dispatchers.IO){
        if(currentFile == null) return@withContext

        val savedPaths = pathsPerPage.flatMap { (page, paths) ->
            paths.map { path -> SaveablePath(path.fillType.toString(), annotationRep.extractPathOps(path), page) }
        }

        annotationRep.saveToJson(savedPaths, currentFile!!.name)
    }

    fun loadSaveData(){

    }

    /**
     * Save working file as PDF and upload to server.
     *
     * @param file Source PDF
     * @param pathsPerPage Map containing all paths per page
     */
    suspend fun uploadAsPdf(file: File, pathsPerPage: MutableMap<Int, List<Path>>, pageWidth: Float) = withContext(Dispatchers.IO) {
        val newFile = annotationRep.exportPdfWithAnnotations(file, pathsPerPage, pageWidth)
        // TODO add path
        WebDavRepository.uploadFile(newFile, "")
    }

    override fun onCleared() {
        runBlocking(Dispatchers.IO) {
            // save data
            closeCurrentFile()
        }
    }
}