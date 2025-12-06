package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private MinecraftClient client;

    /**
     * Run AFTER vanilla / Sodium have done their own targeting.
     * We then optionally replace the crosshair target with our 2D-plane-aware result.
     */
    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void immersive2d$refineHitWith2DPlane(float tickDelta, CallbackInfo ci) {
        Plane plane = Immersive2DClient.plane;
        if (plane == null) return;

        if (client.world == null || client.player == null) return;
        Entity camera = client.getCameraEntity();
        if (camera == null) return;

        // Keep vanilla result around as fallback
        HitResult vanillaHit = client.crosshairTarget;

        double blockRange = client.player.getBlockInteractionRange();
        double entityRange = client.player.getEntityInteractionRange();
        double maxRange = Math.max(blockRange, entityRange);

        // --------------------------------------------------------------------
        // 1) CAMERA RAY → constrain to plane, find block/entity hit on plane
        // --------------------------------------------------------------------
        Vec3d cameraPos = camera.getCameraPosVec(tickDelta);
        Vec3d lookVec = camera.getRotationVec(tickDelta);

        // Project camera and a distant point onto the 2D plane line
        Vec3d projectedStart = plane.intersectPoint(cameraPos);
        Vec3d projectedEnd = plane.intersectPoint(cameraPos.add(lookVec.multiply(maxRange)));

        // Block raycast along plane line
        BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                projectedStart,
                projectedEnd,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                camera
        ));

        // Entity raycast along same line
        Box searchBox = camera.getBoundingBox()
                .stretch(lookVec.multiply(maxRange))
                .expand(1.0, 1.0, 1.0);

        EntityHitResult entityHit = ProjectileUtil.raycast(
                camera,
                projectedStart,
                projectedEnd,
                searchBox,
                entity -> !entity.isSpectator()
                        && entity.canHit()
                        && !Plane.shouldCull(entity.getBlockPos(), plane),
                maxRange * maxRange
        );

        // Choose closest hit on the plane (if any)
        HitResult planeHit = null;
        double planeHitDistSq = Double.MAX_VALUE;

        if (blockHit.getType() != HitResult.Type.MISS) {
            planeHit = blockHit;
            planeHitDistSq = blockHit.getPos().squaredDistanceTo(projectedStart);
        }

        if (entityHit != null) {
            double d = entityHit.getPos().squaredDistanceTo(projectedStart);
            if (d < planeHitDistSq) {
                planeHit = entityHit;
                planeHitDistSq = d;
            }
        }

        if (planeHit == null) {
            // Nothing on the plane under the crosshair – keep vanilla target.
            return;
        }

        // --------------------------------------------------------------------
        // 2) HEAD RAY → from player eye to planeHit to validate reach & occlusion
        // --------------------------------------------------------------------
        Vec3d playerEyePos = client.player.getEyePos();
        Vec3d hitPos = planeHit.getPos();
        double headToHitDistSq = playerEyePos.squaredDistanceTo(hitPos);

        // Use block or entity range depending on what we hit
        double allowedRange = (planeHit.getType() == HitResult.Type.ENTITY)
                ? entityRange
                : blockRange;

        // Out of reach → keep vanilla result
        if (headToHitDistSq > allowedRange * allowedRange) {
            return;
        }

        // LOS / occlusion test from head to hit point
        HitResult occlusion = client.world.raycast(new RaycastContext(
                playerEyePos,
                hitPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));

        if (occlusion.getType() == HitResult.Type.BLOCK) {
            if (planeHit instanceof BlockHitResult planeBlockHit) {
                // If another block is closer than our plane block, it's occluded.
                BlockHitResult occBlock = (BlockHitResult) occlusion;
                if (!occBlock.getBlockPos().equals(planeBlockHit.getBlockPos())) {
                    return;
                }
            } else if (planeHit instanceof EntityHitResult) {
                // For entity hits: if a block is strictly closer than the entity, it's occluded.
                double occDistSq = occlusion.getPos().squaredDistanceTo(playerEyePos);
                if (occDistSq + 1.0e-4 < headToHitDistSq) {
                    return;
                }
            }
        }

        // --------------------------------------------------------------------
        // 3) Passed all checks → override vanilla target with plane-aware hit
        // --------------------------------------------------------------------
        client.crosshairTarget = planeHit;

        if (planeHit instanceof EntityHitResult entityPlaneHit) {
            client.targetedEntity = entityPlaneHit.getEntity();
        } else {
            client.targetedEntity = null;
        }
    }
}
