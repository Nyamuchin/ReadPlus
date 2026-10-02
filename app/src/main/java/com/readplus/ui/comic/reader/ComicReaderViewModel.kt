package com.readplus.ui.comic.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplus.data.source.ZipArchiveManager
import com.readplus.data.source.ZipImageEntry
import com.readplus.data.source.ZipPage
import com.readplus.domain.model.Comic
import com.readplus.domain.model.ReadingMode
import com.readplus.domain.repository.ComicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val comic: Comic? = null,
    val pages: List<ZipImageEntry> = emptyList(),
    val currentPage: Int = 0,
    val mode: ReadingMode = ReadingMode.LEFT_TO_RIGHT,
    val loading: Boolean = true
)

@HiltViewModel
class ComicReaderViewModel @Inject constructor(
    private val comicRepo: ComicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val comicId: Long = savedStateHandle.get<Long>("comicId") ?: -1L
    private val startPage: Int = savedStateHandle.get<Int>("startPage") ?: 0

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val comic = comicRepo.getById(comicId)
            val pages = if (comic != null)
                ZipArchiveManager.listImageEntries(comic.zipPath)
            else emptyList()
            val start = startPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            _state.value = ReaderUiState(
                comic = comic,
                pages = pages,
                currentPage = start,
                loading = false
            )
        }
    }

    fun setMode(mode: ReadingMode) {
        _state.value = _state.value.copy(mode = mode)
    }

    fun onPageChanged(index: Int) {
        if (index == _state.value.currentPage) return
        _state.value = _state.value.copy(currentPage = index)
        viewModelScope.launch { comicRepo.updateProgress(comicId, index) }
    }

    fun zipPage(index: Int): ZipPage? {
        val s = _state.value
        val comic = s.comic ?: return null
        val entry = s.pages.getOrNull(index) ?: return null
        return ZipPage(comic.zipPath, entry.entryPath)
    }
}