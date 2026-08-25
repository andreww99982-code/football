package com.rmatch.football.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rmatch.football.ui.theme.RMatchOutline
import com.rmatch.football.ui.theme.RMatchSurface
import com.rmatch.football.ui.theme.RMatchSurfaceVariant

/** Animated placeholder shown while provider data is being loaded. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    val start = 900f * progress - 300f
    val brush = Brush.linearGradient(
        colors = listOf(RMatchSurface, RMatchSurfaceVariant, RMatchOutline, RMatchSurface),
        start = Offset(start, 0f),
        end = Offset(start + 300f, 300f)
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(brush)
            .semantics { contentDescription = "Загрузка данных" }
    ) {}
}

@Composable
fun ShimmerList(
    itemCount: Int = 6,
    itemHeight: Int = 84,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(itemCount) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(itemHeight.dp)
            )
        }
    }
}
