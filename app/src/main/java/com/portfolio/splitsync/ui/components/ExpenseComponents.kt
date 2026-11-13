package com.portfolio.splitsync.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.splitsync.data.local.dao.ExpenseWithSplits
import com.portfolio.splitsync.domain.model.SyncStatus
import com.portfolio.splitsync.ui.theme.AmberWarning
import com.portfolio.splitsync.ui.theme.ElectricEmerald
import com.portfolio.splitsync.ui.theme.WarmCoral

@Composable
fun MembersBar(
    members: List<String>,
    currentUser: String,
    onAddMemberClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(members) { member ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(ElectricEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricEmerald
                        )
                    }
                    Text(
                        text = if (member == currentUser) "$member (You)" else member,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            AssistChip(
                onClick = onAddMemberClick,
                shape = CircleShape,
                label = { Text("+ Add") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = ElectricEmerald.copy(alpha = 0.12f),
                    labelColor = ElectricEmerald
                ),
                border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f))
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseItemCard(
    expenseWithSplits: ExpenseWithSplits,
    currentUserName: String,
    onConflictClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expense = expenseWithSplits.expense
    val splits = expenseWithSplits.splits
    val isConflict = expense.syncStatus == SyncStatus.CONFLICT
    val isSettlement = expense.category == "Settlement"
    var expanded by remember { mutableStateOf(!isSettlement) }

    val userSummaryText: String
    val userSummaryColor: Color

    if (isSettlement) {
        val receiverName = splits.firstOrNull { it.userId != expense.paidByUserId }?.userId ?: ""
        userSummaryText = when {
            expense.paidByUserId == currentUserName -> "You paid $receiverName ₹${"%.2f".format(expense.amount)}"
            receiverName == currentUserName -> "${expense.paidByUserId} paid you ₹${"%.2f".format(expense.amount)}"
            else -> "Paid ₹${"%.2f".format(expense.amount)}"
        }
        userSummaryColor = ElectricEmerald
    } else if (expense.paidByUserId == currentUserName) {
        val lentAmount = splits.filter { it.userId != currentUserName && !it.isSettled }.sumOf { it.amountOwed }
        if (lentAmount > 0.01) {
            userSummaryText = "You lent ₹${"%.2f".format(lentAmount)}"
            userSummaryColor = ElectricEmerald
        } else {
            userSummaryText = "All settled"
            userSummaryColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        }
    } else {
        val mySplit = splits.find { it.userId == currentUserName }
        if (mySplit != null && !mySplit.isSettled && mySplit.amountOwed > 0.01) {
            userSummaryText = "You owe ${expense.paidByUserId} ₹${"%.2f".format(mySplit.amountOwed)}"
            userSummaryColor = WarmCoral
        } else {
            userSummaryText = "Not involved / Settled"
            userSummaryColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        }
    }

    val cardBorder = when {
        isConflict -> BorderStroke(1.5.dp, WarmCoral)
        isSettlement -> BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.45f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    }

    val cardContainerColor = if (isSettlement) {
        ElectricEmerald.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = {
            if (isConflict) onConflictClick() else if (!isSettlement) expanded = !expanded
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isConflict -> WarmCoral.copy(alpha = 0.15f)
                                    isSettlement -> ElectricEmerald.copy(alpha = 0.22f)
                                    else -> ElectricEmerald.copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isConflict -> Icons.Rounded.WarningAmber
                                isSettlement -> Icons.Rounded.Payments
                                else -> Icons.Rounded.ReceiptLong
                            },
                            contentDescription = null,
                            tint = if (isConflict) WarmCoral else ElectricEmerald
                        )
                    }

                    Column {
                        if (isSettlement) {
                            Surface(
                                shape = CircleShape,
                                color = ElectricEmerald.copy(alpha = 0.18f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "SETTLEMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = ElectricEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = expense.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = when {
                                isConflict -> "Tap to resolve sync conflict"
                                isSettlement -> "Recorded payment • Offline-ready"
                                else -> "${expense.paidByUserId} paid ₹${"%.2f".format(expense.amount)} • ${expense.category}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConflict) WarmCoral else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = userSummaryText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = userSummaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val (statusIcon, statusTint) = when (expense.syncStatus) {
                            SyncStatus.SYNCED -> Icons.Rounded.CheckCircle to ElectricEmerald
                            SyncStatus.CONFLICT -> Icons.Rounded.WarningAmber to WarmCoral
                            else -> Icons.Rounded.Schedule to AmberWarning
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(14.dp)
                        )
                        if (!isSettlement) {
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = "Toggle breakdown",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
// Expandable Per-Person Split Breakdown with Progress Bar & Installment Pills
            AnimatedVisibility(
                visible = !isSettlement && expanded && splits.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SPLIT BREAKDOWN (${splits.size} PEOPLE)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    splits.forEach { split ->
                        val isPayer = split.userId == expense.paidByUserId
                        val totalShare = split.amountOwed + split.paidAmount
                        val paidRatio = if (totalShare > 0.0) (split.paidAmount / totalShare).toFloat().coerceIn(0f, 1f) else 0f
                        val installments = split.paymentHistory.split("|").filter { it.isNotBlank() }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(ElectricEmerald.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = split.userId.take(1).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricEmerald
                                        )
                                    }
                                    Text(
                                        text = if (split.userId == currentUserName) "${split.userId} (You)" else split.userId,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                val detailText = when {
                                    isPayer -> "Paid ₹${"%.2f".format(expense.amount)} (Share: ₹${"%.2f".format(totalShare)})"
                                    split.isSettled -> "Settled (₹${"%.2f".format(totalShare)}) ✓"
                                    split.paidAmount > 0.01 -> "₹${"%.2f".format(split.amountOwed)} left of ₹${"%.2f".format(totalShare)}"
                                    else -> "Owes ${expense.paidByUserId} ₹${"%.2f".format(split.amountOwed)}"
                                }
                                val detailColor = when {
                                    isPayer || split.isSettled -> ElectricEmerald
                                    split.paidAmount > 0.01 -> AmberWarning
                                    else -> WarmCoral
                                }
                                Text(
                                    text = detailText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = detailColor
                                )
                            }
                            if (!isPayer && (split.paidAmount > 0.01 || installments.isNotEmpty())) {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = if (split.isSettled) 1f else paidRatio,
                                    label = "splitProgress"
                                )
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape),
                                    color = if (split.isSettled) ElectricEmerald else AmberWarning,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    installments.forEachIndexed { index, entry ->
                                        Surface(
                                            shape = CircleShape,
                                            color = ElectricEmerald.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "✓ #${index + 1}: $entry",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricEmerald,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}