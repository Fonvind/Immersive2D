package github.fonvind.immersive2d.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseNormalizedGetter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
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

    @Inject(method = "updateMouse", at = @At("HEAD"))
    public void immersive2d$clampAndNormalize(CallbackInfo ci) {
        if (Immersive2DClient.plane != null) {
            double windowW = this.client.getWindow().getWidth();
            double windowH = this.client.getWindow().getHeight();

            double oldX = this.x;
            double oldY = this.y;

            // Clamp the internal software coordinates
            this.x = MathHelper.clamp(this.x, 0.0, windowW);
            this.y = MathHelper.clamp(this.y, 0.0, windowH);

            // If the coordinates were changed, it means the hardware cursor was out of bounds.
            // Force the hardware cursor to jump back to the clamped position.
            if (this.x != oldX || this.y != oldY) {
                GLFW.glfwSetCursorPos(this.client.getWindow().getHandle(), this.x, this.y);
            }
        }

        // Compute normalized coordinates for camera / renderer usage (center-based).
        double halfW = this.client.getWindow().getWidth() / 2.0;
        double halfH = this.client.getWindow().getHeight() / 2.0;
        immersive2d$normalizedX = (halfW - this.x) / halfW;
        immersive2d$normalizedY = (halfH - this.y) / halfH;

        if (immersive2d$normalizedX.isNaN() || immersive2d$normalizedX.isInfinite())
            immersive2d$normalizedX = 0d;
        if (immersive2d$normalizedY.isNaN() || immersive2d$normalizedY.isInfinite())
            immersive2d$normalizedY = 0d;
    }

    @Override
    public double immersive2d$getNormalizedX() {
        return Objects.requireNonNullElse(immersive2d$normalizedX, 0d);
    }

    @Override
    public double immersive2d$getNormalizedY() {
        return Objects.requireNonNullElse(immersive2d$normalizedY, 0d);
    }

    @WrapWithCondition(
            method = "lockCursor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V")
    )
    private boolean immersive2d$overrideLockCursor(long handle, int mode, double x, double y) {
        if (Immersive2DClient.plane != null) {
            InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_CAPTURED, 0, 0);
            return false;
        }
        return true;
    }

    @WrapWithCondition(
            method = "unlockCursor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V")
    )
    private boolean immersive2d$blockUnlock(long handle, int mode, double x, double y) {
        if (Immersive2DClient.plane != null) {
            InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_CAPTURED, 0, 0);
            return false;
        }
        return true;
    }
}
