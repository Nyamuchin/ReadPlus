package com.readplus.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.readplus.ui.comic.detail.ComicDetailScreen
import com.readplus.ui.comic.reader.ComicReaderScreen
import com.readplus.ui.home.HomeScreen
import com.readplus.ui.settings.SettingsScreen
import com.readplus.ui.video.feed.VideoFeedScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val COMIC_DETAIL = "comic_detail/{comicId}"
    const val COMIC_READER = "comic_reader/{comicId}?startPage={startPage}"

    // categoryId：-1L 表示"全部视频"，其它值表示分类 id
    const val VIDEO_FEED = "video_feed/{videoId}?categoryId={categoryId}"

    fun comicDetail(id: Long) = "comic_detail/$id"
    fun comicReader(id: Long, startPage: Int = 0) = "comic_reader/$id?startPage=$startPage"
    fun videoFeed(id: Long, categoryId: Long?) =
        "video_feed/$id?categoryId=${categoryId ?: -1L}"
}

// ============================================================
// 动效参数
// ============================================================
private const val SLIDE_DURATION = 500
private const val FADE_DURATION = 380

private val elegantEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

@Composable
fun ReadPlusNavHost() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Routes.HOME
    ) {
        composable(
            route = Routes.HOME,
            enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION)) },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromLeft() },
            popExitTransition = { fadeOut(animationSpec = tween(FADE_DURATION)) }
        ) {
            HomeScreen(
                onComicClick = { nav.navigate(Routes.comicDetail(it)) },
                onVideoClick = { videoId, categoryId ->
                    nav.navigate(Routes.videoFeed(videoId, categoryId))
                },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.SETTINGS,
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromRight() },
            popExitTransition = { slideOutToRight() }
        ) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.COMIC_DETAIL,
            arguments = listOf(navArgument("comicId") { type = NavType.LongType }),
            enterTransition = { slideInFromRight() },
            exitTransition = { slideOutToLeft() },
            popEnterTransition = { slideInFromTop() },
            popExitTransition = { slideOutToRight() }
        ) { entry ->
            val id = entry.arguments?.getLong("comicId") ?: return@composable
            ComicDetailScreen(
                comicId = id,
                onBack = { nav.popBackStack() },
                onReadFrom = { page -> nav.navigate(Routes.comicReader(id, page)) }
            )
        }

        composable(
            route = Routes.COMIC_READER,
            arguments = listOf(
                navArgument("comicId") { type = NavType.LongType },
                navArgument("startPage") { type = NavType.IntType; defaultValue = 0 }
            ),
            enterTransition = { slideInFromBottom() },
            exitTransition = { slideOutToBottom() },
            popEnterTransition = { slideInFromBottom() },
            popExitTransition = { slideOutToBottom() }
        ) { entry ->
            val id = entry.arguments?.getLong("comicId") ?: return@composable
            val start = entry.arguments?.getInt("startPage") ?: 0
            ComicReaderScreen(
                comicId = id,
                startPage = start,
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.VIDEO_FEED,
            arguments = listOf(
                navArgument("videoId") { type = NavType.LongType },
                navArgument("categoryId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            enterTransition = { slideInFromBottom() },
            exitTransition = { slideOutToBottom() },
            popEnterTransition = { slideInFromBottom() },
            popExitTransition = { slideOutToBottom() }
        ) { entry ->
            val videoId = entry.arguments?.getLong("videoId") ?: return@composable
            VideoFeedScreen(
                startVideoId = videoId,
                onBack = { nav.popBackStack() }
            )
        }
    }
}

// ============================================================
// 动画构造器
// ============================================================

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromRight() =
    slideIntoContainer(
        towards = SlideDirection.Left,
        animationSpec = tween(SLIDE_DURATION, easing = elegantEasing)
    ) + fadeIn(animationSpec = tween(FADE_DURATION))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToLeft() =
    slideOutOfContainer(
        towards = SlideDirection.Left,
        animationSpec = tween(SLIDE_DURATION, easing = elegantEasing),
        targetOffset = { fullWidth -> fullWidth / 6 }
    ) + fadeOut(animationSpec = tween(FADE_DURATION))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromLeft() =
    slideIntoContainer(
        towards = SlideDirection.Right,
        animationSpec = tween(SLIDE_DURATION, easing = elegantEasing),
        initialOffset = { fullWidth -> fullWidth / 6 }
    ) + fadeIn(animationSpec = tween(FADE_DURATION))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToRight() =
    slideOutOfContainer(
        towards = SlideDirection.Right,
        animationSpec = tween(SLIDE_DURATION, easing = elegantEasing)
    ) + fadeOut(animationSpec = tween(FADE_DURATION))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromBottom() =
    slideIntoContainer(
        towards = SlideDirection.Up,
        animationSpec = tween(SLIDE_DURATION, easing = elegantEasing)
    ) + fadeIn(animationSpec = tween(FADE_DURATION))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToBottom() =
    slideOutOfContainer(
        towards = SlideDirection.Down,
        animationSpec = tween(SLIDE_DURATION, easing = elegantEasing)
    ) + fadeOut(animationSpec = tween(FADE_DURATION))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromTop() =
    slideIntoContainer(
        towards = SlideDirection.Down,
        animationSpec = tween(SLIDE_DURATION, easing = elegantEasing),
        initialOffset = { fullHeight -> fullHeight / 10 }
    ) + fadeIn(animationSpec = tween(FADE_DURATION))