package org.firstinspires.ftc.teamcode.robot.drive;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.util.FieldCoordinates;
import org.firstinspires.ftc.teamcode.util.Pose2d;

/**
 * Pedro Pathing mecanum drive. All public poses are FTC official center frame; Pedro corner frame
 * is applied internally via {@link FieldCoordinates}.
 */
public final class PedroMecanumDrive implements DriveSubsystem {
    private static final double[] ZERO_VELOCITY = {0, 0, 0};

    private final Follower follower;

    public PedroMecanumDrive(HardwareMap hardwareMap, Pose2d initialPose) {
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(toPedro(initialPose));
    }

    public static Pose toPedro(Pose2d p) {
        return FieldCoordinates.toPedro(p);
    }

    public static Pose2d fromPedro(Pose p) {
        return FieldCoordinates.fromPedro(p);
    }

    @Override
    public void update() { follower.update(); }

    @Override
    public void startTeleopDrive() { follower.startTeleopDrive(); }

    @Override
    public void setTeleOpDrive(double lx, double ly, double rx, boolean fieldCentric) {
        follower.setTeleOpDrive(lx, ly, rx, fieldCentric);
    }

    @Override
    public void breakFollowing() {
        follower.breakFollowing();
        follower.startTeleopDrive();
    }

    @Override
    public Pose2d getPose() { return fromPedro(follower.getPose()); }

    @Override
    public void setPose(Pose2d pose) { follower.setStartingPose(toPedro(pose)); }

    @Override
    public double[] getVelocity() {
        try {
            com.pedropathing.math.Vector v = follower.getVelocity();
            if (v == null) return ZERO_VELOCITY;
            return new double[]{v.getXComponent(), v.getYComponent(), follower.getAngularVelocity()};
        } catch (Throwable t) {
            return ZERO_VELOCITY;
        }
    }

    @Override
    public PathChain buildPath(Pose2d start, Pose2d end) {
        Pose ps = toPedro(start), pe = toPedro(end);
        return follower.pathBuilder()
                .addPath(new BezierLine(ps, pe))
                .setLinearHeadingInterpolation(ps.getHeading(), pe.getHeading())
                .build();
    }

    @Override
    public PathChain buildPathChain(Pose2d... waypoints) {
        if (waypoints == null || waypoints.length < 2) {
            throw new IllegalArgumentException("Need at least 2 waypoints");
        }
        com.pedropathing.paths.PathBuilder b = follower.pathBuilder();
        for (int i = 0; i < waypoints.length - 1; i++) {
            Pose s = toPedro(waypoints[i]), e = toPedro(waypoints[i + 1]);
            b.addPath(new BezierLine(s, e)).setLinearHeadingInterpolation(s.getHeading(), e.getHeading());
        }
        return b.build();
    }

    @Override
    public void followPath(PathChain pathChain, boolean holdEnd) {
        follower.followPath(pathChain, holdEnd);
    }

    @Override
    public boolean isBusy() { return follower.isBusy(); }
}
