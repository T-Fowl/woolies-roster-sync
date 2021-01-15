package com.tfowl.rosters

import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.util.Matrix
import java.awt.geom.Point2D
import java.util.*

data class PdfLine(
    val start: Point2D,
    val finish: Point2D,
)

data class PdfLines(
    val lines: Set<PdfLine>,
    val connections: Map<PdfLine, Set<PdfLine>>,
)

data class PdfText(
    val matrix: Matrix,
    val text: String,
)

data class PdfLinesAndTexts(
    val lines: PdfLines,
    val texts: List<PdfText>,
)

class LinesGraphAppender(
    private val graphLines: MutableSet<PdfLine>,
    private val graphConnections: MutableMap<PdfLine, MutableSet<PdfLine>>,
) {
    private fun List<PdfLine>.isClosed(): Boolean {
        if (size <= 2) return false
        return first().start.distance(last().finish) <= 0.001
    }

    fun appendPath(
        path: List<Point2D>,
    ) {
        if (path.size < 2) return

        val lines = path.zipWithNext { start, finish ->
            PdfLine(start, finish)
        }
        graphLines.addAll(lines)

        lines.forEachWithNext { from, to ->
            graphConnections.computeIfAbsent(from) { mutableSetOf() } += to
        }

        /* If the path is closed */
        if (lines.isClosed()) {
            graphConnections.computeIfAbsent(lines.last()) { mutableSetOf() } += lines.first()
        }
    }

    fun appendRectangle(p0: Point2D, p1: Point2D, p2: Point2D, p3: Point2D) {
        val a = PdfLine(p0, p1)
        val b = PdfLine(p1, p2)
        val c = PdfLine(p2, p3)
        val d = PdfLine(p3, p0)

        graphLines.addAll(arrayOf(a, b, c, d))
        graphConnections.computeIfAbsent(a) { mutableSetOf() } += b
        graphConnections.computeIfAbsent(b) { mutableSetOf() } += c
        graphConnections.computeIfAbsent(c) { mutableSetOf() } += d
        graphConnections.computeIfAbsent(d) { mutableSetOf() } += a
    }
}

class VisualElementsExtractor(val debugger: VisualDebugger) {
    fun extract(page: PDPage): PdfLinesAndTexts {
        val events = page.streamGraphicsEvents()

        val lines = linkedSetOf<PdfLine>()
        val connections = mutableMapOf<PdfLine, MutableSet<PdfLine>>()
        val appender = LinesGraphAppender(lines, connections)

        val texts = mutableListOf<PdfText>()

        val currentPath = LinkedList<Point2D>()

        events.forEach { event ->
            when (event) {
                is PdfGraphicsEvent.MoveTo          -> {
                    currentPath.clear()
                    currentPath.add(event.point)
                }
                is PdfGraphicsEvent.LineTo          -> {
                    currentPath.add(event.point)
                }
                is PdfGraphicsEvent.AppendRectangle -> {
                    appender.appendRectangle(event.p0, event.p1, event.p2, event.p3)
                }
                is PdfGraphicsEvent.ShowTextString  -> {
                    texts.add(PdfText(event.transform, event.text))
                }
                is PdfGraphicsEvent.FillPath        -> {
                    currentPath.clear()
                }
                is PdfGraphicsEvent.StrokePath      -> {
                    appender.appendPath(currentPath)
                    currentPath.clear()
                }
            }
        }

        return PdfLinesAndTexts(PdfLines(lines, connections), texts)
    }
}

fun PDPage.getVisualElements(debugger: VisualDebugger): PdfLinesAndTexts {
    val extractor = VisualElementsExtractor(debugger)
    return extractor.extract(this)
}
