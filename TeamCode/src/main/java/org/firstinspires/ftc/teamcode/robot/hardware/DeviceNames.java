package org.firstinspires.ftc.teamcode.robot.hardware;

/**
 * Single source of truth for Robot Configuration device names.
 * Names must match the Driver Station hardware config exactly.
 */
public final class DeviceNames {
    private DeviceNames() {}

    // --- Drive (mecanum) ---
    public static final String FL = "fl";
    public static final String BL = "bl";
    public static final String FR = "fr";
    public static final String BR = "br";

    // --- Odometry ---
    public static final String PINPOINT = "pinpoint";

    // --- Shooter / intake ---
    public static final String INTAKE = "intake";
    public static final String LAUNCH = "launch";
    public static final String TRANSFER = "trans";
    public static final String HOOD = "hood";
    public static final String TURRET = "turret";

    // --- Sensing ---
    public static final String COLOR_1 = "c1";
    public static final String COLOR_2 = "c2";
    public static final String COLOR_3 = "c3";
    public static final String LED = "LED";

    // --- Vision ---
    /** Must match Robot Configuration (space before 1). */
    public static final String WEBCAM = "Webcam 1";

    // --- Swerve placeholders (future hardware) ---
    public static final String FL_POD_DRIVE = "fl_pod_drive";
    public static final String FL_POD_ANGLE = "fl_pod_angle";
    public static final String FR_POD_DRIVE = "fr_pod_drive";
    public static final String FR_POD_ANGLE = "fr_pod_angle";
    public static final String BL_POD_DRIVE = "bl_pod_drive";
    public static final String BL_POD_ANGLE = "bl_pod_angle";
    public static final String BR_POD_DRIVE = "br_pod_drive";
    public static final String BR_POD_ANGLE = "br_pod_angle";
}
