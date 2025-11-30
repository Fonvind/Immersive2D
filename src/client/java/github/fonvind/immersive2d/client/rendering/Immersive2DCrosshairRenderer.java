package github.fonvind.immersive2d.client.rendering;

import github.fonvind.immersive2d.client.Immersive2DClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class Immersive2DCrosshairRenderer {

    // The correct, hardcoded Identifier for the vanilla crosshair texture
    private static final Identifier CROSSHAIR_TEXTURE = Identifier.ofVanilla("hud/crosshair");

    public static void intialize() {
        HudRenderCallback.EVENT.register(((drawContext, renderTickCounter) -> {
            if (Immersive2DClient.plane != null) {
                Mouse mouse = MinecraftClient.getInstance().mouse;
                int scaledWidth = drawContext.getScaledWindowWidth();
                int scaledHeight = drawContext.getScaledWindowHeight();
                
                // Get the mouse position in scaled coordinates
                double mouseX = mouse.getX() * (double)scaledWidth / MinecraftClient.getInstance().getWindow().getWidth();
                double mouseY = mouse.getY() * (double)scaledHeight / MinecraftClient.getInstance().getWindow().getHeight();

                // Use the correct Identifier and draw the texture
                drawContext.drawGuiTexture(CROSSHAIR_TEXTURE, (int)mouseX - 8, (int)mouseY - 8, 15, 15);
            }
        }));
    }
}
