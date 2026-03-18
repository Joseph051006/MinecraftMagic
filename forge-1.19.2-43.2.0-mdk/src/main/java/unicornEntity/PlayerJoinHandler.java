package unicornEntity;

import com.example.examplemod.ExampleMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "meinemod") // Use your actual MOD_ID string here
public class PlayerJoinHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Use the RegistryObject from ExampleMod
            UnicornEntity unicorn = ExampleMod.UNICORN.get().create(player.getLevel());

            if (unicorn != null) {
                // Spawn at player's location
                unicorn.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                player.getLevel().addFreshEntity(unicorn);

                // Set the owner so the FollowGoal works
                unicorn.setOwner(player);
            }
        }
    }
}