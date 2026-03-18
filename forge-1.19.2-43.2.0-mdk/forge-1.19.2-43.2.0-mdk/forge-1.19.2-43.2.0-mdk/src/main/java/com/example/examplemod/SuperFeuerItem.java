package com.example.examplemod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

public class SuperFeuerItem extends Item {
    public SuperFeuerItem() {
        super(new Item.Properties()
                .tab(CreativeModeTab.TAB_COMBAT)
                .stacksTo(1)
                .durability(100)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 2.0F, 0.5F);

            for (int i = 0; i < 3; i++) {
                SmallFireball fireball = new SmallFireball(world, player,
                        player.getLookAngle().x * 2,
                        player.getLookAngle().y * 2,
                        player.getLookAngle().z * 2);

                fireball.setPos(player.getX(), player.getEyeY() - 0.5, player.getZ());
                world.addFreshEntity(fireball);

                if (world.isClientSide) {
                    for (int j = 0; j < 5; j++) {
                        world.addParticle(ParticleTypes.FLAME,
                                player.getX(), player.getEyeY(), player.getZ(),
                                player.getRandom().nextGaussian() * 0.1,
                                player.getRandom().nextGaussian() * 0.1,
                                player.getRandom().nextGaussian() * 0.1);
                    }
                }
            }

            stack.hurtAndBreak(2, player, (p) -> p.broadcastBreakEvent(hand));
        }

        player.getCooldowns().addCooldown(this, 10);
        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level.isClientSide) {
            target.hurt(DamageSource.mobAttack(attacker), 15.0F);

            target.push(
                    attacker.getLookAngle().x * 3,
                    2.0,
                    attacker.getLookAngle().z * 3
            );

            target.setSecondsOnFire(5);

            attacker.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);

            if (attacker.level.isClientSide) {
                for (int i = 0; i < 10; i++) {
                    attacker.level.addParticle(ParticleTypes.FLAME,
                            target.getX(), target.getY() + 1, target.getZ(),
                            attacker.getRandom().nextGaussian() * 0.2,
                            0.2,
                            attacker.getRandom().nextGaussian() * 0.2);
                }
            }

            stack.hurtAndBreak(3, attacker, (p) -> p.broadcastBreakEvent(attacker.getUsedItemHand()));
        }

        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§c§lFEUERZAUBERSTAB"));
        tooltip.add(Component.literal("§6Rechtsklick: §7Schießt Feuerbälle"));
        tooltip.add(Component.literal("§6Linksklick: §6 Schaden + 5s Feuer"));
        tooltip.add(Component.literal("§c\"Die Hitze der Hölle in deiner Hand!\""));
        super.appendHoverText(stack, world, tooltip, flag);
    }
}
