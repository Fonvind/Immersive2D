package github.fonvind.immersive2d;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import github.fonvind.immersive2d.access.EntityPlaneGetterSetter;
import github.fonvind.immersive2d.utils.Plane;
import github.fonvind.immersive2d.utils.PlanePersistentState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Immersive2D implements ModInitializer {
    public static final String MOD_ID = "immersive2d";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier PLANE_SYNC = Identifier.of(MOD_ID, "plane_sync");
    public static final Identifier PLANE_REMOVE = Identifier.of(MOD_ID, "plane_remove");

    public record PlaneSyncPayload(double x, double z, double radYaw) implements CustomPayload {
        public static final CustomPayload.Id<PlaneSyncPayload> ID = new CustomPayload.Id<>(Immersive2D.PLANE_SYNC);
        public static final PacketCodec<PacketByteBuf, PlaneSyncPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeDouble(value.x());
                    buf.writeDouble(value.z());
                    buf.writeDouble(value.radYaw());
                },
                buf -> new PlaneSyncPayload(buf.readDouble(), buf.readDouble(), buf.readDouble())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record PlaneRemovePayload() implements CustomPayload {
        public static final CustomPayload.Id<PlaneRemovePayload> ID = new CustomPayload.Id<>(Immersive2D.PLANE_REMOVE);
        public static final PacketCodec<PacketByteBuf, PlaneRemovePayload> CODEC = PacketCodec.unit(new PlaneRemovePayload());


        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private static float snapYawToCardinal(float yaw) {
        float wrappedYaw = MathHelper.wrapDegrees(yaw);
        return Math.round(wrappedYaw / 90.0F) * 90.0F;
    }


    public static Text updatePlane(MinecraftServer minecraftServer, ServerPlayerEntity player, double x, double z, double yaw) {
        final double radYaw = yaw * MathHelper.RADIANS_PER_DEGREE;

        // Removed original yaw storage logic
        // LOGGER.info("updatePlane called for {}. Current original yaw in state: {}", player.getName().getString(), PlanePersistentState.getPlayerOriginalYaw(player));
        // if (PlanePersistentState.getPlayerOriginalYaw(player) == null) {
        //     PlanePersistentState.setPlayerOriginalYaw(player, player.getYaw());
        // }

        PlanePersistentState.setPlayerPlane(player, x, z, radYaw);
        minecraftServer.execute(() -> {
            ServerPlayNetworking.send(player, new PlaneSyncPayload(x, z, radYaw));

            Plane newPlane = new Plane(new Vec3d(x, 0., z), radYaw);
            Plane oldPlane = ((EntityPlaneGetterSetter) player).immersive2d$getPlane();
            if (oldPlane != null) {
                newPlane.containedEntities = oldPlane.containedEntities;
                newPlane.containedEntities.forEach(entity -> ((EntityPlaneGetterSetter) entity).immersive2d$setPlane(newPlane));
            }

            ((EntityPlaneGetterSetter) player).immersive2d$setPlane(newPlane);
            player.setPos(x, player.getPos().y, z);
        });

        return Text.literal("Active plane of %s set to an offset of [%f, %f], and a yaw of %f.".formatted(player.getName().getString(), x, z, yaw));
    }

    private Text removePlane(MinecraftServer minecraftServer, ServerPlayerEntity player) {
        // Removed original yaw restoration logic
        // LOGGER.info("removePlane called for {}", player.getName().getString());
        PlanePersistentState.removePlayerPlane(player);
        minecraftServer.execute(() -> {
            ServerPlayNetworking.send(player, new PlaneRemovePayload());

            Plane oldPlane = ((EntityPlaneGetterSetter) player).immersive2d$getPlane();
            if (oldPlane != null) {
                oldPlane.containedEntities.forEach(entity -> ((EntityPlaneGetterSetter) entity).immersive2d$setPlane(null));
            }

            PlanePersistentState.removePlayerPlane(player);
            ((EntityPlaneGetterSetter) player).immersive2d$setPlane(null);

            // Removed original yaw restoration logic
            // Float originalYaw = PlanePersistentState.getPlayerOriginalYaw(player);
            // LOGGER.info("Attempting to restore original yaw for {}: {}", player.getName().getString(), originalYaw);
            // if (originalYaw != null) {
            //     player.setYaw(originalYaw);
            //     PlanePersistentState.removePlayerOriginalYaw(player);
            // }
        });
        return Text.literal("Active plane set to none.");
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(PlaneSyncPayload.ID, PlaneSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlaneRemovePayload.ID, PlaneRemovePayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register(((serverPlayNetworkHandler, packetSender, minecraftServer) -> {
            Plane plane = PlanePersistentState.getPlayerPlane(serverPlayNetworkHandler.getPlayer());
            if (plane != null) {
                // Do NOT snap yaw when loading a saved plane
                updatePlane(minecraftServer, serverPlayNetworkHandler.getPlayer(), plane.getOffset().x, plane.getOffset().z, plane.getYaw());
            }
        }));


        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            if (registrationEnvironment.integrated) {
                commandDispatcher.register(CommandManager.literal("immersive2d").
                        then(CommandManager.literal("default").executes(commandContext -> {
                            ServerPlayerEntity player = commandContext.getSource().getPlayer();
                            if (player != null) {
                                return updatePlane(commandContext.getSource().getServer(), player,
                                        player.getBlockX() + 0.5, player.getBlockZ() + 0.5, snapYawToCardinal(player.getYaw())
                                ).equals(Text.empty()) ? 1 : 0; // Simplified lambda
                            }

                            commandContext.getSource().sendError(Text.literal("This command can only be called from a player!"));
                            return 0;
                        }))
                        .then(CommandManager.literal("from_yaw").then(CommandManager.argument("yaw", DoubleArgumentType.doubleArg()).executes(commandContext -> {
                            ServerPlayerEntity player = commandContext.getSource().getPlayer();
                            if (player != null) {
                                return updatePlane(commandContext.getSource().getServer(), player,
                                        player.getBlockX() + 0.5, player.getBlockZ() + 0.5, snapYawToCardinal((float) DoubleArgumentType.getDouble(commandContext, "yaw"))
                                ).equals(Text.empty()) ? 1 : 0; // Simplified lambda
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
                                                        return updatePlane(commandContext.getSource().getServer(), player,
                                                                DoubleArgumentType.getDouble(commandContext, "offset_x"), DoubleArgumentType.getDouble(commandContext, "offset_z"), snapYawToCardinal((float) DoubleArgumentType.getDouble(commandContext, "yaw"))
                                                        ).equals(Text.empty()) ? 1 : 0; // Simplified lambda
                                                    }

                                                    commandContext.getSource().sendError(Text.literal("This command can only be called from a player!"));
                                                    return 0;
                                                })))))
                        .then(CommandManager.literal("disable").executes(commandContext -> {
                            ServerPlayerEntity player = commandContext.getSource().getPlayer();
                            if (player != null) {
                                return removePlane(commandContext.getSource().getServer(), commandContext.getSource().getPlayer()).equals(Text.empty()) ? 1 : 0; // Simplified lambda
                            }

                            commandContext.getSource().sendError(Text.literal("This command can only be called from a player!"));
                            return 0;
                        }))
                );
            }
        }));
    }
}
