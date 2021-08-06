package com.cheng.audio

import android.content.ComponentName
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.cheng.audio.databinding.ActivityMediaPlayerBinding

class MediaPlayerActivity : AppCompatActivity() {
//    onCreate() 构造 MediaBrowserCompat。传入您的 MediaBrowserService 的名称和您已定义的 MediaBrowserCompat.ConnectionCallback。
//    onStart() 连接到 MediaBrowserService。这里体现了 MediaBrowserCompat.ConnectionCallback 的神奇之处。如果连接成功，onConnect() 回调会创建媒体控制器，将其链接到媒体会话，将您的界面控件链接到 MediaController，并注册控制器以接收来自媒体会话的回调。
//    onResume() 设置音频流，以便您的应用响应设备上的音量控制。
//    onStop() 断开 MediaBrowser 的连接，并在 Activity 停止时取消注册 MediaController.Callback。

    private var playPause: ImageView? = null
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {

            // Get the token for the MediaSession
            mediaBrowser.sessionToken.also { token ->

                // Create a MediaControllerCompat
                val mediaController = MediaControllerCompat(
                    this@MediaPlayerActivity, // Context
                    token
                )

                // Save the controller
                MediaControllerCompat.setMediaController(this@MediaPlayerActivity, mediaController)
            }

            // Finish building the UI
            buildTransportControls()
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }
    }
    private lateinit var binding: ActivityMediaPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mediaBrowser =
            MediaBrowserCompat(
                this,
                ComponentName(this, MediaPlaybackService::class.java),
                connectionCallback,
                null
            )

    }

    override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStop() {
        super.onStop()
        // (see "stay in sync with the MediaSession")
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
    }

    fun buildTransportControls() {
        val mediaController = MediaControllerCompat.getMediaController(this@MediaPlayerActivity)
        // Grab the view for the play/pause button

        binding.btnPlayNew.setOnClickListener {
            var extras = Bundle().apply { putString(MediaMetadataCompat.METADATA_KEY_TITLE, "圣诞歌") }
            mediaController.transportControls.playFromUri(rawToUri(R.raw.jinglebells), extras)
        }

        binding.btnPlay.setOnClickListener {
            // Since this is a play/pause button, you'll need to test the current state
            // and choose the action accordingly
            val pbState = mediaController.playbackState.state
            if (pbState == PlaybackStateCompat.STATE_PLAYING) return@setOnClickListener
            mediaController.transportControls.play()
        }

        binding.btnPause.setOnClickListener {
            if (mediaController.playbackState.state != PlaybackStateCompat.STATE_PLAYING) return@setOnClickListener
            mediaController.transportControls.pause()
        }

        binding.btnSkipNext.setOnClickListener {
            mediaController.transportControls.skipToNext()
        }

        binding.btnSkipPrevious.setOnClickListener {
            mediaController.transportControls.skipToPrevious()
        }

        // Display the initial state
        val metadata = mediaController.metadata
        val pbState = mediaController.playbackState

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback)
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            binding.tvNameSong.text = metadata?.getText(MediaMetadataCompat.METADATA_KEY_TITLE)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        }
    }

    private fun rawToUri(id: Int): Uri? {
        val uriStr = "android.resource://$packageName/$id"
        return Uri.parse(uriStr)
    }


}