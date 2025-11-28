package github.fonvind.immersive2d;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import github.fonvind.immersive2d.access.EntityPlaneGetterSetter;
import github.fonvind.immersive2d.utils.Plane;
import github.fonvind.immersive2d.utils.PlanePersistentState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.logging.Logger;

public class Immersive2D implements ModInitializer {
    public static final String MOD_ID = "immersive2d";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    public static final Identifier PLANE_SYNC = new Identifier(MOD_ID, "plane_sync");
    public static final Identifier PLANE_REMOVE = new Identifier(MOD_ID, "plane_remove");

    public static Text updatePlane(MinecraftServer minecraftServer, ServerPlayerEntity player, double x, double z, double yaw) {
        final double radYaw = yaw * MathHelper.RADIANS_PER_DEGREE;

        PlanePersistentState.setPlayerPlane(player, x, z, radYaw);
        PacketByteBuf data = PacketByteBufs.create();
        data.writeDouble(x);
        data.writeDouble(z);
        data.writeDouble(radYaw);
        minecraftServer.execute(() -> {
            ServerPlayNetworking.send(player, PLANE_SYNC, data);

            Plane newPlane = new Plane(new Vec3d(x, 0., z), radYaw);
            Plane oldPlane = ((EntityPlaneGetterSetter) player).immersive2d$getPlane();
            if (oldPlane != null) {
                newPlane.containedEntities = oldPlane.containedEntities;
                newPlane.containedEntities.forEach(entity -> {
                    ((EntityPlaneGetterSetter) entity).immersive2d$setPlane(newPlane);
                });
            }

            ((EntityPlaneGetterSetter) player).immersive2d$setPlane(newPlane);
            player.setPos(x, player.getPos().y, z);
        });

        return Text.literal("Active plane of %s set to an offset of [%f, %f], and a yaw of %f.".formatted(player.getName().getString(), x, z, yaw));
    }

    private Text removePlane(MinecraftServer minecraftServer, ServerPlayerEntity player) {
        PlanePersistentState.removePlayerPlane(player);
        minecraftServer.execute(() -> {
            ServerPlayNetworking.send(player, PLANE_REMOVE, PacketByteBufs.empty());

            Plane oldPlane = ((EntityPlaneGetterSetter) player).immersive2d$getPlane();
            if (oldPlane != null) {
                oldPlane.containedEntities.forEach(entity -> {
                    ((EntityPlaneGetterSetter) entity).immersive2d$setPlane(null);
                });
            }

            PlanePersistentState.removePlayerPlane(player);
            ((EntityPlaneGetterSetter) player).immersive2d$setPlane(null);
        });
        return Text.literal("Active plane set to none.");
    }

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register(((serverPlayNetworkHandler, packetSender, minecraftServer) -> {
            Plane plane = PlanePersistentState.getPlayerPlane(serverPlayNetworkHandler.getPlayer());
            if (plane != null) {
                updatePlane(minecraftServer, serverPlayNetworkHandler.getPlayer(), plane.getOffset().x, plane.getOffset().z, plane.getYaw());
            }
        }));

        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            if (registrationEnvironment.integrated) {
                commandDispatcher.register(CommandManager.literal("immersive2d").
                        then(CommandManager.literal("default").executes(commandContext -> {
                            ServerPlayerEntity player = commandContext.getSource().getPlayer();
                            if (player != null) {
                                commandContext.getSource().sendFeedback(() -> updatePlane(commandContext.getSource().getServer(), player,
                                        player.getBlockX() + 0.5, player.getBlockZ() + 0.5, 0.
                                ), false);
                                return 1;
                            }

                            commandContext.getSource().sendError(Text.literal("This command can only be called from a player!"));
                            return 0;
                        }))
                        .then(CommandManager.literal("from_yaw").then(CommandManager.argument("yaw", DoubleArgumentType.doubleArg()).executes(commandContext -> {
                            ServerPlayerEntity player = commandContext.getSource().getPlayer();
                            if (player != null) {
                                commandContext.getSource().sendFeedback(() -> updatePlane(commandContext.getSource().getServer(), player,
                                        player.getBlockX() + 0.5, player.getBlockZ() + 0.5, DoubleArgumentType.getDouble(commandContext, "yaw")
                                ), false);
                                return 1;
                            }

                            commandContext.getSource().sendError(Text.literal("This command can only be called from a player!"));
                            return 0;
                        })))
                        .then(CommandManager.literal("custom").requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(1)).then(
                                CommandManager.argument("offset_x", DoubleArgumentType.doubleArg()).then(
                                        CommandManager.argument("offset_z", DoubleArgumentType.doubleArg()).then(
                                                CommandManager.argument("yaw", DoubleArgumentType.doubleArg()).executes(commandContext -> {
                                                    ServerPlayerEntity player = commandContext.getSource().getPlayer();
                                                    if (player != null) {
                                                        commandContext.getSource().sendFeedback(() -> updatePlane(commandContext.getSource().getServer(), player,
                                                                DoubleArgumentType.getDouble(commandContext, "offset_x"), DoubleArgumentType.getDouble(commandContext, "offset_z"), DoubleArgumentType.getDouble(commandContext, "yaw")
                                                        ), true);
                                                        return 1;
                                                    }

                                                    commandContext.getSource().sendError(Text.literal("This command can only be called from a player!"));
                                                    return 0;
                                                })))))
                        .then(CommandManager.literal("disable").executes(commandContext -> {
                            ServerPlayerEntity player = commandContext.getSource().getPlayer();
                            if (player != null) {
                                commandContext.getSource().sendFeedback(() -> removePlane(commandContext.getSource().getServer(), commandContext.getSource().getPlayer()), false);
                                return 1;
                            }

                            commandContext.getSource().sendError(Text.literal("This command can only be called from a player!"));
                            return 0;
                        }))
                );
            }
        }));
    }

}
