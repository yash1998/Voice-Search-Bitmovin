package com.example.firetv

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnErrorListener
import com.bitmovin.player.api.event.listener.OnPausedListener
import com.bitmovin.player.api.event.listener.OnPlayListener
import com.bitmovin.player.config.PlaybackConfiguration
import com.bitmovin.player.config.PlayerConfiguration
import com.bitmovin.player.config.StyleConfiguration
import com.bitmovin.player.config.media.DASHSource
import com.bitmovin.player.config.media.SourceConfiguration
import com.bitmovin.player.config.media.SourceItem
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : FragmentActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private val SEEKING_OFFSET = 10

    private var bitmovinPlayer: BitmovinPlayer? = null
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from splash screen to main theme when we are done loading
        this.setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.activity_main)

        this.initializePlayer()

        this.initializeMediaSession()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


    }

    override fun onStart() {
        super.onStart()
        bitmovinPlayerView.onStart()
    }

    override fun onResume() {
        super.onResume()

        bitmovinPlayerView.onResume()
        this.addEventListener()
        resumeMediaSession()    // Resumed MediaSession
        this.bitmovinPlayer?.play()
    }

    override fun onPause() {
        this.removeEventListener()
        bitmovinPlayerView.onPause()
        pauseMediaSession()     // Paused MediaSession
        super.onPause()

    }

    override fun onStop() {
        bitmovinPlayerView.onStop()
        stopMediaSession()      // Stopped MediaSession
        super.onStop()
    }

    override fun onDestroy() {
        bitmovinPlayerView.onDestroy()
        destroyMediaSession()      // Destroyed MediaSession
        super.onDestroy()
    }

    private fun initializePlayer() {
        // Initialize BitmovinPlayerView from layout
        // Fetch BitmovinPlayer from BitmovinPlayerView
        this.bitmovinPlayer = bitmovinPlayerView.player

        this.bitmovinPlayer?.setup(this.createPlayerConfiguration())
    }

    private fun createPlayerConfiguration(): PlayerConfiguration {
        // Create a new SourceItem. In this case we are loading a DASH source.
        val sourceURL =
            "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd"
        val sourceItem = SourceItem(DASHSource(sourceURL))

        // Creating a new PlayerConfiguration
        val playerConfiguration = PlayerConfiguration()

        // Assign created SourceConfiguration to the PlayerConfiguration
        val sourceConfiguration = SourceConfiguration()
        sourceConfiguration.addSourceItem(sourceItem)
        playerConfiguration.sourceConfiguration = sourceConfiguration

        // Here a custom bitmovinplayer-ui.js is loaded which utilizes the Cast-UI as this matches our needs here perfectly.
        // I.e. UI controls get shown / hidden whenever the Player API is called. This is needed due to the fact that on Android TV no touch events are received
        val styleConfiguration = StyleConfiguration()
        styleConfiguration.playerUiJs = "file:///android_asset/bitmovinplayer-ui.js"
        playerConfiguration.styleConfiguration = styleConfiguration

        val playbackConfiguration = PlaybackConfiguration()
        playbackConfiguration.isAutoplayEnabled = true
        playerConfiguration.playbackConfiguration = playbackConfiguration

        return playerConfiguration
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // This method is called on key down and key up, so avoid being called twice
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (this.handleUserInput(event.keyCode)) {
                return true
            }
        }

        // Make sure to return super.dispatchKeyEvent(event) so that any key not handled yet will work as expected
        return super.dispatchKeyEvent(event)
    }

    private fun handleUserInput(keycode: Int): Boolean {
        Log.d(TAG, "Keycode $keycode")
        when (keycode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                togglePlay()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                this.bitmovinPlayer?.play()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                this.bitmovinPlayer?.pause()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                seekForward()
            }
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                seekBackward()
            }
        }

        return false
    }

    private fun togglePlay() {
        bitmovinPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    private fun seekForward() {
        bitmovinPlayer?.let { player ->
            val currentTime = player.currentTime
            player.seek(currentTime.plus(SEEKING_OFFSET))
        }
    }

    private fun seekBackward() {
        bitmovinPlayer?.let { player ->
            val currentTime = player.currentTime
            player.seek(currentTime.minus(SEEKING_OFFSET))
        }
    }

    private fun addEventListener() {
        this.bitmovinPlayer?.addEventListener(this.onErrorListener)
        this.bitmovinPlayer?.addEventListener(this.onPlayListener)
        this.bitmovinPlayer?.addEventListener(this.onPauseListener)

    }

    private fun removeEventListener() {
        this.bitmovinPlayer?.removeEventListener(this.onErrorListener)
        this.bitmovinPlayer?.removeEventListener(this.onPlayListener)
        this.bitmovinPlayer?.removeEventListener(this.onPauseListener)
    }

    private val onErrorListener = OnErrorListener { errorEvent ->
        Log.e(TAG, "An Error occurred (${errorEvent.code}): ${errorEvent.message}")
    }
    private val onPlayListener = OnPlayListener {
        updateMediaSession(PlaybackStateCompat.STATE_PLAYING, true)
    }
    private val onPauseListener = OnPausedListener {
        updateMediaSession(PlaybackStateCompat.STATE_PAUSED, false)
    }


    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(applicationContext, TAG)
        mediaSession?.setCallback(getMediaSessionCallback())
        mediaSession?.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
    }

    private fun pauseMediaSession() {
        bitmovinPlayer?.let { bitmovinPlayer ->
            mediaSession?.isActive = false
            mediaSession?.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        (bitmovinPlayer.currentTime * 1000).toLong(),
                        1.0f
                    )
                    .setActions(getActions())
                    .build()
            )
        }
    }

    private fun resumeMediaSession() {
        bitmovinPlayer?.let { bitmovinPlayer ->
            mediaSession?.isActive = true
            mediaSession?.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        (bitmovinPlayer.currentTime * 1000).toLong(),
                        1.0f
                    )
                    .setActions(getActions())
                    .build()
            )
        }
    }

    private fun stopMediaSession() {
        bitmovinPlayer?.let { bitmovinPlayer ->
            mediaSession?.isActive = false
            mediaSession?.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(
                        PlaybackStateCompat.STATE_STOPPED,
                        (bitmovinPlayer.currentTime * 1000).toLong(),
                        1.0f
                    )
                    .setActions(getActions())
                    .build()
            )
        }
    }

    private fun destroyMediaSession() {
        if (mediaSession != null) {
            mediaSession?.release()
            mediaSession = null
        }
    }

    private fun updateMediaSession(state: Int, active: Boolean) {
        bitmovinPlayer?.let { bitmovinPlayer ->
            mediaSession?.isActive = active
            mediaSession?.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(
                        state,
                        (bitmovinPlayer.currentTime * 1000).toLong(),
                        1.0f
                    )
                    .setActions(getActions())
                    .build()
            )
        }
    }

    private fun getActions(): Long {
        return (PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_SEEK_TO
                or PlaybackStateCompat.ACTION_FAST_FORWARD
                or PlaybackStateCompat.ACTION_REWIND
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    }

    private fun getMediaSessionCallback(): MediaSessionCompat.Callback {
        return object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                updateMediaSession(PlaybackStateCompat.STATE_PLAYING, true)
                bitmovinPlayer?.play()
            }

            override fun onPause() {
                super.onPause()
                bitmovinPlayer?.pause()
                updateMediaSession(PlaybackStateCompat.STATE_PAUSED, false)
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                updateMediaSession(PlaybackStateCompat.STATE_BUFFERING, true)
                bitmovinPlayer?.let { bitmovinPlayer ->
                    bitmovinPlayer.seek(pos / 1000.0)
                    if (bitmovinPlayer.isPlaying) {
                        updateMediaSession(PlaybackStateCompat.STATE_PLAYING, true)
                    } else if (bitmovinPlayer.isPaused) {
                        updateMediaSession(PlaybackStateCompat.STATE_PAUSED, false)
                    }
                }
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
            }
        }

    }
}