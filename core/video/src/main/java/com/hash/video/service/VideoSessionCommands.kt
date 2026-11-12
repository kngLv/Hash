package com.hash.video.service

import android.os.Bundle
import androidx.media3.session.SessionCommand

/** 仅供 App 内部控制器与视频播放服务通信的会话命令。 */
internal object VideoSessionCommands {

    const val ARGUMENT_NOTIFICATION_ACTIVITY = "notification_activity"

    val setNotificationActivity = SessionCommand(
        "com.hash.video.SET_NOTIFICATION_ACTIVITY",
        Bundle.EMPTY,
    )

    val releasePlaybackSession = SessionCommand(
        "com.hash.video.RELEASE_PLAYBACK_SESSION",
        Bundle.EMPTY,
    )
}
