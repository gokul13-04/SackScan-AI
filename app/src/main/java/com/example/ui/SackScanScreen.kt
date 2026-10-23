package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.Language
import com.example.model.Translations

// Gradient colors
val GradientStart = Color(0xFF0F6E56)
val GradientEnd = Color(0xFF6B4A12)

// Theme buttons
val GreenAction = Color(0xFF0F6E56)
val GreenActionHover = Color(0xFF138569)
val BrownScanAction = Color(0xFF6B4A12)
val BrownScanActionDisabled = Color(0xFF9E8B71)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SackScanScreen(
    viewModel: SackScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.currentLanguage.collectAsState()
    val selectedBitmap by viewModel.selectedBitmap.collectAsState()
    val optionalHint by viewModel.optionalHint.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val agentState by viewModel.agentState.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.onImageSelected(bitmap)
        }
    }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    // Gallery picker launcher (Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImageUriSelected(uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Adaptive layout constraint: center with max 600.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- HEADER ---
                        AppHeader(
                            language = language,
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            onOpenKeySettings = {
                                apiKeyInput = viewModel.getEffectiveApiKey()
                                showApiKeyDialog = true
                            }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // --- MAIN WHITE ROUNDED CARD ---
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                                .testTag("main_sack_card"),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Optional hint text field
                                OutlinedTextField(
                                    value = optionalHint,
                                    onValueChange = { viewModel.setOptionalHint(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("optional_hint_input"),
                                    label = { Text(Translations.optionalHintLabel(language)) },
                                    placeholder = {
                                        Text(
                                            Translations.optionalHintPlaceholder(language),
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9E9E9E))
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GreenAction,
                                        unfocusedBorderColor = Color(0xFFCFD8DC),
                                        focusedLabelColor = GreenAction
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Image action buttons: Camera & Upload Image
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val permission = Manifest.permission.CAMERA
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                permission
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPermission) {
                                                cameraLauncher.launch(null)
                                            } else {
                                                permissionLauncher.launch(permission)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("camera_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GreenAction,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Translations.cameraBtn(language),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("upload_image_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GreenAction,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Translations.uploadBtn(language),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Image Preview Area
                                if (selectedBitmap != null) {
                                    ImagePreviewCard(
                                        bitmap = selectedBitmap!!,
                                        onClear = { viewModel.clearImage() },
                                        language = language
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Button(
                                    onClick = {
                                        if (viewModel.getEffectiveApiKey().isBlank()) {
                                            apiKeyInput = ""
                                            showApiKeyDialog = true
                                        }
                                        viewModel.scanSack()
                                    },
                                    enabled = selectedBitmap != null && !isScanning,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("scan_sack_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrownScanAction,
                                        contentColor = Color.White,
                                        disabledContainerColor = BrownScanActionDisabled,
                                        disabledContentColor = Color(0xFFD7CCC8)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = Translations.analyzingBtn(language),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = Translations.scanSackBtn(language),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                // Error Box
                                if (!errorMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    ErrorBanner(
                                        message = errorMessage!!,
                                        onDismiss = { viewModel.clearError() }
                                    )
                                }

                                // Reset Button
                                if (selectedBitmap != null || scanResult != null || errorMessage != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.reset() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("reset_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF5D4037)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBCAAA4))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Translations.resetBtn(language),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        // --- AGENT PROGRESS STEPPER ---
                        if (agentState != null && agentState!!.stepLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            AgentStepperView(
                                agentState = agentState!!,
                                language = language
                            )
                        }

                        // --- RESULT PANEL ---
                        if (scanResult != null) {
                            Spacer(modifier = Modifier.height(18.dp))
                            ResultPanelView(
                                result = scanResult!!,
                                language = language
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Optional API Key dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = GreenAction
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Translations.apiKeySettings(language))
                }
            },
            text = {
                Column {
                    Text(
                        text = if (language == Language.TAMIL) {
                            "ஜெமினி API சாவியை உள்ளிடவும் அல்லது AI Studio Secrets பலகத்தில் உள்ளமைக்கவும்."
                        } else {
                            "Enter your Gemini API key or configure it in the AI Studio Secrets panel."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text(Translations.enterApiKey(language)) },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomApiKey(apiKeyInput)
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAction)
                ) {
                    Text(Translations.saveApiKey(language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text(Translations.cancel(language), color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun AppHeader(
    language: Language,
    onToggleLanguage: () -> Unit,
    onOpenKeySettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo + Title + Subtitle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .shadow(4.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "SackScan Logo",
                    tint = GreenAction,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = Translations.appTitle(language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = Translations.appSubtitle(language),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE0ECE6),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Actions: Language Toggle & Key Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Language Toggle Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onToggleLanguage() }
                    .testTag("language_toggle_button"),
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Change Language",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (language == Language.ENGLISH) "தமிழ்" else "English",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Settings button
            IconButton(
                onClick = onOpenKeySettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .testTag("key_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API Key",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewCard(
    bitmap: Bitmap,
    onClear: () -> Unit,
    language: Language
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            .testTag("image_preview_box")
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Selected sack image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Clear button
        IconButton(
            onClick = onClear,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .testTag("clear_image_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = Translations.clearImage(language),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Image stats chip
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${bitmap.width} × ${bitmap.height} px",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_banner"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFEBEE),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFC62828),
                    lineHeight = 16.sp
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
