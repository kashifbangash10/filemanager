package com.nextguidance.filesexplorer.filemanager.smartfiles.activities

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.widget.TextView
import com.nextguidance.filesexplorer.filemanager.smartfiles.R

class VideoPlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var audioArtContainer: View
    private lateinit var audioTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        playerView = findViewById(R.id.player_view)
        audioArtContainer = findViewById(R.id.audio_art_container)
        audioTitle = findViewById(R.id.audio_title)
        
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }
        
        // Fullscreen
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                
        val uri = intent.data
        if (uri != null) {
            val fileName = getFileName(uri)
            if (isAudioFile(fileName)) {
                audioArtContainer.visibility = View.VISIBLE
                audioTitle.text = fileName
                // playerView.useController = true // Still show controls
            } else {
                audioArtContainer.visibility = View.GONE
            }
            initializePlayer(uri)
        }
    }

    private fun getFileName(uri: Uri): String {
        return uri.path?.substringAfterLast('/') ?: "Media Player"
    }

    private fun isAudioFile(name: String): Boolean {
        val n = name.lowercase()
        return n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".m4a") || 
               n.endsWith(".flac") || n.endsWith(".aac") || n.endsWith(".ogg")
    }

    private fun initializePlayer(uri: Uri) {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        val mediaItem = MediaItem.fromUri(uri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
