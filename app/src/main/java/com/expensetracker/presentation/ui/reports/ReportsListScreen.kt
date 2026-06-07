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
fun ReportsListScreen(
    reports: List<Report>,
    transactions: List<Transaction>,
    currentReportId: Long?,
    sortMode: ReportSortMode,
    onSelectReport: (Long) -> Unit,
    onOpenReport: (Long) -> Unit,
    onAddTransaction: () -> Unit,
    onSetSortMode: (ReportSortMode) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit
) {
    val colors = MaterialTheme.appColors
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Reports", fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.primaryText
                ),
                actions = {
                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, "Sort", tint = colors.secondaryText)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            ReportSortMode.values().forEach { mode ->
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
                    // New transaction button
                    IconButton(onClick = onAddTransaction) {
                        Icon(Icons.Default.Add, "New Transaction", tint = colors.accentBlue)
                    }
                }
            )
        },
        containerColor = colors.background
    ) { padding ->
        if (reports.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Default.Description, "No Reports", "Tap + to create your first report")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    val reportTxs = transactions.filter { it.reportId == report.id }
                    val summary = AnalysisUtil.computeReportSummary(reportTxs)
                    val isActive = report.id == currentReportId
                    val isFirst = reports.first().id == report.id
                    val isLast = reports.last().id == report.id

                    ReportCard(
                        report = report,
                        summary = summary,
                        isActive = isActive,
                        isCustomSort = sortMode == ReportSortMode.CUSTOM,
                        canMoveUp = !isFirst,
                        canMoveDown = !isLast,
                        onClick = {
                            onSelectReport(report.id)
                            onOpenReport(report.id)
                        },
                        onMoveUp = { onMoveUp(report.id) },
                        onMoveDown = { onMoveDown(report.id) }
                    )
                }
                item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: Report,
    summary: ReportSummary,
    isActive: Boolean,
    isCustomSort: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val bgColor = if (isActive) colors.activeReport else colors.surface
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .border(1.dp, if (isActive) colors.accentBlue.copy(0.4f) else colors.border, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                report.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "+${formatCurrency(summary.credit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accentGreen
                )
                Text(
                    "-${formatCurrency(summary.debit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accentRed
                )
                Text(
                    "${summary.count} txn",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }
        }
        if (isCustomSort) {
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ArrowUpward, "Move up", modifier = Modifier.size(16.dp), tint = if (canMoveUp) colors.secondaryText else colors.mutedText)
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ArrowDownward, "Move down", modifier = Modifier.size(16.dp), tint = if (canMoveDown) colors.secondaryText else colors.mutedText)
                }
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = colors.mutedText)
    }
}

private fun ReportSortMode.label() = when (this) {
    ReportSortMode.NAME -> "By Name"
    ReportSortMode.RECENT -> "Recently Updated"
    ReportSortMode.AMOUNT -> "By Amount"
    ReportSortMode.CUSTOM -> "Custom Order"
}
