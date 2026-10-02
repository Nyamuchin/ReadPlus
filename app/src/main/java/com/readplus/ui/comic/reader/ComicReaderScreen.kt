package com.readplus.ui.comic.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readplus.domain.model.ReadingMode

@Composable
fun ComicReaderScreen(
    comicId: Long,
    startPage: Int,
    onBack: () -> Unit,
    viewModel: ComicReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showBar by remember { mutableStateOf(true) }
    var zooming by remember { mutableStateOf(false) }

    val view = LocalView.current
    val window = (view.context as android.app.Activity).window
    val controller = remember(window, view) {
        WindowCompat.getInsetsController(window, view)
    }

    DisposableEffect(Unit) {
        val originalLightStatus = controller.isAppearanceLightStatusBars
        val originalLightNav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        onDispose {
            controller.isAppearanceLightStatusBars = originalLightStatus
            controller.isAppearanceLightNavigationBars = originalLightNav
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(showBar) {
        if (showBar) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when (state.mode) {
            ReadingMode.LEFT_TO_RIGHT -> HorizontalReader(
                state = state,
                reverse = false,
                zooming = zooming,
                onZoomChange = { zooming = it },
                onPageChanged = viewModel::onPageChanged,
                onToggleBar = { showBar = !showBar },
                zipPage = viewModel::zipPage
            )
            ReadingMode.RIGHT_TO_LEFT -> HorizontalReader(
                state = state,
                reverse = true,
                zooming = zooming,
                onZoomChange = { zooming = it },
                onPageChanged = viewModel::onPageChanged,
                onToggleBar = { showBar = !showBar },
                zipPage = viewModel::zipPage
            )
            ReadingMode.VERTICAL_SCROLL -> VerticalReader(
                state = state,
                zooming = zooming,
                onZoomChange = { zooming = it },
                onPageChanged = viewModel::onPageChanged,
                onToggleBar = { showBar = !showBar },
                zipPage = viewModel::zipPage
            )
        }

        ReaderTopBar(
            visible = showBar,
            title = "${state.comic?.title ?: ""}  ${state.currentPage + 1}/${state.pages.size}",
            currentMode = state.mode,
            onBack = onBack,
            onSelectMode = viewModel::setMode,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ReaderBottomBar(
            visible = showBar,
            currentPage = state.currentPage,
            totalPages = state.pages.size,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReaderTopBar(
    visible: Boolean,
    title: String,
    currentMode: ReadingMode,
    onBack: () -> Unit,
    onSelectMode: (ReadingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.92f))
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.Black
                    )
                }
                Text(
                    text = title,
                    color = Color.Black,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    maxLines = 1
                )
                // 关键：用 Box 包裹 IconButton + DropdownMenu，
                // 让菜单锚定到按钮下方
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "菜单",
                            tint = Color.Black
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        ReadingMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = modeLabel(mode),
                                        color = if (mode == currentMode) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    onSelectMode(mode)
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    visible: Boolean,
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 0) return
    val progress = (currentPage + 1).toFloat() / totalPages.toFloat()

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.92f))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${currentPage + 1} / $totalPages",
                color = Color.Black,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun HorizontalReader(
    state: ReaderUiState,
    reverse: Boolean,
    zooming: Boolean,
    onZoomChange: (Boolean) -> Unit,
    onPageChanged: (Int) -> Unit,
    onToggleBar: () -> Unit,
    zipPage: (Int) -> com.readplus.data.source.ZipPage?
) {
    if (state.pages.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = state.currentPage.coerceIn(0, state.pages.size - 1)
    ) { state.pages.size }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    HorizontalPager(
        state = pagerState,
        reverseLayout = reverse,
        userScrollEnabled = !zooming,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        ZoomablePage(
            model = zipPage(page),
            onZoomChange = onZoomChange,
            onSingleTap = onToggleBar
        )
    }
}

@Composable
private fun VerticalReader(
    state: ReaderUiState,
    zooming: Boolean,
    onZoomChange: (Boolean) -> Unit,
    onPageChanged: (Int) -> Unit,
    onToggleBar: () -> Unit,
    zipPage: (Int) -> com.readplus.data.source.ZipPage?
) {
    if (state.pages.isEmpty()) return
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.currentPage.coerceIn(0, state.pages.size - 1)
    )

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onPageChanged(listState.firstVisibleItemIndex)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(state.pages.size) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
            ) {
                ZoomablePage(
                    model = zipPage(index),
                    onZoomChange = onZoomChange,
                    onSingleTap = onToggleBar
                )
            }
        }
    }
}

private fun modeLabel(mode: ReadingMode): String = when (mode) {
    ReadingMode.LEFT_TO_RIGHT -> "从左到右"
    ReadingMode.RIGHT_TO_LEFT -> "从右到左（日漫）"
    ReadingMode.VERTICAL_SCROLL -> "纵向滚动"
}