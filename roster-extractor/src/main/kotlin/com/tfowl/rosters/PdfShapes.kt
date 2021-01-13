package com.tfowl.rosters

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.image.PDImage
import org.apache.pdfbox.util.Matrix
import java.awt.geom.Point2D
import kotlin.math.abs

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

fun PDPage.getVisualElements(): PdfShapes {
    val engine = PdfGraphicsStreamEvents(this)
    val events = engine.process()

    val lines = mutableListOf<PdfLine>()
    val rectangles = mutableListOf<PdfRectangle>()
    val texts = mutableListOf<PdfText>()

    val position = Point2D.Float(0.0f, 0.0f)

    events.forEach { event ->
        when (event) {
            is PdfGraphicsEvent.MoveTo          -> position.setLocation(event.x, event.y)
            is PdfGraphicsEvent.LineTo          -> {
                lines.add(
                    PdfLine(
                        Point2D.Float(position.x, position.y),
                        Point2D.Float(event.x, event.y)
                    )
                )
                position.setLocation(event.x, event.y)
            }
            is PdfGraphicsEvent.AppendRectangle -> {
                rectangles.add(PdfRectangle(event.p0, event.p1, event.p2, event.p3))
            }
            is PdfGraphicsEvent.ShowTextString  -> {
                texts.add(PdfText(event.transform, event.text))
            }
            else                                -> {
            }
        }
    }

    return PdfShapes(lines, rectangles, texts)
}

sealed class PdfGraphicsEvent {
    data class MoveTo(val x: Float, val y: Float) : PdfGraphicsEvent()
    data class LineTo(val x: Float, val y: Float) : PdfGraphicsEvent()
    data class AppendRectangle(val p0: Point2D, val p1: Point2D, val p2: Point2D, val p3: Point2D) : PdfGraphicsEvent()
    data class ShowTextString(val text: String, val transform: Matrix) : PdfGraphicsEvent()

    object FillPath : PdfGraphicsEvent()
    object StrokePath : PdfGraphicsEvent()
}

private class PdfGraphicsStreamEvents(
    page: PDPage
) : PDFGraphicsStreamEngine(page) {

    fun process(): List<PdfGraphicsEvent> {
        events.clear()
        processPage(page)
        return events.toList().also { events.clear() }
    }

    private val events = mutableListOf<PdfGraphicsEvent>()

    private fun emit(event: PdfGraphicsEvent) {
        events += event
    }

    private val position = Point2D.Float(0.0f, 0.0f)

    private fun unsupported(msg: () -> Any) {
        error("Unsupported Event: ${msg()}")
    }

    override fun appendRectangle(p0: Point2D, p1: Point2D, p2: Point2D, p3: Point2D) {
        emit(PdfGraphicsEvent.AppendRectangle(p0, p1, p2, p3))
    }

    override fun drawImage(pdImage: PDImage) {
        unsupported { "drawImage($pdImage)" }
    }

    override fun clip(windingRule: Int) {
        unsupported { "clip($windingRule)" }
    }

    override fun moveTo(x: Float, y: Float) {
        emit(PdfGraphicsEvent.MoveTo(x, y))

        position.setLocation(x, y)
    }

    override fun lineTo(x: Float, y: Float) {
        emit(PdfGraphicsEvent.LineTo(x, y))

        position.setLocation(x, y)
    }

    override fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        // A lot of lines / paths seem to be terminated with an empty curveTo
        if (abs(x1 - x3) > 0.001f || abs(y1 - y3) > 0.001f) {
            unsupported { "curveTo($x1, $y1, $x2, $y2, $x3, $y3)" }
        }
    }

    override fun getCurrentPoint(): Point2D = position

    override fun closePath() {
        unsupported { "closePath()" }
    }

    override fun endPath() {
        unsupported { "endPath()" }
    }

    override fun strokePath() {
        emit(PdfGraphicsEvent.StrokePath)
    }

    override fun fillPath(windingRule: Int) {
        emit(PdfGraphicsEvent.FillPath)
    }

    override fun fillAndStrokePath(windingRule: Int) {
        unsupported { "fillAndStrokePath" }
    }

    override fun shadingFill(shadingName: COSName) {
        unsupported { "shadingFill" }
    }

    override fun showTextString(string: ByteArray) {
        emit(PdfGraphicsEvent.ShowTextString(string.decodeToString(), textMatrix.clone()))

        super.showTextString(string)
    }
}