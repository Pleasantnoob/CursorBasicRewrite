package com.bylazar.cursoragent

data class DriveCommand(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val rotate: Double = 0.0,
    val fieldCentric: Boolean = false,
)

data class MotorCommand(
    val name: String,
    val power: Double,
    val mode: String = "power",
)

data class ServoCommand(
    val name: String,
    val position: Double,
)

data class PathWaypoint(
    val x: Double,
    val y: Double,
    val headingDeg: Double = Double.NaN,
)

data class PathCommand(
    val waypoints: List<PathWaypoint>,
    val holdEnd: Boolean = true,
)

data class ConfigChange(
    val id: String,
    val value: String,
)

data class AgentSnapshot(
    val connected: Boolean = false,
    val estop: Boolean = false,
    val opModeActive: Boolean = false,
    val poseX: Double = 0.0,
    val poseY: Double = 0.0,
    val headingDeg: Double = 0.0,
    val velocityX: Double = 0.0,
    val velocityY: Double = 0.0,
    val lastCommand: String = "",
    val lastError: String = "",
    val pathBusy: Boolean = false,
    val visionTags: List<VisionTagSnapshot> = emptyList(),
    val telemetry: Map<String, String> = emptyMap(),
)

data class VisionTagSnapshot(
    val id: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val range: Double,
    val bearing: Double,
)

data class CommandResult(
    val ok: Boolean,
    val message: String = "",
    val requestId: String = "",
)
