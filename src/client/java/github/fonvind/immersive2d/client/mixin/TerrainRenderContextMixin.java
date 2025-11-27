package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.utils.Plane;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Injecting into fabric is kinda junk
@Mixin(TerrainRenderContext.class)
public class TerrainRenderContextMixin {
    // culls blocks like tall grass which get through the initial cull phase in BlockMixin
    @Inject(method = "tessellateBlock", at = @At("HEAD"), cancellable = true)
    public void cullBlocks(BlockState blockState, BlockPos blockPos, BakedModel model, MatrixStack matrixStack, CallbackInfo ci) {
        if (Plane.shouldCull(blockPos, Immersive2DClient.plane)) {
            ci.cancel();
        }
    }
}
