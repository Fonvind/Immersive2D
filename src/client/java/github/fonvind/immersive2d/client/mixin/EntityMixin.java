package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.TridentItem;
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

    // --- Aiming state (for projectiles) ---
    private boolean immersive2d$isAiming = false;
    private float immersive2d$aimYaw = 0.0f;
    private float immersive2d$aimPitch = 0.0f;
    private static final float AIM_LERP_SPEED = 0.25f; // 0..1, higher = snappier

    /** Checks if the player's current main-hand item is something you "throw/shoot". */
    private boolean immersive2d$isProjectileItem(PlayerEntity player) {
        Item item = player.getMainHandStack().getItem();
        return item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof SnowballItem
                || item instanceof EnderPearlItem
                || item instanceof EggItem
                || item instanceof TridentItem
                || item instanceof ExperienceBottleItem
                || item instanceof PotionItem; // thrown potions
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    public void changeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        Plane plane = Immersive2DClient.plane;
        MinecraftClient client = MinecraftClient.getInstance();

        if (plane == null || (Object) this != client.player) {
            return;
        }

        PlayerEntity player = client.player;

        this.prevPitch = this.getPitch();
        this.prevYaw = this.getYaw();

        // --- Shared mouse → normalized mapping (same as your original logic) ---

        final float maxPitchAngle = 75.0F; // Max angle player can look up/down
        final float maxYawAngle   = 75.0F; // Max angle player can turn left/right from center
        final double aimingSquarePercentage = 0.6; // Use 1.0 for 100%, 0.7 for 70%, etc.
        final double baseSensitivity = 1.0; // 1.1 = 10% more, 0.9 = 10% less

        Mouse mouse = client.mouse;

        double windowWidth  = client.getWindow().getWidth();
        double windowHeight = client.getWindow().getHeight();
        double smallestDimension = Math.min(windowWidth, windowHeight);
        double aimingSquareSize  = smallestDimension * aimingSquarePercentage;

        double mouseXFromCenter = mouse.getX() - (windowWidth / 2.0);
        double mouseYFromCenter = mouse.getY() - (windowHeight / 2.0);

        double processedMouseX = mouseXFromCenter / (aimingSquareSize / 2.0);
        double processedMouseY = mouseYFromCenter / (aimingSquareSize / 2.0);

        // FOV-compensated sensitivity
        double currentFov   = client.options.getFov().getValue();
        double referenceFov = 70.0;
        double fovScale = Math.tan(Math.toRadians(currentFov / 2.0)) /
                Math.tan(Math.toRadians(referenceFov / 2.0));

        processedMouseX *= fovScale * baseSensitivity;
        processedMouseY *= fovScale * baseSensitivity;

        processedMouseX = MathHelper.clamp(processedMouseX, -1.0, 1.0);
        processedMouseY = MathHelper.clamp(processedMouseY, -1.0, 1.0);

        // --- Detect whether we *want* to be in aiming mode ---
        boolean wantsAiming = immersive2d$isProjectileItem(player) && player.isUsingItem();

        // ===============================
        //  PROJECTILE AIM MODE (NEW)
        // ===============================
        if (wantsAiming) {
            if (!immersive2d$isAiming) {
                immersive2d$isAiming = true;
                immersive2d$aimYaw = this.getYaw();
                immersive2d$aimPitch = this.getPitch();
            }

            var camera = client.gameRenderer.getCamera();
            float cameraYaw = camera.getYaw(); // this is aligned to the plane's *normal*

            // We want to look ALONG the plane:
            //  - one direction if the crosshair is on the right side
            //  - the opposite direction if it's on the left side
            float yawTarget;
            if (processedMouseX >= 0.0) {
                // Looking to the RIGHT on screen → turn to the "right along the plane"
                yawTarget = cameraYaw + 90.0f;
            } else {
                // Looking to the LEFT on screen → turn to the "left along the plane"
                yawTarget = cameraYaw - 90.0f;
            }

            // Pitch: still mapped from vertical mouse position
            float pitchLerpFactor = (float) (processedMouseY * 0.5 + 0.5);
            float pitchTarget = MathHelper.lerp(pitchLerpFactor, -maxPitchAngle, maxPitchAngle);

            // Smoothly lerp yaw & pitch towards targets
            immersive2d$aimYaw   = MathHelper.lerp(AIM_LERP_SPEED, immersive2d$aimYaw, yawTarget);
            immersive2d$aimPitch = MathHelper.lerp(AIM_LERP_SPEED, immersive2d$aimPitch, pitchTarget);

            this.setYaw(immersive2d$aimYaw);
            this.setPitch(immersive2d$aimPitch);

            if (this.vehicle != null) {
                this.vehicle.onPassengerLookAround((Entity) (Object) this);
            }

            ci.cancel();
            return;
        } else if (immersive2d$isAiming) {
            // Just stopped aiming → leave aiming state, fall back to normal logic
            immersive2d$isAiming = false;
        }

        // ===============================
        //  NORMAL 2D HEAD CLAMP (OLD BEHAVIOR)
        // ===============================

        // --- Pitch (Y-axis) ---
        float pitchLerpFactor = (float) (processedMouseY * 0.5 + 0.5);
        float playerPitch = MathHelper.lerp(pitchLerpFactor, -maxPitchAngle, maxPitchAngle);
        this.setPitch(playerPitch);

        // --- Yaw (X-axis) based on plane orientation ---
        double basePlaneYawDegrees = Math.toDegrees(plane.getYaw());
        float yawLerpFactor = (float) (processedMouseX * 0.5 + 0.5);
        float yawLeftTarget;
        float yawRightTarget;

        if (Immersive2DClient.turnedAround.isPressed()) {
            yawLeftTarget  = (float) (basePlaneYawDegrees + maxYawAngle);
            yawRightTarget = (float) (basePlaneYawDegrees - maxYawAngle);
        } else {
            yawLeftTarget  = (float) (basePlaneYawDegrees + 180.0F - maxYawAngle);
            yawRightTarget = (float) (basePlaneYawDegrees + 180.0F + maxYawAngle);
        }

        this.setYaw(MathHelper.lerp(yawLerpFactor, yawRightTarget, yawLeftTarget));

        if (this.vehicle != null) {
            this.vehicle.onPassengerLookAround((Entity) (Object) this);
        }

        ci.cancel();
    }

    @Inject(method = "shouldRender(DDD)Z", at = @At("HEAD"), cancellable = true)
    public void disableRenderingOutsidePlane(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!Plane.shouldCull(this.getBlockPos(), Immersive2DClient.plane));
    }
}
