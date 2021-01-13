package com.tfowl.rosters

import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.util.Matrix
import java.awt.geom.Point2D

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
    val events = streamGraphicsEvents()

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
