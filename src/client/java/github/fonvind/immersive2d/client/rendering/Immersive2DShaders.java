package github.fonvind.immersive2d.client.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import github.fonvind.immersive2d.Immersive2D;
import github.fonvind.immersive2d.client.Immersive2DClient;
import github.fonvind.immersive2d.utils.Plane;
import ladysnake.satin.api.event.PostWorldRenderCallback;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import ladysnake.satin.api.experimental.ReadableDepthFramebuffer;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import ladysnake.satin.api.managed.uniform.*;
import ladysnake.satin.api.util.GlMatrices;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2f;

public class Immersive2DShaders implements PostWorldRenderCallback, ShaderEffectRenderCallback {
    public static final Identifier PLANE_SHADERS_ID = new Identifier(Immersive2D.MOD_ID, "shaders/post/plane_shaders.json");
    public static final Immersive2DShaders INSTANCE = new Immersive2DShaders();

    private final MinecraftClient minecraftClient = MinecraftClient.getInstance();

    private final Matrix4f projectionMatrix = new Matrix4f();

    final ManagedShaderEffect PLANE_SHADERS = ShaderEffectManager.getInstance().manage(PLANE_SHADERS_ID, shader -> {
        shader.setSamplerUniform("DepthSampler", ((ReadableDepthFramebuffer)minecraftClient.getFramebuffer()).getStillDepthMap());
    });
    private final UniformMat4 uniformInverseTransformMatrix = PLANE_SHADERS.findUniformMat4("InverseTransformMatrix");
    private final Uniform3f uniformCameraPos = PLANE_SHADERS.findUniform3f("CameraPos");
    private final Uniform3f uniformPlayerPos = PLANE_SHADERS.findUniform3f("PlayerPos");
    private final Uniform3f uniformPlaneOffset = PLANE_SHADERS.findUniform3f("PlaneOffset");
    private final Uniform1f uniformPlaneSlope = PLANE_SHADERS.findUniform1f("PlaneSlope");
    private final Uniform3f uniformPlaneNormal = PLANE_SHADERS.findUniform3f("PlaneNormal");
    private final Uniform3f uniformSkyColor = PLANE_SHADERS.findUniform3f("SkyColor");
    private final Uniform2f uniformLightLevel = PLANE_SHADERS.findUniform2f("LightLevel");

    private Vector2f lightLevel = new Vector2f(15f);

    @Override
    public void onWorldRendered(Camera camera, float tickDelta, long nanoTime) {
        Plane plane = Immersive2DClient.plane;
        if (plane != null) {
            uniformInverseTransformMatrix.set(GlMatrices.getInverseTransformMatrix(projectionMatrix));

            uniformCameraPos.set(camera.getPos().toVector3f());

            uniformPlaneOffset.set(plane.getOffset().toVector3f());
            uniformPlaneSlope.set((float) plane.getSlope());
            uniformPlaneNormal.set(plane.getNormal().toVector3f());

            float[] fogColor = RenderSystem.getShaderFogColor();
            uniformSkyColor.set(fogColor[0], fogColor[1], fogColor[2]);

            World world = MinecraftClient.getInstance().world;
            PlayerEntity player = MinecraftClient.getInstance().player;
            Vector2f targetLightLevel = new Vector2f(world.getLightLevel(LightType.BLOCK, player.getBlockPos()), world.getLightLevel(LightType.SKY, player.getBlockPos()));
            player.getHandItems().forEach(itemStack -> {
                if (itemStack.getItem() instanceof BlockItem blockItem) {
                    targetLightLevel.x = Math.max(targetLightLevel.x, MathHelper.clamp(blockItem.getBlock().getDefaultState().getLuminance(), 0f, 10f));
                }
            });

            lightLevel = lightLevel.lerp(targetLightLevel, 0.2f);
            uniformLightLevel.set(lightLevel);

            uniformPlayerPos.set(player.getPos().toVector3f());
        }
    }

    @Override
    public void renderShaderEffects(float tickDelta) {
        if (Immersive2DClient.plane != null) {
            PLANE_SHADERS.render(tickDelta);
        }
    }
}
