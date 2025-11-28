package github.fonvind.immersive2d.utils;

import github.fonvind.immersive2d.Immersive2D;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class PlanePersistentState extends PersistentState {
    private final HashMap<UUID, Plane> players = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound playersNbt = new NbtCompound();
        players.forEach(((uuid, plane) -> {
            NbtCompound playerNbt = new NbtCompound();

            // Really we don't need the y component of the offset for anything whatsoever
            playerNbt.putDouble("offset.x", plane.getOffset().x);
            playerNbt.putDouble("offset.z", plane.getOffset().z);

            playerNbt.putDouble("yaw", plane.getYaw());

            playersNbt.put(uuid.toString(), playerNbt);
        }));
        nbt.put("players", playersNbt);

        return nbt;
    }

    public static PlanePersistentState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        PlanePersistentState state = new PlanePersistentState();

        NbtCompound playersNbt = nbt.getCompound("players");
        playersNbt.getKeys().forEach(key -> {
            Plane plane = new Plane(
                    new Vec3d(playersNbt.getCompound(key).getDouble("offset.x"), 0, playersNbt.getCompound(key).getDouble("offset.z")),
                    playersNbt.getCompound(key).getDouble("yaw")
            );

            state.players.put(UUID.fromString(key), plane);
        });

        return state;
    }

    private static final Type<PlanePersistentState> type = new Type<>(
            PlanePersistentState::new,
            PlanePersistentState::createFromNbt,
            null
    );

    public static PlanePersistentState getServerState(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        //noinspection ConstantConditions
        if (overworld == null) {
            throw new IllegalStateException("Overworld not found!");
        }
        PersistentStateManager persistentStateManager = overworld.getPersistentStateManager();

        PlanePersistentState state = persistentStateManager.getOrCreate(type, Immersive2D.MOD_ID);

        state.markDirty();

        return state;
    }

    @Nullable
    public static Plane getPlayerPlane(PlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        PlanePersistentState serverState = getServerState(server);

        return serverState.players.get(player.getUuid());
    }

    public static void setPlayerPlane(PlayerEntity player, double x, double z, double yaw) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        PlanePersistentState serverState = getServerState(server);

        serverState.players.put(player.getUuid(), new Plane(new Vec3d(x, 0, z), yaw));
        serverState.markDirty();
    }

    public static void removePlayerPlane(PlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        PlanePersistentState serverState = getServerState(server);

        serverState.players.remove(player.getUuid());
        serverState.markDirty();
    }
}
