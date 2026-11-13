package com.portfolio.splitsync.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portfolio.splitsync.data.local.entity.GroupEntity
import com.portfolio.splitsync.domain.model.SyncStatus
import com.portfolio.splitsync.ui.components.LiveSyncStatusPill
import com.portfolio.splitsync.ui.components.NetBalanceHeroCard
import com.portfolio.splitsync.ui.components.SwipeToSignOutSlider
import com.portfolio.splitsync.ui.theme.AmberWarning
import com.portfolio.splitsync.ui.theme.ElectricEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGroupClick: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateGroupSheet by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var inviteCodeInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.joinMessage) {
        state.joinMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearJoinMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable { showProfileSheet = true }
                    ) {
                        // Circular User Initial Avatar
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ElectricEmerald.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.currentUserName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricEmerald
                            )
                        }
                        Column {
                            Text(
                                text = "SplitSync",
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Hi, ${state.currentUserName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricEmerald
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showJoinDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode,
                            contentDescription = "Join Group via Code",
                            tint = ElectricEmerald
                        )
                    }
                    LiveSyncStatusPill(
                        isOnline = state.isOnline,
                        pendingCount = state.pendingSyncCount,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateGroupSheet = true },
                containerColor = ElectricEmerald,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                icon = { Icon(Icons.Rounded.GroupAdd, contentDescription = null) },
                text = { Text("New Group", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NetBalanceHeroCard(
                    youAreOwed = state.youAreOwed,
                    youOwe = state.youOwe
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Groups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showJoinDialog = true }) {
                        Text("+ Join with Code", color = ElectricEmerald, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (state.groups.isEmpty()) {
                item {
                    EmptyGroupsPlaceholder(onCreateClick = { showCreateGroupSheet = true })
                }
            } else {
                items(state.groups, key = { it.id }) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onGroupClick(group.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // 1. Profile & Logout BottomSheet
        if (showProfileSheet) {
            ProfileAccountBottomSheet(
                userName = state.currentUserName,
                userPhone = state.currentUserPhone,
                onDismiss = { showProfileSheet = false },
                onSignOut = {
                    showProfileSheet = false
                    viewModel.logout(onLogout)
                }
            )
        }

        // 2. Create Group BottomSheet
        if (showCreateGroupSheet) {
            CreateGroupBottomSheet(
                onDismiss = { showCreateGroupSheet = false },
                onCreate = { name, desc, membersMap ->
                    viewModel.createNewGroup(name, desc, membersMap)
                    showCreateGroupSheet = false
                }
            )
        }

        // 3. Join via Invite Code Dialog
        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Join Existing Group", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Enter the 6-digit invite code shared by your friend.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it.uppercase() },
                            label = { Text("Invite Code (e.g. A7K9P2)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inviteCodeInput.isNotBlank()) {
                                viewModel.joinGroupByCode(inviteCodeInput)
                                inviteCodeInput = ""
                                showJoinDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
                    ) {
                        Text("Join Group", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAccountBottomSheet(
    userName: String,
    userPhone: String,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(ElectricEmerald.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElectricEmerald
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (userPhone.length == 10) {
                    Text(
                        text = "+91 $userPhone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))

            // Fintech Swipe-to-Sign-Out Slider
            SwipeToSignOutSlider(
                onSwipeComplete = onSignOut
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}





@Composable
fun GroupCard(
    group: GroupEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElectricEmerald.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = ElectricEmerald
                    )
                }

                Column {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${group.memberIds.size} members • ${group.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            val isSynced = group.syncStatus == SyncStatus.SYNCED
            Icon(
                imageVector = if (isSynced) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                contentDescription = "Sync state",
                tint = if (isSynced) ElectricEmerald else AmberWarning,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyGroupsPlaceholder(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = ElectricEmerald,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "No Groups Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create a group for trips, flatmates, or dinners to split expenses offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            OutlinedButton(onClick = onCreateClick) {
                Text("Create First Group")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, membersWithPhones: Map<String, String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var memberNameInput by remember { mutableStateOf("") }
    var memberPhoneInput by remember { mutableStateOf("") }

    // Member Name -> Optional 10-digit Phone
    val addedMembersMap = remember { mutableStateMapOf<String, String>() }

    fun addMember() {
        val trimmedName = memberNameInput.trim()
        val cleanPhone = memberPhoneInput.filter { it.isDigit() }.take(10)

        if (trimmedName.isNotBlank() &&
            !trimmedName.equals("You", ignoreCase = true) &&
            addedMembersMap.keys.none { it.equals(trimmedName, ignoreCase = true) }
        ) {
            addedMembersMap[trimmedName] = cleanPhone
            memberNameInput = ""
            memberPhoneInput = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricEmerald.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = ElectricEmerald
                        )
                    }
                    Column {
                        Text(
                            text = "Create New Group",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add friends with optional mobile number for auto-sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name (e.g. Goa Trip)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Add Member Card (Name + Optional Phone)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ADD FRIENDS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        OutlinedTextField(
                            value = memberNameInput,
                            onValueChange = { memberNameInput = it },
                            label = { Text("Friend's Name (e.g. Rahul)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = memberPhoneInput,
                                onValueChange = { input ->
                                    memberPhoneInput = input.filter { it.isDigit() }.take(10)
                                },
                                label = { Text("Mobile (Optional)") },
                                prefix = { Text("+91 ") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            FilledIconButton(
                                onClick = { addMember() },
                                enabled = memberNameInput.isNotBlank(),
                                modifier = Modifier.size(54.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = ElectricEmerald,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Member")
                            }
                        }
                    }
                }
            }

            // Added Members Chips with Phone Indicator
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text("You (Admin)") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ElectricEmerald
                            )
                        }
                    )

                    addedMembersMap.forEach { (memberName, phone) ->
                        val chipLabel = if (phone.length == 10) "$memberName • $phone" else memberName
                        InputChip(
                            selected = true,
                            onClick = { addedMembersMap.remove(memberName) },
                            label = { Text(chipLabel) },
                            trailingIcon = {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(name, description, addedMembersMap.toMap())
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricEmerald,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Create Group (${addedMembersMap.size + 1} Members)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseBottomSheet(
    members: List<String> = listOf("You"),
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        category: String,
        paidBy: String,
        splitType: com.portfolio.splitsync.domain.model.SplitType,
        customSplits: Map<String, Double>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var selectedPayer by remember { mutableStateOf(members.firstOrNull() ?: "You") }
    var isCustomSplit by remember { mutableStateOf(false) }

    // Har member ke custom amount inputs store karne ke liye map
    val customInputs = remember {
        mutableStateMapOf<String, String>().apply {
            members.forEach { put(it, "") }
        }
    }

    val categories = listOf("Food", "Travel", "Stay", "Shopping")
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val equalShare = if (members.isNotEmpty() && parsedAmount > 0) parsedAmount / members.size else 0.0

    // Custom split validation calculations
    val customAmountsMap = members.associateWith { customInputs[it]?.toDoubleOrNull() ?: 0.0 }
    val customSum = customAmountsMap.values.sum()
    val remainingAmount = parsedAmount - customSum
    val isCustomValid = !isCustomSplit || (parsedAmount > 0 && kotlin.math.abs(remainingAmount) < 0.05)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)
        ) {
            item {
                Text(
                    text = "Split New Expense",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What was this for? (e.g. Dinner)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Total Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Paid By Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Paid By",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { member ->
                            FilterChip(
                                selected = selectedPayer == member,
                                onClick = { selectedPayer = member },
                                label = { Text(member) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricEmerald.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricEmerald
                                )
                            )
                        }
                    }
                }
            }

            // Category Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            // Split Mode Toggle (Equal vs Custom)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Split Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !isCustomSplit,
                            onClick = { isCustomSplit = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Split Equally", fontWeight = FontWeight.SemiBold)
                        }
                        SegmentedButton(
                            selected = isCustomSplit,
                            onClick = { isCustomSplit = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Custom Amounts", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Live Per-Member Split Preview / Custom Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.65f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE SPLIT PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (isCustomSplit && parsedAmount > 0) {
                                val remColor = if (isCustomValid) ElectricEmerald else com.portfolio.splitsync.ui.theme.WarmCoral
                                val remText = if (isCustomValid) "Matched ✓" else "Left: ₹${"%.2f".format(remainingAmount)}"
                                Text(
                                    text = remText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = remColor
                                )
                            }
                        }

                        members.forEach { member ->
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
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(ElectricEmerald.copy(alpha = 0.18f)),
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
                                        text = if (member == selectedPayer) "$member (Paid)" else member,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (!isCustomSplit) {
                                    Text(
                                        text = "₹${"%.2f".format(equalShare)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricEmerald
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = customInputs[member] ?: "",
                                        onValueChange = { customInputs[member] = it },
                                        placeholder = { Text("0") },
                                        prefix = { Text("₹") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(115.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
// Save Button
            item {
                Button(
                    onClick = {
                        if (title.isNotBlank() && parsedAmount > 0 && isCustomValid) {
                            val type = if (isCustomSplit) {
                                com.portfolio.splitsync.domain.model.SplitType.EXACT
                            } else {
                                com.portfolio.splitsync.domain.model.SplitType.EQUAL
                            }
                            onSave(title, parsedAmount, selectedCategory, selectedPayer, type, customAmountsMap)
                        }
                    },
                    enabled = title.isNotBlank() && parsedAmount > 0 && isCustomValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricEmerald,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save & Split Expense", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
