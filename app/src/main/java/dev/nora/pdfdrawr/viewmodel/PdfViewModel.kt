package dev.nora.pdfdrawr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class PdfViewModel : ViewModel() {
    fun openFile(file: File) = viewModelScope.launch {

    }

    fun save() {

    }

    override fun onCleared() {
        // save and close renderer
    }
}