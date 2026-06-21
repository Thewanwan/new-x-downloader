package com.twitter.downloader.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twitter.downloader.data.local.entity.UserEntity
import com.twitter.downloader.service.DownloadService
import com.twitter.downloader.service.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val downloadState by DownloadService.downloadState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf<UserEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("X Downloader") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Download state banner
            when (val state = downloadState) {
                is DownloadState.Loading -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                is DownloadState.Success -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "下载完成! 共 ${state.count} 个文件",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is DownloadState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "错误: ${state.message}",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                    Text(
                        text = "点击右下角 + 添加用户",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { user ->
                        UserCard(
                            user = user,
                            onDownload = { showDownloadDialog = user },
                            onDelete = { viewModel.deleteUser(user.id) }
                        )
                    }
                }
            }
        }
    }

    // Add user dialog
    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { screenName, cookie ->
                viewModel.addUser(screenName, cookie)
                showAddDialog = false
            }
        )
    }

    // Download dialog
    showDownloadDialog?.let { user ->
        DownloadDialog(
            user = user,
            onDismiss = { showDownloadDialog = null },
            onDownload = { incremental ->
                val context = androidx.compose.ui.platform.LocalContext.current
                DownloadService.start(context, user, incremental)
                showDownloadDialog = null
            }
        )
    }
}

@Composable
fun UserCard(
    user: UserEntity,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName.ifEmpty { user.screenName },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "@${user.screenName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "已下载: ${user.totalCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDownload) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var screenName by remember { mutableStateOf("") }
    var cookie by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加用户") },
        text = {
            Column {
                OutlinedTextField(
                    value = screenName,
                    onValueChange = { screenName = it },
                    label = { Text("用户名 (不含@)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    label = { Text("Cookie") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(screenName, cookie) },
                enabled = screenName.isNotBlank() && cookie.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun DownloadDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onDownload: (Boolean) -> Unit
) {
    var incremental by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("下载 @${user.screenName}") },
        text = {
            Column {
                Text("已下载: ${user.totalCount} 个文件")
                Spacer(modifier = Modifier.height(16.dp))
                Text("下载方式:")
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = incremental,
                        onClick = { incremental = true }
                    )
                    Text("仅下载未下载的")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !incremental,
                        onClick = { incremental = false }
                    )
                    Text("全部重新下载")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDownload(incremental) }) {
                Text("开始下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
