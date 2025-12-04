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
public class MouseMixin implements MouseNormalizedGetter, MouseForceUpdate {

    @Shadow @Final private MinecraftClient client;
    @Shadow private double x;
    @Shadow private double y;

    @Unique
    private double immersive2d$normalizedX = 0.0;
    @Unique
    private double immersive2d$normalizedY = 0.0;
    @Unique
    private double immersive2d$lastX = 0.0;
    @Unique
    private double immersive2d$lastY = 0.0;

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void immersive2d$trackAndClampCursorPos(long window, double xpos, double ypos, CallbackInfo ci) {
        if (Immersive2DClient.plane != null) {
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
    public double immersive2d$getNormalizedX() {
        return immersive2d$normalizedX;
    }

    @Override
    public double immersive2d$getNormalizedY() {
        return immersive2d$normalizedY;
    }

    @WrapWithCondition(
            method = "lockCursor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/InputUtil;setCursorParameters(JIDD)V")
    )
    private boolean immersive2d$overrideLockCursor(long handle, int mode, double x, double y) {
        if (Immersive2DClient.plane != null) {
            // In gameplay (no UI): hide but DO NOT capture
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
            // In UI (screen open): cursor must be visible and free
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
    public double immersive2d$getLastX() {
        return this.immersive2d$lastX;
    }



    @Unique
    @Override
    public double immersive2d$getLastY() {
        return this.immersive2d$lastY;
    }

    @Unique
    @Override
    public void immersive2d$forceNormalizedUpdate() {
        this.x = this.immersive2d$lastX;
        this.y = this.immersive2d$lastY;

        double windowW = this.client.getWindow().getWidth();
        double windowH = this.client.getWindow().getHeight();
        this.x = MathHelper.clamp(this.x, 0.0, windowW);
        this.y = MathHelper.clamp(this.y, 0.0, windowH);

        double halfW = windowW / 2.0;
        double halfH = windowH / 2.0;
        immersive2d$normalizedX = (halfW - this.x) / halfW;
        immersive2d$normalizedY = (halfH - this.y) / halfH;
    }
}
