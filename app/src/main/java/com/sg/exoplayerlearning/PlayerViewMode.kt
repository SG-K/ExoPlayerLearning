package com.sg.exoplayerlearning

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime
import com.sg.exoplayerlearning.analytics.LearningsPlayerAnalytics
import com.sg.exoplayerlearning.analytics.logEvent
import com.sg.exoplayerlearning.models.ActionType
import com.sg.exoplayerlearning.models.PlayerAction
import com.sg.exoplayerlearning.models.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@UnstableApi
@HiltViewModel
class PlayerViewModel: ViewModel() {

    companion object {
        // Source for videos: https://gist.github.com/jsturgis/3b19447b304616f18657
        const val Video_1 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        const val Video_2 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        const val Video_3 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        const val Video_4 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    }

    //region Variables
    private val _playerState = MutableStateFlow<ExoPlayer?>(null)
    val playerState: StateFlow<ExoPlayer?> = _playerState

    private val hashMapVideoStates = mutableMapOf<String,VideoItem>()
    private lateinit var analytics: LearningsPlayerAnalytics

    fun createPlayerWithMediaItems(context: Context) {
        if (_playerState.value == null) {
            // Create Media items list
            val mediaItems = listOf(
                MediaItem.Builder().setUri(Video_1).setMediaId("Video_1").setTag("Video_1").build(),
                MediaItem.Builder().setUri(Video_2).setMediaId("Video_2").setTag("Video_2").build(),
                MediaItem.Builder().setUri(Video_3).setMediaId("Video_3").setTag("Video_3").build(),
                MediaItem.Builder().setUri(Video_4).setMediaId("Video_4").setTag("Video_4").build(),
            )

            // Create hashmap with video items to persist current playing position when shifting between videos
            mediaItems.forEach {
                hashMapVideoStates[it.mediaId] = VideoItem()
            }

            // Create the player instance and update it to UI via stateFlow
            _playerState.update {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItems(mediaItems)
                    prepare()
                    playWhenReady = true
                    play()
                }
            }

            trackMediaItemTransitions()
            addAnalytics()
        }
    }
    //endregion

    //region User actions
    fun executeAction(playerAction: PlayerAction) {
        when(playerAction.actionType) {
            ActionType.PLAY -> {
                logEvent("Action Play")
                _playerState.value?.play()
            }
            ActionType.PAUSE -> {
                logEvent("Action Pause")
                _playerState.value?.pause()
            }
            ActionType.REWIND -> _playerState.value?.rewind()
            ActionType.FORWARD -> _playerState.value?.forward()
            ActionType.NEXT -> _playerState.value?.playNext()
            ActionType.PREVIOUS -> _playerState.value?.playPrevious()
            ActionType.SEEK -> _playerState.value?.seekWithValidation(playerAction.data as? Long)
        }
    }

    private fun ExoPlayer.seekWithValidation(position: Long?) {
        logEvent("seeking")
        position?.let {
            seekTo(position)
        }
    }

    private fun ExoPlayer.rewind() {
        logEvent("Action rewind")
        val newPosition = (currentPosition - 10_000).coerceAtLeast(0)
        seekTo(newPosition)
    }

    private fun ExoPlayer.forward() {
        logEvent("Action forward")
        val newPosition = (currentPosition + 10_000)
            .coerceAtMost(duration)
        seekTo(newPosition)
    }

    private fun ExoPlayer.playNext() {
        if (hasNextMediaItem()) {
            logEvent("Action next")
            val nextIndex = currentMediaItemIndex + 1
            val mediaItemId = getMediaItemAt(nextIndex)
            val seekPosition = hashMapVideoStates[mediaItemId.mediaId]?.currentPosition ?: 0L
            seekTo(nextIndex, seekPosition)
        }
    }

    private fun ExoPlayer.playPrevious() {
        if (
            isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM) &&
            hasPreviousMediaItem()
        ) {
            logEvent("Action previous")
            val previousIndex = currentMediaItemIndex - 1
            val mediaItemId = getMediaItemAt(previousIndex)
            val seekPosition = hashMapVideoStates[mediaItemId.mediaId]?.currentPosition ?: 0L
            seekTo(previousIndex, seekPosition)
        }
    }

    private fun currentMediaItemTag(): String? = _playerState.value?.currentMediaItem?.localConfiguration?.tag as? String
    //endregion

    //region player listeners
    private fun trackMediaItemTransitions() {
        _playerState.value?.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _playerState.value?.currentMediaItemIndex?.let {
                        checkAndResetPreviousMediaItemProgress(it)
                    }
                }
            }
        )
    }

    private fun addAnalytics() {
        if (::analytics.isInitialized.not()) {
            analytics = LearningsPlayerAnalytics(_playerState.value)
        }
        _playerState.value?.addAnalyticsListener(analytics)
    }
    // endregion

    //region Player position updates
    private fun checkAndResetPreviousMediaItemProgress(currentMediaItemIndex: Int) {
        val previousIndex = currentMediaItemIndex - 1
        if (previousIndex >= 0) {
            _playerState.value?.getMediaItemAt(previousIndex)?.let { previousMediaItem ->
                hashMapVideoStates[previousMediaItem.mediaId]?.let { previousVideoItem ->
                    if (previousVideoItem.duration - previousVideoItem.currentPosition <= 3000) {
                        hashMapVideoStates[previousMediaItem.mediaId] = previousVideoItem.copy(currentPosition = 0)
                    }
                }
            }
        }
    }

    fun updateCurrentPosition(id: String, position: Long, duration: Long) {
        hashMapVideoStates[id] = hashMapVideoStates[id]?.copy(currentPosition = position, duration = duration)
            ?: VideoItem(currentPosition = position, duration = duration)
    }
    //endregion

    override fun onCleared() {
        super.onCleared()
        _playerState.value?.release()
    }
}