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

    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    void immersive2d$overwriteRaycastWith2D(float tickDelta, CallbackInfo ci) {
        Plane plane = Immersive2DClient.plane;
        if (plane == null) return;

        Entity cameraEntity = this.client.getCameraEntity();
        if (cameraEntity == null || this.client.world == null || this.client.player == null) {
            return;
        }

        double range = this.client.player.getBlockInteractionRange();

        // --- Logic moved from Immersive2DRaycaster ---
        Vec3d cameraPos = cameraEntity.getCameraPosVec(tickDelta);
        Vec3d lookVec = cameraEntity.getRotationVec(tickDelta);

        Vec3d projectedStart = plane.intersectPoint(cameraPos);
        Vec3d projectedEnd = plane.intersectPoint(cameraPos.add(lookVec.multiply(range)));

        // Block raycast
        HitResult blockHitResult = cameraEntity.getWorld().raycast(new RaycastContext(
                projectedStart,
                projectedEnd,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                cameraEntity
        ));

        // Entity raycast
        Box searchBox = cameraEntity.getBoundingBox().stretch(lookVec.multiply(range)).expand(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(
            cameraEntity,
            projectedStart,
            projectedEnd,
            searchBox,
            (entity -> !entity.isSpectator() && entity.canHit() && !Plane.shouldCull(entity.getBlockPos(), plane)),
            range * range
        );

        // Compare hits
        double blockHitDistanceSq = blockHitResult.getType() != HitResult.Type.MISS ? blockHitResult.getPos().squaredDistanceTo(projectedStart) : Double.MAX_VALUE;
        double entityHitDistanceSq = entityHitResult != null ? entityHitResult.getPos().squaredDistanceTo(projectedStart) : Double.MAX_VALUE;

        if (entityHitResult != null && entityHitDistanceSq < blockHitDistanceSq) {
            this.client.crosshairTarget = entityHitResult;
        } else {
            this.client.crosshairTarget = blockHitResult;
        }
        // --- End of moved logic ---
    }
}
