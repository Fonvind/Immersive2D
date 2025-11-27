package github.fonvind.immersive2D.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import github.fonvind.immersive2D.access.EntityPlaneGetterSetter;
import github.fonvind.immersive2D.utils.Plane;
import github.fonvind.immersive2D.utils.PlanePersistentState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityPlaneGetterSetter {
    @Unique
    private Plane twoDimensional$plane = null;

    @Override
    @Nullable
    public Plane twoDimensional$getPlane() {
        if ((Object)this instanceof ServerPlayerEntity player) {
            return PlanePersistentState.getPlayerPlane(player);
        }

        return twoDimensional$plane;
    }

    @Override
    public void twoDimensional$setPlane(Plane plane) {
        this.twoDimensional$plane = plane;
    }
    @Shadow public abstract void setVelocity(Vec3d velocity);

    @Shadow public abstract Vec3d getVelocity();

    @Shadow private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {return null;}

    @Shadow private Vec3d velocity;

    @Shadow private Vec3d pos;

    @Shadow private BlockPos blockPos;

    @Shadow public abstract Vec3d getPos();

    @Shadow public abstract float getYaw();

    @Shadow public double prevX;

    @Shadow public double prevY;

    @Shadow public double prevZ;

    @Inject(method = "updateVelocity", at = @At("HEAD"), cancellable = true)
    public void updateVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        if (twoDimensional$getPlane() != null) {
            // convert z movement into movement in direction of yaw
            double planeYaw = twoDimensional$getPlane().getYaw() * MathHelper.DEGREES_PER_RADIAN;
            movementInput = new Vec3d(movementInput.x + movementInput.z * MathHelper.sign(this.getYaw() - 180 - planeYaw), movementInput.y, 0.);
            this.setVelocity(this.getVelocity().add(movementInputToVelocity(movementInput, speed, (float) (planeYaw))));
            ci.cancel();
        }
    }

    @Inject(method = "setVelocity(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    public void clampVelocityToPlane(Vec3d velocity, CallbackInfo ci) {
        Plane plane = twoDimensional$getPlane();
        if (plane != null) {
            this.velocity = plane.intersectPoint(velocity.add(this.getPos())).subtract(this.getPos());
            ci.cancel();
        }
    }

    @Inject(method = "setPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;<init>(DDD)V", shift = At.Shift.AFTER))
    public void clampSetPos(double x, double y, double z, CallbackInfo ci) {
        Plane plane = twoDimensional$getPlane();
        if (plane != null) {
            this.pos = plane.intersectPoint(new Vec3d(x, y, z));
        }
    }

    @Inject(method = "setPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;<init>(III)V", shift = At.Shift.AFTER))
    public void clampBlockPos(double x, double y, double z, CallbackInfo ci) {
        Plane plane = twoDimensional$getPlane();
        if (plane != null) {
            this.blockPos = BlockPos.ofFloored(plane.intersectPoint(new Vec3d(x, y, z)));
        }
    }

    @Inject(method = "updatePosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setPosition(DDD)V"))
    private void clampPrevPos(double x, double y, double z, CallbackInfo ci, @Local (ordinal = 0) double d, @Local (ordinal = 1) double e) {
        Plane plane = twoDimensional$getPlane();
        if (plane != null) {
            Vec3d clampedPos = plane.intersectPoint(new Vec3d(d, y, e));
            this.prevX = clampedPos.x;
            this.prevY = clampedPos.y;
            this.prevZ = clampedPos.z;
        }
    }
}
