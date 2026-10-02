package com.readplus.ui.comic.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage

@Composable
fun ZoomablePage(
    model: Any?,
    onZoomChange: (Boolean) -> Unit = {},
    onSingleTap: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
        onZoomChange(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            // 单击 / 双击手势（不消费滑动事件）
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            reset()
                        } else {
                            scale = 2.5f
                            onZoomChange(true)
                        }
                    }
                )
            }
            // 缩放/拖动：只在多指或已放大时才消费事件
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        // 关键判断：
                        // 单指 + 未缩放 → 不消费，让 Pager 处理翻页
                        // 多指 或 已放大 → 消费，用于缩放/拖动
                        val shouldConsume = pressedCount > 1 || scale > 1f

                        if (shouldConsume) {
                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)

                            if (newScale > 1f) {
                                val maxX = (containerSize.width * (newScale - 1f)) / 2f
                                val maxY = (containerSize.height * (newScale - 1f)) / 2f
                                val newOffset = Offset(
                                    (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                    (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                )
                                scale = newScale
                                offset = newOffset
                                onZoomChange(true)
                            } else {
                                // 缩放回 1x，复位
                                scale = 1f
                                offset = Offset.Zero
                                onZoomChange(false)
                            }

                            // 消费事件，防止 Pager 收到
                            event.changes.forEach {
                                if (it.positionChanged()) it.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale, scaleY = scale,
                    translationX = offset.x, translationY = offset.y
                )
        )
    }
}