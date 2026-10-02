package com.readplus.ui.comic.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplus.data.source.ZipArchiveManager
import com.readplus.data.source.ZipImageEntry
import com.readplus.data.source.ZipPage
import com.readplus.domain.model.Comic
import com.readplus.domain.repository.ComicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComicDetailUiState(
    val comic: Comic? = null,
    val pages: List<ZipImageEntry> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class ComicDetailViewModel @Inject constructor(
    private val comicRepo: ComicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val comicId: Long = savedStateHandle.get<Long>("comicId") ?: -1L
    private val _state = MutableStateFlow(ComicDetailUiState())
    val state: StateFlow<ComicDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val comic = comicRepo.getById(comicId)
            val pages = if (comic != null) {
                ZipArchiveManager.listImageEntries(comic.zipPath)
            } else emptyList()
            _state.value = ComicDetailUiState(comic = comic, pages = pages, loading = false)
        }
    }

    fun zipPage(index: Int): ZipPage? {
        val s = _state.value
        val comic = s.comic ?: return null
        val entry = s.pages.getOrNull(index) ?: return null
        return ZipPage(comic.zipPath, entry.entryPath)
    }

    /** 删除当前漫画，完成后调用 onDone 返回上一页 */
    fun delete(onDone: () -> Unit) {
        val comic = _state.value.comic ?: return
        viewModelScope.launch {
            comicRepo.delete(comic)
            onDone()
        }
    }
}