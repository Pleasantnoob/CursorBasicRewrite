package org.firstinspires.ftc.teamcode.robot;



import com.qualcomm.robotcore.hardware.Gamepad;

import com.qualcomm.robotcore.hardware.HardwareMap;



import org.firstinspires.ftc.teamcode.RR.PoseBridge;

import org.firstinspires.ftc.teamcode.robot.drive.DriveSubsystem;

import org.firstinspires.ftc.teamcode.robot.drive.PedroMecanumDrive;

import org.firstinspires.ftc.teamcode.robot.intake.AirSort;

import org.firstinspires.ftc.teamcode.robot.intake.BallSensorArray;

import org.firstinspires.ftc.teamcode.robot.intake.IntakeSubsystem;

import org.firstinspires.ftc.teamcode.robot.intake.TransferSubsystem;

import org.firstinspires.ftc.teamcode.robot.led.LedSubsystem;

import org.firstinspires.ftc.teamcode.robot.shooter.FlywheelSubsystem;

import org.firstinspires.ftc.teamcode.robot.shooter.HoodSubsystem;

import org.firstinspires.ftc.teamcode.robot.shooter.ShootSequence;

import org.firstinspires.ftc.teamcode.robot.shooter.ShooterTeleop;

import org.firstinspires.ftc.teamcode.robot.teleop.TeleopBindings;

import org.firstinspires.ftc.teamcode.robot.turret.TurretSubsystem;

import org.firstinspires.ftc.teamcode.robot.vision.AprilTagVision;

import org.firstinspires.ftc.teamcode.util.Pose2d;



import java.util.Objects;



/**

 * Composition root: constructs every subsystem exactly once per OpMode.

 *

 * <p>Call {@link #init(HardwareMap, Gamepad, Gamepad, boolean)} before using any field.

 * {@link #vision} and {@link #airSort} stay null when vision is disabled (DemoTeleop).

 *

 * See repo {@code docs/CODE_GUIDE.md} for a human-readable map of this package.

 */

public class RobotContainer {

    public final RobotState state = new RobotState();



    /** Set by {@link #init}; null before init. */

    public DriveSubsystem drive;

    public FlywheelSubsystem flywheel;

    public HoodSubsystem hood;

    public IntakeSubsystem intake;

    public TransferSubsystem transfer;

    public BallSensorArray balls;

    public ShootSequence shootSequence;

    public ShooterTeleop shooterTeleop;

    public TurretSubsystem turret;

    public LedSubsystem led;



    /** Null when {@code enableVision=false}. */

    public AprilTagVision vision;

    /** Null when vision disabled — never call without null check. */

    public AirSort airSort;



    public TeleopBindings bindings;



    private boolean initialized;



    /**

     * Wire all hardware. Safe to call once; repeated calls are ignored.

     *

     * @param driver          Gamepad 1 (may be null in demo — slow toggles skipped)

     * @param operator        Gamepad 2 (required for teleop shooter controls)

     * @param enableVision    false for DemoTeleop (no webcam / airsort)

     */

    public void init(HardwareMap hardwareMap, Gamepad driver, Gamepad operator, boolean enableVision) {

        if (initialized) return;

        Objects.requireNonNull(hardwareMap, "hardwareMap");



        bindings = new TeleopBindings(driver, operator);



        // --- Shooter stack (order: motors → sequence → teleop coordinator) ---

        flywheel = new FlywheelSubsystem(hardwareMap);

        hood = new HoodSubsystem(hardwareMap);

        intake = new IntakeSubsystem(hardwareMap);

        transfer = new TransferSubsystem(hardwareMap);

        balls = new BallSensorArray(hardwareMap);

        shootSequence = new ShootSequence(state, flywheel, hood, intake, transfer, balls);

        shooterTeleop = new ShooterTeleop(shootSequence);

        led = new LedSubsystem(hardwareMap);



        // --- Alliance / start pose (PoseBridge hands off from auto → teleop) ---

        if (PoseBridge.hasAlliance()) {

            state.setAllianceBlue(PoseBridge.getAlliance());

            PoseBridge.clearAlliance();

        } else {

            state.setAllianceBlue(true);

        }



        Pose2d initialPose;

        boolean cameFromAuto = PoseBridge.hasPose();

        if (cameFromAuto) {

            initialPose = PoseBridge.getPose();

            PoseBridge.clear();

        } else {

            initialPose = new Pose2d(state.getStartX(), state.getStartY(), Math.toRadians(state.getStartHeadingDeg()));

        }



        drive = new PedroMecanumDrive(hardwareMap, initialPose);

        turret = new TurretSubsystem(hardwareMap, cameFromAuto);



        if (enableVision) {

            vision = new AprilTagVision(hardwareMap, true);

            airSort = new AirSort(vision.processor, balls.slot1, balls.slot2, balls.slot3);

        }



        initialized = true;

    }



    /** True after {@link #init} completed successfully. */

    public boolean isInitialized() {

        return initialized;

    }



    /**

     * Updates Pedro follower pose and propagates heading to turret + RobotState.

     * Call once per loop in teleop and inside auto actions.

     */

    public void updateOdometry() {

        if (!initialized || drive == null || turret == null) {

            throw new IllegalStateException("RobotContainer.init() must be called before updateOdometry()");

        }

        drive.update();

        state.updateFromPose(drive.getPose());

        turret.setRobotHeadingDeg(Math.toDegrees(drive.getPose().heading));

    }

}


