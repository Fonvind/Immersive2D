package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseForceUpdate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow @Final public Mouse mouse;
    @Shadow public Screen currentScreen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void immersive2d$beforeSetScreen(Screen screen, CallbackInfo ci) {
        if (Immersive2DClient.plane == null) return;

        if (screen != null && this.mouse instanceof MouseForceUpdate mfu) {
            mfu.immersive2d$storePreUIScreenPosition();
        }
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void immersive2d$afterSetScreen(Screen newScreen, CallbackInfo ci) {
        if (Immersive2DClient.plane == null) return;

        if (this.mouse instanceof MouseForceUpdate mfu) {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();

            if (newScreen != null) {
                // UI opened: restore current cursor internally
                double lx = mfu.immersive2d$getLastX();
                double ly = mfu.immersive2d$getLastY();
                GLFW.glfwSetCursorPos(handle, lx, ly);
                mfu.immersive2d$forceInternalCursorUpdate();
                InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_NORMAL, lx, ly);
            } else {
                // UI closed: restore pre-UI snapshot
                mfu.immersive2d$restorePreUIScreenPosition();
            }
        }
    }
}
