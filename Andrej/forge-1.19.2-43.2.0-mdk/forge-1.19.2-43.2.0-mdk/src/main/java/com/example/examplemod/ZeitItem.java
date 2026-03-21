package com.example.examplemod;

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
    private static final String ZEIT_TAG = "ZeitGefroren";
    private static final int COOLDOWN_TICKS = 20; // 1 second cooldown

    public ZeitItem(Item.Properties properties) {
        super(properties);
    }

    public ZeitItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Check cooldown
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!world.isClientSide) {
            boolean isFrozen = getZeitGefroren(stack);
            boolean newState = !isFrozen;
            setZeitGefroren(stack, newState);

            if (newState) {
                // FREEZE
                freezeAlleMobs(world, player);
                spawnFreezeEffekt(world, player);
                player.displayClientMessage(Component.literal("⏸ ZEIT GEFROREN!").withStyle(ChatFormatting.DARK_RED), false);
            } else {
                // UNFREEZE
                unfreezeAlleMobs(world, player);
                spawnUnfreezeEffekt(world, player);
                player.displayClientMessage(Component.literal("▶ ZEIT LAUFT!").withStyle(ChatFormatting.GREEN), false);
            }
        }

        player.swing(hand);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.success(stack);
    }

 private void freezeAlleMobs(Level world, Player player) {
    for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50))) {
        if (entity != player) {
            entity.setDeltaMovement(Vec3.ZERO);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999999, 255, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 999999, 255, false, false));
        }
    }
}

private void unfreezeAlleMobs(Level world, Player player) {
    for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50))) {
        if (entity != player) {
            entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            entity.removeEffect(MobEffects.DIG_SLOWDOWN);
            entity.setDeltaMovement(Vec3.ZERO);
        }
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

    private boolean getZeitGefroren(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(ZEIT_TAG);
    }

    private void setZeitGefroren(ItemStack stack, boolean value) {
        stack.getOrCreateTag().putBoolean(ZEIT_TAG, value);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        boolean isFrozen = getZeitGefroren(stack);
        tooltip.add(Component.literal("§4§lZEITFREEZER").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§6Rechtsklick: §c⏸ FREEZE / ▶ UNFREEZE").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§850 Block Radius - §cEPISCH!").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§7Status: " + (isFrozen ? "§c⏸ GEFROREN" : "§a▶ AKTIV")).withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, world, tooltip, flag);
    }
}