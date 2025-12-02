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
public abstract class MinecraftClientMixin {
    @Shadow @Final private Window window;
    @Shadow @Final public Mouse mouse;

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void immersive2d$restoreCursorAfterSetScreen(Screen screen, CallbackInfo ci) {
        if (Immersive2DClient.plane == null) return;
        if (!(this.mouse instanceof MouseNormalizedGetter getter)) return;

        // Compute pixel coordinates from stored normalized coordinates
        double normX = getter.immersive2d$getNormalizedX();
        double normY = getter.immersive2d$getNormalizedY();
        double windowWidth = this.window.getWidth();
        double windowHeight = this.window.getHeight();

        double x = (windowWidth / 2.0) - (normX * (windowWidth / 2.0));
        double y = (windowHeight / 2.0) - (normY * (windowHeight / 2.0));
        long handle = this.window.getHandle();

        int inputMode = (screen == null) ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL;
        try {
            InputUtil.setCursorParameters(handle, inputMode, x, y);
        } catch (Throwable ignored) {}

        // Fire synthetic cursor event (GLFW) so native state is consistent
        if (screen == null) {
            try {
                ((MouseInvoker) this.mouse).invokeOnCursorPos(handle, x, y);
            } catch (Throwable ignored) {}
            // Schedule a single forced look update for the next tick after mouse.updateMouse() runs
            Immersive2DClient.forceNextLookUpdate = true;
        }
    }

    /**
     * Force the player's aim update on the tick AFTER the screen closes.
     * This runs at the TAIL of tick: after input/mouse.updateMouse logic.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void immersive2d$forceInitialLookAfterScreenClose(CallbackInfo ci) {
        if (!Immersive2DClient.forceNextLookUpdate) return;
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null) {
            Immersive2DClient.forceNextLookUpdate = false;
            return;
        }

        // Now that mouse.x/mouse.y were updated, trigger the player's aiming logic,
        // which reads absolute mouse coords. changeLookDirection is cancellable and
        // will run your EntityMixin logic.
        client.player.changeLookDirection(0.0, 0.0);

        Immersive2DClient.forceNextLookUpdate = false;
    }
}
