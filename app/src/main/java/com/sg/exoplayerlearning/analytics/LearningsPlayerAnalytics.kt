package com.sg.exoplayerlearning.analytics

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime

@UnstableApi
class LearningsPlayerAnalytics(
    private val exoPlayer: ExoPlayer?
): AnalyticsListener {

    @OptIn(UnstableApi::class)
    override fun onPlayerReleased(eventTime: EventTime) {
        super.onPlayerReleased(eventTime)
        logEvent("Player Released")
    }
    override fun onPlaybackStateChanged(
        eventTime: EventTime,
        state: Int,
    ) {
        val itemId = currentMediaItemTag()
        when (state) {
            Player.STATE_READY -> logEvent("Playback Ready time = ${eventTime.realtimeMs} and itemId = $itemId")
            Player.STATE_ENDED -> logEvent("Playback Ended time = ${eventTime.realtimeMs} and itemId = $itemId")
            Player.STATE_BUFFERING -> logEvent("Buffering time = ${eventTime.realtimeMs} and itemId = $itemId")
            Player.STATE_IDLE -> logEvent("Player Idle time = ${eventTime.realtimeMs} and itemId = $itemId")
        }
    }

    override fun onPlayerError(
        eventTime: EventTime,
        error: PlaybackException,
    ) {
        currentMediaItemTag().let { itemId -> logEvent("Playback Error: ${error.message}  time = ${eventTime.realtimeMs} and itemId = $itemId") }
    }

    override fun onIsPlayingChanged(eventTime: EventTime, isPlaying: Boolean) {
        currentMediaItemTag().let { itemId ->
            logEvent("Is Playing: $isPlaying time = ${eventTime.realtimeMs} and itemId = $itemId")
        }
    }

    override fun onMediaItemTransition(eventTime: EventTime, mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(eventTime, mediaItem, reason)
        logEvent("Media Item transitioned, reason = $reason and itemId = ${mediaItem?.mediaId}")
    }

    private fun currentMediaItemTag(): String? = exoPlayer?.currentMediaItem?.localConfiguration?.tag as? String

}

fun logEvent(value: String) {
    Log.d("SivaGanesh_Debug", "PlayerViewModel logEvent 153: value = $value");
}