package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseInvoker;
import github.fonvind.immersive2d.client.access.MouseNormalizedGetter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Final private Window window;
    @Shadow @Final public Mouse mouse;

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void immersive2d$restoreCursorAfterSetScreen(Screen screen, CallbackInfo ci) {
        if (Immersive2DClient.plane == null) return;

        if (!(this.mouse instanceof MouseNormalizedGetter getter)) return;

        double normX = getter.immersive2d$getNormalizedX();
        double normY = getter.immersive2d$getNormalizedY();

        double windowWidth = this.window.getWidth();
        double windowHeight = this.window.getHeight();

        double x = (windowWidth / 2.0) - (normX * (windowWidth / 2.0));
        double y = (windowHeight / 2.0) - (normY * (windowHeight / 2.0));

        long handle = this.window.getHandle();

        int inputMode;
        if (screen == null) {
            inputMode = GLFW.GLFW_CURSOR_DISABLED;
        } else {
            inputMode = GLFW.GLFW_CURSOR_NORMAL;
        }

        try {
            InputUtil.setCursorParameters(handle, inputMode, x, y);
        } catch (Throwable ignored) {}

        if (screen == null) {
            try {
                // Use the invoker to safely call the private method
                ((MouseInvoker) this.mouse).invokeOnCursorPos(handle, x, y);
            } catch (Throwable ignored) {}
        }
    }
}
