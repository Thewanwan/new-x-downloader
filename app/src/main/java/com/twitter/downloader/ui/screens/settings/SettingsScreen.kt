package com.twitter.downloader.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                title = {
                    Text(
                        "设置",
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // === Cookie 配置 ===
            SettingsSection(title = "Cookie 配置", icon = Icons.Outlined.Key) {
                OutlinedTextField(
                    value = authToken,
                    onValueChange = { viewModel.updateAuthToken(it) },
                    label = { Text("auth_token") },
                    placeholder = { Text("输入 auth_token") },
                    leadingIcon = { Icon(Icons.Outlined.VpnKey, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ct0,
                    onValueChange = { viewModel.updateCt0(it) },
                    label = { Text("ct0") },
                    placeholder = { Text("输入 ct0") },
                    leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                InfoCard(
                    text = "登录 x.com → 按 F12 → Network → 找到任意请求 → Headers → Cookie",
                    icon = Icons.Outlined.HelpOutline
                )
            }

            // === 保存路径 ===
            SettingsSection(title = "保存路径", icon = Icons.Outlined.Folder) {
                OutlinedTextField(
                    value = savePath.ifEmpty { "默认目录" },
                    onValueChange = {},
                    label = { Text("保存目录") },
                    leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                    trailingIcon = {
                        FilledTonalIconButton(
                            onClick = { folderPicker.launch(null) }
                        ) {
                            Icon(Icons.Outlined.Folder, contentDescription = "选择文件夹")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // === 下载内容类型 ===
            SettingsSection(title = "下载内容类型", icon = Icons.Outlined.Category) {
                Text(
                    "选择要下载的内容（三选一）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceCard(
                    title = "仅媒体",
                    subtitle = "仅下载用户自己发的图片和视频",
                    selected = !hasRetweet && !highLights && !likes,
                    onClick = {
                        viewModel.updateHasRetweet(false)
                        viewModel.updateHighLights(false)
                        viewModel.updateLikes(false)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceCard(
                    title = "包含转推",
                    subtitle = "同时下载转推内容（消耗大量API次数）",
                    selected = hasRetweet,
                    onClick = {
                        viewModel.updateHasRetweet(true)
                        viewModel.updateHighLights(false)
                        viewModel.updateLikes(false)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceCard(
                    title = "亮点标签",
                    subtitle = "下载用户的亮点内容",
                    selected = highLights,
                    onClick = {
                        viewModel.updateHasRetweet(false)
                        viewModel.updateHighLights(true)
                        viewModel.updateLikes(false)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceCard(
                    title = "喜欢列表",
                    subtitle = "下载用户点赞的内容（仅限本人账号）",
                    selected = likes,
                    onClick = {
                        viewModel.updateHasRetweet(false)
                        viewModel.updateHighLights(false)
                        viewModel.updateLikes(true)
                    }
                )
            }

            // === 下载选项 ===
            SettingsSection(title = "下载选项", icon = Icons.Outlined.Tune) {
                OutlinedTextField(
                    value = timeRange,
                    onValueChange = { viewModel.updateTimeRange(it) },
                    label = { Text("时间范围") },
                    placeholder = { Text("1990-01-01:2030-01-01") },
                    leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "格式: 起始日期:结束日期，不填默认无限制",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                SwitchItem(
                    title = "下载视频",
                    subtitle = "是否下载推文中的视频",
                    checked = hasVideo,
                    onCheckedChange = { viewModel.updateHasVideo(it) }
                )

                SwitchItem(
                    title = "记录已下载内容",
                    subtitle = "避免重复下载，节省带宽",
                    checked = downLog,
                    onCheckedChange = { viewModel.updateDownLog(it) }
                )

                SwitchItem(
                    title = "自动同步最新内容",
                    subtitle = "基于本地已有的内容自动同步",
                    checked = autoSync,
                    onCheckedChange = { viewModel.updateAutoSync(it) }
                )

                SwitchItem(
                    title = "输出 Markdown 文件",
                    subtitle = "记录获取到的推文信息",
                    checked = mdOutput,
                    onCheckedChange = { viewModel.updateMdOutput(it) }
                )
            }

            // === 图片格式 ===
            SettingsSection(title = "图片格式", icon = Icons.Outlined.Image) {
                SingleChoiceCard(
                    title = "orig",
                    subtitle = "自适应原图（以推特服务器为准）",
                    selected = imageFormat == "orig",
                    onClick = { viewModel.updateImageFormat("orig") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceCard(
                    title = "jpg",
                    subtitle = "全部以jpg格式保存",
                    selected = imageFormat == "jpg",
                    onClick = { viewModel.updateImageFormat("jpg") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceCard(
                    title = "png",
                    subtitle = "全部以png格式保存（文件较大）",
                    selected = imageFormat == "png",
                    onClick = { viewModel.updateImageFormat("png") }
                )
            }

            // === 高级设置 ===
            SettingsSection(title = "高级设置", icon = Icons.Outlined.Settings) {
                OutlinedTextField(
                    value = maxConcurrentRequests.toString(),
                    onValueChange = {
                        val value = it.toIntOrNull() ?: 8
                        viewModel.updateMaxConcurrentRequests(value.coerceIn(1, 32))
                    },
                    label = { Text("最大并发数") },
                    leadingIcon = { Icon(Icons.Outlined.Speed, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "默认 8，网络好可调高，下载失败多则调低",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = mediaCountLimit.toString(),
                    onValueChange = {
                        val value = it.toIntOrNull() ?: 350
                        viewModel.updateMediaCountLimit(value.coerceAtLeast(0))
                    },
                    label = { Text("单个 MD 文件媒体数量上限") },
                    leadingIcon = { Icon(Icons.Outlined.Numbers, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "默认 350，填 0 则不限制",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = proxy,
                    onValueChange = { viewModel.updateProxy(it) },
                    label = { Text("代理（可选）") },
                    placeholder = { Text("http://localhost:port") },
                    leadingIcon = { Icon(Icons.Outlined.Wifi, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "留空则不使用代理",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                SwitchItem(
                    title = "下载日志输出",
                    subtitle = "显示详细的下载过程信息",
                    checked = logOutput,
                    onCheckedChange = { viewModel.updateLogOutput(it) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        content()
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun InfoCard(text: String, icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SingleChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
