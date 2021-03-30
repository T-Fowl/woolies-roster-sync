package com.tfowl.rosters.pdf

import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import kotlin.math.sqrt

/*
    Immutable cousin to java.awt.geom.Point2D
 */
data class Point(val x: Double, val y: Double) {
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    fun distance(other: Point): Double {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    fun lerp(other: Point, t: Double): Point = Point(x + (other.x - x) * t, y + (other.y - y) * t)
}

internal fun Point2D.toPoint() = Point(x, y)


internal operator fun AffineTransform.invoke(point: Point): Point {
    val a = Point2D.Double(point.x, point.y)
    val b = transform(a, null)
    return b.toPoint()
}