package com.readplus.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readplus.domain.model.Category
import com.readplus.ui.comic.list.ComicGrid
import com.readplus.ui.video.list.VideoGrid
import kotlinx.coroutines.launch

private val COMIC_MIME_TYPES = arrayOf(
    // ZIP 及变体
    "application/zip",
    "application/x-zip-compressed",
    "application/x-zip",
    "multipart/x-zip",
    // PDF
    "application/pdf",
    // MOBI / AZW
    "application/x-mobipocket-ebook",
    "application/vnd.amazon.ebook",
    // EPUB
    "application/epub+zip",
    // CBZ / CBR
    "application/vnd.comicbook+zip",
    "application/vnd.comicbook-rar",
    "application/x-cbz",
    "application/x-cbr",
    "application/x-cbr-archive"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onComicClick: (Long) -> Unit,
    onVideoClick: (Long, Long?) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(pagerState.currentPage)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // ---- 漫画：多选文件 ----
    val comicPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        viewModel.importComics(uris)
    }

    // ---- 漫画：选文件夹批量导入 ----
    val comicFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.importComicFolder(it)
        }
    }

    // ---- 视频：多选文件 ----
    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        viewModel.importVideos(uris)
    }

    // ---- 视频：选文件夹 ----
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.importVideoFolder(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("ReadPlus") },
                actions = {
                    if (state.tab == 0) {
                        // 漫画 Tab：多选文件 + 选文件夹
                        IconButton(onClick = {
                            comicPicker.launch(COMIC_MIME_TYPES)
                        }) {
                            Icon(Icons.Default.Add, "批量导入漫画")
                        }
                        IconButton(onClick = { comicFolderPicker.launch(null) }) {
                            Icon(Icons.Default.Folder, "从文件夹导入漫画")
                        }
                    } else {
                        // 视频 Tab：多选文件 + 选文件夹
                        IconButton(onClick = {
                            videoPicker.launch(arrayOf("video/*"))
                        }) {
                            Icon(Icons.Default.Add, "导入视频")
                        }
                        IconButton(onClick = { folderPicker.launch(null) }) {
                            Icon(Icons.Default.Folder, "授权文件夹")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "设置")
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
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("漫画") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("视频") }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ComicPage(
                        state = state,
                        onComicClick = onComicClick,
                        viewModel = viewModel
                    )
                    1 -> VideoPage(
                        state = state,
                        onVideoClick = onVideoClick,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun ComicPage(
    state: HomeUiState,
    onComicClick: (Long) -> Unit,
    viewModel: HomeViewModel
) {
    Column(Modifier.fillMaxSize()) {
        CategoryBar(
            categories = state.comicCategories,
            selectedId = state.selectedComicCategory,
            onSelect = viewModel::selectComicCategory,
            onCreate = { name -> viewModel.createCategory(name, "COMIC") },
            onDelete = { category -> viewModel.deleteCategory(category.id, "COMIC") }
        )
        if (state.comics.isEmpty()) {
            EmptyHint("还没有漫画，点击右上角导入 ZIP / PDF / MOBI / EPUB / CBZ / CBR")
        } else {
            ComicGrid(
                comics = state.comics,
                onClick = { onComicClick(it.id) },
                onDelete = viewModel::deleteComic,
                onRename = { comic, newTitle ->
                    viewModel.renameComic(comic.id, newTitle)
                },
                categories = state.comicCategories,
                onAddToCategory = { comic, category ->
                    viewModel.addComicToCategory(comic.id, category.id)
                }
            )
        }
    }
}

@Composable
private fun VideoPage(
    state: HomeUiState,
    onVideoClick: (Long, Long?) -> Unit,
    viewModel: HomeViewModel
) {
    Column(Modifier.fillMaxSize()) {
        CategoryBar(
            categories = state.videoCategories,
            selectedId = state.selectedVideoCategory,
            onSelect = viewModel::selectVideoCategory,
            onCreate = { name -> viewModel.createCategory(name, "VIDEO") },
            onDelete = { category -> viewModel.deleteCategory(category.id, "VIDEO") }
        )
        if (state.videos.isEmpty()) {
            EmptyHint("还没有视频，点击右上角导入视频")
        } else {
            VideoGrid(
                videos = state.videos,
                onClick = { video ->
                    onVideoClick(video.id, state.selectedVideoCategory)
                },
                onDelete = viewModel::deleteVideo,
                onRename = { video, newTitle ->
                    viewModel.renameVideo(video.id, newTitle)
                },
                categories = state.videoCategories,
                onAddToCategory = { video, category ->
                    viewModel.addVideoToCategory(video.id, category.id)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryBar(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Category) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(
                name = "全部",
                selected = selectedId == null,
                onClick = { onSelect(null) },
                onLongClick = null
            )
        }
        items(categories, key = { it.id }) { c ->
            CategoryChip(
                name = c.name,
                selected = selectedId == c.id,
                onClick = { onSelect(c.id) },
                onLongClick = { pendingDelete = c }
            )
        }
        item {
            CategoryChip(
                name = "+ 新建",
                selected = false,
                onClick = { showCreateDialog = true },
                onLongClick = null,
                isAssist = true
            )
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建分类") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) onCreate(newName.trim())
                    newName = ""
                    showCreateDialog = false
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
            }
        )
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除分类") },
            text = { Text("确定删除分类「${c.name}」吗？\n分类内的漫画/视频不会被删除，只会解除分类关系。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(c)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    isAssist: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        isAssist -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}