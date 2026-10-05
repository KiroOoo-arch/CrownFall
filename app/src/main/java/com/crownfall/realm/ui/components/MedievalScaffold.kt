package com.crownfall.realm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import com.crownfall.realm.ui.theme.CrownfallPalette

/**
 * The shared stone-and-torch backdrop.
 *
 * It is intentionally *static*: a full-screen animated gradient would force a
 * software rasteriser to repaint the whole window every frame and drain the
 * battery for no gameplay value. Ambient life comes from the board and its
 * pieces instead. The torch glow is baked into one draw pass and cached by the
 * draw scope, so the menu sits at effectively zero cost.
 */
@Composable
fun MedievalBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(CrownfallPalette.Night)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CrownfallPalette.Void,
                        CrownfallPalette.StoneDeep,
                        CrownfallPalette.Night
                    )
                )
            )
            // Torches in the upper corners.
            drawTorchGlow(Offset(size.width * 0.08f, size.height * 0.10f), 1f)
            drawTorchGlow(Offset(size.width * 0.92f, size.height * 0.10f), 0.9f)
            // A hint of a great hall floor.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, CrownfallPalette.WoodDark.copy(alpha = 0.35f)),
                    startY = size.height * 0.6f
                )
            )
        }
        content()
    }
}

private fun DrawScope.drawTorchGlow(center: Offset, intensity: Float) {
    val radius = size.minDimension * 0.30f * intensity
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                CrownfallPalette.GoldBright.copy(alpha = 0.22f * intensity),
                CrownfallPalette.Royal.copy(alpha = 0.06f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/** A carved stone panel with an iron border. */
@Composable
fun StonePanel(
    modifier: Modifier = Modifier,
    borderColor: Color = CrownfallPalette.IronDark,
    borderWidth: Dp = 2.dp,
    fill: Brush = Brush.verticalGradient(
        listOf(CrownfallPalette.StoneDark, CrownfallPalette.StoneDeep)
    ),
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding)
            // Scroll the padded content only, so the stone fill and border stay put.
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        content = content
    )
}

/** A royal banner title with a gold rule. */
@Composable
fun BannerTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = CrownfallPalette.GoldBright,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = CrownfallPalette.ParchmentDim,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(0.6f)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, CrownfallPalette.Gold, Color.Transparent)
                    )
                )
                .padding(vertical = 1.dp)
        )
    }
}

/** A wide, metal-framed menu button. */
@Composable
fun MedievalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = CrownfallPalette.Gold,
    supportingText: String? = null,
    /** Tighter padding for side rails where vertical space is scarce. */
    compact: Boolean = false
) {
    val fill = if (enabled) {
        Brush.verticalGradient(
            listOf(CrownfallPalette.Stone, CrownfallPalette.StoneDeep)
        )
    } else {
        Brush.verticalGradient(listOf(CrownfallPalette.StoneDeep, CrownfallPalette.Void))
    }
    val textColor = if (enabled) CrownfallPalette.Parchment else CrownfallPalette.ParchmentDim.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .widthIn(min = 96.dp, max = 420.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(fill)
            .border(2.dp, if (enabled) accent.copy(alpha = 0.75f) else CrownfallPalette.IronDark, RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (compact) 14.dp else 22.dp,
                vertical = if (compact) 7.dp else 13.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = CrownfallPalette.ParchmentDim
            )
        }
    }
}

/** A compact chip used for HUD facts such as difficulty or move count. */
@Composable
fun StatChip(label: String, value: String, accent: Color = CrownfallPalette.Gold) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CrownfallPalette.StoneDeep.copy(alpha = 0.85f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = CrownfallPalette.ParchmentDim)
        Text(value, style = MaterialTheme.typography.labelMedium, color = accent)
    }
}

/** Row of small bars showing achievement progress. */
@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = CrownfallPalette.Gold
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CrownfallPalette.Void)
            .border(1.dp, CrownfallPalette.IronDark, RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.6f), accent)))
        ) { Box(Modifier.fillMaxSize()) }
    }
}

/** Small decorative divider: a gold rule broken by a royal gem. */
@Composable
fun SwordDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Rule()
        Box(
            Modifier
                .padding(horizontal = 8.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CrownfallPalette.Gold)
        )
        Rule()
    }
}

@Composable
private fun Rule() {
    Box(
        Modifier
            .widthIn(max = 96.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, CrownfallPalette.GoldDim, Color.Transparent)
                )
            )
    )
}

/** Rectangular dust helper so callers can reuse the same noise overlay. */
@Composable
fun StoneNoiseOverlay(modifier: Modifier = Modifier, alpha: Float = 0.05f) {
    Canvas(modifier.fillMaxSize()) {
        val step = size.minDimension / 28f
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                val shade = ((x / step).toInt() + (y / step).toInt()) % 3
                if (shade == 0) {
                    drawRect(
                        color = Color.White.copy(alpha = alpha),
                        topLeft = Offset(x, y),
                        size = Size(step * 0.8f, step * 0.8f)
                    )
                }
                x += step
            }
            y += step
        }
    }
}
