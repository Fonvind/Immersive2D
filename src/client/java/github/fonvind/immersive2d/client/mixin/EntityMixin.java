package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (plane != null && (Object)this == client.player) {
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();

            // --- TUNING PARAMETERS ---
            final float maxPitchAngle = 75.0F; // Max angle player can look up/down
            final float maxYawAngle = 75.0F;   // Max angle player can turn left/right from center
            final double aimingSquarePercentage = 0.8; // Use 1.0 for 100%, 0.7 for 70%, etc.
            final double baseSensitivity = 1.0; // Adjusts overall sensitivity. 1.1 = 10% more, 0.9 = 10% less.
            // --- END TUNING PARAMETERS ---

            Mouse mouse = client.mouse;

            // --- "Aiming Square" Logic ---
            double windowWidth = client.getWindow().getWidth();
            double windowHeight = client.getWindow().getHeight();
            double smallestDimension = Math.min(windowWidth, windowHeight);
            double aimingSquareSize = smallestDimension * aimingSquarePercentage; 

            double mouseXFromCenter = mouse.getX() - (windowWidth / 2.0);
            double mouseYFromCenter = mouse.getY() - (windowHeight / 2.0);

            double processedMouseX = mouseXFromCenter / (aimingSquareSize / 2.0);
            double processedMouseY = mouseYFromCenter / (aimingSquareSize / 2.0);

            // --- FOV-Compensated Sensitivity ---
            double currentFov = client.options.getFov().getValue();
            double referenceFov = 70.0;
            double fovScale = Math.tan(Math.toRadians(currentFov / 2.0)) / Math.tan(Math.toRadians(referenceFov / 2.0));
            
            processedMouseX *= fovScale * baseSensitivity;
            processedMouseY *= fovScale * baseSensitivity;

            processedMouseX = MathHelper.clamp(processedMouseX, -1.0, 1.0);
            processedMouseY = MathHelper.clamp(processedMouseY, -1.0, 1.0);

            // --- Pitch (Y-Axis) ---
            float pitchLerpFactor = (float) (processedMouseY * 0.5 + 0.5);
            float playerPitch = MathHelper.lerp(pitchLerpFactor, -maxPitchAngle, maxPitchAngle);
            this.setPitch(playerPitch);

            // --- Yaw (X-Axis) ---
            double basePlaneYawDegrees = Math.toDegrees(plane.getYaw());
            float yawLerpFactor = (float) (processedMouseX * 0.5 + 0.5);
            float yawLeftTarget;
            float yawRightTarget;

            if (Immersive2DClient.turnedAround.isPressed()) {
                yawLeftTarget = (float) (basePlaneYawDegrees + maxYawAngle);
                yawRightTarget = (float) (basePlaneYawDegrees - maxYawAngle);
            } else {
                yawLeftTarget = (float) (basePlaneYawDegrees + 180.0F - maxYawAngle);
                yawRightTarget = (float) (basePlaneYawDegrees + 180.0F + maxYawAngle);
            }

            this.setYaw(MathHelper.lerp(yawLerpFactor, yawRightTarget, yawLeftTarget));

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
