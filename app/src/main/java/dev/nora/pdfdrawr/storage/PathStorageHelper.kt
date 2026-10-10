package dev.nora.pdfdrawr.storage

import android.content.Context
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

/**
 * Extract path operations from a path object.
 *
 * @param path The path object.
 */
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

/**
 * Save list of paths to JSON file.
 *
 * @param paths List of paths.
 * @param context Global context.
 * @param pdfName PDF filename.
 */
suspend fun saveToJson(paths: List<SaveablePath>, context: Context, pdfName: String) = withContext(Dispatchers.IO) {
    val file = File(context.filesDir, "$pdfName.annotations.json")

    try {
        file.writeText(mapper.writeValueAsString(paths))
    } catch (e: IOException) {
        e.printStackTrace()
    }
    catch(e: JacksonException) {
        e.printStackTrace()
    }
}

/**
 * Find corresponding file and extract path list.
 *
 * @param context Global context.
 * @param pdfName PDF filename.
 * @return List of serializable paths.
 */
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