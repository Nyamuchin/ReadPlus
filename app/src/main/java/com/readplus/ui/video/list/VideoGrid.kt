package com.readplus.ui.video.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size
import com.readplus.domain.model.Category
import com.readplus.domain.model.Video
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoGrid(
    videos: List<Video>,
    onClick: (Video) -> Unit,
    onDelete: (Video) -> Unit,
    onRename: (Video, String) -> Unit,
    categories: List<Category>,
    onAddToCategory: (Video, Category) -> Unit,
    columns: Int = 3
) {
    var actionTarget by remember { mutableStateOf<Video?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(videos, key = { it.id }) { v ->
            Column(
                modifier = Modifier.combinedClickable(
                    onClick = { onClick(v) },
                    onLongClick = {
                        actionTarget = v
                        showActionMenu = true
                    }
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (v.coverPath != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(v.coverPath))
                                // ★ 竖屏比例，与漫画网格保持一致
                                .size(Size(360, 520))
                                .precision(Precision.INEXACT)
                                .crossfade(false)
                                .build(),
                            contentDescription = v.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = v.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatDuration(v.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // ---- 长按操作菜单 ----
    if (showActionMenu && actionTarget != null) {
        val target = actionTarget!!
        AlertDialog(
            onDismissRequest = { showActionMenu = false },
            title = { Text(target.title) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showActionMenu = false
                            renameText = target.title
                            showRenameDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("重命名") }
                    HorizontalDivider()
                    TextButton(
                        onClick = {
                            showActionMenu = false
                            showCategoryPicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("添加到分类") }
                    HorizontalDivider()
                    TextButton(
                        onClick = {
                            showActionMenu = false
                            showDeleteConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("删除视频") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionMenu = false }) { Text("取消") }
            }
        )
    }

    // ---- 重命名 ----
    if (showRenameDialog && actionTarget != null) {
        val target = actionTarget!!
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                actionTarget = null
            },
            title = { Text("重命名视频") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        onRename(target, renameText)
                        showRenameDialog = false
                        actionTarget = null
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    actionTarget = null
                }) { Text("取消") }
            }
        )
    }

    // ---- 选择分类 ----
    if (showCategoryPicker && actionTarget != null) {
        val target = actionTarget!!
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("添加到分类") },
            text = {
                if (categories.isEmpty()) {
                    Text("还没有分类，请先在首页点『+ 新建』创建")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(categories, key = { it.id }) { c ->
                            TextButton(
                                onClick = {
                                    onAddToCategory(target, c)
                                    showCategoryPicker = false
                                    actionTarget = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(c.name) }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showCategoryPicker = false
                    actionTarget = null
                }) { Text("取消") }
            }
        )
    }

    // ---- 删除确认 ----
    if (showDeleteConfirm && actionTarget != null) {
        val target = actionTarget!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除视频") },
            text = { Text("确定删除《${target.title}》吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    showDeleteConfirm = false
                    actionTarget = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    actionTarget = null
                }) { Text("取消") }
            }
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}