package github.fonvind.immersive2D.utils;

import github.fonvind.immersive2D.Immersive2D;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class PlanePersistentState extends PersistentState {
    private HashMap<UUID, Plane> players = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
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

    public static PlanePersistentState createFromNbt(NbtCompound nbt) {
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

    public static PlanePersistentState createNew() {
        PlanePersistentState state = new PlanePersistentState();
        state.players = new HashMap<>();
        return state;
    }

    public static PlanePersistentState getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        PlanePersistentState state = persistentStateManager.getOrCreate(PlanePersistentState::createFromNbt, PlanePersistentState::createNew, Immersive2D.MOD_ID);

        state.markDirty();

        return state;
    }

    @Nullable
    public static Plane getPlayerPlane(PlayerEntity player) {
        PlanePersistentState serverState = getServerState(player.getWorld().getServer());

        return serverState.players.get(player.getUuid());
    }

    public static void setPlayerPlane(PlayerEntity player, double x, double z, double yaw) {
        PlanePersistentState serverState = getServerState(player.getWorld().getServer());

        serverState.players.put(player.getUuid(), new Plane(new Vec3d(x, 0, z), yaw));
    }

    public static void removePlayerPlane(PlayerEntity player) {
        PlanePersistentState serverState = getServerState(player.getWorld().getServer());

        serverState.players.remove(player.getUuid());
    }
}
