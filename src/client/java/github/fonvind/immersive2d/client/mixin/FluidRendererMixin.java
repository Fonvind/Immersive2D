package github.fonvind.immersive2d.client.mixin;


import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidRenderer.class)
public class FluidRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cullFluids(BlockRenderView world, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        if (Plane.shouldCull(pos, Immersive2DClient.plane)) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldRenderSide", at = @At("HEAD"), cancellable = true)
    private static void enableCulledFluidSide(BlockRenderView world, BlockPos pos, FluidState fluidState, BlockState blockState, Direction direction, FluidState neighborFluidState, CallbackInfoReturnable<Boolean> cir) {
        if (Plane.shouldCull(pos.offset(direction), Immersive2DClient.plane)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isSideCovered(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/Direction;FLnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", at = @At("HEAD"), cancellable = true)
    private static void enableCulledSides(BlockView world, Direction direction, float height, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (Plane.shouldCull(pos.offset(direction), Immersive2DClient.plane)) {
            cir.setReturnValue(false);
        }
    }
}
