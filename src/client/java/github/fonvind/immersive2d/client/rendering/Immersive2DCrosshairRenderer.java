package github.fonvind.immersive2d.client.rendering;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseNormalizedGetter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class Immersive2DCrosshairRenderer {

    private static final Identifier CROSSHAIR_TEXTURE = Identifier.ofVanilla("hud/crosshair");
    private static final int CROSSHAIR_SIZE = 15;

    public static void initialize() {
        HudRenderCallback.EVENT.register(((drawContext, renderTickCounter) -> {
            if (Immersive2DClient.plane == null) return;

            MinecraftClient client = MinecraftClient.getInstance();
            MouseNormalizedGetter mouseGetter = (MouseNormalizedGetter) client.mouse;

            int scaledWidth = drawContext.getScaledWindowWidth();
            int scaledHeight = drawContext.getScaledWindowHeight();

            // Convert normalized center-based coords back to scaled GUI pixels
            double normX = mouseGetter.immersive2d$getNormalizedX();
            double normY = mouseGetter.immersive2d$getNormalizedY();

            double mouseX = (scaledWidth / 2.0) - (normX * (scaledWidth / 2.0));
            double mouseY = (scaledHeight / 2.0) - (normY * (scaledHeight / 2.0));

            drawContext.drawGuiTexture(
                    CROSSHAIR_TEXTURE,
                    (int) (mouseX - (CROSSHAIR_SIZE / 2.0)),
                    (int) (mouseY - (CROSSHAIR_SIZE / 2.0)),
                    CROSSHAIR_SIZE,
                    CROSSHAIR_SIZE
            );
        }));
    }
}
