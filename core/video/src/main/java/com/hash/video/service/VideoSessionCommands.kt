package com.hash.video.service

import android.os.Bundle
import androidx.media3.session.SessionCommand

/** 系统媒体通知可调用的会话命令。 */
internal object VideoSessionCommands {

    /** 暂停应用播放器并销毁 MediaSession，以彻底移除系统媒体通知。 */
    val dismissNotification = SessionCommand(
        "com.hash.video.DISMISS_NOTIFICATION",
        Bundle.EMPTY,
    )
}
