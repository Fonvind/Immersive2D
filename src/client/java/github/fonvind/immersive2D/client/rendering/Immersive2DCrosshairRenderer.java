package github.fonvind.immersive2D.client.rendering;

import github.fonvind.immersive2D.client.Immersive2DClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.util.Identifier;

public class Immersive2DCrosshairRenderer {
    public static void intialize() {
        HudRenderCallback.EVENT.register(((drawContext, tickDelta) -> {
            if (Immersive2DClient.plane != null) {
                Mouse mouse = MinecraftClient.getInstance().mouse;
                int scaleFactor = MinecraftClient.getInstance().getWindow().getWidth()/drawContext.getScaledWindowWidth();
                drawContext.drawTexture(new Identifier("textures/gui/icons.png"), (int) (mouse.getX()/scaleFactor) - 6, (int) (mouse.getY()/scaleFactor) - 5, 0, 0, 15, 15);
            }
        }));
    }
}
