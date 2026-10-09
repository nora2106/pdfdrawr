package dev.nora.pdfdrawr.storage

import android.content.Context
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import tools.jackson.core.JacksonException
import tools.jackson.core.type.TypeReference
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


@Serializable
data class PathOp (
    val type: PathSegment.Type,
    val points: List<Float>
)

@Serializable
data class SaveablePath(val fillType: String = "NonZero", val ops: List<PathOp> = emptyList()) {
    fun toPath(): Path {
        val p = Path()
        p.fillType = if (fillType == "EvenOdd") PathFillType.EvenOdd else PathFillType.NonZero
        ops.forEach { op ->
            when (op.type) {
                PathSegment.Type.Move -> p.moveTo(op.points[0], op.points[1])
                PathSegment.Type.Line    -> p.lineTo(op.points[0], op.points[1])
                PathSegment.Type.Quadratic, PathSegment.Type.Conic    -> p.quadraticTo(op.points[0], op.points[1], op.points[2], op.points[3])
                PathSegment.Type.Cubic   -> p.cubicTo(op.points[0], op.points[1], op.points[2], op.points[3], op.points[4], op.points[5])
                PathSegment.Type.Close, PathSegment.Type.Done -> p.close()
            }
        }
        return p
    }
}

fun extractPathOps(path: Path): List<PathOp> {
    val ops = mutableListOf<PathOp>()
    val iterator = path.iterator()  // from androidx.graphics:graphics-path
    val points = FloatArray(8)

    while (iterator.hasNext()) {
        val type = iterator.next(points)
        ops.add(PathOp(type = type, points = points.toList()))
    }
    return ops
}
val mapper = jacksonObjectMapper()

suspend fun saveToJson(paths: List<SaveablePath>, context: Context, pdfName: String) = withContext(Dispatchers.IO) {
    val path = ""
    val file = File(context.filesDir, "$pdfName.annotations.json")
    val jsonArray = mapper.writeValueAsString(paths)

    try {
        mapper.writeValue(file, jsonArray)
        file.writeText(jsonArray)
    } catch (e: IOException) {
        e.printStackTrace()
    }

}

suspend fun loadFromJson(context: Context, pdfName: String): List<SaveablePath>? = withContext(Dispatchers.IO) {
    val file = File(context.filesDir, "$pdfName.annotations.json")
    if(!file.exists()) return@withContext null

    try {
        val jsonString = file.readText()
        mapper.readValue(jsonString, object : TypeReference<List<SaveablePath>>() {})
    } catch(e: IOException) {
        e.printStackTrace()
        return@withContext null
    }
    catch(e: JacksonException) {
        e.printStackTrace()
        return@withContext null
    }
}