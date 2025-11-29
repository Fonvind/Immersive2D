package github.fonvind.immersive2d.client;

import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class Immersive2DRaycaster {

    // Helper to create a missed BlockHitResult
    private static BlockHitResult createMissedBlockHitResult(Vec3d pos, net.minecraft.util.math.Direction side, net.minecraft.util.math.BlockPos blockPos) {
        return new BlockHitResult(pos, side, blockPos, true);
    }

    // Adapting the raycastCrosshair logic for our 2D plane
    public static HitResult raycast2D(Entity camera, double range, float tickDelta) {
        Plane plane = Immersive2DClient.plane;
        if (plane == null) {
            // If no plane is active, fall back to vanilla raycasting (or return null/miss)
            // For now, let's return a miss to avoid unexpected behavior
            return createMissedBlockHitResult(camera.getPos(), net.minecraft.util.math.Direction.UP, camera.getBlockPos());
        }

        // Get player's eye position and look vector
        Vec3d cameraPos = camera.getCameraPosVec(tickDelta);
        Vec3d lookVec = camera.getRotationVec(tickDelta);

        // Calculate the end point of the ray in 3D space
        Vec3d rayEndPoint = cameraPos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);

        // --- Custom 2D Raycast Logic ---
        // We need to constrain the raycast to the 2D plane.
        // This means the ray should effectively be "flattened" onto the plane.
        // The player's look vector (lookVec) is already constrained by EntityMixin's setYaw/setPitch.
        // So, we can perform a vanilla raycast, but then filter its result.

        // Perform vanilla block raycast first
        HitResult blockHitResult = camera.getWorld().raycast(new RaycastContext( // Corrected access to world
                cameraPos,
                rayEndPoint,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                camera
        ));


        // Filter the block hit result to the 2D plane
        if (blockHitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) blockHitResult;
            if (Plane.shouldCull(blockHit.getBlockPos(), plane)) {
                // If the hit block is off the plane, treat it as a miss
                blockHitResult = createMissedBlockHitResult(blockHit.getPos(), blockHit.getSide(), blockHit.getBlockPos());
            }
        }

        // Now, perform entity raycast, also filtering to the 2D plane
        // The original snippet uses a box expanded by 1.0, 1.0, 1.0
        Box searchBox = camera.getBoundingBox().stretch(lookVec.multiply(range)).expand(1.0, 1.0, 1.0);
        
        EntityHitResult entityHitResult = ProjectileUtil.raycast(
            camera,
            cameraPos,
            rayEndPoint,
            searchBox,
            (entity -> !entity.isSpectator() && entity.canHit() && !Plane.shouldCull(entity.getBlockPos(), plane)), // Filter entities to the plane
            range * range // squaredDistance
        );

        // Compare block and entity hits
        double blockHitDistanceSq = blockHitResult.getType() != HitResult.Type.MISS ? blockHitResult.getPos().squaredDistanceTo(cameraPos) : Double.MAX_VALUE;
        double entityHitDistanceSq = entityHitResult != null ? entityHitResult.getPos().squaredDistanceTo(cameraPos) : Double.MAX_VALUE;

        if (entityHitResult != null && entityHitDistanceSq < blockHitDistanceSq) {
            return entityHitResult; // Entity is closer
        } else {
            return blockHitResult; // Block is closer or no entity hit
        }
    }
}
