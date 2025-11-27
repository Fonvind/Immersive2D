package github.fonvind.immersive2D.client.mixin;

import github.fonvind.immersive2D.client.Immersive2DClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Inject(method = "getPerspective", at = @At("HEAD"), cancellable = true)
    public void getPerspective(CallbackInfoReturnable<Perspective> cir) {
        if (Immersive2DClient.plane != null) {
            cir.setReturnValue(Perspective.THIRD_PERSON_BACK);
        }
    }
}
