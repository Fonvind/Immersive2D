package github.fonvind.immersive2D.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.fonvind.immersive2D.client.Immersive2DClient;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Input.class)
public class InputMixin {
    @Shadow public float movementSideways;

    @ModifyReturnValue(method = "hasForwardMovement", at = @At("RETURN"))
    private boolean countSidewaysMovementOnPlane(boolean original) {
        if (Immersive2DClient.plane != null) {
            return original || MathHelper.abs(this.movementSideways) > 1.0E-5F;
        }
        return original;
    }
}
