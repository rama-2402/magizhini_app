package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.app.Instrumentation
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.TransitionSet
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import androidx.core.transition.doOnEnd
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityPreviewBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadOriginal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private lateinit var player: ExoPlayer

    private lateinit var contentType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_preview)
        val url = intent.getStringExtra("url").toString()
        contentType = intent.getStringExtra("contentType").toString()
        if (contentType == "video") {
            binding.ivPreviewImage.remove()
//            binding.videoView.visible()
//        val sampleUrl = "https://drive.google.com/uc?id=1ZIyHXhh7UOGWFFs2aBs2TCSHh7MXUcPs"
            lifecycleScope.launch {
                delay(1000)
                val exo = ExoPlayer.Builder(this@PreviewActivity)
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
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
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
        } else {

            intent.data?.let { uri ->
                window.sharedElementEnterTransition = TransitionSet()
                    .addTransition(ChangeImageTransform())
                    .addTransition(ChangeBounds())
                    .apply {
                        doOnEnd { binding.ivPreviewImage.loadOriginal(uri) {} }
                    }

                binding.ivPreviewImage.loadOriginal(uri) {}
                progressGone()
                binding.videoView.remove()
            } ?: let {
                window.sharedElementEnterTransition = TransitionSet()
                    .addTransition(ChangeImageTransform())
                    .addTransition(ChangeBounds())
                    .apply {
                        doOnEnd { binding.ivPreviewImage.loadOriginal(url) {} }
                    }


                binding.ivPreviewImage.loadOriginal(url) {}
//            GlideLoader().loadUserPictureWithoutCrop(this, url, binding.ivPreviewImage)
//            binding.ivPreviewImage.visible()
                progressGone()
                binding.videoView.remove()
            }
        }
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
    if (contentType == "video") {
        player.stop()
    }
//        finish()
    super.onBackPressed()
}

    override fun onStop() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isFinishing) {
            Instrumentation().callActivityOnSaveInstanceState(this, Bundle())
        }
        super.onStop()
    }
}