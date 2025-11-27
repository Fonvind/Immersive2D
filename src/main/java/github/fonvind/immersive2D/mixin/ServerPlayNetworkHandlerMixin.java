package github.fonvind.immersive2D.mixin;

import github.fonvind.immersive2D.access.EntityPlaneGetterSetter;
import github.fonvind.immersive2D.utils.Plane;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;
    @Unique
    Vec3d TwoDimensional$intersectPoint;

    // this is kinda jank
    @Inject(method = "requestTeleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void clampInput(double x, double y, double z, float yaw, float pitch, Set<PositionFlag> flags, CallbackInfo ci) {
        Plane plane = ((EntityPlaneGetterSetter) this.player).twoDimensional$getPlane();
        if (plane != null) {
            TwoDimensional$intersectPoint = plane.intersectPoint(new Vec3d(x, y, z));
        } else {
            TwoDimensional$intersectPoint = new Vec3d(x, y, z);
        }
    }

    @ModifyVariable(method = "requestTeleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double clampX(double x) {
        return TwoDimensional$intersectPoint.x;
    }

    @ModifyVariable(method = "requestTeleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private double clampZ(double Z) {
        return TwoDimensional$intersectPoint.z;
    }
}
