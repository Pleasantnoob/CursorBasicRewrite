package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.shooter.ShooterTeleop;
import org.firstinspires.ftc.teamcode.robot.telemetry.LiveTelemetry;
import org.firstinspires.ftc.teamcode.robot.turret.TurretSubsystem;
import org.firstinspires.ftc.teamcode.util.Pose2d;

import java.util.List;

/**
 * Showcase teleop: drive disabled, all controls on Gamepad 2.
 *
 * <p><b>G2 controls</b>
 * <ul>
 *   <li>Left stick X — manual turret (robot-relative)</li>
 *   <li>D-pad Up/Down — target velocity ±100</li>
 *   <li>D-pad Left/Right — hood angle ±2°</li>
 *   <li>Left trigger — rev flywheel</li>
 *   <li>Right trigger — shoot when at speed</li>
 *   <li>A — center turret</li>
 *   <li>X / Start — turret encoder re-sync</li>
 *   <li>Right bumper — intake in</li>
 *   <li>Left bumper — intake out</li>
 *   <li>B — hard stop (flywheel, intake, transfer)</li>
 * </ul>
 */
@TeleOp(name = "Demo / Showcase", group = "Toros")
@Config
public class DemoTeleop extends LinearOpMode {
    private static final int LOOP_SLEEP_MS = 20;
    private static final double MANUAL_DEG_PER_UNIT = 4.0;
    private static final int RESYNC_RUMBLE_MS = 300;

    public static double startX = -50.0;
    public static double startY = -50.0;
    public static double startHeadingDeg = -128.0;

    /** false = FTC Dashboard (default). true = also forward to Panels. */
    public static boolean usePanelsDashboard = false;

    private RobotContainer robot;
    private TurretSubsystem turret;
    private LiveTelemetry liveTelemetry;

    private double k;
    private double manualTurretDeg;
    private boolean prevStart;
    private boolean turretInitialized;

    @Override
    public void runOpMode() {
        List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        ShooterTeleop.manualMode = true;
        ShooterTeleop.manualTargetTicksPerSec = -1600.0;
        ShooterTeleop.manualHoodAngleDeg = 55.0;
        ShooterTeleop.demoTargetTicksPerSec = -1600.0;

        robot = new RobotContainer();
        robot.init(hardwareMap, null, gamepad2, false);
        liveTelemetry = new LiveTelemetry();
        liveTelemetry.attach(this, usePanelsDashboard);
        if (!usePanelsDashboard) {
            telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        }
        robot.drive.setPose(new Pose2d(startX, startY, Math.toRadians(startHeadingDeg)));
        turret = robot.turret.turret;

        while (!isStarted() && opModeIsActive()) {
            telemetry.addLine("--- Demo / Showcase (Gamepad 2 only) ---");
            telemetry.addLine("Stick X = turret | D-pad U/D = vel ±100 | D-pad L/R = hood ±2°");
            telemetry.addLine("LT = rev | RT = fire at speed | A = center | X/Start = resync");
            telemetry.addLine("RB = intake in | LB = intake out | B = hard stop");
            telemetry.addData("Target vel", ShooterTeleop.demoTargetTicksPerSec);
            telemetry.addData("Hood deg", ShooterTeleop.manualHoodAngleDeg);
            telemetry.update();
            sleep(LOOP_SLEEP_MS);
        }

        waitForStart();

        while (opModeIsActive()) {
            robot.updateOdometry();
            robot.drive.setTeleOpDrive(0, 0, 0, false);

            if (!turretInitialized) {
                k = turret.botHeading;
                manualTurretDeg = turret.getTurretAngleRobot();
                turretInitialized = true;
            }

            if (gamepad2.xWasPressed() || (gamepad2.start && !prevStart)) {
                turret.resyncEncoder();
                manualTurretDeg = 0;
                turret.targetAngle = k;
                gamepad2.rumble(RESYNC_RUMBLE_MS);
            }
            prevStart = gamepad2.start;

            updateManualTurret();
            turret.runTurretNoGyro(k);

            robot.shooterTeleop.updateDemo(gamepad2);

            updateTelemetry();
            sleep(LOOP_SLEEP_MS);
        }
    }

    private void updateManualTurret() {
        if (gamepad2.aWasPressed()) {
            manualTurretDeg = 0;
        }
        manualTurretDeg += gamepad2.left_stick_x * MANUAL_DEG_PER_UNIT;
        manualTurretDeg = Math.max(-TurretSubsystem.manualModeLimitDeg, Math.min(TurretSubsystem.manualModeLimitDeg, manualTurretDeg));
        turret.setAngle(k + manualTurretDeg);
    }

    private void updateTelemetry() {
        double actualVel = robot.shootSequence.getFlywheelVelocity();
        double targetVel = robot.shootSequence.getTargetTicksPerSec();
        double velError = Math.abs(actualVel - targetVel);
        boolean atSpeed = robot.shootSequence.isAtTargetSpeed();

        telemetry.addLine("--- Demo / Showcase (G2 only) ---");
        telemetry.addData("Drive", "DISABLED");
        telemetry.addData("Turret robot deg", turret.getTurretAngleRobot());
        telemetry.addData("Turret target field deg", turret.targetAngle);
        telemetry.addLine("");
        telemetry.addLine("--- Launcher ---");
        telemetry.addData("Target vel", "%.0f", targetVel);
        telemetry.addData("Actual vel", "%.0f", actualVel);
        telemetry.addData("Vel error", "%.0f", velError);
        telemetry.addData("AT SPEED", atSpeed && gamepad2.left_trigger > 0.1);
        telemetry.addData("Hood deg", ShooterTeleop.manualHoodAngleDeg);
        telemetry.addLine("");
        telemetry.addLine("--- Intake ---");
        telemetry.addData("RB", "intake in | LB = out | B = hard stop");
        telemetry.update();
        if (liveTelemetry != null) {
            liveTelemetry.forwardPanelsOnly(this);
        }
    }
}
