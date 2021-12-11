package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_player)
        val url = intent.getStringExtra("url").toString()

//        val sampleUrl = "https://drive.google.com/uc?id=1ZIyHXhh7UOGWFFs2aBs2TCSHh7MXUcPs"
        val exo = ExoPlayer.Builder(this)
        player = exo.build()
        binding.videoView.player = player
        // Build the media item.
        val mediaItem: MediaItem = MediaItem.fromUri(Uri.parse(url))
        // Set the media item to be played.
        player.setMediaItem(mediaItem)
        // Prepare the player.
        player.prepare()
        // Start the playback.
        player.play()
        player.addListener(object : Player.Listener{
            override fun onPlaybackStateChanged(playbackState: Int) {
                when(playbackState) {
                    ExoPlayer.STATE_BUFFERING -> progressVisible()
                    ExoPlayer.STATE_READY -> progressGone()
                    ExoPlayer.STATE_ENDED -> {
                        player.stop()
                        finish()
                    }
                    else -> Unit
                }
            }
        })
    }

    private fun progressGone() {
        binding.apply {
            progressCircular.visibility = View.GONE
        }
    }
    private fun progressVisible() {
        binding.apply {
            progressCircular.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        player.stop()
        finish()
        super.onBackPressed()
    }
}