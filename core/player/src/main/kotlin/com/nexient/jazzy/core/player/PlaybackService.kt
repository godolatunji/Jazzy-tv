package com.nexient.jazzy.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MediaSession around [PlayerHolder]'s player. Media3 promotes the service to the foreground
 * (mediaPlayback) while playing, which also keeps the torrent engine running (TE-8, PL-11).
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var holder: PlayerHolder

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        session = MediaSession.Builder(this, holder.get()).apply { launch?.let { setSessionActivity(it) } }.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
