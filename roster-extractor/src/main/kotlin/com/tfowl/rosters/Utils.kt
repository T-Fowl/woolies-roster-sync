package com.tfowl.rosters

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Ellipse2D
import kotlin.math.roundToInt

fun Double.roundToEighth(): Double = (this * 8.0).roundToInt() / 8.0

fun Point.roundToEighth(): Point = Point(x.roundToEighth(), y.roundToEighth())

/**
 * Unsafe but convenient
 */
fun MatchGroupCollection.getValue(name: String): String = get(name)!!.value

object DistinctColours {
    private val colours = arrayOf(
        0x000000,
        0x696969, 0xd3d3d3, 0x556b2f, 0x228b22, 0x7f0000,
        0x483d8b, 0x008b8b, 0xcd853f, 0x9acd32, 0x00008b,
        0x8fbc8f, 0x8b008b, 0xb03060, 0xff4500, 0xff8c00,
        0xffff00, 0x7fff00, 0x00ff7f, 0xdc143c, 0x00ffff,
        0x00bfff, 0x0000ff, 0xf08080, 0xff00ff, 0x1e90ff,
        0xf0e68c, 0x90ee90, 0xff1493, 0x7b68ee, 0xee82ee,
        0xdcbeff,
    ).map(::Color)

    operator fun get(index: Int): Color {
        return colours[index % colours.size]
    }
}

fun Graphics2D.draw(colour: Color, shape: Shape) {
    val originalColour = color
    color = colour
    draw(shape)
    color = originalColour
}

@Suppress("FunctionName")
fun CenteredEllipse(x: Double, y: Double, radiusA: Double, radiusB: Double = radiusA): Ellipse2D =
    Ellipse2D.Double(x - radiusA, y - radiusB, 2 * radiusA, 2 * radiusB)

inline fun <T> Iterable<T>.forEachWithNext(consumer: (a: T, b: T) -> Unit) {
    val iterator = iterator()
    if (!iterator.hasNext()) return
    var current = iterator.next()
    while (iterator.hasNext()) {
        val next = iterator.next()
        consumer(current, next)
        current = next
    }
}