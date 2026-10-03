package com.readplus.ui.comic.detail

import android.net.Uri
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
    val loading: Boolean = true,
    val message: String? = null
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
            // 直接观察数据库，封面变更后 UI 自动刷新
            comicRepo.observeById(comicId).collect { comic ->
                val pages = if (comic != null && comic.zipPath.isNotBlank()) {
                    runCatching {
                        ZipArchiveManager.listImageEntries(comic.zipPath)
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
                _state.value = _state.value.copy(
                    comic = comic,
                    pages = pages,
                    loading = false
                )
            }
        }
    }

    fun zipPage(index: Int): ZipPage? {
        val s = _state.value
        val comic = s.comic ?: return null
        val entry = s.pages.getOrNull(index) ?: return null
        return ZipPage(comic.zipPath, entry.entryPath)
    }

    /** 从漫画内部某一页设置封面 */
    fun setCoverFromPage(pageIndex: Int) = viewModelScope.launch {
        val ok = comicRepo.setCoverFromPage(comicId, pageIndex)
        _state.value = _state.value.copy(
            message = if (ok) "封面已更新" else "设置失败"
        )
    }

    /** 从外部图片设置封面 */
    fun setCoverFromUri(uri: Uri) = viewModelScope.launch {
        val ok = comicRepo.setCoverFromUri(comicId, uri)
        _state.value = _state.value.copy(
            message = if (ok) "封面已更新" else "设置失败，请确认文件是图片"
        )
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
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