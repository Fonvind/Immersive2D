package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseForceUpdate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.DoubleBuffer;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow @Final public Mouse mouse;
    @Shadow public Screen currentScreen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void immersive2d$beforeSetScreen(Screen screen, CallbackInfo ci) {
        if (Immersive2DClient.plane == null) return;

        if (screen != null) { // A UI is being opened
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                DoubleBuffer xb = stack.mallocDouble(1);
                DoubleBuffer yb = stack.mallocDouble(1);
                GLFW.glfwGetCursorPos(handle, xb, yb);
                double hx = xb.get(0);
                double hy = yb.get(0);

                if (this.mouse instanceof MouseForceUpdate setter) {
                    setter.immersive2d$setLastPosition(hx, hy);
                }

                // Immediately restore the cursor to this position and make it visible for the UI.
                InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_NORMAL, hx, hy);
            }
        }
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void immersive2d$afterSetScreen(Screen screen, CallbackInfo ci) {
        if (Immersive2DClient.plane == null) return;

        if (screen == null) { // A UI was just closed
            if (this.mouse instanceof MouseForceUpdate getter) {
                double lx = getter.immersive2d$getLastX();
                double ly = getter.immersive2d$getLastY();
                long handle = MinecraftClient.getInstance().getWindow().getHandle();

                // Restore the hardware cursor position to our stored value
                GLFW.glfwSetCursorPos(handle, lx, ly);

                // Force the mixin to recompute normalized coords immediately
                getter.immersive2d$forceNormalizedUpdate();
            }
        }
    }
}
