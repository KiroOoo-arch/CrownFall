package com.crownfall.realm.ui.board

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * The battlefield camera. Holds zoom, pan, rotation and the transient shake
 * offset used by heavy capture animations.
 *
 * The transform is computed with plain trigonometry instead of
 * `graphicsLayer` so tap coordinates can be inverted exactly: what the player
 * touches is always the square they see.
 */
@Stable
class BoardCamera(
    val minZoom: Float = 1f,
    val maxZoom: Float = 3.2f
) {
    var zoom by mutableFloatStateOf(1f)
        private set

    var pan by mutableStateOf(Offset.Zero)
        private set

    var rotation by mutableFloatStateOf(0f)
        private set

    var shakeOffset by mutableStateOf(Offset.Zero)
        private set

    /** True while the capture camera is framing a duel; the UI shows a hint then. */
    var isFraming by mutableStateOf(false)
        private set

    val isZoomed: Boolean get() = zoom > minZoom + 0.01f

    fun applyGesture(panChange: Offset, zoomChange: Float, rotationChange: Float, side: Float) {
        setZoom(zoom * zoomChange, side)
        applyRotation(rotation + rotationChange * 0.35f)
        pan = clampPan(pan + panChange, side)
    }

    fun setZoom(value: Float, side: Float) {
        zoom = value.coerceIn(minZoom, maxZoom)
        pan = clampPan(pan, side)
    }

    fun applyRotation(value: Float) {
        rotation = value.coerceIn(-28f, 28f)
    }

    /** Used by the auto-rotate feature, which needs a full half turn. */
    fun applyFramingRotation(value: Float) {
        rotation = value.coerceIn(-180f, 180f)
    }

    fun setPan(value: Offset, side: Float) {
        pan = clampPan(value, side)
    }

    fun setShake(value: Offset) {
        shakeOffset = value
    }

    fun markFraming(framing: Boolean) {
        isFraming = framing
    }

    fun reset(side: Float = 0f) {
        zoom = minZoom
        pan = Offset.Zero
        rotation = 0f
        shakeOffset = Offset.Zero
        isFraming = false
    }

    /** Smoothly eases toward a target framing; callers suspend on this. */
    fun setImmediate(zoom: Float, pan: Offset, rotation: Float, side: Float) {
        setZoom(zoom, side)
        applyFramingRotation(rotation)
        setPan(pan, side)
    }

    fun boardToView(point: Offset, side: Float): Offset {
        val center = Offset(side / 2f, side / 2f)
        val v = point - center
        val radians = rotation * (Math.PI.toFloat() / 180f)
        val cosR = cos(radians)
        val sinR = sin(radians)
        val rotated = Offset(v.x * cosR - v.y * sinR, v.x * sinR + v.y * cosR)
        return Offset(
            rotated.x * zoom + center.x + pan.x + shakeOffset.x,
            rotated.y * zoom + center.y + pan.y + shakeOffset.y
        )
    }

    fun viewToBoard(point: Offset, side: Float): Offset {
        val center = Offset(side / 2f, side / 2f)
        val v = Offset(
            point.x - center.x - pan.x - shakeOffset.x,
            point.y - center.y - pan.y - shakeOffset.y
        )
        val scaled = Offset(v.x / zoom, v.y / zoom)
        val radians = -rotation * (Math.PI.toFloat() / 180f)
        val cosR = cos(radians)
        val sinR = sin(radians)
        val rotated = Offset(
            scaled.x * cosR - scaled.y * sinR,
            scaled.x * sinR + scaled.y * cosR
        )
        return Offset(rotated.x + center.x, rotated.y + center.y)
    }

    /** Keeps the visible window inside the board so the player never sees the void. */
    private fun clampPan(candidate: Offset, side: Float): Offset {
        if (zoom <= minZoom + 0.001f || side <= 0f) return Offset.Zero
        val maxOffset = side * (zoom - 1f) / 2f
        return Offset(
            candidate.x.coerceIn(-maxOffset, maxOffset),
            candidate.y.coerceIn(-maxOffset, maxOffset)
        )
    }
}
