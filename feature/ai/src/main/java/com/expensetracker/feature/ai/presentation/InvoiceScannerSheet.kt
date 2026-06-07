package com.expensetracker.feature.ai.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.InvoiceFields
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScannerSheet(
    categories: List<Category>,
    visionAvailable: Boolean,
    onDismiss: () -> Unit,
    onOpenModelHub: () -> Unit,
    onApply: (InvoiceFields) -> Unit,
    viewModel: InvoiceScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    val pendingPhotoUri = rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingPhotoUri.value?.let(viewModel::setPreviewImage)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "invoice-${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingPhotoUri.value = file.absolutePath
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            persistUriImage(context, it)?.let(viewModel::setPreviewImage)
        }
    }

    ModalBottomSheet(onDismissRequest = {
        viewModel.reset()
        onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Invoice Scanner", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        when {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                                val file = File(context.cacheDir, "invoice-${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                pendingPhotoUri.value = file.absolutePath
                                cameraLauncher.launch(uri)
                            }
                            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) { Text("Take Photo") }
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) { Text("Choose from Gallery") }
            }

            state.imagePath?.let { path ->
                val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Invoice preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }

            when (state.step) {
                InvoiceScannerStep.IDLE -> Text("Pick an invoice image to begin.")
                InvoiceScannerStep.PREVIEW -> {
                    if (visionAvailable) {
                        Button(onClick = { viewModel.extract(categories) }) { Text("Extract") }
                    } else {
                        Text("The active model is text-only. Switch to Gemma 4 E2B to scan invoices.", color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onOpenModelHub) { Text("Open Model Hub") }
                    }
                }
                InvoiceScannerStep.LOADING -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Analyzing receipt...")
                }
                InvoiceScannerStep.SUCCESS -> {
                    val fields = state.extractedFields
                    Text("Merchant: ${fields?.merchant.orEmpty()}")
                    Text("Amount: ${fields?.amount ?: 0.0}")
                    Text("Date: ${fields?.date.orEmpty()}")
                    Text("Category: ${fields?.category.orEmpty()}")
                    Button(onClick = { fields?.let(onApply) }) { Text("Apply to Form") }
                }
                InvoiceScannerStep.ERROR -> {
                    Text(state.errorMessage ?: "Could not extract fields.", color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { viewModel.reset() }) { Text("Retry") }
                }
            }
        }
    }
}

private fun persistUriImage(context: Context, uri: Uri): String? = runCatching {
    val file = File(context.cacheDir, "invoice-${System.currentTimeMillis()}.jpg")
    val input = context.contentResolver.openInputStream(uri) ?: return null
    val bitmap = BitmapFactory.decodeStream(input) ?: return null
    FileOutputStream(file).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    }
    file.absolutePath
}.getOrNull()
