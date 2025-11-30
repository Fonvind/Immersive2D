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

            Mouse mouse = client.mouse;

            // --- "Aiming Square" Logic ---
            double windowWidth = client.getWindow().getWidth();
            double windowHeight = client.getWindow().getHeight();
            double smallestDimension = Math.min(windowWidth, windowHeight);
            double aimingSquareSize = smallestDimension * 1.0; // Using 100% for now

            // Get mouse position relative to the center of the screen
            double mouseXFromCenter = mouse.getX() - (windowWidth / 2.0);
            double mouseYFromCenter = mouse.getY() - (windowHeight / 2.0);

            // Normalize mouse position based on the aiming square's size
            double processedMouseX = mouseXFromCenter / (aimingSquareSize / 2.0);
            double processedMouseY = mouseYFromCenter / (aimingSquareSize / 2.0);

            // Clamp the input to the -1.0 to 1.0 range
            processedMouseX = MathHelper.clamp(processedMouseX, -1.0, 1.0);
            processedMouseY = MathHelper.clamp(processedMouseY, -1.0, 1.0);

            // --- Consistent Lerp-based Aiming Logic for Both Axes ---
            float sensitivity = 1.0f; // Neutral sensitivity multiplier

            // --- Pitch (Y-Axis) ---
            float maxPitchAngle = 75.0F;
            // Map mouse Y from [-1, 1] to a lerp factor of [0, 1]
            float pitchLerpFactor = (float) (processedMouseY * sensitivity * 0.5 + 0.5);
            // Lerp between the max down angle and max up angle
            float playerPitch = MathHelper.lerp(pitchLerpFactor, maxPitchAngle, -maxPitchAngle);
            this.setPitch(playerPitch);

            // --- Yaw (X-Axis) ---
            float maxYawAngle = 75.0F;
            double basePlaneYawDegrees = Math.toDegrees(plane.getYaw());
            // Map mouse X from [-1, 1] to a lerp factor of [0, 1]
            float yawLerpFactor = (float) (processedMouseX * sensitivity * 0.5 + 0.5);
            float yawLeftTarget;
            float yawRightTarget;

            if (Immersive2DClient.turnedAround.isPressed()) {
                // If turned around, player faces South (0 degrees)
                yawLeftTarget = (float) (basePlaneYawDegrees + maxYawAngle);
                yawRightTarget = (float) (basePlaneYawDegrees - maxYawAngle);
            } else {
                // Default: player faces North (180 degrees)
                yawLeftTarget = (float) (basePlaneYawDegrees + 180.0F - maxYawAngle);
                yawRightTarget = (float) (basePlaneYawDegrees + 180.0F + maxYawAngle);
            }

            this.setYaw(MathHelper.lerp(yawLerpFactor, yawLeftTarget, yawRightTarget));

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
