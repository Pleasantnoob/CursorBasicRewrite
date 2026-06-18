package com.bylazar.cursoragent

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared bridge between the Panels plugin (WebSocket thread) and AgentTeleop (OpMode loop).
 */
object CursorAgentRuntime {
    private const val DRIVE_TTL_MS = 200L

    interface AgentExecutor {
        fun executeDrive(command: DriveCommand): CommandResult
        fun executeMotor(command: MotorCommand): CommandResult
        fun executeServo(command: ServoCommand): CommandResult
        fun executePath(command: PathCommand): CommandResult
        fun executeEstop(): CommandResult
        fun captureSnapshot(): AgentSnapshot
    }

    @JvmField
    val estop = AtomicBoolean(false)

    private val executorRef = AtomicReference<AgentExecutor?>(null)
    private val pendingDrive = AtomicReference<DriveCommand?>(null)
    private val driveExpiryMs = AtomicLong(0L)
    private val motorQueue = ConcurrentLinkedQueue<MotorCommand>()
    private val servoQueue = ConcurrentLinkedQueue<ServoCommand>()
    private val pathQueue = ConcurrentLinkedQueue<PathCommand>()
    private val lastCommand = AtomicReference("")
    private val lastError = AtomicReference("")
    private val lastSnapshot = AtomicReference(AgentSnapshot())
    private val pendingRequestId = AtomicReference("")

    @JvmStatic
    fun registerExecutor(executor: AgentExecutor?) {
        executorRef.set(executor)
    }

    @JvmStatic
    fun isExecutorReady(): Boolean = executorRef.get() != null

    @JvmStatic
    fun queueDrive(command: DriveCommand, requestId: String = ""): CommandResult {
        if (estop.get()) return fail("E-stop active", requestId)
        pendingDrive.set(command)
        driveExpiryMs.set(System.currentTimeMillis() + DRIVE_TTL_MS)
        lastCommand.set("drive")
        pendingRequestId.set(requestId)
        return ok("Drive queued", requestId)
    }

    @JvmStatic
    fun queueMotor(command: MotorCommand, requestId: String = ""): CommandResult {
        if (estop.get()) return fail("E-stop active", requestId)
        motorQueue.add(command)
        lastCommand.set("setMotor:${command.name}")
        pendingRequestId.set(requestId)
        return ok("Motor command queued", requestId)
    }

    @JvmStatic
    fun queueServo(command: ServoCommand, requestId: String = ""): CommandResult {
        if (estop.get()) return fail("E-stop active", requestId)
        servoQueue.add(command)
        lastCommand.set("setServo:${command.name}")
        pendingRequestId.set(requestId)
        return ok("Servo command queued", requestId)
    }

    @JvmStatic
    fun queuePath(command: PathCommand, requestId: String = ""): CommandResult {
        if (estop.get()) return fail("E-stop active", requestId)
        pathQueue.clear()
        pathQueue.add(command)
        lastCommand.set("followPath")
        pendingRequestId.set(requestId)
        return ok("Path queued", requestId)
    }

    @JvmStatic
    fun triggerEstop(requestId: String = ""): CommandResult {
        estop.set(true)
        pendingDrive.set(null)
        motorQueue.clear()
        servoQueue.clear()
        pathQueue.clear()
        executorRef.get()?.executeEstop()
        lastCommand.set("estop")
        pendingRequestId.set(requestId)
        return ok("E-stop engaged", requestId)
    }

    @JvmStatic
    fun clearEstop(requestId: String = ""): CommandResult {
        estop.set(false)
        lastCommand.set("clearEstop")
        pendingRequestId.set(requestId)
        return ok("E-stop cleared", requestId)
    }

    @JvmStatic
    fun getSnapshot(): AgentSnapshot {
        val executor = executorRef.get()
        val snapshot = if (executor != null) {
            try {
                executor.captureSnapshot()
            } catch (t: Throwable) {
                lastSnapshot.get().copy(
                    connected = true,
                    estop = estop.get(),
                    lastError = t.message ?: "snapshot failed",
                )
            }
        } else {
            AgentSnapshot(
                connected = false,
                estop = estop.get(),
                lastCommand = lastCommand.get(),
                lastError = "AgentTeleop not running",
            )
        }
        lastSnapshot.set(snapshot)
        return snapshot.copy(
            estop = estop.get(),
            lastCommand = lastCommand.get(),
            lastError = lastError.get(),
        )
    }

    @JvmStatic
    fun processLoopTick() {
        val executor = executorRef.get() ?: return
        if (estop.get()) {
            executor.executeEstop()
            return
        }

        val now = System.currentTimeMillis()
        val drive = pendingDrive.get()
        if (drive != null) {
            if (now <= driveExpiryMs.get()) {
                val result = executor.executeDrive(drive)
                if (!result.ok) lastError.set(result.message)
            } else {
                pendingDrive.set(null)
                executor.executeDrive(DriveCommand())
            }
        }

        while (true) {
            val motor = motorQueue.poll() ?: break
            val result = executor.executeMotor(motor)
            if (!result.ok) lastError.set(result.message)
        }

        while (true) {
            val servo = servoQueue.poll() ?: break
            val result = executor.executeServo(servo)
            if (!result.ok) lastError.set(result.message)
        }

        while (true) {
            val path = pathQueue.poll() ?: break
            val result = executor.executePath(path)
            if (!result.ok) lastError.set(result.message)
        }
    }

    private fun ok(message: String, requestId: String) = CommandResult(true, message, requestId)
    private fun fail(message: String, requestId: String): CommandResult {
        lastError.set(message)
        return CommandResult(false, message, requestId)
    }
}
