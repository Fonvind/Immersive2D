package github.fonvind.immersive2d.client.mixin;

import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void cullPlane(BlockState state, BlockView world, BlockPos pos, Direction side, BlockPos otherPos, CallbackInfoReturnable<Boolean> cir) {
        Plane plane = Immersive2DClient.plane;
        if (plane != null) {
            double dist = plane.sdf(pos.toCenterPos());
            if (dist <= Plane.CULL_DIST || dist > 32){
                cir.setReturnValue(false);
                return;
            } else if (dist <= 0.5) {
                if (side.getOffsetY() == 0 && plane.sdf(pos.offset(side).toCenterPos()) <= Plane.CULL_DIST){
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
//
//        BlockState blockState = world.getBlockState(pos.offset(side));
//        if (!blockState.isFullCube(world, pos.offset(side))) {
//            cir.setReturnValue(true);
//        }
    }
}
