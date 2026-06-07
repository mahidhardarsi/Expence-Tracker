package com.expensetracker.presentation.ui.reports

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.ui.components.*
import com.expensetracker.presentation.ui.theme.appColors
import com.expensetracker.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    report: Report?,
    transactions: List<Transaction>,
    sortMode: TransactionSortMode,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onRenameReport: (String) -> Unit,
    onDeleteReport: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onSetSortMode: (TransactionSortMode) -> Unit,
    isAiAvailable: Boolean = false,
    onOpenAiChat: () -> Unit = {}
) {
    val colors = MaterialTheme.appColors
    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingTxId by remember { mutableStateOf<Long?>(null) }

    val sortedTxs = remember(transactions, sortMode) {
        AnalysisUtil.sortTransactions(transactions, sortMode)
    }
    val summary = remember(transactions) { AnalysisUtil.computeReportSummary(transactions) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = colors.secondaryText)
                    }
                },
                windowInsets = WindowInsets.statusBars,
                title = {
                    Text(
                        report?.name ?: "Report",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.primaryText
                ),
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, "Sort", tint = colors.secondaryText)
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            TransactionSortMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label()) },
                                    onClick = { onSetSortMode(mode); showSortMenu = false },
                                    leadingIcon = if (sortMode == mode) {
                                        { Icon(Icons.Default.Check, null, tint = colors.accentBlue) }
                                    } else null
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More", tint = colors.secondaryText)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = { showRenameDialog = true; showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export JSON") },
                                onClick = { onExportJson(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Download, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = { onExportCsv(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.TableView, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete Report", color = colors.accentRed) },
                                onClick = { showDeleteDialog = true; showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = colors.accentRed) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isAiAvailable) {
                    FloatingActionButton(
                        onClick = onOpenAiChat,
                        containerColor = colors.surface,
                        contentColor = colors.accentBlue
                    ) {
                        Icon(Icons.Default.Psychology, "Open AI Chat")
                    }
                }
                FloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = colors.accentBlue,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ) {
                    Icon(Icons.Default.Add, "Add Transaction")
                }
            }
        },
        containerColor = colors.background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary card
            item {
                ReportSummaryCard(summary)
            }
            if (sortedTxs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            Icons.Default.Receipt, "No Transactions",
                            "Tap + to add your first transaction"
                        )
                    }
                }
            } else {
                items(sortedTxs, key = { it.id }) { tx ->
                    TransactionCard(
                        transaction = tx,
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { deletingTxId = tx.id }
                    )
                }
            }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }

    // Dialogs
    if (showRenameDialog) {
        RenameDialog(
            currentName = report?.name ?: "",
            onConfirm = { onRenameReport(it); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }
    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete Report",
            message = "This will permanently delete \"${report?.name}\" and all its transactions.",
            onConfirm = { onDeleteReport(); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }
    deletingTxId?.let { txId ->
        ConfirmDialog(
            title = "Delete Transaction",
            message = "Delete this transaction permanently?",
            onConfirm = { onDeleteTransaction(txId); deletingTxId = null },
            onDismiss = { deletingTxId = null }
        )
    }
}

@Composable
private fun ReportSummaryCard(summary: ReportSummary) {
    val colors = MaterialTheme.appColors
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.secondaryText)
            Spacer(Modifier.height(10.dp))
            SummaryRow("Income", summary.credit, TransactionType.CREDIT)
            SummaryRow("Expense", summary.debit, TransactionType.DEBIT)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = colors.border)
            SummaryRow("Net", summary.net)
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val bgColor = if (transaction.type == TransactionType.CREDIT) colors.creditTint else colors.debitTint
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .clickable(onClick = onEdit)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type indicator
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                transaction.category.take(1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.CREDIT) colors.accentGreen else colors.accentRed
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(transaction.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(isoDateToDisplay(transaction.date), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                Text("·", color = colors.mutedText)
                Text(transaction.category, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
            }
        }
        AmountText(transaction.amount, transaction.type, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp), tint = colors.mutedText)
        }
    }
}

@Composable
private fun RenameDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Report") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Report name") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun TransactionSortMode.label() = when (this) {
    TransactionSortMode.RECENT -> "Most Recent"
    TransactionSortMode.OLDEST -> "Oldest First"
    TransactionSortMode.AMOUNT -> "By Amount"
    TransactionSortMode.NAME -> "By Name"
}
