package com.readplus.ui.comic.list

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size
import com.readplus.domain.model.Category
import com.readplus.domain.model.Comic
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComicGrid(
    comics: List<Comic>,
    onClick: (Comic) -> Unit,
    onDelete: (Comic) -> Unit,
    categories: List<Category>,
    onAddToCategory: (Comic, Category) -> Unit,
    columns: Int = 3
) {
    var actionTarget by remember { mutableStateOf<Comic?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(comics, key = { it.id }) { comic ->
            Column(
                modifier = Modifier.combinedClickable(
                    onClick = { onClick(comic) },
                    onLongClick = {
                        actionTarget = comic
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
                    if (comic.coverPath != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(File(comic.coverPath))
                                // ★ 关键：直接告诉 Coil 目标尺寸，避免解码原图
                                .size(Size(360, 520))
                                .precision(Precision.INEXACT)
                                .crossfade(false)
                                .build(),
                            contentDescription = comic.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${comic.pageCount} 页",
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
                    ) { Text("删除漫画") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionMenu = false }) { Text("取消") }
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
            title = { Text("删除漫画") },
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