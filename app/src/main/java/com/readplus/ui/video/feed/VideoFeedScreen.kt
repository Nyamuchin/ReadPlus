package com.readplus.ui.video.feed

import android.graphics.Color as AndroidColor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import coil.load
import com.readplus.R
import com.readplus.domain.model.Video
import kotlinx.coroutines.delay
import java.io.File

// 切页后延迟显示视频的时长（毫秒），确保 TextureView 内部切到新视频
private const val PAGE_SETTLE_DELAY = 150L
// 首帧就绪后的额外等待
private const val FIRST_FRAME_EXTRA_DELAY = 40L

@Composable
fun VideoFeedScreen(
    startVideoId: Long,
    onBack: () -> Unit,
    viewModel: VideoFeedViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val videos = state.videos
    val categoryMap = state.categoryMap
    val context = LocalContext.current

    // 播放进度
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    // ★ 用户主动暂停标志：UI 显隐完全由它驱动，与 ExoPlayer 内部状态解耦
    var userPaused by remember { mutableStateOf(false) }

    // 切页稳定期 & 首帧就绪页
    var pageSettled by remember { mutableStateOf(true) }
    var renderedFirstFramePage by remember { mutableIntStateOf(-1) }
    val currentPageRef = remember { mutableIntStateOf(0) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            setSeekParameters(SeekParameters.EXACT)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                renderedFirstFramePage = currentPageRef.value
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    if (videos.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(8.dp)
                    .statusBarsPadding()
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
            }
        }
        return
    }

    val initialIndex = remember(videos) {
        videos.indexOfFirst { it.id == startVideoId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialIndex) { videos.size }

    // 切页：重置状态 + 换视频
    LaunchedEffect(pagerState.currentPage, videos) {
        val page = pagerState.currentPage
        val video = videos.getOrNull(page) ?: return@LaunchedEffect

        currentPageRef.value = page
        renderedFirstFramePage = -1
        pageSettled = false
        userPaused = false

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        position = 0L
        duration = 0L
        exoPlayer.setMediaItem(MediaItem.fromUri(video.uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        delay(PAGE_SETTLE_DELAY)
        pageSettled = true
    }

    // 进度刷新
    LaunchedEffect(Unit) {
        while (true) {
            if (!isDragging) {
                position = exoPlayer.currentPosition.coerceAtLeast(0L)
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(80)
        }
    }

    val currentVideo = videos.getOrNull(pagerState.currentPage)
    val currentCategories = currentVideo?.let { categoryMap[it.id] }.orEmpty()

    val sliderValue = when {
        isDragging -> dragFraction
        duration > 0 -> (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }

    val displayMs = if (isDragging) (dragFraction * duration).toLong() else position

    // 用户点击视频区域：切换暂停/播放
    val togglePlayPause: () -> Unit = {
        userPaused = !userPaused
        if (userPaused) exoPlayer.pause() else exoPlayer.play()
    }

    // 点击中央播放按钮：恢复播放
    val resumePlay: () -> Unit = {
        userPaused = false
        exoPlayer.play()
    }

    // UI 显隐条件：由 userPaused 驱动，不受 ExoPlayer 状态影响
    val showPausedUi = userPaused && !isDragging

    // ============================================================
    // 沉浸式系统栏控制
    // - 播放中（userPaused=false）→ 隐藏状态栏+导航栏，全屏沉浸
    // - 暂停/拖拽中 → 显示系统栏
    // - 离开页面 → 恢复系统栏
    // ============================================================
    val view = LocalView.current
    val window = (view.context as android.app.Activity).window
    val insetsController = remember(window, view) {
        WindowCompat.getInsetsController(window, view)
    }

    DisposableEffect(showPausedUi) {
        if (showPausedUi) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose { }
    }

    // 离开视频页时，无条件恢复系统栏显示
    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val shouldShowVideo = (page == pagerState.currentPage) &&
                    pageSettled &&
                    (renderedFirstFramePage == page)
            VideoPage(
                video = videos[page],
                isCurrentPage = page == pagerState.currentPage,
                shouldShowVideo = shouldShowVideo,
                exoPlayer = exoPlayer,
                onTogglePlayPause = togglePlayPause
            )
        }

        // 中央播放按钮
        AnimatedVisibility(
            visible = showPausedUi,
            enter = fadeIn(tween(160)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(160)
            ),
            exit = fadeOut(tween(160)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(160)
            ),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { resumePlay() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        // 顶部信息
        AnimatedVisibility(
            visible = showPausedUi && currentVideo != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(160)),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            VideoTopInfo(
                title = currentVideo?.title ?: "",
                categories = currentCategories
            )
        }

        // 底部进度条
        VideoProgressBar(
            expanded = showPausedUi,
            sliderValue = sliderValue,
            currentMs = displayMs,
            durationMs = duration,
            onDrag = { fraction ->
                isDragging = true
                dragFraction = fraction
                if (duration > 0) {
                    exoPlayer.seekTo((fraction * duration).toLong())
                }
            },
            onDragFinished = {
                if (duration > 0) {
                    exoPlayer.seekTo((dragFraction * duration).toLong())
                }
                isDragging = false
            },
            onTapSeek = { fraction ->
                if (duration > 0) {
                    val target = (fraction * duration).toLong()
                    exoPlayer.seekTo(target)
                    position = target
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(4.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
        }
    }
}

/**
 * 单页视频：
 *   - PlayerView 用 alpha 控制显隐（alpha=0 时仍渲染，能触发 onRenderedFirstFrame）
 *   - 封面 ImageView 带黑底，alpha 控制显隐
 *   - 通过 remember 缓存已加载的封面路径，避免重复 load
 *   - 通过 lastTargetAlpha 判断避免重复启动动画
 */
@Composable
private fun VideoPage(
    video: Video,
    isCurrentPage: Boolean,
    shouldShowVideo: Boolean,
    exoPlayer: ExoPlayer,
    onTogglePlayPause: () -> Unit
) {
    var revealVideo by remember(video.id) { mutableStateOf(false) }
    // 缓存已加载的封面路径，避免每次 update 都调用 load
    val loadedCoverPath = remember(video.id) { mutableStateOf<String?>(null) }
    // 记录上一次的目标 alpha，避免重复启动动画
    val lastTargetAlpha = remember(video.id) { mutableFloatStateOf(-1f) }

    LaunchedEffect(shouldShowVideo) {
        if (shouldShowVideo) {
            delay(FIRST_FRAME_EXTRA_DELAY)
            revealVideo = true
        } else {
            revealVideo = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    val playerView = LayoutInflater.from(ctx)
                        .inflate(R.layout.view_player, this, false) as PlayerView
                    playerView.setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                    playerView.alpha = 0f
                    playerView.setOnClickListener { onTogglePlayPause() }
                    addView(playerView)

                    val coverView = ImageView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setBackgroundColor(AndroidColor.BLACK)
                        alpha = 1f
                    }
                    addView(coverView)
                }
            },
            update = { frame ->
                val playerView = frame.getChildAt(0) as PlayerView
                val coverView = frame.getChildAt(1) as ImageView

                // 1. 绑定 player
                val desiredPlayer = if (isCurrentPage) exoPlayer else null
                if (playerView.player !== desiredPlayer) {
                    playerView.player = desiredPlayer
                }

                // 2. PlayerView alpha
                val targetPlayerAlpha = if (revealVideo) 1f else 0f
                if (playerView.alpha != targetPlayerAlpha) {
                    playerView.alpha = targetPlayerAlpha
                }

                // 3. 封面加载：只在路径变化时调用，避免重复
                video.coverPath?.let { path ->
                    if (loadedCoverPath.value != path) {
                        coverView.load(File(path))
                        loadedCoverPath.value = path
                    }
                }

                // 4. 封面 alpha 动画：避免重复启动
                val targetCoverAlpha = if (revealVideo) 0f else 1f
                if (lastTargetAlpha.floatValue != targetCoverAlpha) {
                    lastTargetAlpha.floatValue = targetCoverAlpha
                    if (targetCoverAlpha == 0f) {
                        coverView.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .start()
                    } else {
                        coverView.animate().cancel()
                        coverView.alpha = 1f
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun VideoTopInfo(title: String, categories: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .statusBarsPadding()
            .padding(start = 56.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "分类：" + categories.joinToString("、"),
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VideoProgressBar(
    expanded: Boolean,
    sliderValue: Float,
    currentMs: Long,
    durationMs: Long,
    onDrag: (Float) -> Unit,
    onDragFinished: () -> Unit,
    onTapSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackHeight = if (expanded) 4.dp else 2.dp
    val touchHeight = if (expanded) 28.dp else 16.dp
    val trackHeightPx = with(LocalDensity.current) { trackHeight.toPx() }
    val thumbRadiusPx = with(LocalDensity.current) { 7.dp.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (expanded) Color.Black.copy(alpha = 0.55f) else Color.Transparent
            )
            .navigationBarsPadding()
            .padding(
                horizontal = if (expanded) 16.dp else 0.dp,
                vertical = if (expanded) 10.dp else 0.dp
            )
            .padding(bottom = if (expanded) 0.dp else 12.dp)
    ) {
        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMs(currentMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatMs(durationMs),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(touchHeight)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onDrag(fraction)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            onDrag(fraction)
                        },
                        onDragEnd = { onDragFinished() },
                        onDragCancel = { onDragFinished() }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onTapSeek(fraction)
                        }
                    )
                }
        ) {
            val centerY = size.height / 2f
            val progress = sliderValue.coerceIn(0f, 1f)
            val progressWidth = size.width * progress

            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                size = Size(size.width, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2f)
            )
            if (progressWidth > 0f) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                    size = Size(progressWidth, trackHeightPx),
                    cornerRadius = CornerRadius(trackHeightPx / 2f)
                )
            }
            if (expanded) {
                val thumbCenterX = progressWidth
                    .coerceIn(thumbRadiusPx, size.width - thumbRadiusPx)
                drawCircle(
                    color = Color.White,
                    radius = thumbRadiusPx,
                    center = Offset(thumbCenterX, centerY)
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSec = safeMs / 1000
    val millisPart = (safeMs % 1000).toInt()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        "%d:%02d:%02d.%03d".format(h, m, s, millisPart)
    } else {
        "%02d:%02d.%03d".format(m, s, millisPart)
    }
}