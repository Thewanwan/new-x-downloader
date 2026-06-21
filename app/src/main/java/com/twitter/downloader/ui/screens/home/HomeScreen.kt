package com.twitter.downloader.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twitter.downloader.data.local.entity.UserEntity
import com.twitter.downloader.service.DownloadService
import com.twitter.downloader.service.DownloadState
import com.twitter.downloader.ui.screens.settings.SettingsViewModel
import com.twitter.downloader.util.UpdateChecker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onNavigateToHistory: (Long, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val downloadState by DownloadService.downloadState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf<UserEntity?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Update check
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateInfo = UpdateChecker.checkForUpdate(context)
        if (updateInfo?.hasUpdate == true) {
            showUpdateDialog = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "X Downloader",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "v${UpdateChecker.getCurrentVersion(context)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("设置") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
                    label = { Text("日志") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToLogs()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Update, contentDescription = null) },
                    label = { Text("检查更新") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        scope.launch {
                            snackbarHostState.showSnackbar("正在检查更新...")
                            val result = UpdateChecker.checkForUpdate(context)
                            if (result != null) {
                                updateInfo = result
                                if (result.hasUpdate) {
                                    showUpdateDialog = true
                                } else {
                                    snackbarHostState.showSnackbar("当前已是最新版本 v${result.latestVersion}")
                                }
                            } else {
                                snackbarHostState.showSnackbar("检查更新失败，请检查网络连接")
                            }
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                Text(
                    text = "© 2024 X Downloader",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                "X Downloader",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${users.size} 个用户",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "菜单"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("添加用户") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Download progress card
                when (val state = downloadState) {
                    is DownloadState.Loading -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (state.currentFile.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.currentFile,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (state.totalCount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { state.downloadedCount.toFloat() / state.totalCount },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                    is DownloadState.Partial -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text("成功: ${state.downloadedCount} / 失败: ${state.failedCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                }
                                if (state.canRetry) {
                                    FilledTonalButton(onClick = { DownloadService.retryPartial(context, state) }) {
                                        Text("重试")
                                    }
                                }
                            }
                        }
                    }
                    is DownloadState.Success -> {
                        LaunchedEffect(state) {
                            val message = "下载完成! 共 ${state.count} 个文件"
                            viewModel.clearState()
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                    is DownloadState.Error -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                                if (state.canRetry) {
                                    FilledTonalButton(onClick = { DownloadService.retry(context, state) }) {
                                        Text("重试")
                                    }
                                }
                            }
                        }
                    }
                    is DownloadState.Idle -> {}
                }

                // User list
                if (users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Text("还没有添加用户", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("点击右下角按钮添加推特用户", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(users, key = { it.id }) { user ->
                            UserCard(
                                user = user,
                                onDownload = { showDownloadDialog = user },
                                onHistory = { onNavigateToHistory(user.id, user.screenName) },
                                onDelete = { viewModel.deleteUser(user.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Update dialog
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            icon = { Icon(Icons.Default.Update, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("发现新版本 v${updateInfo!!.latestVersion}") },
            text = {
                Column {
                    Text("当前版本: v${UpdateChecker.getCurrentVersion(context)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(updateInfo!!.releaseNotes, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    UpdateChecker.openDownloadPage(context, updateInfo!!.downloadUrl)
                    showUpdateDialog = false
                }) {
                    Text("下载更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("稍后")
                }
            }
        )
    }

    // Dialogs
    if (showAddDialog) {
        val isCookieSet = settingsViewModel.isCookieConfigured()
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { screenName ->
                val cookie = settingsViewModel.getCookieString()
                viewModel.addUser(screenName, cookie)
                showAddDialog = false
            },
            isCookieConfigured = isCookieSet
        )
    }

    showDownloadDialog?.let { user ->
        DownloadDialog(
            user = user,
            onDismiss = { showDownloadDialog = null },
            onDownload = { incremental ->
                DownloadService.start(context, user, incremental)
                showDownloadDialog = null
            }
        )
    }

    // Handle add user result
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is HomeUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearState()
            }
            is HomeUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearState()
            }
            else -> {}
        }
    }
}

@Composable
fun UserCard(
    user: UserEntity,
    onDownload: () -> Unit,
    onHistory: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = user.screenName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.displayName.ifEmpty { "@${user.screenName}" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(text = "@${user.screenName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${user.totalCount} 个文件", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onHistory) { Icon(Icons.Outlined.History, contentDescription = "历史", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onDownload) { Icon(Icons.Default.Download, contentDescription = "下载", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除用户") },
            text = { Text("确定要删除 @${user.screenName} 吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("删除")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

@Composable
fun AddUserDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit, isCookieConfigured: Boolean = false) {
    var screenName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("添加用户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = screenName, onValueChange = { screenName = it }, label = { Text("用户名") }, placeholder = { Text("例如: elonmusk") }, leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (!isCookieConfigured) {
                    AssistChip(onClick = {}, label = { Text("请先在设置中配置Cookie") }, leadingIcon = { Icon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(screenName) }, enabled = screenName.isNotBlank() && isCookieConfigured) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun DownloadDialog(user: UserEntity, onDismiss: () -> Unit, onDownload: (Boolean) -> Unit) {
    var incremental by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("下载 @${user.screenName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("已下载: ${user.totalCount} 个文件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = incremental, onClick = { incremental = true }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("仅新内容") }
                    SegmentedButton(selected = !incremental, onClick = { incremental = false }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("全部重下") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDownload(incremental) }) { Text("开始下载") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
