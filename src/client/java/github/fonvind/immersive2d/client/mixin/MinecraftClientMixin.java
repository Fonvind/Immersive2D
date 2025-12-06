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

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void immersive2d$afterSetScreen(Screen newScreen, CallbackInfo ci) {
        if (Immersive2DClient.plane == null) return;

        if (this.mouse instanceof MouseForceUpdate mfu) {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            double lx = mfu.immersive2d$getLastX();
            double ly = mfu.immersive2d$getLastY();

            if (newScreen != null) {
                // Restore cursor to the last known position
                GLFW.glfwSetCursorPos(handle, lx, ly);
                mfu.immersive2d$forceInternalCursorUpdate();

                // Don't move the cursor to 0,0 — use the restored coordinates
                InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_NORMAL, lx, ly);
            } else {
                GLFW.glfwSetCursorPos(handle, lx, ly);
                mfu.immersive2d$forceInternalCursorUpdate();
                mfu.immersive2d$forceNormalizedUpdate();
            }
        }
    }
}
