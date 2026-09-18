package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.MainActivity
import com.example.R
import com.example.model.AudioTrack

object MediaNotificationHelper {

    private const val CHANNEL_ID = "channel_music_playback_v3"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        val name = "Music Playback"
        val descriptionText = "Lockscreen & media playback controls"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        context: Context,
        track: AudioTrack?,
        isPlaying: Boolean,
        coverArt: Bitmap? = null,
        mediaSessionToken: MediaSessionCompat.Token? = null
    ): Notification {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action Intent
        val prevIntent = Intent(context, MediaPlaybackReceiver::class.java).apply {
            action = MediaPlaybackReceiver.ACTION_PREV
        }
        val pendingPrev = PendingIntent.getBroadcast(
            context,
            1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play / Pause Action Intent
        val playPauseIntent = Intent(context, MediaPlaybackReceiver::class.java).apply {
            action = MediaPlaybackReceiver.ACTION_TOGGLE_PLAY_PAUSE
        }
        val pendingPlayPause = PendingIntent.getBroadcast(
            context,
            2,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Next Action Intent
        val nextIntent = Intent(context, MediaPlaybackReceiver::class.java).apply {
            action = MediaPlaybackReceiver.ACTION_NEXT
        }
        val pendingNext = PendingIntent.getBroadcast(
            context,
            3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Close / Dismiss Player Action Intent
        val closeIntent = Intent(context, MediaPlaybackReceiver::class.java).apply {
            action = MediaPlaybackReceiver.ACTION_CLOSE
        }
        val pendingClose = PendingIntent.getBroadcast(
            context,
            4,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val mediaStyle = MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(pendingClose)

        if (mediaSessionToken != null) {
            mediaStyle.setMediaSession(mediaSessionToken)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(track?.title ?: "Raga Music Player")
            .setContentText(track?.artist ?: "Local Audio Player")
            .setSubText(track?.fileExtension?.uppercase() ?: "AUDIO")
            .setContentIntent(pendingOpenApp)
            .setDeleteIntent(pendingClose)
            .setOngoing(isPlaying)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_skip_previous, "Previous", pendingPrev)
            .addAction(playPauseIcon, playPauseTitle, pendingPlayPause)
            .addAction(R.drawable.ic_skip_next, "Next", pendingNext)
            .addAction(R.drawable.ic_close, "Close", pendingClose)
            .setStyle(mediaStyle)

        if (coverArt != null) {
            try {
                val scaledArt = if (coverArt.width > 128 || coverArt.height > 128) {
                    Bitmap.createScaledBitmap(coverArt, 128, 128, true)
                } else {
                    coverArt
                }
                builder.setLargeIcon(scaledArt)
            } catch (e: Exception) {
                builder.setLargeIcon(coverArt)
            }
        }

        return builder.build()
    }

    fun updateNotification(
        context: Context,
        track: AudioTrack?,
        isPlaying: Boolean,
        coverArt: Bitmap? = null
    ) {
        try {
            if (track != null) {
                AudioService.startOrUpdate(context, track, isPlaying, coverArt)
            } else {
                clearNotification(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearNotification(context: Context) {
        try {
            AudioService.stop(context)
            clearNotificationDirectly(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearNotificationDirectly(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {}
    }
}
