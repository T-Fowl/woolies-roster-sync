package com.tfowl.rosters

import org.apache.pdfbox.pdmodel.PDPage
import org.joml.Matrix3x2f
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import kotlin.math.floor
import kotlin.math.max

internal data class ImageWithGraphics(
    val image: BufferedImage,
    val graphics: Graphics2D,
)

interface VisualDebugger {
    fun visualise(layer: String, block: Graphics2D.() -> Unit)

    fun images(): LinkedHashMap<String, BufferedImage>
}

class NoOpVisualDebugger : VisualDebugger {
    override fun visualise(layer: String, block: Graphics2D.() -> Unit) {
        //no-op
    }

    override fun images(): LinkedHashMap<String, BufferedImage> {
        return LinkedHashMap()
    }
}

class BufferedImageVisualDebugger(private val page: PDPage) : VisualDebugger {

    private val dpi = 300f
    private val pageRenderingTransform = page.displayTransform(dpi)
    private val debugImages = LinkedHashMap<String, ImageWithGraphics>()

    private fun createImage(): ImageWithGraphics {
        val image = page.createRenderableImage(dpi)
        val graphics: Graphics2D = image.createGraphics().apply {
            background = Color.WHITE
            clearRect(0, 0, image.width, image.height)
            color = Color.BLACK
            stroke = BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
            transform(pageRenderingTransform.get(AffineTransform()))
        }
        return ImageWithGraphics(image, graphics)
    }

    override fun visualise(layer: String, block: Graphics2D.() -> Unit) {
        val (_, graphics) = debugImages.computeIfAbsent(layer) { createImage() }
        graphics.block()
    }

    override fun images(): LinkedHashMap<String, BufferedImage> =
        debugImages.mapValuesTo(LinkedHashMap()) { (_, iwg) -> iwg.image }
}

internal fun PDPage.createRenderableImage(dpi: Float = 300.0f): BufferedImage {
    val scale = dpi / 72.0f

    val widthPt = cropBox.width
    val heightPt = cropBox.height

    val widthPx = max(floor(widthPt * scale), 1.0f).toInt()
    val heightPx = max(floor(heightPt * scale), 1.0f).toInt()

    return when (rotation) {
        90, 270 -> BufferedImage(heightPx, widthPx, BufferedImage.TYPE_INT_ARGB)
        else    -> BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB)
    }
}

internal fun PDPage.displayTransform(dpi: Float = 300.0f): Matrix3x2f = Matrix3x2f().apply {
    this.scale(dpi / 72.0f)

    val rotationAngle = rotation
    val cropBox = cropBox
    val pageSize = cropBox

    when (rotationAngle) {
        90   -> this.translate(cropBox.height, 0.0f)
        270  -> this.translate(0.0f, cropBox.width)
        180  -> this.translate(cropBox.width, cropBox.height)
        else -> this.translate(0.0f, 0.0f)
    }

    // TODO: Given that rotationAngle can only be multiples of 90 degrees, simply swap elements in matrix (floating-point precision errors)
    this.rotate(Math.toRadians(rotationAngle.toDouble()).toFloat())

    this.translate(0.0f, pageSize.height)
    this.scale(1.0f, -1.0f)
    this.translate(-pageSize.lowerLeftX, -pageSize.lowerLeftY)
}

fun <T> VisualDebugger.visualise(layer: String, arg: T, block: Graphics2D.(T) -> Unit = {}) {
    visualise(layer) { this.block(arg) }
}

fun <T> VisualDebugger.visualiseEach(layer: String, args: Iterable<T>, block: Graphics2D.(T) -> Unit = {}) {
    visualise(layer) { args.forEach { arg -> this.block(arg) } }
}