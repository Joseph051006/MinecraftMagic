package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Random;

public class Catalyst extends Item {
    private static final Random RANDOM = new Random();

    public Catalyst(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            int count = spawnBoomCreepers(level, player);
            player.sendSystemMessage(Component.literal("💥 " + count + " INSTANT EXPLOSIONS!").withStyle(ChatFormatting.DARK_RED));
        }

        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    private int spawnBoomCreepers(Level level, Player player) {
        int count = 12 + RANDOM.nextInt(8); // 12-20 BOOMS!
        Vec3 pos = player.position();

        for (int i = 0; i < count; i++) {
            double ox = (RANDOM.nextDouble() - 0.5) * 12;
            double oy = 0;
            double oz = (RANDOM.nextDouble() - 0.5) * 12;
            spawnBoomer(level, pos.x + ox, pos.y + oy, pos.z + oz);
        }
        return count;
    }

    private void spawnBoomer(Level level, double x, double y, double z) {
        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper != null) {
            creeper.moveTo(x, y, z, 0, 0);
            creeper.setHealth(1.0F);
            creeper.ignite();  // ✅ KEIN Parameter! Explodiert sofort!
            level.addFreshEntity(creeper);
        }
    }
}
