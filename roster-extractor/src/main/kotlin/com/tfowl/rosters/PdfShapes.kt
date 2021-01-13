package com.tfowl.rosters

import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.util.Matrix
import java.awt.geom.Point2D
import java.util.*

data class PdfLine(
    val start: Point2D,
    val end: Point2D
)

data class PdfRectangle(
    val p0: Point2D,
    val p1: Point2D,
    val p2: Point2D,
    val p3: Point2D
)

fun PdfRectangle.lines(): List<PdfLine> = listOf(
    PdfLine(p0, p1),
    PdfLine(p1, p2),
    PdfLine(p2, p3),
    PdfLine(p3, p0)
)

data class PdfText(
    val matrix: Matrix,
    val text: String
)

data class PdfShapes(
    val lines: List<PdfLine>,
    val rectangles: List<PdfRectangle>,
    val texts: List<PdfText>
)

class VisualElementsExtractor(val debugger: VisualDebugger) {
    fun extract(page: PDPage): PdfShapes {
        val events = page.streamGraphicsEvents()

        val lines = mutableListOf<PdfLine>()
        val rectangles = mutableListOf<PdfRectangle>()
        val texts = mutableListOf<PdfText>()

        val path = LinkedList<Point2D>()

        events.forEach { event ->
            when (event) {
                is PdfGraphicsEvent.MoveTo          -> {
                    path.clear()
                    path.add(Point2D.Float(event.x, event.y))
                }
                is PdfGraphicsEvent.LineTo          -> {
                    path.add(Point2D.Float(event.x, event.y))
                }
                is PdfGraphicsEvent.AppendRectangle -> {
                    rectangles.add(PdfRectangle(event.p0, event.p1, event.p2, event.p3))
                }
                is PdfGraphicsEvent.ShowTextString  -> {
                    texts.add(PdfText(event.transform, event.text))
                }
                is PdfGraphicsEvent.FillPath        -> {
                    path.clear()
                }
                is PdfGraphicsEvent.StrokePath      -> {
                    var curr = path.removeFirst()
                    while (path.isNotEmpty()) {
                        val next = path.removeFirst()
                        lines.add(PdfLine(curr, next))
                        curr = next
                    }
                }
            }
        }

        return PdfShapes(lines, rectangles, texts)
    }
}

fun PDPage.getVisualElements(debugger: VisualDebugger): PdfShapes {
    val extractor = VisualElementsExtractor(debugger)
    return extractor.extract(this)
}
