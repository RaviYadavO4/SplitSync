package com.portfolio.splitsync.ui.screens.group

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.ui.components.ExpenseItemCard
import com.portfolio.splitsync.ui.components.GroupAnalyticsBottomSheet
import com.portfolio.splitsync.ui.components.MembersBar
import com.portfolio.splitsync.ui.screens.home.AddExpenseBottomSheet
import com.portfolio.splitsync.ui.theme.ElectricEmerald
import com.portfolio.splitsync.ui.theme.WarmCoral
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailRoute(
    onBackClick: () -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showSettleSheet by remember { mutableStateOf(false) }
    var showAnalyticsSheet by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var selectedConflictExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberPhone by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.recentActivityMessage) {
        state.recentActivityMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (msg.startsWith("Deleted")) "UNDO" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastDelete()
            }
            viewModel.clearActivityMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.groupName, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Code: ${state.inviteCode} • ₹${"%.0f".format(state.totalGroupSpend)}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = ElectricEmerald
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAnalyticsSheet = true }) {
                        Icon(Icons.Rounded.PieChart, contentDescription = "Analytics", tint = ElectricEmerald)
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my group '${state.groupName}' on SplitSync! Use Invite Code: ${state.inviteCode}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Invite Code"))
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share Code", tint = ElectricEmerald)
                    }
                    IconButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Member", tint = ElectricEmerald)
                    }
                    IconButton(onClick = { showDeleteGroupDialog = true }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Group", tint = WarmCoral)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddExpenseSheet = true },
                containerColor = ElectricEmerald,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add Expense", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MembersBar(
                members = state.members,
                currentUser = state.currentUserName,
                onAddMemberClick = { showAddMemberDialog = true }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity (${state.expenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = { showSettleSheet = true },
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ElectricEmerald.copy(alpha = 0.16f),
                        contentColor = ElectricEmerald
                    )
                ) {
                    Icon(Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Settle Up", fontWeight = FontWeight.Bold)
                }
            }

            if (state.expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses yet. Tap 'Add Expense' to split with ${state.members.size} members!",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    items(state.expenses, key = { it.expense.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteExpense(item.expense.id, item.expense.title)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.animateItem(),
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                                if (isSwiping) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(WarmCoral.copy(alpha = 0.22f))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = WarmCoral)
                                    }
                                }
                            }
                        ) {
                            ExpenseItemCard(
                                expenseWithSplits = item,
                                currentUserName = state.currentUserName,
                                onConflictClick = { selectedConflictExpense = item.expense }
                            )
                        }
                    }
                }
            }
        }
        if (showAddExpenseSheet) {
            AddExpenseBottomSheet(
                members = state.members,
                onDismiss = { showAddExpenseSheet = false },
                onSave = { title, amount, category, paidBy, splitType, customSplits ->
                    viewModel.addExpenseToThisGroup(title, amount, category, paidBy, splitType, customSplits)
                    showAddExpenseSheet = false
                }
            )
        }
        if (showSettleSheet) {
            SettleUpBottomSheet(
                memberBalances = state.memberBalances,
                onDismiss = { showSettleSheet = false },
                onRecordSettlement = { memberName, amount, isMemberPayingMe ->
                    viewModel.recordSettlementPayment(memberName, amount, isMemberPayingMe)
                    showSettleSheet = false
                }
            )
        }
        if (showAnalyticsSheet) {
            GroupAnalyticsBottomSheet(
                groupName = state.groupName,
                totalSpend = state.totalGroupSpend,
                categoryTotals = state.categoryTotals,
                onDismiss = { showAnalyticsSheet = false },
                onShareReport = {
                    val reportText = viewModel.generateShareableReportText()
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Expense Report"))
                }
            )
        }
        if (showAddMemberDialog) {
            AlertDialog(
                onDismissRequest = { showAddMemberDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Add Member to ${state.groupName}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Add a member with their mobile number so this group auto-syncs when they log in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        OutlinedTextField(
                            value = newMemberName,
                            onValueChange = { newMemberName = it },
                            label = { Text("Member Name (e.g. Priya)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newMemberPhone,
                            onValueChange = { input -> newMemberPhone = input.filter { it.isDigit() }.take(10) },
                            label = { Text("Mobile Number (Optional)") },
                            prefix = { Text("+91 ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newMemberName.isNotBlank()) {
                                viewModel.addMember(newMemberName, newMemberPhone)
                                newMemberName = ""
                                newMemberPhone = ""
                                showAddMemberDialog = false
                            }
                        },
                        enabled = newMemberName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
                    ) {
                        Text("Add Member", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMemberDialog = false }) { Text("Cancel") }
                }
            )
        }
        if (showDeleteGroupDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteGroupDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = WarmCoral) },
                title = { Text("Delete '${state.groupName}'?", fontWeight = FontWeight.Bold) },
                text = { Text("This group and all its expenses will be removed from your active groups and synced across members.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteGroupDialog = false
                            viewModel.deleteGroup(onGroupDeleted = onBackClick)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                    ) {
                        Text("Delete Group", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteGroupDialog = false }) { Text("Cancel") }
                }
            )
        }
        selectedConflictExpense?.let { conflictItem ->
            AlertDialog(
                onDismissRequest = { selectedConflictExpense = null },
                containerColor = MaterialTheme.colorScheme.surface,
                icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = WarmCoral) },
                title = { Text("Sync Conflict Detected") },
                text = {
                    Text(
                        "Another member edited '${conflictItem.title}' while you were offline. " +
                                "Your local amount is ₹${conflictItem.amount}. Which version do you want to keep?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resolveConflict(conflictItem, keepLocal = true)
                            selectedConflictExpense = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
                    ) {
                        Text("Keep My Edit (₹${conflictItem.amount})")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.resolveConflict(conflictItem, keepLocal = false)
                            selectedConflictExpense = null
                        }
                    ) {
                        Text("Accept Cloud Version")
                    }
                }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpBottomSheet(
    memberBalances: List<MemberBalance>,
    onDismiss: () -> Unit,
    onRecordSettlement: (memberName: String, amount: Double, isMemberPayingMe: Boolean) -> Unit
) {
    val context = LocalContext.current
    var activeSettleMember by remember { mutableStateOf<MemberBalance?>(null) }
    var customSettleAmount by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settle Balances",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            activeSettleMember?.let { target ->
                val maxPending = abs(target.netAmount)
                val isPayingMe = target.netAmount > 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ElectricEmerald),
                    colors = CardDefaults.cardColors(containerColor = ElectricEmerald.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isPayingMe) "${target.memberName} is paying you"
                            else "You are paying ${target.memberName}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ElectricEmerald
                        )
                        OutlinedTextField(
                            value = customSettleAmount,
                            onValueChange = { customSettleAmount = it },
                            label = { Text("Amount (Max ₹${"%.2f".format(maxPending)})") },
                            prefix = { Text("₹") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { activeSettleMember = null }) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amt = customSettleAmount.toDoubleOrNull() ?: 0.0
                                    if (amt > 0.0 && amt <= maxPending + 0.01) {
                                        onRecordSettlement(target.memberName, amt, isPayingMe)
                                        activeSettleMember = null
                                        onDismiss()
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
                            ) {
                                Text("Confirm Payment", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (memberBalances.isEmpty()) {
                Text(
                    text = "Add other members to this group to track and settle balances.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                memberBalances.forEach { balance ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = balance.memberName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val statusText = when {
                                    balance.netAmount > 0.01 -> "Owes you ₹${"%.2f".format(balance.netAmount)}"
                                    balance.netAmount < -0.01 -> "You owe ₹${"%.2f".format(-balance.netAmount)}"
                                    else -> "All settled up"
                                }
                                val statusColor = when {
                                    balance.netAmount > 0.01 -> ElectricEmerald
                                    balance.netAmount < -0.01 -> WarmCoral
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (balance.netAmount > 0.01 && balance.phone.length == 10) {
                                    IconButton(
                                        onClick = {
                                            val msg = "Hi ${balance.memberName}, reminder from SplitSync: ₹${"%.2f".format(balance.netAmount)} is pending."
                                            val url = "wa.me{balance.phone}?text=${Uri.encode(msg)}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Send, contentDescription = "Remind", tint = ElectricEmerald)
                                    }
                                }
                                if (abs(balance.netAmount) > 0.01) {
                                    Button(
                                        onClick = {
                                            activeSettleMember = balance
                                            customSettleAmount = "%.2f".format(abs(balance.netAmount))
                                        },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricEmerald,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text("Settle", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Settled", tint = ElectricEmerald)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}