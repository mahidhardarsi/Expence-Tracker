package com.expensetracker.presentation.ui.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.ui.components.*
import com.expensetracker.presentation.ui.theme.appColors
import com.expensetracker.util.isoDateToDisplay
import com.expensetracker.util.todayIsoDate
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    editingTransaction: Transaction?,
    reports: List<Report>,
    categories: List<Category>,
    currentReportId: Long?,
    prefillFields: InvoiceFields? = null,
    onPrefillConsumed: () -> Unit = {},
    onScanInvoice: () -> Unit = {},
    onSave: (TransactionInput) -> Unit,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val isEditing = editingTransaction != null

    var name by remember { mutableStateOf(editingTransaction?.name ?: "") }
    var amountText by remember { mutableStateOf(editingTransaction?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var date by remember { mutableStateOf(editingTransaction?.date ?: todayIsoDate()) }
    var selectedCategory by remember { mutableStateOf(editingTransaction?.category ?: (categories.firstOrNull()?.name ?: "Other")) }
    var selectedType by remember { mutableStateOf(editingTransaction?.type ?: TransactionType.DEBIT) }
    var selectedReportId by remember { mutableStateOf(editingTransaction?.reportId ?: currentReportId ?: reports.firstOrNull()?.id ?: 0L) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showReportDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(prefillFields) {
        prefillFields?.let { fields ->
            if (fields.merchant.isNotBlank()) name = fields.merchant
            fields.amount?.let { amountText = it.toString() }
            if (fields.date.isNotBlank()) date = fields.date
            if (fields.category.isNotBlank()) selectedCategory = fields.category
            selectedType = fields.type
            onPrefillConsumed()
        }
    }

    fun pickDate() {
        val current = try { LocalDate.parse(date) } catch (e: Exception) { LocalDate.now() }
        val cal = Calendar.getInstance()
        cal.set(current.year, current.monthValue - 1, current.dayOfMonth)
        DatePickerDialog(context, { _, y, m, d -> date = "%04d-%02d-%02d".format(y, m + 1, d) },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun validate(): Boolean {
        var valid = true
        nameError = null
        amountError = null
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) { amountError = "Enter a valid amount"; valid = false }
        return valid
    }

    fun save() {
        if (!validate()) return
        onSave(
            TransactionInput(
                reportId = selectedReportId,
                name = name.trim().ifBlank { "Transaction" },
                amount = amountText.toDouble(),
                date = date,
                category = selectedCategory,
                type = selectedType
            )
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Cancel") } },
                title = { Text(if (isEditing) "Edit Transaction" else "New Transaction", fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface, titleContentColor = colors.primaryText),
                actions = {
                    IconButton(onClick = onScanInvoice) {
                        Icon(Icons.Default.PhotoCamera, "Scan Invoice")
                    }
                    TextButton(onClick = ::save) {
                        Text("Save", fontWeight = FontWeight.Bold, color = colors.accentBlue)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Type selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TransactionTypeChip(TransactionType.CREDIT, selectedType == TransactionType.CREDIT) {
                    selectedType = TransactionType.CREDIT
                }
                TransactionTypeChip(TransactionType.DEBIT, selectedType == TransactionType.DEBIT) {
                    selectedType = TransactionType.DEBIT
                }
            }

            // Name
            InputField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = "Name (optional)",
                error = nameError
            )

            // Amount
            InputField(
                value = amountText,
                onValueChange = { amountText = it; amountError = null },
                label = "Amount (₹)",
                error = amountError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Date
            InputField(
                value = isoDateToDisplay(date),
                onValueChange = {},
                label = "Date",
                readOnly = true,
                onClick = ::pickDate,
                trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = colors.secondaryText) }
            )

            // Category
            ExposedDropdownMenuBox(expanded = showCategoryDropdown, onExpandedChange = { showCategoryDropdown = it }) {
                InputField(
                    value = selectedCategory,
                    onValueChange = {},
                    label = "Category",
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = showCategoryDropdown, onDismissRequest = { showCategoryDropdown = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = { selectedCategory = cat.name; showCategoryDropdown = false }
                        )
                    }
                }
            }

            // Report (only when editing or multiple reports)
            if (reports.size > 1) {
                ExposedDropdownMenuBox(expanded = showReportDropdown, onExpandedChange = { showReportDropdown = it }) {
                    val currentReport = reports.find { it.id == selectedReportId }
                    InputField(
                        value = currentReport?.name ?: "Select Report",
                        onValueChange = {},
                        label = "Report",
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showReportDropdown) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = showReportDropdown, onDismissRequest = { showReportDropdown = false }) {
                        reports.forEach { report ->
                            DropdownMenuItem(
                                text = { Text(report.name) },
                                onClick = { selectedReportId = report.id; showReportDropdown = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = ::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue)
            ) {
                Text(if (isEditing) "Update Transaction" else "Add Transaction", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
