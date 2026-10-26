package com.hash.mine.floorplan

import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.hash.mine.data.repository.FloorplanTicketRepository
import com.hash.router.FloorplanTicketResult
import com.hash.router.RouterActivityPath
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.launch

/**
 * Mine 模块中的户型 Flutter Add-to-App 页面。
 *
 * Flutter 仅能通过 MethodChannel 获得短期 CloudBase Ticket，不会接触 Hash 的 Cookie。
 */
@Route(path = RouterActivityPath.Floorplan.EDITOR)
class FloorplanFlutterActivity : FlutterActivity() {

    private val identityProvider = FloorplanTicketRepository()
    private var authChannel: MethodChannel? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        authChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            AUTH_CHANNEL,
        ).also { channel ->
            channel.setMethodCallHandler { call, result ->
                if (call.method != ISSUE_TICKET_METHOD) {
                    result.notImplemented()
                    return@setMethodCallHandler
                }
                lifecycleScope.launch {
                    when (val ticketResult = identityProvider.issueCloudBaseTicket()) {
                        is FloorplanTicketResult.Success -> result.success(ticketResult.ticket)
                        FloorplanTicketResult.NotLoggedIn -> result.error(
                            HASH_NOT_LOGGED_IN,
                            "请先登录 Hash 后再同步户型",
                            null,
                        )
                        is FloorplanTicketResult.Failed -> result.error(
                            HASH_TICKET_UNAVAILABLE,
                            ticketResult.message,
                            null,
                        )
                    }
                }
            }
        }
    }

    override fun cleanUpFlutterEngine(flutterEngine: FlutterEngine) {
        authChannel?.setMethodCallHandler(null)
        authChannel = null
        super.cleanUpFlutterEngine(flutterEngine)
    }

    private companion object {
        const val AUTH_CHANNEL = "com.hash.floorplan/auth"
        const val ISSUE_TICKET_METHOD = "issueCloudBaseTicket"
        const val HASH_NOT_LOGGED_IN = "HASH_NOT_LOGGED_IN"
        const val HASH_TICKET_UNAVAILABLE = "HASH_TICKET_UNAVAILABLE"
    }
}
