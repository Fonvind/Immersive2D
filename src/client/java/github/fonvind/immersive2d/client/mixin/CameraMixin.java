package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.CameraScaleAccessor;
import github.fonvind.immersive2d.client.access.MouseNormalizedGetter;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin implements CameraScaleAccessor {

    @Unique private double immersive2d$xMouseOffset = 0;
    @Unique private double immersive2d$yMouseOffset = 0;

    @Unique private float immersive2d$currentScale = 1.0f;
    @Unique private float immersive2d$targetScale  = 1.0f;

    @Unique private long immersive2d$zoomHoldUntil = 0;

    @Shadow private boolean thirdPerson;
    @Shadow private Vec3d pos;
    @Shadow private final Vector3f horizontalPlane = new Vector3f();
    @Shadow private final Vector3f verticalPlane   = new Vector3f();

    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract float getYaw();

    @Inject(method = "update", at = @At("TAIL"))
    public void update(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                       float tickDelta, CallbackInfo ci)
    {
        Plane plane = Immersive2DClient.plane;
        if (plane == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null || focusedEntity == null) return;

        this.thirdPerson = true;
        this.setRotation((float)(plane.getYaw() * MathHelper.DEGREES_PER_RADIAN), 0);

        // Follow entity position
        Vec3d lerpedPos = new Vec3d(
                MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX()),
                MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY()) + focusedEntity.getStandingEyeHeight(),
                MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ())
        );
        this.setPos(lerpedPos.x, lerpedPos.y, lerpedPos.z);

        MouseNormalizedGetter mouse = (MouseNormalizedGetter) mc.mouse;

        // -----------------------------------------------------
        // Determine TARGET scale with linger logic
        // -----------------------------------------------------
        float desiredScale = immersive2d$getMouseOffsetScale(player);
        long now = System.currentTimeMillis();

        if (desiredScale > 1) {
            immersive2d$targetScale = desiredScale;
            immersive2d$zoomHoldUntil = now + 1000;  // 1s linger
        } else {
            if (now >= immersive2d$zoomHoldUntil) {
                immersive2d$targetScale = 1.0f;
            }
        }

        // -----------------------------------------------------
        // Smooth zoom scale change
        // -----------------------------------------------------
        float zoomLerpSpeed = 0.12f;
        immersive2d$currentScale = MathHelper.lerp(
                zoomLerpSpeed,
                immersive2d$currentScale,
                immersive2d$targetScale
        );

        // -----------------------------------------------------
        // Smooth offset PREVENTING snap when scale changes
        // -----------------------------------------------------
        double mouseNormX = mouse.immersive2d$getNormalizedX();
        double mouseNormY = mouse.immersive2d$getNormalizedY();

        // Additional smoothing layer for zoom transitions
        double zoomAdjustSpeed = 0.04;

        double zoomAdjustedX = MathHelper.lerp(
                zoomAdjustSpeed,
                immersive2d$xMouseOffset / Math.max(immersive2d$currentScale, 0.0001),
                mouseNormX
        );

        double zoomAdjustedY = MathHelper.lerp(
                zoomAdjustSpeed,
                immersive2d$yMouseOffset / Math.max(immersive2d$currentScale, 0.0001),
                mouseNormY
        );

        // Now compute final smoothed targets
        double targetX = zoomAdjustedX * immersive2d$currentScale;
        double targetY = zoomAdjustedY * immersive2d$currentScale;

        // Final positional lerp
        double offsetLerpSpeed = 0.08;
        immersive2d$xMouseOffset = MathHelper.lerp(offsetLerpSpeed, immersive2d$xMouseOffset, targetX);
        immersive2d$yMouseOffset = MathHelper.lerp(offsetLerpSpeed, immersive2d$yMouseOffset, targetY);

        // -----------------------------------------------------
        // Apply camera offset in 3D
        // -----------------------------------------------------
        Vector3f backward = new Vector3f(this.verticalPlane).cross(this.horizontalPlane);
        backward.normalize();

        this.setPos(
                this.pos.x + backward.x() * immersive2d$xMouseOffset
                        + this.verticalPlane.x() * immersive2d$yMouseOffset
                        + this.horizontalPlane.x() * -8,

                this.pos.y + backward.y() * immersive2d$xMouseOffset
                        + this.verticalPlane.y() * immersive2d$yMouseOffset
                        + this.horizontalPlane.y() * -8,

                this.pos.z + backward.z() * immersive2d$xMouseOffset
                        + this.verticalPlane.z() * immersive2d$yMouseOffset
                        + this.horizontalPlane.z() * -8
        );
    }

    // Zoom amounts based on item
    @Unique
    private float immersive2d$getMouseOffsetScale(PlayerEntity player) {
        if (player == null || !player.isUsingItem()) return 1;

        return switch (player.getActiveItem().getItem().getTranslationKey()) {
            case "item.minecraft.bow"      -> 8;
            case "item.minecraft.spyglass" -> 20;
            default -> 1;
        };
    }

    @Override
    public float immersive2d$getCurrentScale() {
        return immersive2d$currentScale;
    }
}
