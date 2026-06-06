package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.RobotState;
import org.firstinspires.ftc.teamcode.robot.teleop.TeleopController;

import java.util.List;

/**
 * Production teleop. Thin entry point — all loop logic lives in {@link org.firstinspires.ftc.teamcode.robot.teleop.TeleopController}.
 *
 * <p>Init: {@link org.firstinspires.ftc.teamcode.robot.RobotContainer} wires hardware once.
 * Tune runtime values via FTC Dashboard {@code @Config} fields below.
 */
@TeleOp(name = "MainTeleop")
@Config
public class MainTeleop extends LinearOpMode {
    private static final int LOOP_SLEEP_MS = 20;

    public static boolean allianceBlue = true;
    public static double blueStartX = -50.0, blueStartY = -50.0, blueStartHeadingDeg = -128.0;
    public static double blueGoalX = -70.0, blueGoalY = -70.0;
    public static double redStartX = -50.0, redStartY = 50.0, redStartHeadingDeg = 128.0;
    public static double redGoalX = -70.0, redGoalY = 70.0;

    public static boolean turretVelocityCompensation = false;
    public static double turretVelocityCompGain = 0.5;
    public static double hoodSpeedVelocityCompGain = 1.5;
    public static double turretAngleOffsetDeg = 0.0;
    public static boolean useCameraRelocalization = false;
    public static boolean positionHoldEnabled = true;
    public static double positionHoldDeadband = 0.08;
    public static double driveScale = 0.75;
    public static int closeZoneRumbleMs = 300;
    public static double airsortAdvanceDelayMs = 250;

    /**
     * false = FTC Dashboard field overlay (default). true = forward telemetry to Panels
     * (https://panels.bylazar.com); requires fullpanels on classpath.
     */
    public static boolean usePanelsDashboard = false;

    @Override
    public void runOpMode() {
        List<LynxModule> hubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : hubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        syncAllianceConstantsToRobotState();
        RobotContainer robot = new RobotContainer();
        robot.init(hardwareMap, gamepad1, gamepad2, true);
        TeleopController teleopController = new TeleopController(this, robot);
        teleopController.attachLiveTelemetry(usePanelsDashboard);
        robot.drive.startTeleopDrive();

        while (!isStarted() && !isStopRequested()) {
            if (gamepad1.dpad_left) allianceBlue = true;
            if (gamepad1.dpad_right) allianceBlue = false;
            telemetry.addLine("--- INIT: Select alliance ---");
            telemetry.addData("Alliance", allianceBlue ? "BLUE" : "RED");
            telemetry.addData("Start", "%.0f, %.0f @ %.0f",
                    allianceBlue ? blueStartX : redStartX,
                    allianceBlue ? blueStartY : redStartY,
                    allianceBlue ? blueStartHeadingDeg : redStartHeadingDeg);
            telemetry.addData("Goal", "%.0f, %.0f",
                    allianceBlue ? blueGoalX : redGoalX,
                    allianceBlue ? blueGoalY : redGoalY);
            telemetry.update();
            sleep(LOOP_SLEEP_MS);
        }

        waitForStart();
        if (isStopRequested()) return;

        while (opModeIsActive()) {
            teleopController.update(buildSettings());
            sleep(LOOP_SLEEP_MS);
        }
    }

    private TeleopController.Settings buildSettings() {
        TeleopController.Settings settings = new TeleopController.Settings();
        settings.allianceBlue = allianceBlue;
        settings.turretVelocityCompensation = turretVelocityCompensation;
        settings.turretVelocityCompGain = turretVelocityCompGain;
        settings.hoodSpeedVelocityCompGain = hoodSpeedVelocityCompGain;
        settings.turretAngleOffsetDeg = turretAngleOffsetDeg;
        settings.useCameraRelocalization = useCameraRelocalization;
        settings.positionHoldEnabled = positionHoldEnabled;
        settings.positionHoldDeadband = positionHoldDeadband;
        settings.driveScale = driveScale;
        settings.closeZoneRumbleMs = closeZoneRumbleMs;
        settings.airsortAdvanceDelayMs = airsortAdvanceDelayMs;
        return settings;
    }

    private static void syncAllianceConstantsToRobotState() {
        RobotState.blueStartX = blueStartX;
        RobotState.blueStartY = blueStartY;
        RobotState.blueStartHeadingDeg = blueStartHeadingDeg;
        RobotState.blueGoalX = blueGoalX;
        RobotState.blueGoalY = blueGoalY;
        RobotState.redStartX = redStartX;
        RobotState.redStartY = redStartY;
        RobotState.redStartHeadingDeg = redStartHeadingDeg;
        RobotState.redGoalX = redGoalX;
        RobotState.redGoalY = redGoalY;
    }
}
