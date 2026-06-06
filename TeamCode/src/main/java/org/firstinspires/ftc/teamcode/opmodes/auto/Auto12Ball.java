package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.RR.PoseBridge;
import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.auto.actions.IntakeRunAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.RevAndAimAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.ShootAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.StopLauncherAction;
import org.firstinspires.ftc.teamcode.util.Action;
import org.firstinspires.ftc.teamcode.util.Actions;
import org.firstinspires.ftc.teamcode.util.FollowPathAction;
import org.firstinspires.ftc.teamcode.util.ParallelAction;
import org.firstinspires.ftc.teamcode.util.Pose2d;
import org.firstinspires.ftc.teamcode.util.SequentialAction;

/**
 * 12-ball autonomous: 3 preloaded + 9 from spike marks.
 * Uses RobotContainer subsystems: Pedro drive, ShootSequence, TurretSubsystem.
 * Blue alliance: start (-55, -47) @ -130°, goal (-70, -64). Path: shoot 3 → spike 1 → shoot 3 → spike 2 → shoot 3 → spike 3 → shoot 3.
 */
@Autonomous(name = "Auto 12-Ball (Blue)")
public class Auto12Ball extends LinearOpMode {

    /** Blue goal (inches). Match MainDrive.blueGoalX/Y. */
    private static final double GOAL_X = -70.0;
    private static final double GOAL_Y = -64.0;
    /** Blue start. Match MainDrive.blueStart*. */
    private static final double START_X = -55.0;
    private static final double START_Y = -47.0;
    private static final double START_HEADING_RAD = Math.toRadians(-130.0);
    /** Shoot position: face goal, ~35–40 in from goal. */
    private static final double SHOOT_X = 38.0;
    private static final double SHOOT_Y = -52.0;
    private static final double SHOOT_HEADING_RAD = Math.PI;
    /** Spike mark waypoints (audience side, then into spike). Blue: negative Y. */
    private static final double SPIKE1_X = -12.0, SPIKE1_Y_END = -53.0;
    private static final double SPIKE2_X = 12.0,  SPIKE2_Y_END = -53.0;
    private static final double SPIKE3_X = 30.0, SPIKE3_Y_END = -55.0;

    @Override
    public void runOpMode() throws InterruptedException {
        RobotContainer robot = new RobotContainer();
        robot.init(hardwareMap, gamepad1, gamepad2, true);
        robot.state.setAllianceBlue(true);
        Pose2d initialPose = new Pose2d(START_X, START_Y, START_HEADING_RAD);
        robot.drive.setPose(initialPose);
        if (robot.vision != null) robot.vision.setDecimation(3);

        telemetry.addData(">", "12-ball Blue. Start when ready.");
        telemetry.update();
        waitForStart();

        if (!opModeIsActive()) return;

        Pose2d shootPose = new Pose2d(SHOOT_X, SHOOT_Y, SHOOT_HEADING_RAD);
        Pose2d spike1 = new Pose2d(SPIKE1_X, SPIKE1_Y_END - 15, SHOOT_HEADING_RAD);
        Pose2d spike1End = new Pose2d(SPIKE1_X, SPIKE1_Y_END, SHOOT_HEADING_RAD);
        Pose2d spike2 = new Pose2d(SPIKE2_X, SPIKE2_Y_END - 15, SHOOT_HEADING_RAD);
        Pose2d spike2End = new Pose2d(SPIKE2_X, SPIKE2_Y_END, SHOOT_HEADING_RAD);
        Pose2d spike3 = new Pose2d(SPIKE3_X, SPIKE3_Y_END - 15, SHOOT_HEADING_RAD);
        Pose2d spike3End = new Pose2d(SPIKE3_X, SPIKE3_Y_END, SHOOT_HEADING_RAD);
        Action toShoot = new FollowPathAction(robot.drive, robot.drive.buildPath(initialPose, shootPose));
        Action revAndAim = new RevAndAimAction(robot, GOAL_X, GOAL_Y, 2.0);
        Action shoot3 = new ShootAction(robot, GOAL_X, GOAL_Y, 4.0);
        Action toSpike1 = new FollowPathAction(robot.drive, robot.drive.buildPathChain(shootPose, spike1, spike1End));
        Action intakeRun1 = new IntakeRunAction(robot, 3.0, -1.0);
        Action backToShoot1 = new FollowPathAction(robot.drive, robot.drive.buildPath(spike1End, shootPose));
        Action toSpike2 = new FollowPathAction(robot.drive, robot.drive.buildPathChain(shootPose, spike2, spike2End));
        Action intakeRun2 = new IntakeRunAction(robot, 3.0, -1.0);
        Action backToShoot2 = new FollowPathAction(robot.drive, robot.drive.buildPath(spike2End, shootPose));
        Action toSpike3 = new FollowPathAction(robot.drive, robot.drive.buildPathChain(shootPose, spike3, spike3End));
        Action intakeRun3 = new IntakeRunAction(robot, 3.0, -1.0);
        Action backToShoot3 = new FollowPathAction(robot.drive, robot.drive.buildPath(spike3End, shootPose));

        SequentialAction main = new SequentialAction(
                toShoot,
                revAndAim,
                shoot3,
                new ParallelAction(toSpike1, intakeRun1),
                backToShoot1,
                revAndAim,
                shoot3,
                new ParallelAction(toSpike2, intakeRun2),
                backToShoot2,
                revAndAim,
                shoot3,
                new ParallelAction(toSpike3, intakeRun3),
                backToShoot3,
                revAndAim,
                shoot3,
                new StopLauncherAction(robot)
        );

        Actions.runBlocking(this, main);

        PoseBridge.save(robot.state.getPose());
        PoseBridge.saveAlliance(true);  // Blue
        robot.shootSequence.stopLauncherAuto();
        robot.shootSequence.runIntakeAuto(false);
    }
}
