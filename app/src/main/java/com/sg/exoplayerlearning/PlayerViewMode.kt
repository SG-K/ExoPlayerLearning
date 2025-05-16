package com.sg.exoplayerlearning

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sg.exoplayerlearning.models.ActionType
import com.sg.exoplayerlearning.models.PlayerAction
import com.sg.exoplayerlearning.models.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class PlayerViewModel: ViewModel() {

    companion object {
        // Source for videos: https://gist.github.com/jsturgis/3b19447b304616f18657
        const val Video_1 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        const val Video_2 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        const val Video_3 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        const val Video_4 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    }

    private val _playerState = MutableStateFlow<ExoPlayer?>(null)
    val playerState: StateFlow<ExoPlayer?> = _playerState

    private val hashMapVideoStates = mutableMapOf<String,VideoItem>()

    fun createPlayerWithMediaItems(context: Context,) {
        if (_playerState.value == null) {
            // Create Media items list
            val mediaItems = listOf(
                MediaItem.Builder().setUri(Video_1).setMediaId("Video_1").build(),
                MediaItem.Builder().setUri(Video_2).setMediaId("Video_2").build(),
                MediaItem.Builder().setUri(Video_3).setMediaId("Video_3").build(),
                MediaItem.Builder().setUri(Video_4).setMediaId("Video_4").build(),
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

        }
    }

    fun updateCurrentPosition(id: String, position: Long) {
        hashMapVideoStates[id] = hashMapVideoStates[id]?.copy(currentPosition = position)
            ?: VideoItem(currentPosition = position)
    }

    fun executeAction(playerAction: PlayerAction) {
        when(playerAction.actionType) {
            ActionType.PLAY -> _playerState.value?.play()
            ActionType.PAUSE -> _playerState.value?.pause()
            ActionType.REWIND -> _playerState.value?.rewind()
            ActionType.FORWARD -> _playerState.value?.forward()
            ActionType.NEXT -> _playerState.value?.playNext()
            ActionType.PREVIOUS -> _playerState.value?.playPrevious()
        }
    }

    private fun ExoPlayer.rewind() {
        val newPosition = (currentPosition - 10_000).coerceAtLeast(0)
        seekTo(newPosition)
    }

    private fun ExoPlayer.forward() {
        val newPosition = (currentPosition + 10_000)
            .coerceAtMost(duration)
        seekTo(newPosition)
    }

    private fun ExoPlayer.playNext() {
        if (hasNextMediaItem()) {
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
            val previousIndex = currentMediaItemIndex - 1
            val mediaItemId = getMediaItemAt(previousIndex)
            val seekPosition = hashMapVideoStates[mediaItemId.mediaId]?.currentPosition ?: 0L
            seekTo(previousIndex, seekPosition)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _playerState.value?.release()
    }
}