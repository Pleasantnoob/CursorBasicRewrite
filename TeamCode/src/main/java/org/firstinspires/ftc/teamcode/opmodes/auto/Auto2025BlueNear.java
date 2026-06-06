package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.RR.PoseBridge;
import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.RobotState;
import org.firstinspires.ftc.teamcode.robot.auto.actions.HoodAndFlywheelUpdateAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.IntakeRunAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.LedFadeAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.RevAndAimAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.ShootAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.StopLauncherAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.TransRunAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.TurretAimAction;
import org.firstinspires.ftc.teamcode.util.Action;
import org.firstinspires.ftc.teamcode.util.Actions;
import org.firstinspires.ftc.teamcode.util.FollowPathAction;
import org.firstinspires.ftc.teamcode.util.ParallelAction;
import org.firstinspires.ftc.teamcode.util.Pose2d;
import org.firstinspires.ftc.teamcode.util.SequentialAction;
import org.firstinspires.ftc.teamcode.util.SleepAction;

@Autonomous(name = "Auto2025BlueNear")
public class Auto2025BlueNear extends LinearOpMode {
    private volatile boolean autoRunning = true;

    @Override
    public void runOpMode() {
        RobotContainer robot = new RobotContainer();
        robot.init(hardwareMap, gamepad1, gamepad2, true);
        robot.state.setAllianceBlue(true);
        robot.drive.setPose(new Pose2d(
                RobotState.blueStartX,
                RobotState.blueStartY,
                Math.toRadians(RobotState.blueStartHeadingDeg)
        ));
        if (robot.vision != null) robot.vision.setDecimation(3);
        Servo led = hardwareMap.get(Servo.class, "LED");

        double goalX = robot.state.getGoalX();
        double goalY = robot.state.getGoalY();
        telemetry.addData(">", "Blue Near");
        telemetry.update();
        waitForStart();
        if (!opModeIsActive()) return;

        double r270 = Math.toRadians(270), r235 = Math.toRadians(235), r250 = Math.toRadians(250), r128 = Math.toRadians(-128);
        Pose2d p0 = robot.state.getPose();
        Pose2d p1 = new Pose2d(-12, -12, r270);
        Pose2d p2 = new Pose2d(-12, -28, r270);
        Pose2d p3 = new Pose2d(-12, -53, r270);
        Pose2d p4 = new Pose2d(-12, -13, r270);
        Pose2d p5 = new Pose2d(12, -28, r270);
        Pose2d p6 = new Pose2d(12, -53, r270);
        Pose2d p7 = new Pose2d(-13, -12, r270);
        Pose2d p8 = new Pose2d(12, -59, r235);
        Pose2d p9 = new Pose2d(-13, -12, r250);
        Pose2d p10 = new Pose2d(-35, -15, r128);
        Action tab1 = new FollowPathAction(robot.drive, robot.drive.buildPath(p0, p1));
        Action tab2 = new FollowPathAction(robot.drive, robot.drive.buildPathChain(p1, p2, p3));
        Action tab3 = new FollowPathAction(robot.drive, robot.drive.buildPath(p3, p4));
        Action tab4 = new FollowPathAction(robot.drive, robot.drive.buildPathChain(p4, p5, p6));
        Action tab5 = new FollowPathAction(robot.drive, robot.drive.buildPath(p6, p7));
        Action tab6Gate = new FollowPathAction(robot.drive, robot.drive.buildPath(p7, p8));
        Action tab7Back = new FollowPathAction(robot.drive, robot.drive.buildPath(p8, p9));
        Action tab8Gate = new FollowPathAction(robot.drive, robot.drive.buildPath(p9, p8));
        Action tab10Park = new FollowPathAction(robot.drive, robot.drive.buildPath(p8, p10));

        Action mainSequence = new SequentialAction(
                tab1,
                new RevAndAimAction(robot, goalX, goalY, 1.0),
                new ShootAction(robot, goalX, goalY, 1.0),
                new ParallelAction(tab2, new IntakeRunAction(robot, 2.0, -1.0), new TransRunAction(robot, 0.3, -0.2)),
                tab3,
                new RevAndAimAction(robot, goalX, goalY, 1.0),
                new ShootAction(robot, goalX, goalY, 1.0),
                new ParallelAction(tab4, new SequentialAction(new SleepAction(0.5),
                        new ParallelAction(new IntakeRunAction(robot, 2.0, -1.0), new TransRunAction(robot, 0.3, -0.2)))),
                tab5,
                new RevAndAimAction(robot, goalX, goalY, 1.0),
                new ShootAction(robot, goalX, goalY, 1.0),
                new ParallelAction(tab6Gate, new SequentialAction(new SleepAction(1.0),
                        new ParallelAction(new IntakeRunAction(robot, 2.0, -1.0), new TransRunAction(robot, 0.3, -0.2)))),
                tab7Back,
                new RevAndAimAction(robot, goalX, goalY, 1.0),
                new ShootAction(robot, goalX, goalY, 1.0),
                new ParallelAction(tab8Gate, new SequentialAction(new SleepAction(1.0),
                        new ParallelAction(new IntakeRunAction(robot, 2.0, -1.0), new TransRunAction(robot, 0.3, -0.2)))),
                tab10Park,
                new StopLauncherAction(robot),
                packet -> {
                    autoRunning = false;
                    return false;
                }
        );

        Actions.runBlocking(this, new ParallelAction(
                new TurretAimAction(robot, goalX, goalY, () -> autoRunning),
                new HoodAndFlywheelUpdateAction(robot, goalX, goalY, () -> autoRunning),
                new LedFadeAction(led, () -> autoRunning),
                mainSequence
        ));

        PoseBridge.save(robot.state.getPose());
        PoseBridge.saveAlliance(true);
        robot.shootSequence.stopLauncherAuto();
        robot.shootSequence.runIntakeAuto(false);
    }
}
