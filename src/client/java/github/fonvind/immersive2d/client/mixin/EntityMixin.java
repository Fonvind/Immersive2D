package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.CameraScaleAccessor;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
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

    // ---------------------------
    //   PROJECTILE AIMING STATE
    // ---------------------------
    private boolean immersive2d$isAimingProjectile = false;
    private float immersive2d$aimYaw   = 0f;
    private float immersive2d$aimPitch = 0f;
    private static final float AIM_LERP_SPEED = 0.25f;

    /** Detect any item that should engage projectile-aim mode. */
    private boolean immersive2d$isProjectileItem(PlayerEntity player) {
        Item item = player.getMainHandStack().getItem();

        // Throwables (instant projectiles)
        if (item instanceof SnowballItem ||
                item instanceof EnderPearlItem ||
                item instanceof EggItem ||
                item instanceof ExperienceBottleItem ||
                item instanceof PotionItem ||
                item instanceof TridentItem) {
            return true;
        }

        // Bow: aim mode WHILE drawing
        if (item instanceof BowItem && player.isUsingItem()) {
            return true;
        }

        // Crossbow: do NOT aim while charging, ONLY aim when charged
        if (item instanceof CrossbowItem &&
                CrossbowItem.isCharged(player.getMainHandStack())) {
            return true;
        }

        return false;
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    public void changeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        Plane plane = Immersive2DClient.plane;
        MinecraftClient client = MinecraftClient.getInstance();

        if (plane == null || (Object) this != client.player) return;

        PlayerEntity player = client.player;

        this.prevPitch = this.getPitch();
        this.prevYaw   = this.getYaw();

        // --------------------------
        //  Shared mouse → [-1..1]
        // --------------------------
        final float maxPitchAngle = 75f;
        final float maxYawAngle   = 75f;
        final double aimingSquarePercentage = 0.6;
        final double baseSensitivity = 1.0;

        Mouse mouse = client.mouse;

        double windowWidth       = client.getWindow().getWidth();
        double windowHeight      = client.getWindow().getHeight();
        double smallestDimension = Math.min(windowWidth, windowHeight);
        double aimingSquareSize  = smallestDimension * aimingSquarePercentage;

        double mouseXFromCenter = mouse.getX() - (windowWidth / 2.0);
        double mouseYFromCenter = mouse.getY() - (windowHeight / 2.0);

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        float scale = ((CameraScaleAccessor) camera).immersive2d$getCurrentScale();

        double processedMouseX =
                (mouseXFromCenter / (aimingSquareSize / 2.0))
                        * Math.sqrt(scale);

        double processedMouseY = mouseYFromCenter / (aimingSquareSize / 2.0);

        // FOV compensation
        double currentFov   = client.options.getFov().getValue();
        double referenceFov = 70.0;
        double fovScale = Math.tan(Math.toRadians(currentFov / 2.0)) /
                Math.tan(Math.toRadians(referenceFov / 2.0));

        processedMouseX *= fovScale * baseSensitivity;
        processedMouseY *= fovScale * baseSensitivity;

        processedMouseX = MathHelper.clamp(processedMouseX, -1, 1);
        processedMouseY = MathHelper.clamp(processedMouseY, -1, 1);

        // --------------------------
        //  PROJECTILE AIM MODE
        // --------------------------
        boolean wantsAiming = immersive2d$isProjectileItem(player);

        if (wantsAiming) {
            if (!immersive2d$isAimingProjectile) {
                immersive2d$isAimingProjectile = true;
                immersive2d$aimYaw   = this.getYaw();
                immersive2d$aimPitch = this.getPitch();
            }

            float cameraYaw = client.gameRenderer.getCamera().getYaw();

            // If cursor is right side → turn right along plane
            // If cursor is left side  → turn left  along plane
            float yawTarget = (processedMouseX >= 0f)
                    ? cameraYaw + 90f
                    : cameraYaw - 90f;

            // Pitch still controlled normally
            float pitchLerp = (float)(processedMouseY * 0.5 + 0.5);
            float pitchTarget = MathHelper.lerp(pitchLerp, -maxPitchAngle, maxPitchAngle);

            immersive2d$aimYaw   = MathHelper.lerp(AIM_LERP_SPEED, immersive2d$aimYaw, yawTarget);
            immersive2d$aimPitch = MathHelper.lerp(AIM_LERP_SPEED, immersive2d$aimPitch, pitchTarget);

            this.setYaw(immersive2d$aimYaw);
            this.setPitch(immersive2d$aimPitch);

            if (this.vehicle != null)
                this.vehicle.onPassengerLookAround((Entity)(Object)this);

            ci.cancel();
            return;
        }

        // Exiting projectile aim mode
        immersive2d$isAimingProjectile = false;

        // --------------------------
        //  NORMAL 2D LOOK MODE
        // --------------------------
        float pitchLerp = (float)(processedMouseY * 0.5 + 0.5);
        float playerPitch = MathHelper.lerp(pitchLerp, -maxPitchAngle, maxPitchAngle);
        this.setPitch(playerPitch);

        double basePlaneYawDegrees = Math.toDegrees(plane.getYaw());
        float yawLerpFactor = (float)(processedMouseX * 0.5 + 0.5);

        float yawLeftTarget;
        float yawRightTarget;

        if (Immersive2DClient.turnedAround.isPressed()) {
            yawLeftTarget  = (float)(basePlaneYawDegrees + maxYawAngle);
            yawRightTarget = (float)(basePlaneYawDegrees - maxYawAngle);
        } else {
            yawLeftTarget  = (float)(basePlaneYawDegrees + 180 - maxYawAngle);
            yawRightTarget = (float)(basePlaneYawDegrees + 180 + maxYawAngle);
        }

        this.setYaw(MathHelper.lerp(yawLerpFactor, yawRightTarget, yawLeftTarget));

        if (this.vehicle != null)
            this.vehicle.onPassengerLookAround((Entity)(Object)this);

        ci.cancel();
    }


    // Culling unchanged
    @Inject(method = "shouldRender(DDD)Z", at = @At("HEAD"), cancellable = true)
    public void disableRenderingOutsidePlane(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!Plane.shouldCull(this.getBlockPos(), Immersive2DClient.plane));
    }
}
