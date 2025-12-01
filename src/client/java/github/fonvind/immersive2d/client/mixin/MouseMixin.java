package github.fonvind.immersive2d.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseNormalizedGetter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(Mouse.class)
public class MouseMixin implements MouseNormalizedGetter {
    @Shadow @Final private MinecraftClient client;
    @Shadow private double x;
    @Shadow private double y;
    @Unique
    private Double immersive2d$normalizedX = 0d;
    @Unique
    private Double immersive2d$normalizedY = 0d;

    /**
     * Injects at the head of onScreenSet and cancels the method if our 2D plane is active.
     * This prevents Minecraft from resetting the mouse cursor to the center of the screen
     * every time a UI (inventory, chest, menu, etc.) is opened.
     */
    @Inject(method = "onScreenSet", at = @At("HEAD"), cancellable = true)
    private void immersive2d$preventMouseReset(int width, int height, CallbackInfo ci) {
        if (Immersive2DClient.plane != null) {
            // If our 2D mode is active, cancel the entire method to prevent the
            // cursor from being centered.
            ci.cancel();
        }
    }

    @Override
    public double immersive2d$getNormalizedX() {
        return Objects.requireNonNullElse(immersive2d$normalizedX, 0d);
    }

    @Override
    public double immersive2d$getNormalizedY() {
        return Objects.requireNonNullElse(immersive2d$normalizedY, 0d);
    }

    @Inject(method = "updateMouse", at = @At("HEAD"))
    public void updateNormalizedPos(CallbackInfo ci) {
        double width = this.client.getWindow().getWidth() / 2f;
        double height = this.client.getWindow().getHeight() / 2f;

        immersive2d$normalizedX = (width - this.x) / width;
        immersive2d$normalizedY = (height - this.y) / height;

        if (immersive2d$normalizedX.isInfinite() || immersive2d$normalizedX.isNaN()) {
            immersive2d$normalizedX = 0d;
        }

        if (immersive2d$normalizedY.isInfinite() || immersive2d$normalizedY.isNaN()) {
            immersive2d$normalizedY = 0d;
        }
    }

    @WrapWithCondition(method = "lockCursor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V"))
    public boolean lockCursor(long handler, int inputModeValue, double x, double y) {
        if (Immersive2DClient.plane != null) {
            InputUtil.setCursorParameters(handler, GLFW.GLFW_CURSOR_HIDDEN, x, y);
            return false;
        }

        return true;
    }
}
