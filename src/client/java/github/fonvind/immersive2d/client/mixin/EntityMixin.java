package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseNormalizedGetter;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow private @Nullable Entity vehicle;
    @Shadow public float prevPitch;

    @Shadow public abstract float getPitch();

    @Shadow public float prevYaw;

    @Shadow public abstract float getYaw();

    @Shadow public abstract void setPitch(float pitch);

    @Shadow public abstract void setYaw(float yaw);

    @Shadow public abstract BlockPos getBlockPos();

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    public void changeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        Plane plane = Immersive2DClient.plane;
        if (plane != null) {
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();

            MouseNormalizedGetter mouse = (MouseNormalizedGetter) MinecraftClient.getInstance().mouse;

            // --- Simplified Pitch Calculation ---
            // Directly map normalized Y mouse input to pitch within a reasonable range
            float pitchSensitivity = 45.0F; // Degrees of pitch for full vertical mouse movement
            float playerPitch = (float) (mouse.immersive2d$getNormalizedY() * pitchSensitivity);
            this.setPitch(MathHelper.clamp(playerPitch, -90, 90));

            // --- Simplified Yaw Calculation ---
            // Base yaw is plane's yaw adjusted to face the camera (which is to the left of the player)
            // plane.getYaw() is already in degrees
            double basePlaneYaw = plane.getYaw();
            float yawSensitivity = 60.0F; // Degrees of yaw for full horizontal mouse movement

            float targetYawCenter;
            float lerpFactor;

            if (Immersive2DClient.turnedAround.isPressed()) {
                // If turned around, base yaw is plane.getYaw() + 90 (facing the other side)
                targetYawCenter = (float) (basePlaneYaw + 90);
                lerpFactor = (float) MathHelper.clamp(3. * mouse.immersive2d$getNormalizedX() + 0.5, 0, 1); // Original sensitivity
            } else {
                // Default: base yaw is plane.getYaw() - 90 (facing the camera)
                targetYawCenter = (float) (basePlaneYaw - 90);
                lerpFactor = (float) MathHelper.clamp(7 * mouse.immersive2d$getNormalizedX() + 0.5, 0, 1); // Original sensitivity
            }

            // Calculate the min and max yaw for the lerp based on the center and sensitivity
            float minYaw = targetYawCenter - yawSensitivity / 2;
            float maxYaw = targetYawCenter + yawSensitivity / 2;

            // Apply the lerp to get the final player yaw
            this.setYaw(MathHelper.lerp(lerpFactor, minYaw, maxYaw));


            if (this.vehicle != null) {
                this.vehicle.onPassengerLookAround((Entity) (Object) this);
            }

            ci.cancel();
        }
    }

    @Inject(method = "shouldRender(DDD)Z", at = @At("HEAD"), cancellable = true)
    public void disableRenderingOutsidePlane(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!Plane.shouldCull(this.getBlockPos(), Immersive2DClient.plane));
    }
}