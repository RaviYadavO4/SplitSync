package com.portfolio.splitsync.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portfolio.splitsync.ui.theme.ElectricEmerald
import com.portfolio.splitsync.ui.theme.WarmCoral

@Composable
fun NetBalanceHeroCard(
    youAreOwed: Double,
    youOwe: Double,
    modifier: Modifier = Modifier
) {
    val netBalance = youAreOwed - youOwe

    val animatedNet by animateFloatAsState(
        targetValue = netBalance.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "netBalanceRoll"
    )
    val animatedOwed by animateFloatAsState(
        targetValue = youAreOwed.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "owedRoll"
    )
    val animatedOwe by animateFloatAsState(
        targetValue = youOwe.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "oweRoll"
    )

    val balanceColor by animateColorAsState(
        targetValue = if (netBalance >= 0) ElectricEmerald else WarmCoral,
        label = "balanceColor"
    )

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E293B),
            Color(0xFF0F172A),
            Color(0xFF064E3B)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradientBrush)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "TOTAL NET BALANCE",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            Text(
                text = if (animatedNet >= 0f) "+₹${"%.2f".format(animatedNet)}"
                else "-₹${"%.2f".format(-animatedNet)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BalanceStatItem(
                    label = "You are owed",
                    amount = animatedOwed.toDouble(),
                    color = ElectricEmerald,
                    isPositive = true
                )
                BalanceStatItem(
                    label = "You owe",
                    amount = animatedOwe.toDouble(),
                    color = WarmCoral,
                    isPositive = false
                )
            }
        }
    }
}

@Composable
private fun BalanceStatItem(
    label: String,
    amount: Double,
    color: Color,
    isPositive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = "₹${"%.2f".format(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
