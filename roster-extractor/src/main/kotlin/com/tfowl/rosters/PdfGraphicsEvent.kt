package com.tfowl.rosters

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.image.PDImage
import org.apache.pdfbox.util.Matrix
import java.awt.geom.Point2D
import kotlin.math.abs

sealed class PdfGraphicsEvent {
    data class MoveTo(val x: Float, val y: Float) : PdfGraphicsEvent()
    data class LineTo(val x: Float, val y: Float) : PdfGraphicsEvent()
    data class AppendRectangle(val p0: Point2D, val p1: Point2D, val p2: Point2D, val p3: Point2D) : PdfGraphicsEvent()
    data class ShowTextString(val text: String, val transform: Matrix) : PdfGraphicsEvent()

    object FillPath : PdfGraphicsEvent()
    object StrokePath : PdfGraphicsEvent()
}


private class PdfGraphicsStreamEvents : PDFGraphicsStreamEngine(null) {

    fun events(page: PDPage): List<PdfGraphicsEvent> {
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

internal fun PDPage.streamGraphicsEvents(): List<PdfGraphicsEvent> {
    val stream = PdfGraphicsStreamEvents()
    return stream.events(this)
}