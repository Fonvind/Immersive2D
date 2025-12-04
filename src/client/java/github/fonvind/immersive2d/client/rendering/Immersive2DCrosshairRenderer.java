package github.fonvind.immersive2d.client.rendering;

import github.fonvind.immersive2d.client.Immersive2DClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class Immersive2DCrosshairRenderer {

    private static final Identifier CROSSHAIR_TEXTURE = Identifier.ofVanilla("hud/crosshair");

    public static void initialize() {
        HudRenderCallback.EVENT.register(((drawContext, renderTickCounter) -> {

            // Only draw when your mod wants a 2D crosshair
            if (Immersive2DClient.plane == null)
                return;

            MinecraftClient client = MinecraftClient.getInstance();
            Mouse mouse = client.mouse;

            int scaledWidth = drawContext.getScaledWindowWidth();
            int scaledHeight = drawContext.getScaledWindowHeight();

            // Convert raw mouse coords → scaled GUI coordinates
            double mouseX = mouse.getX() * (double) scaledWidth / client.getWindow().getWidth();
            double mouseY = mouse.getY() * (double) scaledHeight / client.getWindow().getHeight();

            // Draw your crosshair at the cursor
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
