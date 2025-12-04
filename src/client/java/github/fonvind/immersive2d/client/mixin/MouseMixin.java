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

    // ============================================================
    // NORMALIZED MOUSE POSITION FOR CAMERA MIXIN
    // ============================================================

    @Inject(method = "updateMouse", at = @At("HEAD"))
    public void immersive2d$updateNormalizedPos(CallbackInfo ci) {
        double w = client.getWindow().getWidth() / 2f;
        double h = client.getWindow().getHeight() / 2f;

        immersive2d$normalizedX = (w - this.x) / w;
        immersive2d$normalizedY = (h - this.y) / h;

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

    // ============================================================
    // FORCE CURSOR CONFINE WHEN PLANE IS ACTIVE
    // ============================================================

    /**
     * Prevent Minecraft from *unlocking* or *showing* the hardware cursor.
     * Forces cursor into GLFW_CURSOR_DISABLED (confined) mode.
     */
    @WrapWithCondition(
            method = "lockCursor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V"
            )
    )
    private boolean immersive2d$overrideLockCursor(long handle, int mode, double x, double y) {

        if (Immersive2DClient.plane != null) {
            // Force hardware cursor into confined mode
            InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_DISABLED, 0, 0);
            return false; // prevent vanilla call
        }

        return true;
    }

    /**
     * Prevent Minecraft from *unlocking* the cursor on GUI events.
     */
    @WrapWithCondition(
            method = "unlockCursor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V"
            )
    )
    private boolean immersive2d$blockUnlock(long handle, int mode, double x, double y) {

        if (Immersive2DClient.plane != null) {
            InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_DISABLED, 0, 0);
            return false; // block unlock
        }

        return true;
    }
}
