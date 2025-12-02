package github.fonvind.immersive2d.client.access;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mouse.class)
public interface MouseInvoker {
    /**
     * Creates a public bridge to the private onCursorPos method in the Mouse class,
     * allowing us to safely call it from our mixins.
     */
    @Invoker("onCursorPos")
    void invokeOnCursorPos(long window, double x, double y);
}
