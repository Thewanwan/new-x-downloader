package com.twitter.downloader.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current

    val authToken by viewModel.authToken.collectAsState()
    val ct0 by viewModel.ct0.collectAsState()
    val savePath by viewModel.savePath.collectAsState()
    val hasRetweet by viewModel.hasRetweet.collectAsState()
    val highLights by viewModel.highLights.collectAsState()
    val likes by viewModel.likes.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val downLog by viewModel.downLog.collectAsState()
    val autoSync by viewModel.autoSync.collectAsState()
    val imageFormat by viewModel.imageFormat.collectAsState()
    val hasVideo by viewModel.hasVideo.collectAsState()
    val logOutput by viewModel.logOutput.collectAsState()
    val maxConcurrentRequests by viewModel.maxConcurrentRequests.collectAsState()
    val proxy by viewModel.proxy.collectAsState()
    val mdOutput by viewModel.mdOutput.collectAsState()
    val mediaCountLimit by viewModel.mediaCountLimit.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)
            viewModel.updateSavePath(it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // === Cookie 配置 ===
            Text("Cookie 配置", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = authToken,
                onValueChange = { viewModel.updateAuthToken(it) },
                label = { Text("auth_token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = ct0,
                onValueChange = { viewModel.updateCt0(it) },
                label = { Text("ct0") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                "从浏览器开发者工具获取，登录x.com后按F12，在Network标签中找到Cookie字段",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // === 保存路径 ===
            Text("保存路径", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = savePath,
                onValueChange = {},
                label = { Text("保存目录") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "选择文件夹")
                    }
                }
            )

            Text(
                "留空则保存到应用默认目录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // === 下载内容类型（三选一）===
            Text("下载内容类型", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = !hasRetweet && !highLights && !likes, onCheckedChange = {})
                Text("仅媒体（默认）")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = hasRetweet,
                    onCheckedChange = {
                        viewModel.updateHasRetweet(it)
                        if (it) {
                            viewModel.updateHighLights(false)
                            viewModel.updateLikes(false)
                        }
                    }
                )
                Text("包含转推（消耗大量API次数）")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = highLights,
                    onCheckedChange = {
                        viewModel.updateHighLights(it)
                        if (it) {
                            viewModel.updateHasRetweet(false)
                            viewModel.updateLikes(false)
                        }
                    }
                )
                Text("亮点标签")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = likes,
                    onCheckedChange = {
                        viewModel.updateLikes(it)
                        if (it) {
                            viewModel.updateHasRetweet(false)
                            viewModel.updateHighLights(false)
                        }
                    }
                )
                Text("喜欢列表（仅限本人账号）")
            }

            HorizontalDivider()

            // === 下载选项 ===
            Text("下载选项", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = timeRange,
                onValueChange = { viewModel.updateTimeRange(it) },
                label = { Text("时间范围") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "格式：1990-01-01:2030-01-01，不填默认无限制",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasVideo, onCheckedChange = { viewModel.updateHasVideo(it) })
                Text("下载视频")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = downLog, onCheckedChange = { viewModel.updateDownLog(it) })
                Text("记录已下载内容（避免重复下载）")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = autoSync, onCheckedChange = { viewModel.updateAutoSync(it) })
                Text("自动同步最新内容")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = mdOutput, onCheckedChange = { viewModel.updateMdOutput(it) })
                Text("输出 Markdown 文件")
            }

            HorizontalDivider()

            // === 图片格式 ===
            Text("图片格式", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = imageFormat == "orig", onClick = { viewModel.updateImageFormat("orig") })
                Text("orig（自适应原图）")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = imageFormat == "jpg", onClick = { viewModel.updateImageFormat("jpg") })
                Text("jpg（全部jpg）")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = imageFormat == "png", onClick = { viewModel.updateImageFormat("png") })
                Text("png（全部png，文件较大）")
            }

            HorizontalDivider()

            // === 高级设置 ===
            Text("高级设置", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = maxConcurrentRequests.toString(),
                onValueChange = { viewModel.updateMaxConcurrentRequests(it.toIntOrNull() ?: 8) },
                label = { Text("最大并发数") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(
                "默认8，网络好可调高，下载失败多则调低",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = mediaCountLimit.toString(),
                onValueChange = { viewModel.updateMediaCountLimit(it.toIntOrNull() ?: 350) },
                label = { Text("单个MD文件媒体数量上限") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(
                "默认350，填0则不限制",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = proxy,
                onValueChange = { viewModel.updateProxy(it) },
                label = { Text("代理（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "格式：http://localhost:port，留空则不使用代理",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = logOutput, onCheckedChange = { viewModel.updateLogOutput(it) })
                Text("下载日志输出")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
