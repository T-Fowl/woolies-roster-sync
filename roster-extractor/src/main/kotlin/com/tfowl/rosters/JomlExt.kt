package com.tfowl.rosters

import org.joml.Matrix3x2f
import org.joml.Vector2fc
import java.awt.geom.AffineTransform

fun Matrix3x2f.get(dst: AffineTransform): AffineTransform {
    dst.setTransform(m00.toDouble(), m01.toDouble(), m10.toDouble(), m11.toDouble(), m20.toDouble(), m21.toDouble())
    return dst
}

val Vector2fc.x: Float get() = x()
val Vector2fc.y: Float get() = y()