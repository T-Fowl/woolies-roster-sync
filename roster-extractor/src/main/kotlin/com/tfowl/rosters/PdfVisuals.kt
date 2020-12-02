package com.tfowl.rosters

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.image.PDImage
import org.apache.pdfbox.util.Matrix
import java.awt.geom.Point2D
import kotlin.math.abs

internal fun Matrix.prettyPrint(): String = "[scale: ($scaleX, $scaleY), translate: ($translateX, $translateY)]"

internal fun Float.approx(other: Float, eps: Float = 0.001f): Boolean = abs(this - other) <= eps

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

data class PdfVisuals(
        val lines: List<PdfLine>,
        val rectangles: List<PdfRectangle>,
        val texts: List<PdfText>
)

fun PDPage.getVisualElements(): PdfVisuals {
    val engine = PdfElementsStreamEngine(this)
    engine.processPage(this)
    return PdfVisuals(engine.lines, engine.rectangles, engine.texts)
}

private class PdfElementsStreamEngine(page: PDPage,
                                      private val debug: Boolean = false) : PDFGraphicsStreamEngine(page) {

    val lines = mutableListOf<PdfLine>()
    val rectangles = mutableListOf<PdfRectangle>()
    val texts = mutableListOf<PdfText>()

    private val position = Point2D.Float(0.0f, 0.0f)

    private fun debug(msg: () -> String) {
        if (debug) {
            println(msg())
        }
    }

    override fun appendRectangle(p0: Point2D, p1: Point2D, p2: Point2D, p3: Point2D) {
        debug { "appendRectangle($p0, $p1, $p2, $p3)" }

        rectangles.add(PdfRectangle(p0, p1, p2, p3))
    }

    override fun drawImage(pdImage: PDImage) {
        debug { "drawImage($pdImage)" }
    }

    override fun clip(windingRule: Int) {
        debug { "clip($windingRule)" }
    }

    override fun moveTo(x: Float, y: Float) {
        debug { "moveTo($x, $y)" }

        position.setLocation(x, y)
    }

    override fun lineTo(x: Float, y: Float) {
        debug { "lineTo($x, $y)" }

        val start = Point2D.Float(position.x, position.y)
        val end = Point2D.Float(x, y)
        lines.add(PdfLine(start, end))

        position.setLocation(x, y)
    }

    override fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        // A lot of lines / paths seem to be terminated with an empty curveTo
        if (!x1.approx(x3) || !y1.approx(y3)) {
            debug { "curveTo($x1, $y1, $x2, $y2, $x3, $y3)" }
        }
    }

    override fun getCurrentPoint(): Point2D = position

    override fun closePath() {
        debug { "closePath()" }
    }

    override fun endPath() {
        debug { "endPath()" }
    }

    override fun strokePath() {
        debug { "strokePath()" }
    }

    override fun fillPath(windingRule: Int) {
        debug { "fillPath($windingRule)" }
    }

    override fun fillAndStrokePath(windingRule: Int) {
        debug { "fillAndStrokePath($windingRule)" }
    }

    override fun shadingFill(shadingName: COSName) {
        debug { "shadingFill($shadingName)" }
    }

    override fun showTextString(string: ByteArray) {
        val text = string.decodeToString()

        debug { """showTextString("$text", matrix=${textMatrix.prettyPrint()})""" }

        texts.add(PdfText(textMatrix.clone(), text))

        super.showTextString(string)
    }
}