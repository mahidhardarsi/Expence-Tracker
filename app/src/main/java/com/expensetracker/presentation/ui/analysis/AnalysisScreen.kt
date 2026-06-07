package com.expensetracker.presentation.ui.analysis

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.ui.components.*
import com.expensetracker.presentation.ui.theme.appColors
import com.expensetracker.util.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    reports: List<Report>,
    transactions: List<Transaction>,
    filter: AnalysisFilter,
    onFilterChange: (AnalysisFilter) -> Unit,
    isAiAvailable: Boolean = false,
    onOpenAiChat: () -> Unit = {}
) {
    val colors = MaterialTheme.appColors

    val filtered = remember(transactions, filter) {
        AnalysisUtil.filterTransactions(transactions, filter)
    }
    val summary = remember(filtered) { AnalysisUtil.computeSummary(filtered) }
    val monthlyBuckets = remember(filtered) { AnalysisUtil.computeMonthlyBuckets(filtered) }
    val expenseBreakdown = remember(filtered) { AnalysisUtil.computeCategoryBreakdown(filtered, TransactionType.DEBIT) }
    val topExpenseCategories = remember(filtered) { AnalysisUtil.computeTopCategories(filtered, TransactionType.DEBIT) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Analysis", fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface, titleContentColor = colors.primaryText),
                actions = {
                    if (isAiAvailable) {
                        IconButton(onClick = onOpenAiChat) {
                            Icon(Icons.Default.Psychology, "AI Chat", tint = colors.secondaryText)
                        }
                    }
                }
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter card
            FilterCard(reports, filter, onFilterChange)

            // Summary card
            SummaryCard(summary)

            // Monthly bar chart
            if (monthlyBuckets.isNotEmpty()) {
                MonthlyChartCard(monthlyBuckets)
            }

            // Category breakdown
            if (expenseBreakdown.isNotEmpty()) {
                CategoryBreakdownCard(expenseBreakdown, topExpenseCategories)
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Default.BarChart, "No Data", "Adjust your filters or add some transactions")
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterCard(reports: List<Report>, filter: AnalysisFilter, onFilterChange: (AnalysisFilter) -> Unit) {
    val colors = MaterialTheme.appColors
    var showReportMenu by remember { mutableStateOf(false) }
    var showPeriodMenu by remember { mutableStateOf(false) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.secondaryText)
            Spacer(Modifier.height(10.dp))

            // Report selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Report:", style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText, modifier = Modifier.width(70.dp))
                Box {
                    TextButton(onClick = { showReportMenu = true }) {
                        val label = when {
                            filter.reportIdIsAll -> "All Reports"
                            filter.reportId != null -> reports.find { it.id == filter.reportId }?.name ?: "Unknown"
                            else -> "Select"
                        }
                        Text(label, color = colors.accentBlue)
                        Icon(Icons.Default.ArrowDropDown, null, tint = colors.accentBlue)
                    }
                    DropdownMenu(expanded = showReportMenu, onDismissRequest = { showReportMenu = false }) {
                        DropdownMenuItem(text = { Text("All Reports") }, onClick = { onFilterChange(filter.copy(reportIdIsAll = true, reportId = null)); showReportMenu = false })
                        reports.forEach { report ->
                            DropdownMenuItem(text = { Text(report.name) }, onClick = { onFilterChange(filter.copy(reportIdIsAll = false, reportId = report.id)); showReportMenu = false })
                        }
                    }
                }
            }

            // Period
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Period:", style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText, modifier = Modifier.width(70.dp))
                Box {
                    TextButton(onClick = { showPeriodMenu = true }) {
                        Text(filter.period.label(), color = colors.accentBlue)
                        Icon(Icons.Default.ArrowDropDown, null, tint = colors.accentBlue)
                    }
                    DropdownMenu(expanded = showPeriodMenu, onDismissRequest = { showPeriodMenu = false }) {
                        AnalysisPeriod.values().forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.label()) },
                                onClick = {
                                    onFilterChange(
                                        filter.copy(
                                            period = period,
                                            scope = if (period == AnalysisPeriod.FULL) AnalysisScope.REPORT else AnalysisScope.REPORT_PERIOD
                                        )
                                    )
                                    showPeriodMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Month/Year for monthly period
            if (filter.period == AnalysisPeriod.MONTHLY) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Month:", style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
                    var showMonthPicker by remember { mutableStateOf(false) }
                    TextButton(onClick = { showMonthPicker = true }) {
                        Text(monthLabel(filter.year, filter.month), color = colors.accentBlue)
                    }
                    if (showMonthPicker) {
                        MonthYearPickerDialog(
                            year = filter.year, month = filter.month,
                            onConfirm = { y, m -> onFilterChange(filter.copy(year = y, month = m)); showMonthPicker = false },
                            onDismiss = { showMonthPicker = false }
                        )
                    }
                }
            }

            if (filter.period == AnalysisPeriod.ANNUALLY) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Year:", style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
                    var showYearPicker by remember { mutableStateOf(false) }
                    TextButton(onClick = { showYearPicker = true }) {
                        Text(filter.year.toString(), color = colors.accentBlue)
                    }
                    if (showYearPicker) {
                        YearPickerDialog(
                            year = filter.year,
                            onConfirm = { year -> onFilterChange(filter.copy(year = year)); showYearPicker = false },
                            onDismiss = { showYearPicker = false }
                        )
                    }
                }
            }

            if (filter.period == AnalysisPeriod.CUSTOM) {
                Spacer(Modifier.height(8.dp))
                InputField(
                    value = filter.fromDate.ifBlank { "Select start date" }.let { if (it == "Select start date") it else isoDateToDisplay(it) },
                    onValueChange = {},
                    label = "From",
                    readOnly = true,
                    onClick = { showFromPicker = true },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = colors.secondaryText) }
                )
                Spacer(Modifier.height(8.dp))
                InputField(
                    value = filter.toDate.ifBlank { "Select end date" }.let { if (it == "Select end date") it else isoDateToDisplay(it) },
                    onValueChange = {},
                    label = "To",
                    readOnly = true,
                    onClick = { showToPicker = true },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = colors.secondaryText) }
                )

                if (showFromPicker) {
                    IsoDatePickerDialog(
                        initialDate = filter.fromDate.ifBlank { todayIsoDate() },
                        onConfirm = { onFilterChange(filter.copy(fromDate = it)); showFromPicker = false },
                        onDismiss = { showFromPicker = false }
                    )
                }
                if (showToPicker) {
                    IsoDatePickerDialog(
                        initialDate = filter.toDate.ifBlank { todayIsoDate() },
                        onConfirm = { onFilterChange(filter.copy(toDate = it)); showToPicker = false },
                        onDismiss = { showToPicker = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: AnalysisSummary) {
    val colors = MaterialTheme.appColors
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.secondaryText)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryMetric("Income", summary.income, colors.accentGreen)
                SummaryMetric("Expense", summary.expense, colors.accentRed)
                SummaryMetric("Net", summary.net, if (summary.net >= 0) colors.accentGreen else colors.accentRed)
            }
            Spacer(Modifier.height(8.dp))
            Text("${summary.count} transactions", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        }
    }
}

@Composable
private fun SummaryMetric(label: String, amount: Double, color: Color) {
    val colors = MaterialTheme.appColors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(formatCurrencyCompact(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
    }
}

@Composable
private fun MonthlyChartCard(buckets: List<MonthlyBucket>) {
    val colors = MaterialTheme.appColors
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Monthly Overview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.secondaryText)
            Spacer(Modifier.height(12.dp))
            // Simple bar chart using Canvas/Box approach (Vico integration placeholder)
            // Using a simple visual representation
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                buckets.takeLast(6).forEach { bucket ->
                    val maxVal = buckets.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0)
                    MonthlyBucketRow(bucket, maxVal)
                }
            }
        }
    }
}

@Composable
private fun MonthlyBucketRow(bucket: MonthlyBucket, maxVal: Double) {
    val colors = MaterialTheme.appColors
    Column {
        Text(bucket.label, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        Spacer(Modifier.height(2.dp))
        // Income bar
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .height(10.dp)
                    .fillMaxWidth((bucket.income / maxVal).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.accentGreen)
            )
            Spacer(Modifier.width(6.dp))
            Text(formatCurrencyCompact(bucket.income), style = MaterialTheme.typography.bodySmall, color = colors.accentGreen)
        }
        Spacer(Modifier.height(2.dp))
        // Expense bar
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .height(10.dp)
                    .fillMaxWidth((bucket.expense / maxVal).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.accentRed)
            )
            Spacer(Modifier.width(6.dp))
            Text(formatCurrencyCompact(bucket.expense), style = MaterialTheme.typography.bodySmall, color = colors.accentRed)
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    breakdown: List<CategoryBreakdownItem>,
    topCategories: List<TopCategoryItem>
) {
    val colors = MaterialTheme.appColors
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Expense by Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.secondaryText)
            Spacer(Modifier.height(12.dp))

            // Simple horizontal bar breakdown
            val total = breakdown.sumOf { it.value }
            topCategories.forEach { item ->
                val fraction = if (total > 0) (item.amount / total).toFloat() else 0f
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(Color(item.color)))
                    Spacer(Modifier.width(8.dp))
                    Text(item.label, style = MaterialTheme.typography.bodySmall, color = colors.primaryText, modifier = Modifier.width(90.dp), maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.elevatedSurface)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(4.dp)).background(Color(item.color)))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("%.0f%%".format(item.percentage), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                }
            }
        }
    }
}

@Composable
private fun MonthYearPickerDialog(year: Int, month: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var selectedYear by remember { mutableStateOf(year) }
    var selectedMonth by remember { mutableStateOf(month) }
    val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month") },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedYear-- }) { Icon(Icons.Default.ChevronLeft, null) }
                    Text("$selectedYear", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { selectedYear++ }) { Icon(Icons.Default.ChevronRight, null) }
                }
                val colors = MaterialTheme.appColors
                (1..12).chunked(4).forEach { rowMonths ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowMonths.forEach { m ->
                            val sel = m == selectedMonth
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) colors.accentBlue else Color.Transparent,
                                modifier = Modifier.clickable { selectedMonth = m }.padding(2.dp)
                            ) {
                                Text(monthNames[m - 1], color = if (sel) Color.White else colors.primaryText, modifier = Modifier.padding(8.dp, 4.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun YearPickerDialog(year: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var selectedYear by remember { mutableStateOf(year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Year") },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedYear-- }) { Icon(Icons.Default.ChevronLeft, null) }
                Text("$selectedYear", fontWeight = FontWeight.Bold)
                IconButton(onClick = { selectedYear++ }) { Icon(Icons.Default.ChevronRight, null) }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selectedYear) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun IsoDatePickerDialog(initialDate: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val initial = remember(initialDate) {
        runCatching { LocalDate.parse(initialDate) }.getOrElse { LocalDate.now() }
    }

    DisposableEffect(context, initialDate) {
        val dialog = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                onConfirm("%04d-%02d-%02d".format(year, month + 1, day))
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        )
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}

private fun AnalysisPeriod.label() = when (this) {
    AnalysisPeriod.FULL -> "All Time"
    AnalysisPeriod.MONTHLY -> "Monthly"
    AnalysisPeriod.ANNUALLY -> "Annual"
    AnalysisPeriod.CUSTOM -> "Custom Range"
}
