package com.aura.player.ui.download

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.player.downloader.YtDownloaderHelper
import com.aura.player.downloader.YtDownloaderHelper.DownloadMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DownloadUiState(
    val url: String = "",
    val mode: DownloadMode = DownloadMode.AUDIO,
    val selectedQuality: String = "320",   // default audio
    val info: YtDownloaderHelper.VideoInfo? = null,
    val isLoadingInfo: Boolean = false,
    val infoError: String? = null,
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val statusText: String = "",
    val downloadDone: Boolean = false,
    val downloadError: String? = null,
    val activeDownloadId: Long = -1L
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState

    private var pollJob: Job? = null

    // ── User actions ────────────────────────────────────────────────────────

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(
            url = url,
            info = null,
            infoError = null,
            downloadDone = false,
            downloadError = null,
            progress = 0,
            statusText = ""
        )
    }

    fun onModeChange(mode: DownloadMode) {
        val defaultQuality = if (mode == DownloadMode.AUDIO) "320" else "1080"
        _uiState.value = _uiState.value.copy(mode = mode, selectedQuality = defaultQuality)
    }

    fun onQualityChange(q: String) {
        _uiState.value = _uiState.value.copy(selectedQuality = q)
    }

    fun fetchInfo() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingInfo = true, infoError = null, info = null)
            val result = YtDownloaderHelper.getInfo(url)
            result.fold(
                onSuccess = { info ->
                    _uiState.value = _uiState.value.copy(isLoadingInfo = false, info = info)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInfo = false,
                        infoError = err.message ?: "Could not fetch info"
                    )
                }
            )
        }
    }

    fun startDownload() {
        val state = _uiState.value
        if (state.isDownloading || state.url.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadDone = false,
                downloadError = null,
                progress = 0,
                statusText = "Resolving URL..."
            )

            val result = YtDownloaderHelper.download(
                context = getApplication(),
                url = state.url.trim(),
                mode = state.mode,
                quality = state.selectedQuality
            )

            if (!result.success) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadError = result.errorMessage
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                activeDownloadId = result.downloadId,
                statusText = "Queued..."
            )
            startPolling(result.downloadId)
        }
    }

    fun reset() {
        pollJob?.cancel()
        _uiState.value = DownloadUiState()
    }

    // ── Progress polling ─────────────────────────────────────────────────────

    private fun startPolling(downloadId: Long) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                val (progress, status) = YtDownloaderHelper.queryProgress(getApplication(), downloadId)
                val done = YtDownloaderHelper.isCompleted(getApplication(), downloadId)

                _uiState.value = _uiState.value.copy(
                    progress = progress,
                    statusText = status,
                    isDownloading = !done,
                    downloadDone = done
                )

                if (done) break
                delay(800)
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
