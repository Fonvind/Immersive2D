package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {

    /**
     * Intercept all projectile velocity assignments and redirect them so that
     * targeting ALWAYS follows the 2D plane instead of the head rotation.
     */
    @Inject(method = "setVelocity(Lnet/minecraft/entity/Entity;FFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void immersive2d$overrideProjectileAim(
            Entity shooter,
            float pitch, float yaw, float roll, float speed, float divergence,
            CallbackInfo ci
    ) {
        Plane plane = Immersive2DClient.plane;
        if (plane == null) return;        // 3D world → vanilla behaviour
        if (!(shooter instanceof net.minecraft.entity.player.PlayerEntity)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        // Shooter eye position
        Vec3d eyePos = shooter.getEyePos();

        // Use the SAME camera cast direction as updateCrosshairTarget()
        Vec3d cameraPos = client.cameraEntity.getCameraPosVec(1.0f);
        Vec3d cameraDir = client.cameraEntity.getRotationVec(1.0f);

        double range = 50; // For direction only, not actual projectile distance
        Vec3d cameraRayEnd = cameraPos.add(cameraDir.multiply(range));

        // Project the camera direction onto the 2D plane
        Vec3d targetOnPlane = plane.intersectPoint(cameraRayEnd);

        // Compute new velocity direction
        Vec3d newVelocityDir = targetOnPlane.subtract(eyePos).normalize();

        // Apply vanilla speed
        Vec3d newVelocity = newVelocityDir.multiply(speed);

        // Override vanilla behavior
        ((ProjectileEntity) (Object) this).setVelocity(newVelocity);

        ci.cancel(); // Stop vanilla setVelocity(pitch,yaw,...)
    }
}
