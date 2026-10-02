package com.readplus.ui.video.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplus.domain.model.Video
import com.readplus.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoFeedState(
    val videos: List<Video> = emptyList(),
    val categoryMap: Map<Long, List<String>> = emptyMap()
)

@HiltViewModel
class VideoFeedViewModel @Inject constructor(
    private val videoRepo: VideoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val startId: Long = savedStateHandle.get<Long>("videoId") ?: -1L
    // -1L 表示"全部"，其它值表示分类 id
    private val categoryId: Long = savedStateHandle.get<Long>("categoryId") ?: -1L

    private val _state = MutableStateFlow(VideoFeedState())
    val state: StateFlow<VideoFeedState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val flow = if (categoryId == -1L) {
                videoRepo.observeAll()
            } else {
                videoRepo.observeByCategory(categoryId)
            }
            flow.collect { list ->
                val catMap = videoRepo.getCategoryNamesByVideo()
                _state.value = VideoFeedState(videos = list, categoryMap = catMap)
            }
        }
    }
}