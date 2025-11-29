package github.fonvind.immersive2d.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseNormalizedGetter;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends Entity {
    @Shadow public Input input;

    @Shadow @Final protected MinecraftClient client;

    public ClientPlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyReturnValue(method = "isWalking", at = @At("RETURN"))
    private boolean countSidewaysMovementOnPlane(boolean original) {
        if (Immersive2DClient.plane != null) {
            return original || this.input.movementSideways * MathHelper.sign(((MouseNormalizedGetter) client.mouse).immersive2d$getNormalizedX()) >= 0.8;
        }

        return original;
    }
}
