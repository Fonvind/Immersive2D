package github.fonvind.immersive2d.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.utils.Plane;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @ModifyExpressionValue(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkBuilder$ChunkData;getBlockEntities()Ljava/util/List;")
    )
    private List<BlockEntity> cullBlockEntites(List<BlockEntity> original) {
        Plane plane = Immersive2DClient.plane;
        if (plane != null) {
            original = original.stream().filter(blockEntity ->
                    !Plane.shouldCull(blockEntity.getPos(), plane))
                    .toList();
        }

        return original;
    }

    @Inject(method = "drawBlockOutline", at = @At(value = "HEAD"), cancellable = true)
    private void disableCulledBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, Entity entity, double cameraX, double cameraY, double cameraZ, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (Plane.shouldCull(pos, Immersive2DClient.plane)) {
            ci.cancel();
        }
    }

    // This mixin is incompatible with Sodium. It is disabled to allow the game to launch.
    // A more robust solution is needed for Sodium compatibility.
//    @ModifyExpressionValue(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Frustum;isVisible(Lnet/minecraft/util/math/Box;)Z"))
//    private boolean disableChunkCulling(boolean original) {
//        if (Immersive2DClient.plane != null) {
//            return true;
//        }
//        return original;
//    }
}
