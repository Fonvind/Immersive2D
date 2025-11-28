package github.fonvind.immersive2d.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Plane {
    public static double CULL_DIST = -0.5;
    public List<Entity> containedEntities = new ArrayList<>();

    private Vec3d offset;
    private double yaw;

    private Vec3d normal;
    private double slope;

    public Plane(Vec3d offset, double yaw) {
        this.offset = offset;
        this.yaw = yaw;
        updateValues();
    }

    public void setOffset(Vec3d offset) {
        this.offset = offset;
        updateValues();
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
        updateValues();
    }

    public Vec3d getOffset() {
        return offset;
    }

    public double getYaw() {
        return yaw;
    }

    public Vec3d getNormal() {
        return normal;
    }

    public double getSlope() {
        return slope;
    }

    private void updateValues() {
        this.slope = Math.tan(yaw);
        this.normal = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    public Vec3d intersectPoint(Vec3d point) {
        // Using a more stable geometric formula based on vector projection
        // This avoids division and issues with vertical/horizontal lines

        // Vector from the plane's origin (offset) to the point
        Vec3d pointToPlaneOrigin = point.subtract(this.offset);

        // The plane's direction vector (tangent)
        Vec3d planeDirection = new Vec3d(Math.cos(this.yaw), 0, Math.sin(this.yaw));

        // Project the vector onto the plane's direction to find the closest point on the line
        double dotProduct = pointToPlaneOrigin.dotProduct(planeDirection);
        Vec3d projectedVector = planeDirection.multiply(dotProduct);

        // The intersection point is the plane's origin plus the projected vector
        Vec3d intersection = this.offset.add(projectedVector);

        return new Vec3d(intersection.x, point.y, intersection.z);
    }

    // Positive is defined as being counter-clockwise
    public double sdf(Vec3d point) {
        Vec3d intersect = intersectPoint(point);

        Vec3d to_point = new Vec3d(point.x - intersect.x, 0, point.z - intersect.z);
        return to_point.length() * MathHelper.sign(to_point.dotProduct(getNormal()));
    }

    public static boolean shouldCull(BlockPos blockPos, Plane plane) {
        if (plane != null) {
            double dist = plane.sdf(blockPos.toCenterPos());
            return dist <= Plane.CULL_DIST || dist > 32;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Plane{" +
                "offset=" + offset +
                ", yaw=" + yaw +
                '}';
    }
}
