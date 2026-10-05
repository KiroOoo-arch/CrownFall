package com.crownfall.realm.ui.board

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.crownfall.realm.domain.animation.IdleTransform
import com.crownfall.realm.domain.animation.PiecePose
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.ui.theme.CrownfallPalette
import kotlin.math.min

private data class ArmorPalette(
    val metal: Color,
    val metalDark: Color,
    val metalBright: Color,
    val cloth: Color,
    val accent: Color,
    val trim: Color,
    val plume: Color
)

private fun paletteFor(faction: Faction): ArmorPalette = if (faction == Faction.DAWN) {
    ArmorPalette(
        metal = CrownfallPalette.DawnArmor,
        metalDark = Color(0xFF8C846C),
        metalBright = Color(0xFFF2E9D2),
        cloth = CrownfallPalette.DawnCloth,
        accent = CrownfallPalette.DawnAccent,
        trim = CrownfallPalette.GoldBright,
        plume = Color(0xFFF5EFE0)
    )
} else {
    ArmorPalette(
        metal = CrownfallPalette.DuskArmor,
        metalDark = Color(0xFF3A343D),
        metalBright = Color(0xFF9A90A0),
        cloth = CrownfallPalette.DuskCloth,
        accent = CrownfallPalette.DuskAccent,
        trim = Color(0xFFC06A78),
        plume = CrownfallPalette.DuskAccent
    )
}

/**
 * Draws one piece inside the given square. The art is vector geometry rather
 * than bitmaps so it stays crisp at every zoom level and ships no binary assets.
 */
object PieceArt {

    fun DrawScope.drawPiece(
        type: PieceType,
        faction: Faction,
        squareSize: Size,
        idle: IdleTransform = IdleTransform.NONE,
        pose: PiecePose = PiecePose.NEUTRAL,
        alpha: Float = 1f
    ) {
        val palette = paletteFor(faction)
        val side = min(squareSize.width, squareSize.height)
        val bob = idle.bobOffset * side
        val sway = idle.swayOffset * side
        val breathing = idle.breatheScale

        withTransform({
            translate(
                left = sway + squareSize.width * 0.02f * pose.lunge,
                top = bob - squareSize.height * 0.06f * pose.leap
            )
            rotate(degrees = pose.tiltDegrees, pivot = Offset(squareSize.width * 0.5f, squareSize.height * 0.9f))
            scale(
                scaleX = 1f,
                scaleY = breathing,
                pivot = Offset(squareSize.width * 0.5f, squareSize.height)
            )
        }) {
            when (type) {
                PieceType.KING -> drawKing(this, palette, squareSize, idle, pose, alpha)
                PieceType.QUEEN -> drawQueen(this, palette, squareSize, idle, pose, alpha)
                PieceType.ROOK -> drawGuardian(this, palette, squareSize, idle, pose, alpha)
                PieceType.BISHOP -> drawCleric(this, palette, squareSize, idle, pose, alpha)
                PieceType.KNIGHT -> drawWarhorseKnight(this, palette, squareSize, idle, pose, alpha)
                PieceType.PAWN -> drawFootSoldier(this, palette, squareSize, idle, pose, alpha)
            }
        }
    }

    // ------------------------------------------------------------ shared bits

    private fun DrawScope.drawLegs(
        palette: ArmorPalette, area: Size, spread: Float, alpha: Float, skirted: Boolean = false
    ) {
        val cx = area.width * 0.5f
        val bottom = area.height * 0.92f
        val hip = area.height * 0.60f
        val legWidth = area.width * 0.09f
        listOf(-1f, 1f).forEach { direction ->
            val x = cx + direction * spread * area.width * 0.5f
            if (skirted) {
                drawRoundRect(
                    color = palette.cloth.copy(alpha = alpha),
                    topLeft = Offset(x - legWidth * 1.6f, hip),
                    size = Size(legWidth * 3.2f, bottom - hip),
                    cornerRadius = CornerRadius(legWidth)
                )
            } else {
                drawRoundRect(
                    color = palette.metalDark.copy(alpha = alpha),
                    topLeft = Offset(x - legWidth, hip),
                    size = Size(legWidth * 2f, bottom - hip),
                    cornerRadius = CornerRadius(legWidth * 0.7f)
                )
                // Greave highlight.
                drawRoundRect(
                    color = palette.metal.copy(alpha = alpha),
                    topLeft = Offset(x - legWidth * 0.6f, hip + legWidth * 0.7f),
                    size = Size(legWidth * 1.2f, (bottom - hip) * 0.55f),
                    cornerRadius = CornerRadius(legWidth * 0.5f)
                )
            }
        }
    }

    private fun DrawScope.drawTorso(
        palette: ArmorPalette, area: Size, width: Float, alpha: Float, chestPlate: Boolean = true
    ) {
        val cx = area.width * 0.5f
        val top = area.height * 0.40f
        val hip = area.height * 0.64f
        val half = area.width * width * 0.5f
        drawRoundRect(
            color = palette.metal.copy(alpha = alpha),
            topLeft = Offset(cx - half, top),
            size = Size(half * 2f, hip - top),
            cornerRadius = CornerRadius(half * 0.45f)
        )
        if (chestPlate) {
            drawRoundRect(
                color = palette.metalBright.copy(alpha = alpha * 0.55f),
                topLeft = Offset(cx - half * 0.55f, top + (hip - top) * 0.18f),
                size = Size(half * 1.1f, (hip - top) * 0.40f),
                cornerRadius = CornerRadius(half * 0.3f)
            )
        }
        // Belt with royal buckle.
        drawRect(
            color = palette.metalDark.copy(alpha = alpha),
            topLeft = Offset(cx - half * 0.95f, hip - (hip - top) * 0.16f),
            size = Size(half * 1.9f, (hip - top) * 0.10f)
        )
        drawCircle(
            color = palette.accent.copy(alpha = alpha),
            radius = half * 0.16f,
            center = Offset(cx, hip - (hip - top) * 0.11f)
        )
    }

    private fun DrawScope.drawHead(
        palette: ArmorPalette, area: Size, alpha: Float, helmet: Boolean, plume: Boolean
    ) {
        val cx = area.width * 0.5f
        val radius = area.width * 0.115f
        val cy = area.height * 0.33f
        if (helmet) {
            drawCircle(color = palette.metal.copy(alpha = alpha), radius = radius, center = Offset(cx, cy))
            drawRect(
                color = palette.metalDark.copy(alpha = alpha),
                topLeft = Offset(cx - radius, cy),
                size = Size(radius * 2f, radius * 0.75f)
            )
            // Visor slit.
            drawRect(
                color = CrownfallPalette.Void.copy(alpha = alpha * 0.85f),
                topLeft = Offset(cx - radius * 0.72f, cy - radius * 0.10f),
                size = Size(radius * 1.44f, radius * 0.22f)
            )
            if (plume) {
                val path = Path().apply {
                    moveTo(cx, cy - radius)
                    cubicTo(
                        cx - radius * 0.4f, cy - radius * 2.4f,
                        cx + radius * 1.4f, cy - radius * 2.2f,
                        cx + radius * 0.6f, cy - radius * 0.7f
                    )
                    close()
                }
                drawPath(path, color = palette.plume.copy(alpha = alpha))
            }
        } else {
            // Face with a simple royal visage.
            drawCircle(color = Color(0xFFD8B79A).copy(alpha = alpha), radius = radius, center = Offset(cx, cy))
            drawRect(
                color = palette.metalDark.copy(alpha = alpha),
                topLeft = Offset(cx - radius, cy - radius * 1.15f),
                size = Size(radius * 2f, radius * 0.5f)
            )
        }
    }

    private fun DrawScope.drawCrown(palette: ArmorPalette, area: Size, alpha: Float, tall: Boolean) {
        val cx = area.width * 0.5f
        val radius = area.width * 0.115f
        val baseY = area.height * 0.33f - radius * 1.05f
        val height = if (tall) radius * 0.95f else radius * 0.62f
        val halfWidth = radius * 1.05f
        val path = Path().apply {
            moveTo(cx - halfWidth, baseY)
            lineTo(cx - halfWidth, baseY - height * 0.45f)
            lineTo(cx - halfWidth * 0.5f, baseY - height * 0.15f)
            lineTo(cx, baseY - height)
            lineTo(cx + halfWidth * 0.5f, baseY - height * 0.15f)
            lineTo(cx + halfWidth, baseY - height * 0.45f)
            lineTo(cx + halfWidth, baseY)
            close()
        }
        drawPath(path, color = palette.accent.copy(alpha = alpha))
        drawCircle(color = palette.trim.copy(alpha = alpha), radius = radius * 0.13f, center = Offset(cx, baseY - height))
        listOf(-0.5f, 0.5f).forEach { dir ->
            drawCircle(
                color = palette.trim.copy(alpha = alpha),
                radius = radius * 0.10f,
                center = Offset(cx + dir * halfWidth, baseY - height * 0.42f)
            )
        }
    }

    private fun DrawScope.drawCape(palette: ArmorPalette, area: Size, alpha: Float, sway: Float) {
        val cx = area.width * 0.5f
        val top = area.height * 0.42f
        val bottom = area.height * 0.88f
        val path = Path().apply {
            moveTo(cx - area.width * 0.18f, top)
            cubicTo(
                cx - area.width * 0.34f - sway * area.width * 0.03f, top + (bottom - top) * 0.4f,
                cx - area.width * 0.26f - sway * area.width * 0.04f, bottom - (bottom - top) * 0.08f,
                cx - area.width * 0.10f + sway * area.width * 0.02f, bottom
            )
            lineTo(cx + area.width * 0.10f + sway * area.width * 0.02f, bottom)
            cubicTo(
                cx + area.width * 0.26f - sway * area.width * 0.04f, bottom - (bottom - top) * 0.08f,
                cx + area.width * 0.34f - sway * area.width * 0.03f, top + (bottom - top) * 0.4f,
                cx + area.width * 0.18f, top
            )
            close()
        }
        drawPath(path, color = palette.accent.copy(alpha = alpha * 0.85f))
        drawPath(path, color = palette.metalDark.copy(alpha = alpha * 0.25f), style = Stroke(width = area.width * 0.012f))
    }

    private fun DrawScope.drawSword(
        palette: ArmorPalette, area: Size, alpha: Float, raise: Float, ornate: Boolean, flip: Boolean = false
    ) {
        val cx = area.width * 0.5f
        val handY = area.height * 0.56f
        val baseX = cx + area.width * 0.19f * (if (flip) -1f else 1f)
        val angle = -18f - raise * 62f
        val bladeLength = area.height * 0.34f
        rotate(degrees = angle, pivot = Offset(baseX, handY)) {
            drawRoundRect(
                color = palette.metalBright.copy(alpha = alpha),
                topLeft = Offset(baseX - area.width * 0.018f, handY - bladeLength),
                size = Size(area.width * 0.036f, bladeLength),
                cornerRadius = CornerRadius(area.width * 0.012f)
            )
            // Cross guard.
            drawRoundRect(
                color = palette.accent.copy(alpha = alpha),
                topLeft = Offset(baseX - area.width * 0.055f, handY - area.height * 0.035f),
                size = Size(area.width * 0.11f, area.height * 0.022f),
                cornerRadius = CornerRadius(area.width * 0.01f)
            )
            // Grip and pommel.
            drawRoundRect(
                color = palette.metalDark.copy(alpha = alpha),
                topLeft = Offset(baseX - area.width * 0.014f, handY),
                size = Size(area.width * 0.028f, area.height * 0.075f),
                cornerRadius = CornerRadius(area.width * 0.01f)
            )
            drawCircle(
                color = palette.accent.copy(alpha = alpha),
                radius = area.width * 0.024f,
                center = Offset(baseX, handY + area.height * 0.082f)
            )
            if (ornate) {
                drawCircle(
                    color = palette.trim.copy(alpha = alpha),
                    radius = area.width * 0.016f,
                    center = Offset(baseX, handY - area.height * 0.05f)
                )
            }
        }
    }

    private fun DrawScope.drawRoundShield(palette: ArmorPalette, area: Size, alpha: Float, forward: Float) {
        val cx = area.width * 0.5f - area.width * (0.22f + forward * 0.10f)
        val cy = area.height * 0.60f
        val radius = area.width * 0.17f
        drawCircle(color = palette.metalDark.copy(alpha = alpha), radius = radius, center = Offset(cx, cy))
        drawCircle(
            color = palette.metal.copy(alpha = alpha),
            radius = radius * 0.82f,
            center = Offset(cx, cy),
            style = Stroke(width = area.width * 0.02f)
        )
        drawLine(
            color = palette.accent.copy(alpha = alpha),
            start = Offset(cx, cy - radius * 0.8f),
            end = Offset(cx, cy + radius * 0.8f),
            strokeWidth = area.width * 0.022f
        )
        drawCircle(color = palette.accent.copy(alpha = alpha), radius = radius * 0.16f, center = Offset(cx, cy))
    }

    private fun DrawScope.drawTowerShield(palette: ArmorPalette, area: Size, alpha: Float, forward: Float) {
        val cx = area.width * 0.5f - area.width * (0.20f + forward * 0.12f)
        val top = area.height * 0.46f
        val width = area.width * 0.30f
        val height = area.height * 0.34f
        val path = Path().apply {
            moveTo(cx - width * 0.5f, top)
            lineTo(cx + width * 0.5f, top)
            lineTo(cx + width * 0.5f, top + height * 0.62f)
            lineTo(cx, top + height)
            lineTo(cx - width * 0.5f, top + height * 0.62f)
            close()
        }
        drawPath(path, color = palette.metal.copy(alpha = alpha))
        drawPath(path, color = palette.metalDark.copy(alpha = alpha), style = Stroke(width = area.width * 0.016f))
        drawLine(
            color = palette.accent.copy(alpha = alpha),
            start = Offset(cx - width * 0.5f, top + height * 0.22f),
            end = Offset(cx + width * 0.5f, top + height * 0.22f),
            strokeWidth = area.width * 0.018f
        )
        drawCircle(color = palette.accent.copy(alpha = alpha), radius = area.width * 0.045f, center = Offset(cx, top + height * 0.48f))
    }

    private fun DrawScope.drawStaff(palette: ArmorPalette, area: Size, alpha: Float, raise: Float, glow: Float) {
        val cx = area.width * 0.5f
        val handY = area.height * 0.56f
        val baseX = cx + area.width * 0.20f
        val angle = -8f - raise * 48f
        rotate(degrees = angle, pivot = Offset(baseX, handY)) {
            drawRoundRect(
                color = Color(0xFF6B4A2A).copy(alpha = alpha),
                topLeft = Offset(baseX - area.width * 0.014f, handY - area.height * 0.40f),
                size = Size(area.width * 0.028f, area.height * 0.58f),
                cornerRadius = CornerRadius(area.width * 0.01f)
            )
            val orbY = handY - area.height * 0.42f
            val orbRadius = area.width * 0.055f
            if (glow > 0.01f || raise > 0.01f) {
                drawCircle(
                    color = CrownfallPalette.MagicGlow.copy(alpha = glow.coerceIn(0f, 1f) * 0.5f * alpha),
                    radius = orbRadius * 3.2f,
                    center = Offset(baseX, orbY)
                )
            }
            drawCircle(color = CrownfallPalette.Magic.copy(alpha = alpha), radius = orbRadius, center = Offset(baseX, orbY))
            drawCircle(
                color = CrownfallPalette.MagicGlow.copy(alpha = alpha),
                radius = orbRadius * 0.45f,
                center = Offset(baseX, orbY)
            )
        }
    }

    private fun DrawScope.drawSpear(palette: ArmorPalette, area: Size, alpha: Float, raise: Float) {
        val cx = area.width * 0.5f
        val handY = area.height * 0.58f
        val baseX = cx + area.width * 0.17f
        val angle = -6f - raise * 30f
        rotate(degrees = angle, pivot = Offset(baseX, handY)) {
            drawRoundRect(
                color = Color(0xFF6B4A2A).copy(alpha = alpha),
                topLeft = Offset(baseX - area.width * 0.011f, handY - area.height * 0.42f),
                size = Size(area.width * 0.022f, area.height * 0.50f),
                cornerRadius = CornerRadius(area.width * 0.008f)
            )
            val tipY = handY - area.height * 0.44f
            val path = Path().apply {
                moveTo(baseX, tipY - area.height * 0.07f)
                lineTo(baseX + area.width * 0.045f, tipY + area.height * 0.02f)
                lineTo(baseX, tipY + area.height * 0.06f)
                lineTo(baseX - area.width * 0.045f, tipY + area.height * 0.02f)
                close()
            }
            drawPath(path, color = palette.metalBright.copy(alpha = alpha))
        }
    }

    private fun DrawScope.drawImpactFlash(area: Size, intensity: Float) {
        if (intensity <= 0.01f) return
        drawCircle(
            color = CrownfallPalette.GoldBright.copy(alpha = (intensity * 0.45f).coerceIn(0f, 1f)),
            radius = area.width * 0.5f * intensity.coerceIn(0f, 1f),
            center = Offset(area.width * 0.5f, area.height * 0.5f)
        )
    }

    // ---------------------------------------------------------------- pieces

    private fun drawKing(
        scope: DrawScope, palette: ArmorPalette, area: Size, idle: IdleTransform, pose: PiecePose, alpha: Float
    ) = with(scope) {
        drawCape(palette, area, alpha * 0.9f, idle.cloakSway)
        drawLegs(palette, area, spread = 0.30f, alpha = alpha)
        drawTorso(palette, area, width = 0.34f, alpha = alpha)
        drawHead(palette, area, alpha, helmet = false, plume = false)
        drawCrown(palette, area, alpha, tall = true)
        drawSword(palette, area, alpha, raise = pose.armRaise, ornate = true, flip = true)
        drawCapeOverlayAccent(palette, area, alpha)
        drawImpactFlash(area, pose.impactFlash)
    }

    private fun DrawScope.drawCapeOverlayAccent(palette: ArmorPalette, area: Size, alpha: Float) {
        val cx = area.width * 0.5f
        drawLine(
            color = palette.trim.copy(alpha = alpha * 0.7f),
            start = Offset(cx - area.width * 0.16f, area.height * 0.45f),
            end = Offset(cx + area.width * 0.16f, area.height * 0.45f),
            strokeWidth = area.width * 0.018f
        )
    }

    private fun drawQueen(
        scope: DrawScope, palette: ArmorPalette, area: Size, idle: IdleTransform, pose: PiecePose, alpha: Float
    ) = with(scope) {
        // Long royal cloak.
        val cx = area.width * 0.5f
        val top = area.height * 0.40f
        val path = Path().apply {
            moveTo(cx - area.width * 0.16f, top)
            cubicTo(
                cx - area.width * 0.36f - idle.cloakSway * area.width * 0.02f, area.height * 0.68f,
                cx - area.width * 0.30f, area.height * 0.90f,
                cx - area.width * 0.12f, area.height * 0.93f
            )
            lineTo(cx + area.width * 0.12f, area.height * 0.93f)
            cubicTo(
                cx + area.width * 0.30f, area.height * 0.90f,
                cx + area.width * 0.36f - idle.cloakSway * area.width * 0.02f, area.height * 0.68f,
                cx + area.width * 0.16f, top
            )
            close()
        }
        drawPath(path, color = palette.cloth.copy(alpha = alpha * 0.9f))
        drawPath(path, color = palette.accent.copy(alpha = alpha * 0.35f), style = Stroke(width = area.width * 0.014f))
        drawLegs(palette, area, spread = 0.26f, alpha = alpha)
        drawTorso(palette, area, width = 0.30f, alpha = alpha)
        drawHead(palette, area, alpha, helmet = false, plume = false)
        drawCrown(palette, area, alpha, tall = false)
        drawSword(palette, area, alpha, raise = pose.armRaise, ornate = true)
        drawImpactFlash(area, pose.impactFlash)
    }

    private fun drawGuardian(
        scope: DrawScope, palette: ArmorPalette, area: Size, idle: IdleTransform, pose: PiecePose, alpha: Float
    ) = with(scope) {
        // Armored tower knight: broad shoulders, crenellated helm, big shield.
        drawLegs(palette, area, spread = 0.36f, alpha = alpha)
        drawTorso(palette, area, width = 0.44f, alpha = alpha)
        // Pauldrons.
        listOf(-1f, 1f).forEach { dir ->
            drawRoundRect(
                color = palette.metalDark.copy(alpha = alpha),
                topLeft = Offset(
                    area.width * 0.5f + dir * area.width * 0.24f - area.width * 0.07f,
                    area.height * 0.42f
                ),
                size = Size(area.width * 0.14f, area.height * 0.12f),
                cornerRadius = CornerRadius(area.width * 0.05f)
            )
        }
        drawHead(palette, area, alpha, helmet = true, plume = false)
        // Crenellations on the helm: the "tower" identity.
        val cx = area.width * 0.5f
        val helmTop = area.height * 0.33f - area.width * 0.115f
        for (i in -1..1) {
            drawRect(
                color = palette.accent.copy(alpha = alpha),
                topLeft = Offset(cx + i * area.width * 0.075f - area.width * 0.028f, helmTop - area.height * 0.05f),
                size = Size(area.width * 0.056f, area.height * 0.055f)
            )
        }
        drawTowerShield(palette, area, alpha, pose.shieldForward)
        drawSword(palette, area, alpha, raise = pose.armRaise * 0.4f, ornate = false)
        drawImpactFlash(area, pose.impactFlash)
    }

    private fun drawCleric(
        scope: DrawScope, palette: ArmorPalette, area: Size, idle: IdleTransform, pose: PiecePose, alpha: Float
    ) = with(scope) {
        // Flowing robe instead of legs.
        val cx = area.width * 0.5f
        val path = Path().apply {
            moveTo(cx - area.width * 0.16f, area.height * 0.46f)
            cubicTo(
                cx - area.width * 0.30f, area.height * 0.70f,
                cx - area.width * 0.30f + idle.swayOffset * area.width * 0.02f, area.height * 0.92f,
                cx - area.width * 0.26f, area.height * 0.94f
            )
            lineTo(cx + area.width * 0.26f, area.height * 0.94f)
            cubicTo(
                cx + area.width * 0.30f + idle.swayOffset * area.width * 0.02f, area.height * 0.92f,
                cx + area.width * 0.30f, area.height * 0.70f,
                cx + area.width * 0.16f, area.height * 0.46f
            )
            close()
        }
        drawPath(path, color = palette.cloth.copy(alpha = alpha))
        drawPath(path, color = palette.accent.copy(alpha = alpha * 0.5f), style = Stroke(width = area.width * 0.014f))
        // Stole.
        drawRoundRect(
            color = palette.accent.copy(alpha = alpha),
            topLeft = Offset(cx - area.width * 0.05f, area.height * 0.46f),
            size = Size(area.width * 0.10f, area.height * 0.42f),
            cornerRadius = CornerRadius(area.width * 0.02f)
        )
        drawTorso(palette, area, width = 0.30f, alpha = alpha, chestPlate = false)
        drawHead(palette, area, alpha, helmet = false, plume = false)
        // Hood.
        val hood = Path().apply {
            moveTo(cx - area.width * 0.14f, area.height * 0.36f)
            cubicTo(
                cx - area.width * 0.14f, area.height * 0.24f,
                cx + area.width * 0.14f, area.height * 0.24f,
                cx + area.width * 0.14f, area.height * 0.36f
            )
            close()
        }
        drawPath(hood, color = palette.accent.copy(alpha = alpha * 0.9f))
        drawStaff(palette, area, alpha, raise = pose.armRaise, glow = pose.impactFlash)
        drawImpactFlash(area, pose.impactFlash)
    }

    private fun drawWarhorseKnight(
        scope: DrawScope, palette: ArmorPalette, area: Size, idle: IdleTransform, pose: PiecePose, alpha: Float
    ) = with(scope) {
        val horse = if (palette.metal == CrownfallPalette.DawnArmor) Color(0xFFE7DCC4) else Color(0xFF2C2730)
        val horseDark = if (palette.metal == CrownfallPalette.DawnArmor) Color(0xFFBFB39A) else Color(0xFF1A171F)

        // Horse body.
        drawRoundRect(
            color = horse,
            topLeft = Offset(area.width * 0.18f, area.height * 0.58f),
            size = Size(area.width * 0.60f, area.height * 0.20f),
            cornerRadius = CornerRadius(area.width * 0.10f)
        )
        // Four legs.
        listOf(0.24f, 0.36f, 0.62f, 0.74f).forEachIndexed { index, fx ->
            val lift = if (pose.leap > 0.01f && index % 2 == 1) area.height * 0.06f * pose.leap else 0f
            drawRoundRect(
                color = horseDark,
                topLeft = Offset(area.width * fx, area.height * 0.72f - lift),
                size = Size(area.width * 0.07f, area.height * 0.21f),
                cornerRadius = CornerRadius(area.width * 0.03f)
            )
        }
        // Neck and head, with a small head bob so the horse feels alive.
        val headDrop = idle.bobOffset * area.height * 0.8f
        val neck = Path().apply {
            moveTo(area.width * 0.72f, area.height * 0.62f)
            lineTo(area.width * 0.86f, area.height * 0.40f + headDrop)
            lineTo(area.width * 0.94f, area.height * 0.44f + headDrop)
            lineTo(area.width * 0.82f, area.height * 0.68f)
            close()
        }
        drawPath(neck, color = horse)
        drawRoundRect(
            color = horse,
            topLeft = Offset(area.width * 0.80f, area.height * 0.34f + headDrop),
            size = Size(area.width * 0.18f, area.height * 0.12f),
            cornerRadius = CornerRadius(area.width * 0.05f)
        )
        // Horse armour plate and saddle cloth.
        drawRoundRect(
            color = palette.metal.copy(alpha = alpha),
            topLeft = Offset(area.width * 0.30f, area.height * 0.56f),
            size = Size(area.width * 0.30f, area.height * 0.10f),
            cornerRadius = CornerRadius(area.width * 0.04f)
        )
        drawRoundRect(
            color = palette.accent.copy(alpha = alpha),
            topLeft = Offset(area.width * 0.34f, area.height * 0.66f),
            size = Size(area.width * 0.22f, area.height * 0.10f),
            cornerRadius = CornerRadius(area.width * 0.02f)
        )

        // Rider.
        val riderX = area.width * 0.44f
        drawRoundRect(
            color = palette.metalDark.copy(alpha = alpha),
            topLeft = Offset(riderX - area.width * 0.10f, area.height * 0.40f),
            size = Size(area.width * 0.20f, area.height * 0.20f),
            cornerRadius = CornerRadius(area.width * 0.06f)
        )
        drawHead(palette, area = Size(area.width * 0.7f, area.height * 0.8f), alpha = alpha, helmet = true, plume = true)
        // Lance, long and forward.
        rotate(degrees = -28f - pose.armRaise * 12f, pivot = Offset(area.width * 0.5f, area.height * 0.5f)) {
            drawRoundRect(
                color = Color(0xFF6B4A2A).copy(alpha = alpha),
                topLeft = Offset(area.width * 0.30f, area.height * 0.44f),
                size = Size(area.width * 0.78f, area.height * 0.035f),
                cornerRadius = CornerRadius(area.width * 0.012f)
            )
            val tipX = area.width * 1.08f
            val tip = Path().apply {
                moveTo(tipX, area.height * 0.415f)
                lineTo(tipX + area.width * 0.07f, area.height * 0.455f)
                lineTo(tipX, area.height * 0.495f)
                close()
            }
            drawPath(tip, color = palette.metalBright.copy(alpha = alpha))
        }
        drawImpactFlash(area, pose.impactFlash)
        // Dust at the hooves while galloping.
        if (pose.lunge > 0.01f || pose.leap > 0.01f) {
            drawCircle(
                color = CrownfallPalette.StoneLight.copy(alpha = alpha * 0.35f * (pose.lunge + pose.leap)),
                radius = area.width * 0.16f,
                center = Offset(area.width * 0.30f, area.height * 0.94f)
            )
        }
    }

    private fun drawFootSoldier(
        scope: DrawScope, palette: ArmorPalette, area: Size, idle: IdleTransform, pose: PiecePose, alpha: Float
    ) = with(scope) {
        drawLegs(palette, area, spread = 0.24f, alpha = alpha)
        drawTorso(palette, area, width = 0.30f, alpha = alpha)
        drawHead(palette, area, alpha, helmet = true, plume = false)
        drawRoundShield(palette, area, alpha, pose.shieldForward)
        drawSpear(palette, area, alpha, raise = pose.armRaise)
        // Small team-coloured tabard.
        drawRoundRect(
            color = palette.accent.copy(alpha = alpha * 0.85f),
            topLeft = Offset(area.width * 0.42f, area.height * 0.50f),
            size = Size(area.width * 0.16f, area.height * 0.16f),
            cornerRadius = CornerRadius(area.width * 0.02f)
        )
        drawImpactFlash(area, pose.impactFlash)
    }
}
