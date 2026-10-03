package com.readplus.ui.comic.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicDetailScreen(
    comicId: Long,
    onBack: () -> Unit,
    onReadFrom: (Int) -> Unit,
    viewModel: ComicDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCoverMenu by remember { mutableStateOf(false) }
    var showCoverPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 从外部选图片
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setCoverFromUri(it) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.comic?.title ?: "加载中") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("删除漫画") },
                            onClick = {
                                menuOpen = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            state.comic?.let { comic ->
                // ---------------- 顶部：封面 + 信息 ----------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 封面预览
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 156.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showCoverMenu = true }
                    ) {
                        val coverPath = comic.coverPath
                        if (coverPath != null && File(coverPath).exists()) {
                            AsyncImage(
                                model = File(coverPath),
                                contentDescription = comic.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "无封面",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 右下角"编辑"图标提示
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "更换封面",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // 右侧信息
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = comic.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 3
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "共 ${comic.pageCount} 页",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "上次读到第 ${comic.lastReadPage + 1} 页",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onReadFrom(comic.lastReadPage) },
                            contentPadding = PaddingValues(
                                horizontal = 16.dp, vertical = 4.dp
                            )
                        ) {
                            Text("继续阅读")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // ---------------- 4 列缩略图 ----------------
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(state.pages) { index, _ ->
                    val page = viewModel.zipPage(index)
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onReadFrom(index) }
                    ) {
                        if (page != null) {
                            AsyncImage(
                                model = page,
                                contentDescription = "第 ${index + 1} 页",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------------- 更换封面：第一层菜单 ----------------
    if (showCoverMenu) {
        AlertDialog(
            onDismissRequest = { showCoverMenu = false },
            title = { Text("更换封面") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showCoverMenu = false
                            showCoverPicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("从漫画中选择") }
                    TextButton(
                        onClick = {
                            showCoverMenu = false
                            imagePicker.launch(arrayOf("image/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("从外部图片选择") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCoverMenu = false }) { Text("取消") }
            }
        )
    }

    // ---------------- 更换封面：从漫画内选页 ----------------
    if (showCoverPicker) {
        Dialog(
            onDismissRequest = { showCoverPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxSize(0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    // 标题栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择一页作为封面",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showCoverPicker = false }) {
                            Text("取消")
                        }
                    }

                    // 缩略图网格
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(state.pages) { index, _ ->
                            val page = viewModel.zipPage(index)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.7f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        viewModel.setCoverFromPage(index)
                                        showCoverPicker = false
                                    }
                            ) {
                                if (page != null) {
                                    AsyncImage(
                                        model = page,
                                        contentDescription = "第 ${index + 1} 页",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------- 删除确认 ----------------
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除漫画") },
            text = { Text("确定删除《${state.comic?.title ?: ""}》吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete { onBack() }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}