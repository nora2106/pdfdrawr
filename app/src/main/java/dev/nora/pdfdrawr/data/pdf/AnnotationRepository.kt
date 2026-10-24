package dev.nora.pdfdrawr.data.pdf

import android.util.Log
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathSegment
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import dev.nora.pdfdrawr.data.storage.extractPathOps
import java.io.File

/**
 * Take size in bytes and return appropriate conversion depending on size
 */
object AnnotationRepository {
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
                Log.d("DebugLog", "ops=${ops.size} first=${ops.firstOrNull()?.type} pts=${ops.firstOrNull()?.points?.toList()}")
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

                            cs.curveTo(
                                scaleX(c1x),
                                scaleY(c1y),
                                scaleX(c2x),
                                scaleY(c2y),
                                scaleX(x1),
                                scaleY(y1)
                            )
                            lastX = op.points[0]
                            lastY = op.points[1]
                        }

                        PathSegment.Type.Cubic -> {
                            cs.curveTo(
                                scaleX(op.points[0]),
                                scaleY(op.points[1]),
                                scaleX(op.points[2]),
                                scaleY(op.points[3]),
                                scaleX(op.points[4]),
                                scaleY(op.points[5])
                            )
                            lastX = op.points[0]
                            lastY = op.points[1]
                        }

                        PathSegment.Type.Conic -> Log.d(
                            "DebugLog",
                            "Unexpected occurrence of conic path."
                        )

                        PathSegment.Type.Done -> {}
                        PathSegment.Type.Close -> cs.closePath()
                    }
                }
                cs.stroke()
            }
        }
    }
}