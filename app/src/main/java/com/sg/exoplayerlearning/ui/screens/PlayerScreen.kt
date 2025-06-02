package com.sg.exoplayerlearning.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import com.sg.exoplayerlearning.PlayerViewModel
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.sg.exoplayerlearning.models.ActionType
import com.sg.exoplayerlearning.models.PlayerAction
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerRoute(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = viewModel(),
) {
    val exoPlayer = playerViewModel.playerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier.fillMaxSize()) {
        exoPlayer.value?.let {
            PlayerScreen(exoPlayer = it, playerActions = playerViewModel::executeAction)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> playerViewModel.executeAction(PlayerAction(ActionType.PAUSE))
                Lifecycle.Event.ON_RESUME -> playerViewModel.executeAction(PlayerAction(ActionType.PLAY))
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        playerViewModel.createPlayerWithMediaItems(context)

        while (true) {
            exoPlayer.value?.currentMediaItem?.mediaId?.let {
                playerViewModel.updateCurrentPosition(
                    it, exoPlayer.value?.currentPosition ?: 0, exoPlayer.value?.duration ?: 0
                )
            }
            delay(1000)
        }
    }
}

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    exoPlayer: ExoPlayer,
    playerActions: (PlayerAction) -> Unit,
) {
    Box(
        modifier = modifier
    ) {
        PlayerSurface(
            player = exoPlayer,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .align(Alignment.Center)
        )

        VideoControls(exoPlayer, playerActions)
    }
}

@Composable
fun VideoControls(
    player: ExoPlayer,
    playerActions: (PlayerAction) -> Unit,
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var formatedTime by remember { mutableStateOf("") }
    var isBuffering by remember { mutableStateOf(player.isLoading) }
    var controlsVisible by remember { mutableStateOf(true) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    if (controlsVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { controlsVisible = !controlsVisible },
            contentAlignment = Alignment.Center
        ) {
            ShowButtonControllers(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                playerActions = playerActions,
                isPlayerPlaying = {
                    player.isPlaying
                }
            )

            TimelineControllers(
                modifier = Modifier.align(Alignment.BottomStart),
                duration = duration,
                playerPosition = position,
                formatedTime = formatedTime,
                seeking = { isSeeking = it }
            ) {
                playerActions(PlayerAction(ActionType.SEEK, it))
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { controlsVisible = true }
        )
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                duration = player.duration.coerceAtLeast(0L)
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
        }
    }

    // Auto-hide controls after 3s
    LaunchedEffect(isPlaying, controlsVisible, isSeeking) {
        if (isPlaying && controlsVisible) {
            delay(3000)
            if (isSeeking.not()) {
                controlsVisible = false
            }
        }
    }

    LaunchedEffect(player, isSeeking) {
        while (isActive) {
            if (player.isPlaying && isSeeking.not()) {
                position = player.currentPosition
                if (player.duration != duration) {
                    duration = player.duration
                }
            }
            formatedTime = "${formatTime(position)} : ${formatTime(duration)}"
            delay(500)
        }
    }
}

@Composable
fun ShowButtonControllers(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isPlayerPlaying: () -> Boolean,
    playerActions: (PlayerAction) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier.fillMaxWidth()
    ) {
        IconButton(onClick = {
            playerActions(PlayerAction(ActionType.PREVIOUS))
        }) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        IconButton(onClick = {
            playerActions(PlayerAction(ActionType.REWIND))
        }) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Rewind 10 seconds",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Row (
            modifier = Modifier.size(48.dp)
        ) {
            AnimatedVisibility(visible = isBuffering) {
                CircularProgressIndicator(
                    Modifier.size(48.dp)
                )
            }

            AnimatedVisibility(isBuffering.not()) {
                IconButton(onClick = {
                    playerActions(PlayerAction(if (isPlayerPlaying()) ActionType.PAUSE else ActionType.PLAY))
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        IconButton(onClick = {
            playerActions(PlayerAction(ActionType.FORWARD))
        }) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Forward 10 seconds",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        IconButton(onClick = {
            playerActions(PlayerAction(ActionType.NEXT))
        }) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineControllers(
    modifier: Modifier = Modifier,
    playerPosition: Long,
    duration: Long,
    seeking: (Boolean) -> Unit,
    formatedTime: String,
    seekPlayerToPosition:(Long) -> Unit,
) {

    var position by remember { mutableLongStateOf(0L) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = position.toFloat().coerceAtMost(duration.toFloat()),
            onValueChange = {
                seeking(true)
                position = it.toLong()
            },
            onValueChangeFinished = {
                seekPlayerToPosition(position)
                seeking(false)
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier
                .weight(1f) // Takes all the space except what Text needs
                .padding(end = 8.dp), // small space between slider and text
            colors = SliderDefaults.colors(
                thumbColor = Color.Red,
                activeTrackColor = Color.Red
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.Red, shape = CircleShape)
                )
            },
            track = {
                val fraction = if (duration == 0L) 0f else (position.toFloat().coerceAtMost(duration.toFloat()) / duration).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(1.5.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .background(Color.Red, shape = RoundedCornerShape(1.5.dp))
                    )
                }
            }
        )

       androidx.compose.animation.AnimatedVisibility(visible = formatedTime.isNotEmpty()) {
            Text(
                text = formatedTime,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }


    LaunchedEffect(playerPosition) {
        position = playerPosition
    }

}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
