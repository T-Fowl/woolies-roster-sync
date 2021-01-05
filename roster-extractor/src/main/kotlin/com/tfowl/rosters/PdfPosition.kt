package com.tfowl.rosters

import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import org.joml.Vector2f
import org.joml.Vector2fc

class PdfPosition private constructor(
        private val _pt: Vector2f,
        private val _transform: Matrix3x2f
) {
    private val _px: Vector2f = _transform.transformPosition(_pt, Vector2f())

    val pt: Vector2fc get() = _pt
    val px: Vector2fc get() = _px
    val transform: Matrix3x2fc get() = _transform

    constructor(position: PdfPosition) : this(Vector2f(position._pt), Matrix3x2f(position._transform))

    constructor(x: Float, y: Float, transform: Matrix3x2f) : this(Vector2f(x, y), Matrix3x2f(transform))

    fun set(x: Float, y: Float, transform: Matrix3x2f) {
        _pt.set(x, y)
        _transform.set(transform)
        _transform.transformPosition(_pt, _px)
    }
}