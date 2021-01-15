package com.tfowl.rosters

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Ellipse2D
import kotlin.math.roundToInt

fun Double.roundToEighth(): Double = (this * 8.0).roundToInt() / 8.0

/**
 * Unsafe but convenient
 */
fun MatchGroupCollection.getValue(name: String): String = get(name)!!.value

fun Graphics2D.draw(colour: Color, shape: Shape) {
    val originalColour = color
    color = colour
    draw(shape)
    color = originalColour
}

@Suppress("FunctionName")
fun CenteredEllipse(x: Double, y: Double, radiusA: Double, radiusB: Double = radiusA): Ellipse2D =
    Ellipse2D.Double(x - 0.5 * radiusA, y - 0.5 * radiusA, radiusA, radiusB)

public inline fun <T> Iterable<T>.forEachWithNext(consumer: (a: T, b: T) -> Unit) {
    val iterator = iterator()
    if (!iterator.hasNext()) return
    var current = iterator.next()
    while (iterator.hasNext()) {
        val next = iterator.next()
        consumer(current, next)
        current = next
    }
}