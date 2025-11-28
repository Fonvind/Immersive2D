package github.fonvind.immersive2d.mixin;

import com.mojang.authlib.GameProfile;
import github.fonvind.immersive2d.Immersive2D;
import github.fonvind.immersive2d.access.EntityPlaneGetterSetter;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow public abstract ServerWorld getServerWorld();
    @Shadow @Nullable public abstract BlockPos getSpawnPointPosition();
    @Shadow public abstract RegistryKey<World> getSpawnPointDimension();
    @Shadow public abstract float getSpawnAngle();
    @Shadow public abstract void setSpawnPoint(RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean spawnPointSet, boolean bl);

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void copyPlaneAndClampSpawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        Plane plane = ((EntityPlaneGetterSetter) oldPlayer).immersive2d$getPlane();
        ((EntityPlaneGetterSetter) this).immersive2d$setPlane(plane);

        if (plane != null) {
            MinecraftServer server = this.getServer();
            if (server == null) {
                return;
            }
            // sync to client
            Immersive2D.updatePlane(server, (ServerPlayerEntity) (PlayerEntity) this, plane.getOffset().x, plane.getOffset().z, plane.getYaw());

            // Clamp spawn point
            BlockPos spawnPos = this.getSpawnPointPosition();
            if (spawnPos != null) {
                Vec3d intersectPoint = plane.intersectPoint(new Vec3d(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()));
                this.setSpawnPoint(this.getSpawnPointDimension(), new BlockPos((int) intersectPoint.x, spawnPos.getY(), (int) intersectPoint.z), this.getSpawnAngle(), true, false);
            }
        }
    }
}
