package com.example.examplemod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;
import java.util.List;

public class ZeitItem extends Item {
    private static boolean ZEIT_GEFROREN = false;

    public ZeitItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            ZEIT_GEFROREN = !ZEIT_GEFROREN;

            if (ZEIT_GEFROREN) {
                // FREEZE
                freezeAlleMobs(world, player);
                spawnFreezeEffekt(world, player);
                player.sendSystemMessage(Component.literal("⏸ ZEIT GEFROREN!").withStyle(ChatFormatting.DARK_RED));
            } else {
                // UNFREEZE
                unfreezeAlleMobs(world, player);
                spawnUnfreezeEffekt(world, player);
                player.sendSystemMessage(Component.literal("▶ ZEIT LAUFT!").withStyle(ChatFormatting.GREEN));
            }
        }

        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    private void freezeAlleMobs(Level world, Player player) {
        // NUR LivingEntitys (Mobs + Tiere) - KEINE UUIDs!
        var entities = world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50));
        for (LivingEntity entity : entities) {
            if (entity == player) continue; // Spieler ausnehmen

            entity.setDeltaMovement(Vec3.ZERO);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999999, 255));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 999999, 255));
        }
    }

    private void unfreezeAlleMobs(Level world, Player player) {
        var entities = world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50));
        for (LivingEntity entity : entities) {
            if (entity == player) continue;

            // Effects werden automatisch entfernt nach Toggle
            entity.setDeltaMovement(Vec3.ZERO); // Reset
        }
    }

    private void spawnFreezeEffekt(Level world, Player player) {
        world.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 0.5F);
        for (int i = 0; i < 50; i++) {
            double x = player.getX() + world.random.nextGaussian() * 5;
            double y = player.getY() + 2 + world.random.nextFloat() * 3;
            double z = player.getZ() + world.random.nextGaussian() * 5;
            world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0, 0);
        }
    }

    private void spawnUnfreezeEffekt(Level world, Player player) {
        world.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 0.5F, 2.0F);
        for (int i = 0; i < 30; i++) {
            double x = player.getX() + world.random.nextGaussian() * 3;
            double y = player.getY() + 2;
            double z = player.getZ() + world.random.nextGaussian() * 3;
            world.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.1, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4§lZEITFREEZER").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§6Rechtsklick: §c⏸ FREEZE / ▶ UNFREEZE").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§850 Block Radius - §cEPISCH!").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, world, tooltip, flag);
    }
}
