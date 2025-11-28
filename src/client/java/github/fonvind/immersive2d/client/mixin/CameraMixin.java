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

    @Shadow protected abstract void setPos(double x, double y, double z);

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Shadow protected abstract void moveBy(double x, double y, double z);

    @Shadow private float cameraY;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"), cancellable = true)
    public void update(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        Plane plane = Immersive2DClient.plane;
        if (plane != null) {
            this.thirdPerson = true;

            this.setRotation((float) (plane.getYaw() * MathHelper.DEGREES_PER_RADIAN), 0);

            Vec3d pos = new Vec3d(MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX()), MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY()) + focusedEntity.getStandingEyeHeight(), MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ()));
            this.setPos(pos.x, pos.y, pos.z);

            MouseNormalizedGetter mouse = (MouseNormalizedGetter) MinecraftClient.getInstance().mouse;

            float mouseOffsetScale = immersive2d$getMouseOffsetScale(MinecraftClient.getInstance().player);
            double delta = 0.2 - (0.15 * mouseOffsetScale/40);

            immersive2d$xMouseOffset = MathHelper.lerp(delta, immersive2d$xMouseOffset, mouse.immersive2d$getNormalizedX() * mouseOffsetScale);
            immersive2d$yMouseOffset = MathHelper.lerp(delta, immersive2d$yMouseOffset, mouse.immersive2d$getNormalizedY() * mouseOffsetScale);

            this.moveBy(-8, immersive2d$yMouseOffset, immersive2d$xMouseOffset);

            ci.cancel();
        }
    }

    @Unique
    private float immersive2d$getMouseOffsetScale(PlayerEntity player) {
        if (player == null || !player.isUsingItem()) {
            return 1;
        }

        return switch (player.getActiveItem().getItem().getTranslationKey()) {
            case "item.minecraft.bow" -> 10;
            case "item.minecraft.spyglass" -> 40;
            default -> 1;
        };
    }
}
