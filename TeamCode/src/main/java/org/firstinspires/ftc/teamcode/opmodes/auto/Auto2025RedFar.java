package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.teamcode.RR.PoseBridge;
import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.RobotState;
import org.firstinspires.ftc.teamcode.robot.auto.actions.HoodAndFlywheelUpdateAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.LedFadeAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.RevAndAimAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.ShootAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.StartIntakeAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.StopIntakeAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.StopLauncherAction;
import org.firstinspires.ftc.teamcode.robot.auto.actions.TurretAimAction;
import org.firstinspires.ftc.teamcode.robot.shooter.FlywheelSubsystem;
import org.firstinspires.ftc.teamcode.util.Action;
import org.firstinspires.ftc.teamcode.util.Actions;
import org.firstinspires.ftc.teamcode.util.FollowPathAction;
import org.firstinspires.ftc.teamcode.util.ParallelAction;
import org.firstinspires.ftc.teamcode.util.Pose2d;
import org.firstinspires.ftc.teamcode.util.SequentialAction;

@Autonomous(name = "Auto2025RedFar")
public class Auto2025RedFar extends LinearOpMode {
    private volatile boolean autoRunning = true;

    @Override
    public void runOpMode() {
        RobotContainer robot = new RobotContainer();
        robot.init(hardwareMap, gamepad1, gamepad2, true);
        robot.state.setAllianceBlue(false);
        robot.drive.setPose(new Pose2d(60, 10, Math.toRadians(90)));
        if (robot.vision != null) robot.vision.setDecimation(3);
        Servo led = hardwareMap.get(Servo.class, "LED");
        double goalX = robot.state.getGoalX();
        double goalY = robot.state.getGoalY();

        telemetry.addData(">", "Red Far. Trajectories from redFar (MeepMeep).");
        telemetry.update();
        waitForStart();
        if (!opModeIsActive()) return;

        double r90 = Math.toRadians(90);
        Pose2d p1 = new Pose2d(60, 62, r90);
        Pose2d p2a = new Pose2d(50, 62, r90);
        Pose2d p2b = new Pose2d(60, 50, r90);
        Pose2d p3 = new Pose2d(60, 20, r90);
        Pose2d p4 = new Pose2d(60, 60, r90);
        Pose2d p5 = new Pose2d(50, 30, r90);
        Action tab1 = new FollowPathAction(robot.drive, robot.drive.buildPath(robot.state.getPose(), p1));
        Action tab2 = new FollowPathAction(robot.drive, robot.drive.buildPathChain(p1, p2a, p2b, p1, p2a));
        Action tab3 = new FollowPathAction(robot.drive, robot.drive.buildPath(p2a, p3));
        Action tab4 = new FollowPathAction(robot.drive, robot.drive.buildPath(p3, p4));
        Action tab5 = new FollowPathAction(robot.drive, robot.drive.buildPath(p4, p3));
        Action tab6 = new FollowPathAction(robot.drive, robot.drive.buildPath(p3, p4));
        Action tab7 = new FollowPathAction(robot.drive, robot.drive.buildPath(p4, p3));
        Action tab10 = new FollowPathAction(robot.drive, robot.drive.buildPath(p3, p5));

        try {
            autoRunning = true;
            FlywheelSubsystem.shootToleranceOverride = 15;
            Action mainSequence = new SequentialAction(
                    new RevAndAimAction(robot, goalX, goalY, 4.0),
                    new ShootAction(robot, goalX, goalY, 1.0),
                    tab1,
                    new StartIntakeAction(robot),
                    tab2,
                    new StopIntakeAction(robot),
                    tab3,
                    new RevAndAimAction(robot, goalX, goalY, 1.0),
                    new ShootAction(robot, goalX, goalY, 1.0),
                    new StartIntakeAction(robot),
                    tab4,
                    new StopIntakeAction(robot),
                    tab5,
                    new RevAndAimAction(robot, goalX, goalY, 1.0),
                    new ShootAction(robot, goalX, goalY, 1.0),
                    new StartIntakeAction(robot),
                    tab6,
                    new StopIntakeAction(robot),
                    tab7,
                    new RevAndAimAction(robot, goalX, goalY, 1.0),
                    new ShootAction(robot, goalX, goalY, 1.0),
                    tab10,
                    new StopIntakeAction(robot),
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
            PoseBridge.saveAlliance(false);
            robot.shootSequence.stopLauncherAuto();
            robot.shootSequence.runIntakeAuto(false);
        } finally {
            FlywheelSubsystem.shootToleranceOverride = 0;
        }
    }
}
