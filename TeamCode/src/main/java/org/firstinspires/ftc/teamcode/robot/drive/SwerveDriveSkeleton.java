package org.firstinspires.ftc.teamcode.robot.drive;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Coaxial swerve skeleton for future hardware. Not used in production OpModes until tuned.
 * Pedro {@code CoaxialPod} wiring is documented here; implementation is added when swerve modules exist.
 *
 * <p>See {@code docs/SWERVE_MIGRATION.md} for device names and tuning steps.
 */
public final class SwerveDriveSkeleton {
    private SwerveDriveSkeleton() {}

    // --- Placeholder pod names (TODO: add to DeviceNames + Robot Config when hardware exists) ---
    public static final String FL_POD_ANGLE = "fl_pod_angle";
    public static final String FL_POD_DRIVE = "fl_pod_drive";
    public static final String FR_POD_ANGLE = "fr_pod_angle";
    public static final String FR_POD_DRIVE = "fr_pod_drive";
    public static final String BL_POD_ANGLE = "bl_pod_angle";
    public static final String BL_POD_DRIVE = "bl_pod_drive";
    public static final String BR_POD_ANGLE = "br_pod_angle";
    public static final String BR_POD_DRIVE = "br_pod_drive";

    /**
     * Builds a Pedro swerve follower once hardware and Pedro swerve APIs are wired.
     *
     * <pre>{@code
     * // Future implementation (per Pedro swerve setup docs):
     * CoaxialPod fl = createPod(hardwareMap, FL_POD_ANGLE, FL_POD_DRIVE, flOffsetRad, analogMin, analogMax);
     * // ... fr, bl, br ...
     * return new FollowerBuilder(Constants.FOLLOWER_CONSTANTS, hardwareMap)
     *         .swerveDrivetrain(swerveConstants, fl, fr, bl, br)
     *         .pinpointLocalizer(Constants.LOCALIZER_CONSTANTS)
     *         .pathConstraints(Constants.PATH_CONSTRAINTS)
     *         .build();
     * }</pre>
     */
    public static Follower createFollower(HardwareMap hardwareMap) {
        throw new UnsupportedOperationException(
                "SWERVE_SKELETON selected but swerve is not implemented yet. "
                        + "Keep Constants.DRIVE_CONFIGURATION = MECANUM until pods are built. "
                        + "See docs/SWERVE_MIGRATION.md.");
    }
}
