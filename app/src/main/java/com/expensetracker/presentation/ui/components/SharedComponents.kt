package com.expensetracker.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.ui.theme.appColors
import com.expensetracker.util.formatCurrency

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.appColors
    val shape = RoundedCornerShape(14.dp)
    val cardModifier = modifier
        .clip(shape)
        .background(colors.surface)
        .border(1.dp, colors.border, shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(modifier = cardModifier, content = content)
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.appColors.secondaryText,
        modifier = modifier.padding(bottom = 8.dp),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun TransactionTypeChip(
    type: TransactionType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.appColors
    val bgColor = if (selected) {
        if (type == TransactionType.CREDIT) colors.accentGreen else colors.accentRed
    } else colors.elevatedSurface
    val textColor = if (selected) Color.White else colors.secondaryText

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (type == TransactionType.CREDIT) "Income" else "Expense",
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AmountText(
    amount: Double,
    type: TransactionType,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    val colors = MaterialTheme.appColors
    val color = if (type == TransactionType.CREDIT) colors.accentGreen else colors.accentRed
    val prefix = if (type == TransactionType.CREDIT) "+" else "-"
    Text(
        text = "$prefix${formatCurrency(amount)}",
        color = color,
        style = style,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
fun AppSnackbar(message: String, onDismiss: () -> Unit) {
    Snackbar(
        action = { TextButton(onClick = onDismiss) { Text("Dismiss") } },
        modifier = Modifier.padding(16.dp)
    ) { Text(message) }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    confirmColor: Color = MaterialTheme.appColors.accentRed,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyState(
    icon: ImageVector = Icons.Default.Receipt,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon, contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.appColors.mutedText
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.appColors.secondaryText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.appColors.mutedText)
    }
}

@Composable
fun LoadingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun SummaryRow(label: String, amount: Double, type: TransactionType? = null) {
    val colors = MaterialTheme.appColors
    val amountColor = when (type) {
        TransactionType.CREDIT -> colors.accentGreen
        TransactionType.DEBIT -> colors.accentRed
        null -> if (amount >= 0) colors.accentGreen else colors.accentRed
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
        Text(formatCurrency(amount), style = MaterialTheme.typography.bodyMedium, color = amountColor, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = trailingIcon,
                keyboardOptions = keyboardOptions,
                singleLine = singleLine,
                enabled = onClick == null,
                readOnly = readOnly || onClick != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.appColors.accentBlue,
                    unfocusedBorderColor = MaterialTheme.appColors.border,
                    focusedLabelColor = MaterialTheme.appColors.accentBlue,
                    unfocusedLabelColor = MaterialTheme.appColors.secondaryText,
                    disabledBorderColor = MaterialTheme.appColors.border,
                    disabledLabelColor = MaterialTheme.appColors.secondaryText,
                    disabledTextColor = MaterialTheme.appColors.primaryText,
                    disabledTrailingIconColor = MaterialTheme.appColors.secondaryText
                )
            )
            if (onClick != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(role = Role.Button, onClick = onClick)
                )
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.appColors.accentRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
        }
    }
}
