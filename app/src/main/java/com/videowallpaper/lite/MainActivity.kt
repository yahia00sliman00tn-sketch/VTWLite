package com.videowallpaper.lite

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnSelectVideo: Button
    private lateinit var btnSetWallpaper: Button
    private lateinit var btnReset: Button

    companion object {
        const val REQUEST_VIDEO = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus        = findViewById(R.id.tvStatus)
        btnSelectVideo  = findViewById(R.id.btnSelectVideo)
        btnSetWallpaper = findViewById(R.id.btnSetWallpaper)
        btnReset        = findViewById(R.id.btnReset)

        val prefs = getSharedPreferences(VideoWallpaperService.PREF_NAME, MODE_PRIVATE)
        if (prefs.getString(VideoWallpaperService.KEY_VIDEO, null) != null) {
            tvStatus.text = getString(R.string.video_selected)
            tvStatus.setTextColor(0xFF1DB954.toInt())
            btnSetWallpaper.isEnabled = true
        }

        btnSelectVideo.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "video/*"
                }, REQUEST_VIDEO
            )
        }

        btnSetWallpaper.setOnClickListener {
            startActivity(
                Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, VideoWallpaperService::class.java)
                    )
                }
            )
        }

        btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_title))
                .setMessage(getString(R.string.reset_message))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    prefs.edit().clear().apply()
                    tvStatus.text = getString(R.string.no_video)
                    tvStatus.setTextColor(0xFFFF5555.toInt())
                    btnSetWallpaper.isEnabled = false
                    sendBroadcast(Intent(VideoWallpaperService.ACTION_RELOAD))
                    Toast.makeText(this, getString(R.string.reset_done), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val prefs = getSharedPreferences(VideoWallpaperService.PREF_NAME, MODE_PRIVATE)
            prefs.edit().putString(VideoWallpaperService.KEY_VIDEO, uri.toString()).apply()
            tvStatus.text = getString(R.string.video_selected)
            tvStatus.setTextColor(0xFF1DB954.toInt())
            btnSetWallpaper.isEnabled = true
            sendBroadcast(Intent(VideoWallpaperService.ACTION_RELOAD))
        }
    }
}
