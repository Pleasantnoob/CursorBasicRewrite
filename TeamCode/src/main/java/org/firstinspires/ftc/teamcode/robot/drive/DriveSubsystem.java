package org.firstinspires.ftc.teamcode.robot.drive;

import com.pedropathing.paths.PathChain;
import org.firstinspires.ftc.teamcode.util.Pose2d;

/**
 * Robot drive abstraction. Implementations: mecanum (Pedro) now; swerve skeleton later.
 */
public interface DriveSubsystem {
    void update();
    void startTeleopDrive();
    void setTeleOpDrive(double forward, double strafe, double turn, boolean fieldCentric);
    void breakFollowing();
    Pose2d getPose();
    void setPose(Pose2d pose);
    double[] getVelocity();
    PathChain buildPath(Pose2d start, Pose2d end);
    PathChain buildPathChain(Pose2d... waypoints);
    void followPath(PathChain path, boolean holdEnd);
    boolean isBusy();
}
