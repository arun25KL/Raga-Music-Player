package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import com.example.model.AudioTrack

class AudioService : Service() {

    companion object {
        const val ACTION_START_OR_UPDATE = "com.example.service.ACTION_START_OR_UPDATE"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        private var currentTrack: AudioTrack? = null
        private var isTrackPlaying: Boolean = false
        private var currentCoverArt: Bitmap? = null

        fun startOrUpdate(
            context: Context,
            track: AudioTrack?,
            isPlaying: Boolean,
            coverArt: Bitmap? = null
        ) {
            currentTrack = track
            isTrackPlaying = isPlaying
            currentCoverArt = coverArt

            if (track == null) {
                stop(context)
                return
            }

            val intent = Intent(context, AudioService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            currentTrack = null
            isTrackPlaying = false
            currentCoverArt = null
            val intent = Intent(context, AudioService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var mediaSession: MediaSessionCompat? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AudioService:PlaybackWakeLock")?.apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initMediaSession() {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(this, "MusicPlayerMediaSession").apply {
                setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        MediaPlaybackReceiver.playbackActionListener?.onPlayPause()
                    }

                    override fun onPause() {
                        MediaPlaybackReceiver.playbackActionListener?.onPlayPause()
                    }

                    override fun onSkipToNext() {
                        MediaPlaybackReceiver.playbackActionListener?.onNext()
                    }

                    override fun onSkipToPrevious() {
                        MediaPlaybackReceiver.playbackActionListener?.onPrevious()
                    }

                    override fun onStop() {
                        MediaPlaybackReceiver.playbackActionListener?.onClose()
                    }
                })
                isActive = true
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP || currentTrack == null) {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            updateMediaSessionState(false)
            mediaSession?.isActive = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            MediaNotificationHelper.clearNotificationDirectly(this)
            stopSelf()
            return START_NOT_STICKY
        }

        val track = currentTrack ?: return START_NOT_STICKY
        val isPlaying = isTrackPlaying
        val art = currentCoverArt

        try {
            if (isPlaying) {
                wakeLock?.acquire(30 * 60 * 1000L) // 30 min safety timeout
            } else {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updateMediaSessionMetadata(track, art)
        updateMediaSessionState(isPlaying)

        val notification = MediaNotificationHelper.buildNotification(
            context = this,
            track = track,
            isPlaying = isPlaying,
            coverArt = art,
            mediaSessionToken = mediaSession?.sessionToken
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    MediaNotificationHelper.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(MediaNotificationHelper.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return START_STICKY
    }

    private fun updateMediaSessionMetadata(track: AudioTrack, art: Bitmap?) {
        val session = mediaSession ?: return
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)

        if (art != null) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art)
        }

        session.setMetadata(builder.build())
    }

    private fun updateMediaSessionState(isPlaying: Boolean) {
        val session = mediaSession ?: return
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP

        val pbState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()

        session.setPlaybackState(pbState)
        session.isActive = true
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
