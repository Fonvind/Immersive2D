package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.Immersive2DRaycaster;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    void immersive2d$overwriteRaycastWith2D(float tickDelta, CallbackInfo ci) { // Changed from private to package-private
        if (Immersive2DClient.plane != null) {
            Entity cameraEntity = this.client.getCameraEntity();
            if (cameraEntity != null && this.client.world != null && this.client.player != null) {
                // Use the player's actual reach distance
                double interactionRange = this.client.player.getBlockInteractionRange();
                // Overwrite the vanilla crosshairTarget with our 2D-constrained raycast result
                this.client.crosshairTarget = Immersive2DRaycaster.raycast2D(cameraEntity, interactionRange, tickDelta);
            }
        }
    }
}
