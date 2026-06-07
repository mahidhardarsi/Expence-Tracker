package com.expensetracker.presentation.ui.options

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.ui.components.*
import com.expensetracker.presentation.ui.theme.appColors
import com.expensetracker.util.ImportExportUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    settings: AppSettings,
    categories: List<Category>,
    reports: List<Report>,
    transactions: List<Transaction>,
    onToggleTheme: () -> Unit,
    onAddCategory: (String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onExportAllJson: (String) -> Unit,
    onExportAllCsv: (String) -> Unit,
    onImportJson: (String) -> Unit,
    onImportCsv: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onOpenAiModels: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val context = LocalContext.current

    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showImportInfoDialog by remember { mutableStateOf(false) }

    // File pickers
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val content = readUriContent(context, it)
            if (content != null) onImportJson(content)
            else onShowMessage("Failed to read file.")
        }
    }
    val csvPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val content = readUriContent(context, it)
            if (content != null) onImportCsv(content)
            else onShowMessage("Failed to read file.")
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Options", fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface, titleContentColor = colors.primaryText)
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
            // Appearance
            SettingsSection("Appearance") {
                SettingsRow(
                    icon = if (settings.themeMode == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                    title = "Theme",
                    subtitle = if (settings.themeMode == ThemeMode.DARK) "Dark mode" else "Light mode",
                    onClick = onToggleTheme,
                    trailing = {
                        Switch(
                            checked = settings.themeMode == ThemeMode.DARK,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accentBlue, checkedTrackColor = colors.accentBlue.copy(alpha = 0.5f))
                        )
                    }
                )
            }

            // Categories
            SettingsSection("Categories") {
                SettingsRow(
                    icon = Icons.Default.Category,
                    title = "Manage Categories",
                    subtitle = "${categories.count { it.kind == CategoryKind.CUSTOM }} custom categories",
                    onClick = { showCategoriesDialog = true }
                )
            }

            SettingsSection("AI") {
                SettingsRow(
                    icon = Icons.Default.Psychology,
                    title = "AI Models",
                    subtitle = "Download and manage on-device models",
                    onClick = onOpenAiModels
                )
            }

            // Data / Export
            SettingsSection("Export Data") {
                SettingsRow(
                    icon = Icons.Default.Download,
                    title = "Export All as JSON",
                    subtitle = "${reports.size} reports, ${transactions.size} transactions",
                    onClick = {
                        val content = ImportExportUtil.serializeLegacyJson(reports, transactions)
                        onExportAllJson(content)
                    }
                )
                HorizontalDivider(color = colors.border)
                SettingsRow(
                    icon = Icons.Default.TableView,
                    title = "Export All as CSV",
                    subtitle = "Spreadsheet-compatible format",
                    onClick = {
                        val content = ImportExportUtil.serializeLegacyCsv(reports, transactions)
                        onExportAllCsv(content)
                    }
                )
            }

            // Import
            SettingsSection("Import Data") {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "Import Format Guide",
                    subtitle = "Learn the supported import formats",
                    onClick = { showImportInfoDialog = true }
                )
                HorizontalDivider(color = colors.border)
                SettingsRow(
                    icon = Icons.Default.Upload,
                    title = "Import JSON",
                    subtitle = "Import from legacy JSON file",
                    onClick = { jsonPickerLauncher.launch("*/*") }
                )
                HorizontalDivider(color = colors.border)
                SettingsRow(
                    icon = Icons.Default.GridOn,
                    title = "Import CSV",
                    subtitle = "Import from CSV spreadsheet",
                    onClick = { csvPickerLauncher.launch("*/*") }
                )
            }

            // App info
            SettingsSection("About") {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "Expense Tracker",
                    subtitle = "Version 1.0.0"
                )
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (showCategoriesDialog) {
        CategoriesDialog(
            categories = categories,
            onAddCategory = onAddCategory,
            onRemoveCategory = onRemoveCategory,
            onDismiss = { showCategoriesDialog = false }
        )
    }

    if (showImportInfoDialog) {
        ImportInfoDialog(onDismiss = { showImportInfoDialog = false })
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.appColors
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = colors.mutedText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )
        AppCard(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.accentBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = colors.primaryText)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = colors.mutedText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CategoriesDialog(
    categories: List<Category>,
    onAddCategory: (String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categories") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Add new
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it; error = null },
                        label = { Text("New category") },
                        singleLine = true,
                        isError = error != null,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (newCategoryName.isBlank()) { error = "Name required"; return@IconButton }
                        onAddCategory(newCategoryName.trim())
                        newCategoryName = ""
                    }) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
                if (error != null) Text(error!!, color = MaterialTheme.appColors.accentRed, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                // List
                val customCats = categories.filter { it.kind == CategoryKind.CUSTOM }
                val defaultCats = categories.filter { it.kind == CategoryKind.DEFAULT }
                if (customCats.isNotEmpty()) {
                    Text("Custom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.appColors.mutedText)
                    customCats.forEach { cat ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onRemoveCategory(cat.name) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp), tint = MaterialTheme.appColors.accentRed)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text("Default", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.appColors.mutedText)
                defaultCats.forEach { cat ->
                    Text(cat.name, Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.appColors.secondaryText)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun ImportInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Formats") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("JSON Format", fontWeight = FontWeight.SemiBold)
                Text(ImportExportUtil.LEGACY_IMPORT_JSON_SAMPLE, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                HorizontalDivider()
                Text("CSV Format", fontWeight = FontWeight.SemiBold)
                Text(ImportExportUtil.LEGACY_IMPORT_CSV_SAMPLE, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun readUriContent(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
    } catch (_: Exception) {
        null
    }
}
