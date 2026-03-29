package com.videowallpaper.lite

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class VideoWallpaperService : WallpaperService() {

    companion object {
        const val PREF_NAME = "wallpaper_prefs"
        const val KEY_VIDEO = "video_uri"
        const val ACTION_RELOAD = "com.videowallpaper.lite.RELOAD"
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var player: MediaPlayer? = null
        private var playerReady = false
        private var playerDuration = 0
        private var isLocked = true
        private var surface: SurfaceHolder? = null
        private val handler = Handler(Looper.getMainLooper())

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> onScreenOff()
                    Intent.ACTION_SCREEN_ON  -> onScreenOn()
                    ACTION_RELOAD            -> reload()
                }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setTouchEventsEnabled(false)
            registerReceiver(receiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(ACTION_RELOAD)
            })
        }

        override fun onDestroy() {
            super.onDestroy()
            try { unregisterReceiver(receiver) } catch (e: Exception) {}
            releasePlayer()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surface = holder
            setupPlayer(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
            surface = null
        }

        override fun onVisibilityChanged(visible: Boolean) {}

        override fun onComputeColors(): WallpaperColors {
            return WallpaperColors(
                Color.valueOf(Color.parseColor("#6200EE")), null, null
            )
        }

        private fun onScreenOff() {
            isLocked = true
            handler.removeCallbacksAndMessages(null)
            handler.post {
                try {
                    if (playerReady) {
                        player?.pause()
                        player?.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    }
                } catch (e: Exception) {}
            }
        }

        private fun onScreenOn() {
            if (!isLocked) return
            isLocked = false
            handler.post {
                try {
                    if (!playerReady) return@post
                    player?.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    player?.start()
                    val stop = (playerDuration - 100).coerceAtLeast(0).toLong()
                    handler.postDelayed({
                        try {
                            if (player?.isPlaying == true) player?.pause()
                        } catch (e: Exception) {}
                    }, stop)
                } catch (e: Exception) {}
            }
        }

        private fun setupPlayer(holder: SurfaceHolder) {
            val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            val uri = prefs.getString(KEY_VIDEO, null)?.let { Uri.parse(it) } ?: return
            releasePlayer()
            try {
                val p = MediaPlayer()
                p.setSurface(holder.surface)
                p.setDataSource(applicationContext, uri)
                p.isLooping = false
                p.setOnPreparedListener { mp ->
                    playerDuration = mp.duration
                    playerReady = true
                    mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    mp.start()
                    handler.postDelayed({ 
                        try { mp.pause() } catch (e: Exception) {}
                    }, 80)
                }
                p.setOnCompletionListener { mp ->
                    try { mp.pause() } catch (e: Exception) {}
                }
                p.setOnErrorListener { _, _, _ ->
                    playerReady = false
                    false
                }
                p.prepareAsync()
                player = p
            } catch (e: Exception) {}
        }

        private fun reload() {
            surface?.let { setupPlayer(it) }
        }

        private fun releasePlayer() {
            handler.removeCallbacksAndMessages(null)
            try {
                player?.stop()
                player?.reset()
                player?.release()
            } catch (e: Exception) {}
            player = null
            playerReady = false
            playerDuration = 0
        }
    }
}
