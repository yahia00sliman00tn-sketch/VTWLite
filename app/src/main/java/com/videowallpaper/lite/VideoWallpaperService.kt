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
        private var isReady = false
        private var duration = 0
        private var isLocked = true
        private var surface: SurfaceHolder? = null
        private val handler = Handler(Looper.getMainLooper())

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF   -> onScreenOff()
                    Intent.ACTION_SCREEN_ON    -> onScreenOn()
                    ACTION_RELOAD              -> reload()
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
            // أعد للفريم الأول
            handler.post {
                try {
                    if (isReady) {
                        player?.pause()
                        player?.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    }
                } catch (e: Exception) {}
            }
        }

        private fun onScreenOn() {
            // يشتغل فور إضاءة الشاشة — في Lock Screen
            if (isLocked) {
                isLocked = false
                handler.post {
                    try {
                        if (!isReady) return@post
                        player?.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                        player?.start()
                        val stop = (duration - 100).coerceAtLeast(0).toLong()
                        handler.postDelayed({
                            try {
                                if (player?.isPlaying == true) player?.pause()
                            } catch (e: Exception) {}
                        }, stop)
                    } catch (e: Exception) {}
                }
            }
        }

        private fun setupPlayer(holder: SurfaceHolder) {
            val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            val uri = prefs.getString(KEY_VIDEO, null)?.let { Uri.parse(it) } ?: return
            releasePlayer()
            try {
                player = MediaPlayer().apply {
                    setSurface(holder.surface)
                    setDataSource(applicationContext, uri)
                    isLooping = false
                    setOnPreparedListener { mp ->
                        duration = mp.duration
                        isReady = true
                        // اعرض الفريم الأول
                        mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                        mp.start()
                        handler.postDelayed({ mp.pause() }, 80)
                    }
                    setOnCompletionListener { mp ->
                        mp.pause()
                    }
                    setOnErrorListener { _, _, _ ->
                        isReady = false
                        false
                    }
                    prepareAsync()
                }
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
            isReady = false
            duration = 0
        }
    }
}
