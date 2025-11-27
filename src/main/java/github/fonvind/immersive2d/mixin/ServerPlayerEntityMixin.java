package github.fonvind.immersive2d.mixin;

import com.mojang.authlib.GameProfile;
import github.fonvind.immersive2d.Immersive2D;
import github.fonvind.immersive2d.access.EntityPlaneGetterSetter;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow public abstract ServerWorld getServerWorld();

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "copyFrom", at = @At("HEAD"))
    private void copyPlane(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        Plane plane = ((EntityPlaneGetterSetter) oldPlayer).twoDimensional$getPlane();
        ((EntityPlaneGetterSetter) this).twoDimensional$setPlane(plane);

        if (plane != null) {
            // sync to client
            Immersive2D.updatePlane(this.getServer(), (ServerPlayerEntity) (PlayerEntity) this, plane.getOffset().x, plane.getOffset().z, plane.getYaw());
        }
    }

    @ModifyArgs(method = "moveToSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/SpawnLocating;findOverworldSpawn(Lnet/minecraft/server/world/ServerWorld;II)Lnet/minecraft/util/math/BlockPos;"))
    private void clampSpawnXZ(Args args) {
        Plane plane = ((EntityPlaneGetterSetter) this).twoDimensional$getPlane();
        if (plane != null) {
            int x = args.get(1);
            int z = args.get(2);
            Vec3d intersectPoint = plane.intersectPoint(new Vec3d(x, 0, z));
            args.set(1, (int) intersectPoint.x);
            args.set(2, (int) intersectPoint.z);
        }
    }
}
