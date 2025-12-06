package github.fonvind.immersive2d.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.client.access.MouseForceUpdate;
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

@Mixin(Mouse.class)
public abstract class MouseMixin implements MouseNormalizedGetter, MouseForceUpdate {

    @Shadow @Final private MinecraftClient client;
    @Shadow private double x;
    @Shadow private double y;

    @Shadow public abstract void onCursorPos(long window, double x, double y);

    @Unique private double immersive2d$normalizedX = 0.0;
    @Unique private double immersive2d$normalizedY = 0.0;
    @Unique private double immersive2d$lastX = 0.0;
    @Unique private double immersive2d$lastY = 0.0;

    @Unique private double immersive2d$preUIScreenX = 0.0;
    @Unique private double immersive2d$preUIScreenY = 0.0;

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void immersive2d$trackAndClampCursorPos(long window, double xpos, double ypos, CallbackInfo ci) {
        // Only clamp during gameplay, not when a screen (GUI) is open
        if (Immersive2DClient.plane != null && client.currentScreen == null) {
            double windowW = this.client.getWindow().getWidth();
            double windowH = this.client.getWindow().getHeight();

            this.x = MathHelper.clamp(xpos, 0.0, windowW);
            this.y = MathHelper.clamp(ypos, 0.0, windowH);

            GLFW.glfwSetCursorPos(window, this.x, this.y);

            this.immersive2d$lastX = this.x;
            this.immersive2d$lastY = this.y;
        }
    }

    @Inject(method = "updateMouse", at = @At("HEAD"))
    public void immersive2d$normalize(CallbackInfo ci) {
        double halfW = this.client.getWindow().getWidth() / 2.0;
        double halfH = this.client.getWindow().getHeight() / 2.0;
        immersive2d$normalizedX = (halfW - this.x) / halfW;
        immersive2d$normalizedY = (halfH - this.y) / halfH;
    }

    @Override
    public double immersive2d$getNormalizedX() { return immersive2d$normalizedX; }

    @Override
    public double immersive2d$getNormalizedY() { return immersive2d$normalizedY; }

    @WrapWithCondition(
            method = "lockCursor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V")
    )
    private boolean immersive2d$overrideLockCursor(long handle, int mode, double x, double y) {
        if (Immersive2DClient.plane != null) {
            if (client.currentScreen == null)
                InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_HIDDEN, 0, 0);
            return false;
        }
        return true;
    }

    @WrapWithCondition(
            method = "unlockCursor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V")
    )
    private boolean immersive2d$blockUnlockCursor(long handle, int mode, double x, double y) {
        if (Immersive2DClient.plane != null) {
            if (client.currentScreen != null)
                InputUtil.setCursorParameters(handle, GLFW.GLFW_CURSOR_NORMAL, 0, 0);
            return false;
        }
        return true;
    }

    @Unique
    @Override
    public void immersive2d$setLastPosition(double x, double y) {
        this.immersive2d$lastX = x;
        this.immersive2d$lastY = y;
    }

    @Unique
    @Override
    public double immersive2d$getLastX() { return this.immersive2d$lastX; }

    @Unique
    @Override
    public double immersive2d$getLastY() { return this.immersive2d$lastY; }

    @Unique
    @Override
    public void immersive2d$forceNormalizedUpdate() {
        this.x = this.immersive2d$lastX;
        this.y = this.immersive2d$lastY;
        this.immersive2d$normalize(null);
    }

    @Unique
    @Override
    public void immersive2d$forceInternalCursorUpdate() {
        long handle = this.client.getWindow().getHandle();
        this.onCursorPos(handle, this.immersive2d$getLastX(), this.immersive2d$getLastY());
    }

    // Pre-UI snapshot methods
    @Override
    public void immersive2d$storePreUIScreenPosition() {
        immersive2d$preUIScreenX = immersive2d$lastX;
        immersive2d$preUIScreenY = immersive2d$lastY;
    }

    @Override
    public void immersive2d$restorePreUIScreenPosition() {
        this.immersive2d$lastX = immersive2d$preUIScreenX;
        this.immersive2d$lastY = immersive2d$preUIScreenY;
        immersive2d$forceInternalCursorUpdate();
        immersive2d$forceNormalizedUpdate();
    }
}
