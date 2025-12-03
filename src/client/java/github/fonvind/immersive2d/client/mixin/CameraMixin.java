package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
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
public abstract class CameraMixin {
    @Unique
    double immersive2d$xMouseOffset = 0;
    @Unique
    double immersive2d$yMouseOffset = 0;

    @Shadow private boolean thirdPerson;
    @Shadow private Vec3d pos;
    @Shadow private final Vector3f horizontalPlane = new Vector3f();
    @Shadow private final Vector3f verticalPlane = new Vector3f();

    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract float getYaw();

    @Inject(method = "update", at = @At("TAIL"))
    public void update(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        Plane plane = Immersive2DClient.plane;
        if (plane == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null || focusedEntity == null) return;

        this.thirdPerson = true;

        this.setRotation((float) (plane.getYaw() * MathHelper.DEGREES_PER_RADIAN), 0);

        Vec3d lerpedPos = new Vec3d(
                MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX()),
                MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY()) + focusedEntity.getStandingEyeHeight(),
                MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ())
        );
        this.setPos(lerpedPos.x, lerpedPos.y, lerpedPos.z);

        MouseNormalizedGetter mouse = (MouseNormalizedGetter) mc.mouse;
        float mouseOffsetScale = immersive2d$getMouseOffsetScale(player);
        double smoothingDelta = 0.2 - (0.15 * mouseOffsetScale / 40.0);

        immersive2d$xMouseOffset = MathHelper.lerp(smoothingDelta, immersive2d$xMouseOffset, mouse.immersive2d$getNormalizedX() * mouseOffsetScale);
        immersive2d$yMouseOffset = MathHelper.lerp(smoothingDelta, immersive2d$yMouseOffset, mouse.immersive2d$getNormalizedY() * mouseOffsetScale);

        Vector3f backward = new Vector3f(this.verticalPlane).cross(this.horizontalPlane);
        backward.normalize();

        this.setPos(
                this.pos.x + backward.x() * immersive2d$xMouseOffset + this.verticalPlane.x() * immersive2d$yMouseOffset + this.horizontalPlane.x() * -8,
                this.pos.y + backward.y() * immersive2d$xMouseOffset + this.verticalPlane.y() * immersive2d$yMouseOffset + this.horizontalPlane.y() * -8,
                this.pos.z + backward.z() * immersive2d$xMouseOffset + this.verticalPlane.z() * immersive2d$yMouseOffset + this.horizontalPlane.z() * -8
        );
    }

    @Unique
    private float immersive2d$getMouseOffsetScale(PlayerEntity player) {
        if (player == null || !player.isUsingItem()) return 1;
        return switch (player.getActiveItem().getItem().getTranslationKey()) {
            case "item.minecraft.bow" -> 10;
            case "item.minecraft.spyglass" -> 40;
            default -> 1;
        };
    }
}
