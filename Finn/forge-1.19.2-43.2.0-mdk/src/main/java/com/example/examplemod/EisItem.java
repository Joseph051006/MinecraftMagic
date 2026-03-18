package com.example.examplemod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public class EisItem extends Item {
    public EisItem() {
        super(new Item.Properties()
                .stacksTo(16)
                .rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            // Bereich um Spieler einfrieren
            AABB area = player.getBoundingBox().inflate(8, 4, 8);
            List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, area, e -> e != player);

            for (LivingEntity entity : entities) {
                entity.hurt(DamageSource.playerAttack(player), 6.0F);
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
            }

            // Eis-Partikel überall
            for (int i = 0; i < 50; i++) {
                world.addParticle(ParticleTypes.SNOWFLAKE,
                        player.getX() + (world.random.nextGaussian() * 8),
                        player.getY() + world.random.nextFloat() * 5,
                        player.getZ() + (world.random.nextGaussian() * 8),
                        0, 0, 0);
            }

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.5F);

            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }

        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b§lEISZAUBER"));
        tooltip.add(Component.literal("§6Rechtsklick: §bFrierte 8-Block Bereich ein"));
        tooltip.add(Component.literal("§b'Winter kommt!'"));
        super.appendHoverText(stack, world, tooltip, flag);
    }
}
