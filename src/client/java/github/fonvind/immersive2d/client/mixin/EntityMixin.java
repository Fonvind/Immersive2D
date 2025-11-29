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
import org.spongepowered.asm.mixin.Unique;
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

            // --- Player Head Pitch Aiming Logic (Simplified & Corrected Inversion) ---
            // Directly map normalized Y mouse input to pitch within a reasonable range
            float pitchSensitivity = 45.0F; // Degrees of pitch for full vertical mouse movement
            // Invert mouse Y for natural up/down movement (mouse up = look up)
            float playerPitch = (float) (-mouse.immersive2d$getNormalizedY() * pitchSensitivity);
            this.setPitch(MathHelper.clamp(playerPitch, -90, 90));

            // --- Player Body Yaw Logic (Smooth Adjustment based on Mouse X, closer to 1.20.1 feel) ---
            double basePlaneYawDegrees = Math.toDegrees(plane.getYaw());
            float lerpFactor;
            float yawLeftTarget;  // Yaw when mouse is far left
            float yawRightTarget; // Yaw when mouse is far right

            if (Immersive2DClient.turnedAround.isPressed()) {
                // If turned around, player faces South (0 degrees)
                lerpFactor = (float) MathHelper.clamp(3. * mouse.immersive2d$getNormalizedX() + 0.5, 0, 1);
                yawLeftTarget = (float) (basePlaneYawDegrees + 0.0F + 20.0F); // Slightly right of "South"
                yawRightTarget = (float) (basePlaneYawDegrees + 0.0F - 20.0F); // Slightly left of "South"
            } else {
                // Default: player faces North (180 degrees)
                lerpFactor = (float) MathHelper.clamp(7 * mouse.immersive2d$getNormalizedX() + 0.5, 0, 1);
                yawLeftTarget = (float) (basePlaneYawDegrees + 180.0F - 20.0F); // Slightly left of "North"
                yawRightTarget = (float) (basePlaneYawDegrees + 180.0F + 20.0F); // Slightly right of "North"
            }

            // Apply the lerp to get the final player body yaw
            this.setYaw(MathHelper.lerp(lerpFactor, yawLeftTarget, yawRightTarget));


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
