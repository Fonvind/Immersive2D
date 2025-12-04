package github.fonvind.immersive2d.client.rendering;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseNormalizedGetter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class Immersive2DCrosshairRenderer {

    private static final Identifier CROSSHAIR_TEXTURE = Identifier.ofVanilla("hud/crosshair");

    public static void initialize() {
        HudRenderCallback.EVENT.register(((drawContext, renderTickCounter) -> {
            if (Immersive2DClient.plane == null) return;

            MinecraftClient client = MinecraftClient.getInstance();

            // Use the normalized mouse getter we added to the Mouse mixin
            MouseNormalizedGetter mouseGetter = (MouseNormalizedGetter) client.mouse;

            int scaledWidth = drawContext.getScaledWindowWidth();
            int scaledHeight = drawContext.getScaledWindowHeight();

            // Convert normalized center-based coords back to scaled GUI pixels
            double normX = mouseGetter.immersive2d$getNormalizedX(); // -1..1 where -1 => left edge, 1 => right edge
            double normY = mouseGetter.immersive2d$getNormalizedY(); // -1..1 where -1 => top, 1 => bottom

            double mouseX = (scaledWidth / 2.0) - (normX * (scaledWidth / 2.0));
            double mouseY = (scaledHeight / 2.0) - (normY * (scaledHeight / 2.0));

            drawContext.drawGuiTexture(
                    CROSSHAIR_TEXTURE,
                    (int) mouseX - 8,
                    (int) mouseY - 8,
                    15,
                    15
            );
        }));
    }
}
