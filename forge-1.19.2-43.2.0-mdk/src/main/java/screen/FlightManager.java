package screen;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class FlightManager {

    // Stores remaining ticks per player UUID
    private static final Map<UUID, Integer> flightTimers = new HashMap<>();

    public static void giveFlight(Player player, int durationSeconds) {
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        flightTimers.put(player.getUUID(), durationSeconds * 20); // convert to ticks
    }

    public static void revokeFlight(Player player) {
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        flightTimers.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only run server-side, at end of tick
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level.isClientSide()) return;

        Player player = event.player;
        UUID uuid = player.getUUID();

        if (!flightTimers.containsKey(uuid)) return;

        int ticksLeft = flightTimers.get(uuid) - 1;

        if (ticksLeft <= 0) {
            revokeFlight(player);
        } else {
            flightTimers.put(uuid, ticksLeft);
        }
    }
}