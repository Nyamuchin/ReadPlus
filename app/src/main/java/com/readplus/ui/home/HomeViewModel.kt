package com.readplus.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplus.domain.model.Category
import com.readplus.domain.model.Comic
import com.readplus.domain.model.Video
import com.readplus.domain.repository.CategoryRepository
import com.readplus.domain.repository.ComicRepository
import com.readplus.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val tab: Int = 0,
    val comics: List<Comic> = emptyList(),
    val videos: List<Video> = emptyList(),
    val comicCategories: List<Category> = emptyList(),
    val videoCategories: List<Category> = emptyList(),
    val selectedComicCategory: Long? = null,
    val selectedVideoCategory: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val comicRepo: ComicRepository,
    private val videoRepo: VideoRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val tabState = MutableStateFlow(0)
    private val comicCategoryState = MutableStateFlow<Long?>(null)
    private val videoCategoryState = MutableStateFlow<Long?>(null)

    // 单次消息（Snackbar 用）
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val comicsFlow = comicCategoryState.flatMapLatest { catId ->
        if (catId == null) comicRepo.observeAll() else comicRepo.observeByCategory(catId)
    }

    private val videosFlow = videoCategoryState.flatMapLatest { catId ->
        if (catId == null) videoRepo.observeAll() else videoRepo.observeByCategory(catId)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        tabState,
        combine(comicsFlow, comicCategoryState) { comics, cat -> comics to cat },
        combine(videosFlow, videoCategoryState) { videos, cat -> videos to cat },
        combine(
            categoryRepo.observeByType("COMIC"),
            categoryRepo.observeByType("VIDEO")
        ) { comicCats, videoCats -> comicCats to videoCats }
    ) { tab, (comics, comicCat), (videos, videoCat), (comicCats, videoCats) ->
        HomeUiState(
            tab = tab,
            comics = comics,
            videos = videos,
            comicCategories = comicCats,
            videoCategories = videoCats,
            selectedComicCategory = comicCat,
            selectedVideoCategory = videoCat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectTab(index: Int) {
        tabState.value = index
    }

    fun selectComicCategory(id: Long?) {
        comicCategoryState.value = id
    }

    fun selectVideoCategory(id: Long?) {
        videoCategoryState.value = id
    }

    fun addComicToCategory(comicId: Long, categoryId: Long) = viewModelScope.launch {
        comicRepo.addToCategory(comicId, categoryId)
    }

    fun addVideoToCategory(videoId: Long, categoryId: Long) = viewModelScope.launch {
        videoRepo.addToCategory(videoId, categoryId)
    }

    fun createCategory(name: String, type: String) = viewModelScope.launch {
        categoryRepo.create(name, type)
    }

    fun deleteCategory(id: Long, type: String) = viewModelScope.launch {
        categoryRepo.delete(id)
        if (type == "COMIC" && comicCategoryState.value == id) {
            comicCategoryState.value = null
        } else if (type == "VIDEO" && videoCategoryState.value == id) {
            videoCategoryState.value = null
        }
    }

    fun deleteComic(comic: Comic) = viewModelScope.launch {
        comicRepo.delete(comic)
    }

    fun deleteVideo(video: Video) = viewModelScope.launch {
        videoRepo.delete(video)
    }

    fun importComic(uri: Uri) = viewModelScope.launch {
        val result = comicRepo.importZip(uri)
        _message.value = if (result != null) {
            "导入成功"
        } else {
            "导入失败：请选择有效的 ZIP 文件（.zip）"
        }
    }

    fun importVideos(uris: List<Uri>) = viewModelScope.launch {
        val count = videoRepo.importMultiple(uris)
        _message.value = if (count > 0) "导入 $count 个视频" else "没有可导入的视频"
    }

    fun importVideoFolder(treeUri: Uri) = viewModelScope.launch {
        val count = videoRepo.importFolder(treeUri)
        _message.value = if (count > 0) "导入 $count 个视频" else "该文件夹中没有找到视频"
    }

    fun clearMessage() {
        _message.value = null
    }
}