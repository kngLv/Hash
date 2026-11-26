package com.hash.video.presentation

import android.app.Application
import android.app.PendingIntent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.MainThread
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoOverlayResult
import com.hash.video.api.VideoPlaybackControlToken
import com.hash.video.player.VideoPlayerEngine
import com.hash.video.player.VideoOverlaySession
import com.hash.video.ui.interaction.FloatingDragListener
import com.hash.video.ui.player.FloatingVideoPlayerView
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/** 进程唯一的系统悬浮窗宿主；只持有 Application Context，不引用页面。 */
object SystemOverlayHost {
    /**
     * 当前添加到系统 WindowManager 的悬浮窗根 View。
     *
     * WindowManager 在窗口存活时负责强持有 View；这里仅保留弱引用，避免进程单例意外延长
     * Android View 的生命周期。
     */
    private var rootRef: WeakReference<FloatingVideoPlayerView>? = null
    private val root: FloatingVideoPlayerView?
        get() = rootRef?.get()

    /** 仅由 Application Context 获取，不保存任何 Activity。 */
    private var windowManager: WindowManager? = null

    /** 悬浮窗展示期间持有独立操作凭证，页面销毁时不会误释放播放器。 */
    private var session: VideoOverlaySession? = null

    /** 返回应用时打开当前播放详情页的目标。 */
    private var restoreIntent: PendingIntent? = null

    /** 仅用于关闭悬浮窗时释放悬浮窗操作凭证的播放会话。 */
    private var application: Application? = null

    /** 记录当前位置，拖动时更新后重新提交给 WindowManager。 */
    private var windowParams: WindowManager.LayoutParams? = null

    /** 管理悬浮窗位置与边界限制；移除窗口时必须清理内部引用。 */
    private var positionController: OverlayWindowPositionController? = null

    /** 检查是否有权限在系统上绘制悬浮窗。 */
    fun canDraw(context: android.content.Context): Boolean = Settings.canDrawOverlays(context)

    /** 返回当前悬浮窗正在播放的媒体；没有悬浮窗会话时返回空。 */
    fun currentMediaOrNull(): VideoMedia? =
        VideoPlayerEngine.playbackState.value.media?.takeIf { session != null }

    /**
     * 创建系统悬浮窗，并将页面操作凭证替换为悬浮窗专用凭证。
     *
     * 先成功添加窗口，再迁移唯一渲染 View；添加失败时页面仍保留原来的播放画面。
     */
    @MainThread
    fun enter(
        application: Application,
        sourceToken: VideoPlaybackControlToken,
        returnIntent: PendingIntent?,
    ): VideoOverlayResult {
        if (root != null) return VideoOverlayResult.ALREADY_SHOWN
        if (!canDraw(application)) return VideoOverlayResult.PERMISSION_REQUIRED
        val currentMedia =
            VideoPlayerEngine.playbackState.value.media ?: return VideoOverlayResult.NO_ACTIVE_MEDIA
        val manager = application.getSystemService(WindowManager::class.java)
        //创建悬浮窗token，并替换
        val overlaySession = VideoPlayerEngine.enterSystemOverlay(sourceToken)
            ?: return VideoOverlayResult.FAILED
        //创建悬浮播放器
        val overlayRoot = FloatingVideoPlayerView(application).apply {
            setCallbacks(
                onClose = ::close,
                onRestore = ::requestRestore,
            )
        }
        return runCatching {
            layoutParams(currentMedia).also {
                windowParams = it
                manager.addView(overlayRoot, it)
                positionController = OverlayWindowPositionController(manager, overlayRoot, it)
                overlayRoot.setDragListener(object : FloatingDragListener {
                    override fun onDragBy(deltaX: Float, deltaY: Float) {
                        positionController?.moveBy(deltaX, deltaY)
                    }

                    override fun onDragEnd() {
                        positionController?.onDragEnd()
                    }
                })
            }
            //更新播放器凭证和渲染 View；由悬浮窗接管唯一画面。
            overlayRoot.playbackToken = overlaySession.token
            overlayRoot.setMedia(currentMedia)
            overlayRoot.attachRender()
            rootRef = WeakReference(overlayRoot)
            this.windowManager = manager
            this.session = overlaySession
            this.restoreIntent = returnIntent
            this.application = application
            VideoOverlayResult.SHOWN
        }.getOrElse { error ->
            Log.w(
                LOG_TAG,
                "系统悬浮窗创建失败：api=${Build.VERSION.SDK_INT}，windowType=${overlayWindowType()}",
                error,
            )
            runCatching { manager.removeViewImmediate(overlayRoot) }
            //异常时恢复页面原来的播放凭证和渲染 View；悬浮窗创建失败不影响页面。
            VideoPlayerEngine.restoreSystemOverlay(overlaySession, sourceToken)
            VideoOverlayResult.FAILED
        }
    }

    /** 页面数据加载完成后调用；仅媒体匹配时将悬浮窗播放画面交回该页面。 */
    @MainThread
    fun restoreTo(
        targetToken: VideoPlaybackControlToken,
        matches: (VideoMedia) -> Boolean,
    ): Boolean {
        val currentSession = session ?: return false
        val currentMedia = VideoPlayerEngine.playbackState.value.media ?: return false
        if (!matches(currentMedia)) return false
        root?.detachRender()
        if (!VideoPlayerEngine.restoreSystemOverlay(currentSession, targetToken)) return false
        removeViewOnly()
        return true
    }

    /** 用户关闭悬浮窗时真正结束播放会话。 */
    @MainThread
    fun close() {
        val activeToken = session?.token
        val app = application
        removeViewOnly()
        if (activeToken != null && app != null) VideoPlayerEngine.release(app, activeToken)
    }

    /** 保留悬浮窗画面，等待目标详情页创建并主动接管。 */
    private fun requestRestore() {
        val intent = restoreIntent ?: return
        runCatching { intent.send() }
    }

    /** 仅移除系统窗口，不停止播放器；恢复详情时仍需继续播放。 */
    private fun removeViewOnly() {
        root?.let { view -> runCatching { windowManager?.removeViewImmediate(view) } }
        rootRef = null
        windowManager = null
        session = null
        restoreIntent = null
        application = null
        windowParams = null
        positionController?.release()
        positionController = null
    }

    /** 创建系统悬浮窗的窗口参数，尺寸由 [resolveOverlaySize] 决定。 */
    private fun layoutParams(media: VideoMedia) = resolveOverlaySize(media).let { (width, height) ->
        WindowManager.LayoutParams(
            width, height,
        overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.END; x = dp(12); y = dp(96) }
    }

    /**
     * 计算悬浮窗尺寸。
     *
     * 有效封面宽高时，以默认宽度按原比例计算高度；超过最大尺寸或屏幕安全范围时等比缩小。
     * 缺少有效封面宽高时，保持默认 180×101dp。
     */
    private fun resolveOverlaySize(media: VideoMedia): Pair<Int, Int> {
        val defaultWidth = dp(DEFAULT_WIDTH_DP)
        val defaultHeight = dp(DEFAULT_HEIGHT_DP)
        val coverWidth = media.coverWidth?.takeIf { it > 0 } ?: return defaultWidth to defaultHeight
        val coverHeight =
            media.coverHeight?.takeIf { it > 0 } ?: return defaultWidth to defaultHeight
        val desiredHeight = defaultWidth.toFloat() * coverHeight / coverWidth
        val metrics = android.content.res.Resources.getSystem().displayMetrics
        val scale = minOf(
            1f,
            dp(MAX_WIDTH_DP).toFloat() / defaultWidth,
            dp(MAX_HEIGHT_DP).toFloat() / desiredHeight,
            (metrics.widthPixels - dp(SCREEN_MARGIN_DP) * 2).coerceAtLeast(1)
                .toFloat() / defaultWidth,
            (metrics.heightPixels - dp(SCREEN_MARGIN_DP) * 2).coerceAtLeast(1)
                .toFloat() / desiredHeight,
        )
        return (defaultWidth * scale).roundToInt() to (desiredHeight * scale).roundToInt()
    }

    /** Android 8.0 起使用专用悬浮窗类型；Android 7.x 仍需使用旧的 TYPE_PHONE。 */
    @Suppress("DEPRECATION")
    private fun overlayWindowType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun dp(value: Int): Int =
        (value * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

    private const val DEFAULT_WIDTH_DP = 180
    private const val DEFAULT_HEIGHT_DP = 101
    private const val MAX_WIDTH_DP = 280
    private const val MAX_HEIGHT_DP = 320
    private const val SCREEN_MARGIN_DP = 12
    private const val LOG_TAG = "HashVideoOverlay"
}
