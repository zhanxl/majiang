package com.majiang.ui.camera

import android.Manifest
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.majiang.ui.components.TileComposable
import com.majiang.vision.CameraManager
import com.majiang.vision.RecognitionMode
import com.majiang.viewmodel.CameraViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.recognitionMode) {
                            RecognitionMode.CLOUD_VISION -> "拍照识别（云端）"
                            RecognitionMode.TFLITE -> "拍照识别（本地）"
                        }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (!cameraPermissionState.status.isGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("需要摄像头权限才能使用识别功能")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("授予权限")
                }
            }
            return@Scaffold
        }

        if (!uiState.isConfigured) {
            CloudVisionConfigContent(
                onConfigured = { apiKey, provider, endpoint ->
                    viewModel.configureCloudVision(apiKey, provider, endpoint)
                },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val cameraManager = remember { CameraManager(context, lifecycleOwner) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            cameraManager.startCamera(
                                surfaceProvider = previewView.surfaceProvider
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                FloatingActionButton(
                    onClick = {
                        cameraManager.capturePhotoToBitmap { bitmap ->
                            if (bitmap != null) {
                                viewModel.recognizeFromBitmap(bitmap)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "拍照识别",
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (uiState.isProcessing) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            Text("正在识别...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (uiState.recognizedTiles.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "识别结果 (${uiState.recognizedTiles.size}张)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            OutlinedButton(
                                onClick = { viewModel.clearResults() }
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "清除",
                                    modifier = Modifier.height(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("清除", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.recognizedTiles) { tile ->
                                TileComposable(tile = tile)
                            }
                        }
                    }
                }
            }

            if (uiState.recognitionResults.isNotEmpty() && uiState.recognizedTiles.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "未识别到有效牌面，请重新拍照",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudVisionConfigContent(
    onConfigured: (String, com.majiang.vision.strategy.CloudVisionProvider, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var apiKey by remember { androidx.compose.runtime.mutableStateOf("") }
    var selectedProvider by remember {
        androidx.compose.runtime.mutableStateOf(com.majiang.vision.strategy.CloudVisionProvider.QWEN)
    }
    var customEndpoint by remember { androidx.compose.runtime.mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "配置云端视觉识别",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "使用大模型视觉能力识别麻将牌，无需训练数据",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.majiang.vision.strategy.CloudVisionProvider.entries.forEach { provider ->
                val isSelected = selectedProvider == provider
                val label = when (provider) {
                    com.majiang.vision.strategy.CloudVisionProvider.QWEN -> "通义千问"
                    com.majiang.vision.strategy.CloudVisionProvider.OPENAI -> "GPT-4o"
                    com.majiang.vision.strategy.CloudVisionProvider.CUSTOM -> "自定义"
                }
                OutlinedButton(
                    onClick = { selectedProvider = provider },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        label,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (selectedProvider == com.majiang.vision.strategy.CloudVisionProvider.CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.OutlinedTextField(
                value = customEndpoint,
                onValueChange = { customEndpoint = it },
                label = { Text("自定义接口地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (apiKey.isNotBlank()) {
                    onConfigured(apiKey, selectedProvider, customEndpoint)
                } else {
                    Toast.makeText(context, "请输入 API Key", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotBlank()
        ) {
            Text("开始使用")
        }
    }
}
