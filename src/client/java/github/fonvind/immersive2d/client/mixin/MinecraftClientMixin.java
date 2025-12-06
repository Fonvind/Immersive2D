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

    // Run *before* the engine actually changes the screen to snapshot the hardware cursor position.
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void immersive2d$beforeSetScreen(Screen screen, CallbackInfo ci) {
        if (Immersive2DClient.plane == null) return;

        // If a screen is being opened (the new screen is not null), snapshot the hardware cursor pos.
        if (screen != null) {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                DoubleBuffer xb = stack.mallocDouble(1);
                DoubleBuffer yb = stack.mallocDouble(1);
                GLFW.glfwGetCursorPos(handle, xb, yb);
                if (this.mouse instanceof MouseForceUpdate setter) {
                    setter.immersive2d$setLastPosition(xb.get(0), yb.get(0));
                }
            }
        }
    }

    // Run *after* the engine has changed the screen to restore positions.
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void immersive2d$afterSetScreen(Screen newScreen, CallbackInfo ci) {
        if (Immersive2DClient.plane == null) return;

        if (this.mouse instanceof MouseForceUpdate mfu) {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();

            // A UI is being opened
            if (newScreen != null) {
                // Restore the hardware cursor to our saved position, AFTER Minecraft's own resets.
                GLFW.glfwSetCursorPos(handle, mfu.immersive2d$getLastX(), mfu.immersive2d$getLastY());
                // Ensure the cursor is visible for the UI.
                InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_NORMAL, 0, 0);
            }
            // A UI was just closed
            else {
                // Restore the hardware cursor position to our stored value.
                GLFW.glfwSetCursorPos(handle, mfu.immersive2d$getLastX(), mfu.immersive2d$getLastY());
                // Force our mixin to recompute its internal state immediately.
                mfu.immersive2d$forceNormalizedUpdate();
            }
        }
    }
}
