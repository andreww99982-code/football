package com.rmatch.football.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rmatch.football.ui.theme.RMatchAccent
import com.rmatch.football.ui.theme.RMatchBackground
import com.rmatch.football.ui.theme.RMatchOnDark
import com.rmatch.football.ui.theme.RMatchOutline

/**
 * Programmatic R-Match brand logo drawn with Canvas.
 *
 * Design:
 *  - Dark background circle
 *  - Thin green outer ring
 *  - Faint pitch center circle and axis lines
 *  - White "R" and green "M" glyphs in the center
 *  - Green accent dot below the letters
 */
@Composable
fun RMatchLogo(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val stroke = r * 0.04f

        // Background circle
        drawCircle(color = RMatchBackground, radius = r, center = Offset(cx, cy))

        // Outer green ring
        drawCircle(
            color = RMatchAccent,
            radius = r - stroke,
            center = Offset(cx, cy),
            style = Stroke(width = stroke)
        )

        // Faint pitch lines
        val pitchAlpha = 0.3f
        val pitchColor = RMatchAccent.copy(alpha = pitchAlpha)
        val pitchStroke = stroke * 0.6f

        // Horizontal axis
        drawLine(
            color = pitchColor,
            start = Offset(cx - r * 0.7f, cy),
            end = Offset(cx + r * 0.7f, cy),
            strokeWidth = pitchStroke
        )
        // Vertical axis
        drawLine(
            color = pitchColor,
            start = Offset(cx, cy - r * 0.7f),
            end = Offset(cx, cy + r * 0.7f),
            strokeWidth = pitchStroke
        )
        // Center circle
        drawCircle(
            color = pitchColor,
            radius = r * 0.28f,
            center = Offset(cx, cy),
            style = Stroke(width = pitchStroke)
        )

        // Letters using native canvas for text
        drawLetters(cx, cy, r, this)

        // Accent dot
        drawCircle(
            color = RMatchAccent,
            radius = r * 0.06f,
            center = Offset(cx, cy + r * 0.52f)
        )
    }
}

private fun drawLetters(cx: Float, cy: Float, r: Float, scope: DrawScope) {
    val letterStroke = r * 0.09f
    val topY = cy - r * 0.35f
    val bottomY = cy + r * 0.35f

    // "R" — drawn as paths: vertical bar + bump + diagonal leg
    val rX = cx - r * 0.3f
    // Vertical bar of R
    scope.drawLine(
        color = RMatchOnDark,
        start = Offset(rX, topY),
        end = Offset(rX, bottomY),
        strokeWidth = letterStroke,
        cap = StrokeCap.Round
    )
    // Top horizontal of R
    scope.drawLine(
        color = RMatchOnDark,
        start = Offset(rX, topY),
        end = Offset(rX + r * 0.22f, topY),
        strokeWidth = letterStroke,
        cap = StrokeCap.Round
    )
    // Mid horizontal of R
    val rMidY = cy - r * 0.03f
    scope.drawLine(
        color = RMatchOnDark,
        start = Offset(rX, rMidY),
        end = Offset(rX + r * 0.22f, rMidY),
        strokeWidth = letterStroke,
        cap = StrokeCap.Round
    )
    // Right vertical of R (bump)
    scope.drawLine(
        color = RMatchOnDark,
        start = Offset(rX + r * 0.22f, topY),
        end = Offset(rX + r * 0.22f, rMidY),
        strokeWidth = letterStroke,
        cap = StrokeCap.Round
    )
    // Diagonal leg of R
    scope.drawLine(
        color = RMatchOnDark,
        start = Offset(rX + r * 0.08f, rMidY),
        end = Offset(rX + r * 0.26f, bottomY),
        strokeWidth = letterStroke,
        cap = StrokeCap.Round
    )

    // "M" in green — left bar, left diagonal, right diagonal, right bar
    val mX = cx + r * 0.05f
    val mWidth = r * 0.32f
    val mMidY = cy + r * 0.1f
    // Left bar
    scope.drawLine(
        color = RMatchAccent,
        start = Offset(mX, topY),
        end = Offset(mX, bottomY),
        strokeWidth = letterStroke,
        cap = StrokeCap.Round
    )
    // Left diagonal (top-left to center)
    scope.drawLine(
        color = RMatchAccent,
        start = Offset(mX, topY),
        end = Offset(mX + mWidth / 2f, mMidY),
        strokeWidth = letterStroke,
        cap = StrokeCap.Round
    )
    // Right diagonal (center to top-right)
    scope.drawLine(
        color = RMatchAccent,
        start = Offset(mX + mWidth / 2f, mMidY),
        end = Offset(mX + mWidth, topY),
        strokeWidth = letterStroke,
        cap = StrokeCap.Round
    )
    // Right bar
    scope.drawLine(
        color = RMatchAccent,
        start = Offset(mX + mWidth, topY),
        end = Offset(mX + mWidth, bottomY),
        strokeWidth = letterStroke,
        cap = StrokeCap.Round
    )
}
