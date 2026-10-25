package dev.nora.pdfdrawr.data.storage

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import tools.jackson.core.JacksonException
import tools.jackson.core.type.TypeReference
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.io.IOException



