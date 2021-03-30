package com.tfowl.rosters.pdf

import java.awt.geom.AffineTransform

data class PdfPoint(val pt: Point, val px: Point) {
    constructor(pt: Point, transform: AffineTransform) : this(pt, transform(pt))

    val x: Double = pt.x
    val y: Double = pt.y
}
