package github.fonvind.immersive2d.client;

import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class Immersive2DRaycaster {

    // Helper to create a missed BlockHitResult
    private static BlockHitResult createMissedBlockHitResult(Vec3d pos, net.minecraft.util.math.Direction side, net.minecraft.util.math.BlockPos blockPos) {
        return new BlockHitResult(pos, side, blockPos, true);
    }

    // Adapting the raycastCrosshair logic for our 2D plane
    public static HitResult raycast2D(Entity camera, double range, float tickDelta) {
        Plane plane = Immersive2DClient.plane;
        if (plane == null) {
            return createMissedBlockHitResult(camera.getPos(), net.minecraft.util.math.Direction.UP, camera.getBlockPos());
        }

        // Get player's eye position and look vector
        Vec3d cameraPos = camera.getCameraPosVec(tickDelta);
        Vec3d lookVec = camera.getRotationVec(tickDelta);

        // --- New Projection-Based Raycast Logic ---
        // Project the start and end points of the ray onto the 2D plane
        Vec3d projectedStart = plane.intersectPoint(cameraPos);
        Vec3d projectedEnd = plane.intersectPoint(cameraPos.add(lookVec.multiply(range)));

        // Perform a block raycast using the projected start and end points
        HitResult blockHitResult = camera.getWorld().raycast(new RaycastContext(
                projectedStart,
                projectedEnd,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                camera
        ));

        // Now, perform entity raycast, also using the projected vectors
        Box searchBox = camera.getBoundingBox().stretch(lookVec.multiply(range)).expand(1.0, 1.0, 1.0);
        
        EntityHitResult entityHitResult = ProjectileUtil.raycast(
            camera,
            projectedStart, // Use projected start
            projectedEnd,   // Use projected end
            searchBox,
            (entity -> !entity.isSpectator() && entity.canHit() && !Plane.shouldCull(entity.getBlockPos(), plane)), // Filter entities to the plane
            range * range // squaredDistance
        );

        // Compare block and entity hits
        double blockHitDistanceSq = blockHitResult.getType() != HitResult.Type.MISS ? blockHitResult.getPos().squaredDistanceTo(projectedStart) : Double.MAX_VALUE;
        double entityHitDistanceSq = entityHitResult != null ? entityHitResult.getPos().squaredDistanceTo(projectedStart) : Double.MAX_VALUE;

        if (entityHitResult != null && entityHitDistanceSq < blockHitDistanceSq) {
            return entityHitResult; // Entity is closer
        } else {
            return blockHitResult; // Block is closer or no entity hit
        }
    }
}
