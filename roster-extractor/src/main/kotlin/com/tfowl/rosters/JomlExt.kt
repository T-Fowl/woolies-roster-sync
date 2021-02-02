@file:Suppress("unused")

package com.tfowl.rosters

import org.joml.AABBd
import org.joml.Matrix3x2f
import org.joml.Vector2fc
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import java.awt.geom.Rectangle2D

fun Matrix3x2f.get(dst: AffineTransform): AffineTransform {
    dst.setTransform(m00.toDouble(), m01.toDouble(), m10.toDouble(), m11.toDouble(), m20.toDouble(), m21.toDouble())
    return dst
}

val Vector2fc.x: Float get() = x()
val Vector2fc.y: Float get() = y()

val AABBd.width: Double get() = maxX - minX
val AABBd.height: Double get() = maxY - minY

class AABBCollectiond(private val _boxes: MutableList<AABBd> = mutableListOf()) {
    constructor(vararg box: AABBd) : this(mutableListOf(*box))

    val boxes: List<AABBd> get() = _boxes

    fun intersects(other: AABBCollectiond): Boolean {
        return _boxes.any { thisBox ->
            other._boxes.any { otherBox ->
                otherBox.intersectsAABB(thisBox)
            }
        }
    }

    fun append(aabb: AABBd) {
        _boxes += aabb
    }

    fun append(aabb: AABBCollectiond) {
        aabb.boxes.forEach(::append)
    }
}

fun Graphics2D.draw(colour: Color, boundingBoxes: AABBCollectiond) {
    val shape = GeneralPath()
    boundingBoxes.boxes.forEach { aabb ->
        shape.append(Rectangle2D.Double(aabb.minX, aabb.minY, aabb.width, aabb.height), false)
    }
    draw(colour, shape)
}