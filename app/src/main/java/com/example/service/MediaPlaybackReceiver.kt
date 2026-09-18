package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.KeyEvent

class MediaPlaybackReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PLAY = "com.example.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.ACTION_TOGGLE_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_CLOSE = "com.example.ACTION_CLOSE"

        private var lastHeadsetClickTime = 0L
        private var headsetClickCount = 0

        // Global callback handler registered by MusicPlayerViewModel or Application
        var playbackActionListener: PlaybackActionListener? = null
    }

    interface PlaybackActionListener {
        fun onPlayPause()
        fun onNext()
        fun onPrevious()
        fun onClose()
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("MediaPlaybackReceiver", "Received action: $action")

        if (Intent.ACTION_MEDIA_BUTTON == action) {
            val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }

            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_HEADSETHOOK -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastHeadsetClickTime < 400) {
                            headsetClickCount++
                        } else {
                            headsetClickCount = 1
                        }
                        lastHeadsetClickTime = currentTime

                        when (headsetClickCount) {
                            1 -> playbackActionListener?.onPlayPause()
                            2 -> playbackActionListener?.onNext()
                            3 -> {
                                playbackActionListener?.onPrevious()
                                headsetClickCount = 0
                            }
                        }
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> playbackActionListener?.onPlayPause()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> playbackActionListener?.onPlayPause()
                    KeyEvent.KEYCODE_MEDIA_NEXT -> playbackActionListener?.onNext()
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> playbackActionListener?.onPrevious()
                    KeyEvent.KEYCODE_MEDIA_STOP -> playbackActionListener?.onClose()
                }
            }
            return
        }

        when (action) {
            ACTION_PLAY, ACTION_PAUSE, ACTION_TOGGLE_PLAY_PAUSE -> {
                playbackActionListener?.onPlayPause()
            }
            ACTION_NEXT -> {
                playbackActionListener?.onNext()
            }
            ACTION_PREV -> {
                playbackActionListener?.onPrevious()
            }
            ACTION_CLOSE -> {
                playbackActionListener?.onClose()
            }
        }
    }
}
