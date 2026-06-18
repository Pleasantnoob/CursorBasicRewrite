package com.bylazar.cursoragent

import android.content.Context
import com.bylazar.panels.Panels
import com.bylazar.panels.json.SocketMessage
import com.bylazar.panels.plugins.BasePluginConfig
import com.bylazar.panels.plugins.Plugin
import com.bylazar.panels.server.Socket
import com.qualcomm.ftccommon.FtcEventLoop
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl

open class CursorAgentPluginConfig : BasePluginConfig()

object Plugin : Plugin<CursorAgentPluginConfig>(CursorAgentPluginConfig()) {
    private var statusTicker: Thread? = null
    @Volatile
    private var tickerRunning = false

    override fun onNewClient(client: Socket.ClientSocket) {
        sendClient(client, "agentSnapshot", CursorAgentRuntime.getSnapshot())
        sendClient(client, "agentReady", CursorAgentRuntime.isExecutorReady())
    }

    override fun onMessage(client: Socket.ClientSocket, type: String, data: Any?) {
        log("message type=$type data=$data")
        val requestId = extractRequestId(data)

        val result = when (type) {
            "drive" -> {
                val cmd = parsePayload<DriveCommand>(data) ?: DriveCommand()
                CursorAgentRuntime.queueDrive(cmd, requestId)
            }
            "setMotor" -> {
                val cmd = parsePayload<MotorCommand>(data)
                    ?: return reply(client, type, CommandResult(false, "invalid motor command", requestId), requestId)
                CursorAgentRuntime.queueMotor(cmd, requestId)
            }
            "setServo" -> {
                val cmd = parsePayload<ServoCommand>(data)
                    ?: return reply(client, type, CommandResult(false, "invalid servo command", requestId), requestId)
                CursorAgentRuntime.queueServo(cmd, requestId)
            }
            "followPath" -> {
                val cmd = parsePayload<PathCommand>(data)
                    ?: return reply(client, type, CommandResult(false, "invalid path command", requestId), requestId)
                CursorAgentRuntime.queuePath(cmd, requestId)
            }
            "estop" -> CursorAgentRuntime.triggerEstop(requestId)
            "clearEstop" -> CursorAgentRuntime.clearEstop(requestId)
            "getSnapshot", "getTelemetry" -> {
                reply(client, type, CursorAgentRuntime.getSnapshot(), requestId)
                return
            }
            "ping" -> CommandResult(true, "pong", requestId)
            else -> CommandResult(false, "unknown message: $type", requestId)
        }

        reply(client, type, result, requestId)
        send("agentSnapshot", CursorAgentRuntime.getSnapshot())
    }

    private fun reply(client: Socket.ClientSocket, type: String, payload: Any, requestId: String) {
        val envelope = mapOf(
            "requestId" to requestId,
            "type" to type,
            "payload" to payload,
        )
        sendClient(client, "agentResponse", envelope)
    }

    private fun extractRequestId(data: Any?): String {
        if (data == null) return ""
        return try {
            val map = SocketMessage.convertData<Map<String, Any>>(data) ?: return ""
            map["requestId"]?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private inline fun <reified T> parsePayload(data: Any?): T? {
        if (data == null) return null
        return try {
            val map = SocketMessage.convertData<MutableMap<String, Any?>>(data)?.toMutableMap() ?: return null
            map.remove("requestId")
            val json = SocketMessage.gson.toJson(map)
            SocketMessage.gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            log("parsePayload failed: ${e.message}")
            null
        }
    }

    override fun onRegister(panelsInstance: Panels, context: Context) {
        startStatusTicker()
        send("agentReady", CursorAgentRuntime.isExecutorReady())
    }

    override fun onAttachEventLoop(eventLoop: FtcEventLoop) {}

    override fun onOpModeManager(o: OpModeManagerImpl) {}

    override fun onOpModePreInit(opMode: OpMode) {
        send("agentReady", CursorAgentRuntime.isExecutorReady())
    }

    override fun onOpModePreStart(opMode: OpMode) {
        send("agentSnapshot", CursorAgentRuntime.getSnapshot())
    }

    override fun onOpModePostStop(opMode: OpMode) {
        CursorAgentRuntime.registerExecutor(null)
        send("agentReady", false)
    }

    override fun onEnablePanels() {}

    override fun onDisablePanels() {
        stopStatusTicker()
    }

    private fun startStatusTicker() {
        if (tickerRunning) return
        tickerRunning = true
        statusTicker = Thread {
            while (tickerRunning) {
                try {
                    send("agentSnapshot", CursorAgentRuntime.getSnapshot())
                    Thread.sleep(250)
                } catch (_: InterruptedException) {
                    break
                } catch (t: Throwable) {
                    error("status ticker failed: ${t.message}")
                }
            }
        }.also {
            it.isDaemon = true
            it.name = "CursorAgent-Status"
            it.start()
        }
    }

    private fun stopStatusTicker() {
        tickerRunning = false
        statusTicker?.interrupt()
        statusTicker = null
    }
}
