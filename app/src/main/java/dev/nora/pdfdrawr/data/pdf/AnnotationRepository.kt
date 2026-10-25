package dev.nora.pdfdrawr.data.pdf

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathSegment
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import tools.jackson.core.JacksonException
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.io.IOException

/**
 * Take size in bytes and return appropriate conversion depending on size
 */
class AnnotationRepository(private val context: Context) {
    val mapper = jacksonObjectMapper()

    fun exportPdfWithAnnotations(file: File, annotations: Map<Int, List<Path>>?, pageWidth: Float): File {
        val outFile = File(file.parentFile, "${file.nameWithoutExtension}_annotated.pdf")

        PDDocument.load(file).use { document ->
            annotations?.forEach { (pageIndex, paths) ->
                drawAnnotationsOnPage(document, document.getPage(pageIndex), paths, pageWidth)
            }

            document.save(outFile)
        }

        return outFile
    }

    private fun drawAnnotationsOnPage(doc: PDDocument, page: PDPage, annotations: List<Path>?, pageWidth: Float) {
        if(annotations == null) return

        val w = page.mediaBox.width
        val h = page.mediaBox.height
        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            val pixelWidth = 1080f

            cs.setStrokingColor(0f, 0f, 0f)
            cs.setLineWidth(10f / pixelWidth * w)
            cs.setLineCapStyle(1)
            cs.setLineJoinStyle(1)
            var lastX = 0f
            var lastY = 0f

            fun scaleX(v: Float) = v / pageWidth * w
            fun scaleY(v: Float) = h - v / pageWidth * w

            annotations.forEach { ann ->
                val ops = extractPathOps(ann)
                ops.forEach { op ->
                    when (op.type) {
                        PathSegment.Type.Move -> {
                            cs.moveTo(scaleX(op.points[0]), scaleY(op.points[1]))
                            lastX = op.points[0]
                            lastY = op.points[1]
                        }

                        PathSegment.Type.Line -> {
                            cs.lineTo(scaleX(op.points[0]), scaleY(op.points[1]))
                            lastX = op.points[0]
                            lastY = op.points[1]
                        }

                        PathSegment.Type.Quadratic -> {
                            val cx = op.points[0]
                            val cy = op.points[1]
                            val x1 = op.points[2]
                            val y1 = op.points[3]

                            val c1x = lastX + 2f / 3f * (cx - lastX)
                            val c1y = lastY + 2f / 3f * (cy - lastY)
                            val c2x = x1 + 2f / 3f * (cx - x1)
                            val c2y = y1 + 2f / 3f * (cy - y1)

                            cs.curveTo(scaleX(c1x), scaleY(c1y), scaleX(c2x), scaleY(c2y), scaleX(x1), scaleY(y1))
                            lastX = op.points[0]
                            lastY = op.points[1]
                        }

                        PathSegment.Type.Cubic -> {
                            cs.curveTo(scaleX(op.points[0]), scaleY(op.points[1]), scaleX(op.points[2]), scaleY(op.points[3]), scaleX(op.points[4]), scaleY(op.points[5]))
                            lastX = op.points[0]
                            lastY = op.points[1]
                        }

                        PathSegment.Type.Conic -> Log.d("DebugLog", "Unexpected occurrence of conic path.")

                        PathSegment.Type.Done -> {}
                        PathSegment.Type.Close -> cs.closePath()
                    }
                }
                cs.stroke()
            }
        }
    }

    /**
     * Extract path operations from a path object.
     *
     * @param path The path object.
     */
    fun extractPathOps(path: Path): List<PathOp> {
        val ops = mutableListOf<PathOp>()
        val iterator = path.iterator()
        val points = FloatArray(8)

        while (iterator.hasNext()) {
            val type = iterator.next(points)
            ops.add(PathOp(type = type, points = points.toList()))
        }
        return ops
    }

    /**
     * Save list of paths to JSON file.
     *
     * @param paths List of paths.
     * @param pdfName PDF filename.
     */
    suspend fun saveToJson(paths: List<SaveablePath>, pdfName: String) = withContext(Dispatchers.IO) {
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
            mapper.readValue(jsonString, object : tools.jackson.core.type.TypeReference<List<SaveablePath>>() {})
        } catch(e: IOException) {
            e.printStackTrace()
            return@withContext null
        }
        catch(e: JacksonException) {
            e.printStackTrace()
            return@withContext null
        }
    }
}

// TODO add custom DrawnPath data class to store path, color and strokewidth
@Serializable
data class PathOp (
    val type: PathSegment.Type,
    val points: List<Float>
)

@Serializable
data class SaveablePath(val fillType: String = "NonZero", val ops: List<PathOp> = emptyList(), val page: Int = 0) {
    fun toPath(): Path {
        val p = Path()
        p.fillType = if (fillType == "EvenOdd") PathFillType.EvenOdd else PathFillType.NonZero
        ops.forEach { op ->
            when (op.type) {
                PathSegment.Type.Move -> p.moveTo(op.points[0], op.points[1])
                PathSegment.Type.Line    -> p.lineTo(op.points[0], op.points[1])
                PathSegment.Type.Quadratic    -> p.quadraticTo(op.points[0], op.points[1], op.points[2], op.points[3])
                PathSegment.Type.Cubic   -> p.cubicTo(op.points[0], op.points[1], op.points[2], op.points[3], op.points[4], op.points[5])
                PathSegment.Type.Conic -> Log.d("DebugLog", "Unexpected occurrence of conic path.")
                PathSegment.Type.Done -> {}
                PathSegment.Type.Close-> p.close()
            }
        }
        return p
    }
}