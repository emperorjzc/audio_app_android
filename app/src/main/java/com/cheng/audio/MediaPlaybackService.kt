package com.cheng.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver

private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"

class MediaPlaybackService : MediaBrowserServiceCompat(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener {

    private lateinit var mPlaybackState: PlaybackStateCompat
    private var mediaSession: MediaSessionCompat? = null
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private val LOG_TAG = this.javaClass.simpleName
    private val notification_id = 10000
    private lateinit var mMediaPlayer: MediaPlayer

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(baseContext, LOG_TAG).apply {
            // 启用MediaButtons 和 TransportControls 回调
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            // 使用 ACTION_PLAY 设置初始 PlaybackState，媒体按钮可以启动播放器
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            setPlaybackState(stateBuilder.build())
            // MySessionCallback() has methods that handle callbacks from a media controller
            setCallback(MySessionCallback())
            // Set the session's token so that client activities can communicate with it.
            setSessionToken(sessionToken)
        }
        mPlaybackState = getPlaybackState(PlaybackStateCompat.STATE_NONE)
        initPlayer()
    }

    private fun initPlayer() {
        mMediaPlayer = MediaPlayer()
        mMediaPlayer.setOnPreparedListener(this)
        mMediaPlayer.setOnCompletionListener(this)
    }

    inner class MySessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            LogUtil.i<MediaPlaybackService>("onPlay()==>")
            mMediaPlayer.start()
            mediaSession?.setPlaybackState(getPlaybackState(PlaybackStateCompat.STATE_PLAYING))
            startForeground()
        }

        override fun onPause() {
            super.onPause()
            mMediaPlayer.pause()
            mediaSession?.setPlaybackState(getPlaybackState(PlaybackStateCompat.STATE_PAUSED))
            LogUtil.i<MediaPlaybackService>("onPause()==>")
        }

        override fun onSkipToNext() {
            LogUtil.i<MediaPlaybackService>("onSkipToNext()==>")
            mediaSession?.setPlaybackState(getPlaybackState(PlaybackStateCompat.STATE_PLAYING))
            super.onSkipToNext()
        }

        override fun onSkipToPrevious() {
            LogUtil.i<MediaPlaybackService>("onSkipToPrevious()==>")
            mediaSession?.setPlaybackState(getPlaybackState(PlaybackStateCompat.STATE_PLAYING))
            super.onSkipToPrevious()
        }

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)
            LogUtil.i<MediaPlaybackService>("onPlayFromUri()==>${uri?.path}")
            if (uri == null) return
            when (mPlaybackState.state) {
                PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_NONE -> {
                    mediaSession?.setPlaybackState(getPlaybackState(PlaybackStateCompat.STATE_CONNECTING))
                    mMediaPlayer.reset()
                    mMediaPlayer.setDataSource(baseContext, uri)
                    mMediaPlayer.prepareAsync()
                    mediaSession?.setMetadata(
                        MediaMetadataCompat.Builder()
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_TITLE,
                                extras?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                            )
                            .build()
                    )
                }
            }
        }
    }

    // 控制客户端连接
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        // (Optional) Control the level of access for the specified package name.
        // You'll need to write your own logic to do this.
        return if (allowBrowsing(clientPackageName, clientUid)) {
            // Returns a root ID that clients can use with onLoadChildren() to retrieve
            // the content hierarchy.
            BrowserRoot(MY_MEDIA_ROOT_ID, null)
        } else {
            // Clients can connect, but this BrowserRoot is an empty hierachy
            // so onLoadChildren returns nothing. This disables the ability to browse for content.
            BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null)
        }
    }

    private fun allowBrowsing(
        clientPackageName: String,
        clientUid: Int,
    ): Boolean {
        // TODO: 2021/8/4 还不知道到具体实现，默认返回 false
        return false
    }

    // 传达内容
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        //  Browsing not allowed
        if (MY_EMPTY_MEDIA_ROOT_ID == parentId) {
            result.sendResult(null)
            return
        }

        // Assume for example that the music catalog is already loaded/cached.

        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        // Check if this is the root menu:
        if (MY_MEDIA_ROOT_ID == parentId) {
            // Build the MediaItem objects for the top level,
            // and put them in the mediaItems list...
        } else {
            // Examine the passed parentMediaId to see which submenu we're at,
            // and put the children of that menu in the mediaItems list...
        }
        result.sendResult(mediaItems)
    }

    private fun startForeground() {
        // Given a media session and its context (usually the component containing the session)
        // Create a NotificationCompat.Builder
        // Get the session's metadata
        val controller = mediaSession!!.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        val builder = NotificationCompat.Builder(baseContext, getChannelId()).apply {
            // Add the metadata for the currently playing track
            setContentTitle(description.title)
            setContentText(description.subtitle)
            setSubText(description.description)
            setLargeIcon(description.iconBitmap)

            // Enable launching the player by clicking the notification
            setContentIntent(controller.sessionActivity)

            // Stop the service when the notification is swiped away
            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    baseContext,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

            // Make the transport controls visible on the lockscreen
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Add an app icon and set its accent color
            // Be careful about the color
            setSmallIcon(R.mipmap.ic_notification)
            color = ContextCompat.getColor(baseContext, R.color.primaryDark)

            // Add a pause button
            addAction(
                NotificationCompat.Action(
                    R.mipmap.ic_notification_pause,
                    getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        baseContext,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )

            // Take advantage of MediaStyle features
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession!!.sessionToken)
                    .setShowActionsInCompactView(0)

                    // Add a cancel button
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            baseContext,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
        }

        // Display the notification and place the service in the foreground
        startForeground(notification_id, builder.build())
    }

    private fun getChannelId(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("my_service", "My Background Service")
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
            ""
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val channel = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mMediaPlayer.start()
        mediaSession?.setPlaybackState(getPlaybackState(PlaybackStateCompat.STATE_PLAYING))
    }

    override fun onCompletion(mp: MediaPlayer?) {
    }

    private fun getPlaybackState(@PlaybackStateCompat.State state: Int): PlaybackStateCompat {
        return PlaybackStateCompat.Builder()
            .setState(state, 0, 1.0f)
            .build()
    }
}