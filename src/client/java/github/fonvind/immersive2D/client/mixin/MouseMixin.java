package github.fonvind.immersive2D.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import github.fonvind.immersive2D.client.Immersive2DClient;
import github.fonvind.immersive2D.client.access.MouseNormalizedGetter;
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
    private Double twoDimensional$normalizedX = 0d;
    @Unique
    private Double twoDimensional$normalizedY = 0d;

    @Override
    public double twoDimensional$getNormalizedX() {
        return Objects.requireNonNullElse(twoDimensional$normalizedX, 0d);
    }

    @Override
    public double twoDimensional$getNormalizedY() {
        return Objects.requireNonNullElse(twoDimensional$normalizedY, 0d);
    }

    @Inject(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getMouseSensitivity()Lnet/minecraft/client/option/SimpleOption;"))
    public void updateNormalizedPos(CallbackInfo ci) {
        double width = this.client.getWindow().getWidth() / 2f;
        double height = this.client.getWindow().getHeight() / 2f;

        twoDimensional$normalizedX = (width - this.x) / width;
        twoDimensional$normalizedY = (height - this.y) / height;

        if (twoDimensional$normalizedX.isInfinite() || twoDimensional$normalizedX.isNaN()) {
            twoDimensional$normalizedX = 0d;
        }

        if (twoDimensional$normalizedY.isInfinite() || twoDimensional$normalizedY.isNaN()) {
            twoDimensional$normalizedY = 0d;
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
