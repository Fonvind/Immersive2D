package github.fonvind.immersive2d.client;

import github.fonvind.immersive2d.Immersive2D;
import github.fonvind.immersive2d.client.rendering.Immersive2DCrosshairRenderer;
import github.fonvind.immersive2d.client.rendering.Immersive2DShaders;
import github.fonvind.immersive2d.access.EntityPlaneGetterSetter;
import github.fonvind.immersive2d.utils.Plane;
import ladysnake.satin.api.event.PostWorldRenderCallback;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class Immersive2DClient implements ClientModInitializer {
    public static Plane plane = null;
    public static KeyBinding turnedAround = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.immersive2d.turn_around",
            GLFW.GLFW_KEY_B,
            "keyGroup.immersive2d"
    ));

    private boolean shouldUpdatePlane = true;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Immersive2D.PLANE_SYNC, ((minecraftClient, clientPlayNetworkHandler, packetByteBuf, packetSender) -> {
            plane = new Plane(new Vec3d(packetByteBuf.readDouble(), 0, packetByteBuf.readDouble()), packetByteBuf.readDouble());
            shouldUpdatePlane = true;

            MinecraftClient.getInstance().mouse.unlockCursor();
        }));
        ClientPlayNetworking.registerGlobalReceiver(Immersive2D.PLANE_REMOVE, ((minecraftClient, clientPlayNetworkHandler, packetByteBuf, packetSender) -> {
            plane =  null;
            shouldUpdatePlane = true;

            MinecraftClient.getInstance().mouse.unlockCursor();
        }));

        ClientTickEvents.START_CLIENT_TICK.register((client -> {
            if (shouldUpdatePlane && client.player != null) {
                ((EntityPlaneGetterSetter) client.player).immersive2d$setPlane(plane);
                client.worldRenderer.reload();
                shouldUpdatePlane = false;

                MinecraftClient.getInstance().mouse.lockCursor();
            }
        }));

        PostWorldRenderCallback.EVENT.register(Immersive2DShaders.INSTANCE);
        ShaderEffectRenderCallback.EVENT.register(Immersive2DShaders.INSTANCE);
        Immersive2DCrosshairRenderer.intialize();
    }
}
