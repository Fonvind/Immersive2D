package github.fonvind.immersive2d.mixin;

import github.fonvind.immersive2d.access.EntityPlaneGetterSetter;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "isBlockBreakingRestricted", at = @At("HEAD"), cancellable = true)
    private void disableBlockBreakingOutsidePlane(World world, BlockPos pos, GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        Plane plane = ((EntityPlaneGetterSetter) this).immersive2d$getPlane();
        if (plane != null) {
            double dist = plane.sdf(pos.toCenterPos());
            if (dist <= Plane.CULL_DIST || dist >= 1.8) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "tick", at =  @At("HEAD"))
    private void updatePlaneContainedEntities(CallbackInfo ci) {
        Plane plane = ((EntityPlaneGetterSetter) this).immersive2d$getPlane();
        if (plane != null && this.age % 20 == 0 && this.getWorld() instanceof ServerWorld world) {
            world.getOtherEntities(this, Box.of(getPos(), 64, 32, 64)).forEach(entity -> {
                EntityPlaneGetterSetter entityPlane = (EntityPlaneGetterSetter) entity;
                if (entityPlane.immersive2d$getPlane() == null && !(entity instanceof PlayerEntity)) {
                    entityPlane.immersive2d$setPlane(plane);
                    plane.containedEntities.add(entity);
                }
            });
        }
    }
}
