package dev.nora.pdfdrawr
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.artifex.mupdf.viewer.DocumentActivity

class PDFViewer {
    fun startMuPDFActivity(context: Context, documentUri: Uri?) {
        val intent = Intent(context, DocumentActivity::class.java)
        intent.setAction(Intent.ACTION_VIEW)
        intent.setDataAndType(documentUri, "application/pdf")
        context.startActivity(intent)
    }
}