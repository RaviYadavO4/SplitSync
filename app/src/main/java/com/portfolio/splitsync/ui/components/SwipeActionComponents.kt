package com.portfolio.splitsync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SwipeToSignOutSlider(
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thumbSize = 48.dp
    val horizontalPadding = 4.dp

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isCompleted by remember { mutableStateOf(false) }

    val thumbSizePx = with(density) { thumbSize.toPx() }
    val paddingPx = with(density) { horizontalPadding.toPx() }
    val maxDragPx = (trackWidthPx - thumbSizePx - (paddingPx * 2)).coerceAtLeast(1f)

    // Smooth spring animation jab user beech mein chhod de
    val animatedOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "sliderThumbOffset"
    )

    val swipeProgress = (animatedOffset / maxDragPx).coerceIn(0f, 1f)
    val coralColor = com.portfolio.splitsync.ui.theme.WarmCoral

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(coralColor.copy(alpha = 0.14f + (0.20f * swipeProgress)))
            .border(
                width = 1.dp,
                color = coralColor.copy(alpha = 0.45f),
                shape = CircleShape
            )
            .onSizeChanged { size ->
                trackWidthPx = size.width.toFloat()
            }
            .padding(horizontalPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        // Center Hint Text (Jaise-jaise thumb aage badhega, text fade hoga)
        Text(
            text = if (isCompleted) "Signing Out..." else "Swipe to Switch / Sign Out  >>>",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = coralColor.copy(alpha = (1f - swipeProgress * 1.3f).coerceIn(0.15f, 1f)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 28.dp)
        )

        // Draggable Circular Thumb
        Box(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        x = animatedOffset.toInt(),
                        y = 0
                    )
                }
                .size(thumbSize)
                .clip(CircleShape)
                .background(coralColor)
                .draggable(
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                    enabled = !isCompleted,
                    state = androidx.compose.foundation.gestures.rememberDraggableState { delta ->
                        dragOffsetPx = (dragOffsetPx + delta).coerceIn(0f, maxDragPx)
                    },
                    onDragStopped = {
                        if (dragOffsetPx >= maxDragPx * 0.82f) {
                            dragOffsetPx = maxDragPx
                            isCompleted = true
                            onSwipeComplete()
                        } else {
                            // 82% se kam drag karne par wapas bounce back karega
                            dragOffsetPx = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = "Swipe to Sign Out",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}