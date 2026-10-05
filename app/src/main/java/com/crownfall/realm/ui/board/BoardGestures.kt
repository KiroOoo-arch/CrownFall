package com.crownfall.realm.ui.board

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Touch controls for the battlefield.
 *
 * Pinch zooms, drag pans (only while zoomed in) and a single tap selects or
 * moves. Double tap snaps the camera back to the full board, which is the
 * quickest way to recover your bearings mid-duel.
 */
fun Modifier.boardGestures(
    camera: BoardCamera,
    sideProvider: () -> Float,
    onTapSquare: (Offset) -> Unit
): Modifier = this
    .pointerInput(camera) {
        detectTransformGestures { _, panChange, zoomChange, rotationChange ->
            val side = sideProvider()
            camera.applyGesture(panChange, zoomChange, rotationChange, side)
        }
    }
    .pointerInput(camera) {
        detectTapGestures(
            onTap = { offset -> onTapSquare(offset) },
            onDoubleTap = { camera.reset(sideProvider()) }
        )
    }
