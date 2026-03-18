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
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class SuperFeuerItem extends Item {

    private int chargeTime = 0;
    private boolean isCharging = false;

    private static final int STAGE_1 = 15;
    private static final int STAGE_2 = 40;
    private static final int STAGE_3 = 80;

    private final Random random = new Random();

    public SuperFeuerItem() {
        super(new Item.Properties()
                .tab(CreativeModeTab.TAB_COMBAT)
                .stacksTo(1)
                .durability(500)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!(user instanceof Player player)) return;
        if (world.isClientSide) return;

        isCharging = true;
        chargeTime++;

        if (chargeTime % 5 == 0) {
            float pitch = 0.6F + (chargeTime * 0.01F);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.5F, pitch);
        }

        if (world instanceof ServerLevel serverLevel) {
            double radius = 0.8 + (chargeTime / 40.0);
            int points = 6 + chargeTime / 10;
            for (int i = 0; i < points; i++) {
                double angle = (chargeTime * 0.25) + (i * (Math.PI * 2 / points));
                double x = player.getX() + radius * Math.cos(angle);
                double y = player.getEyeY() - 0.4 + Math.sin(angle * 2) * 0.2;
                double z = player.getZ() + radius * Math.sin(angle);

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        x, y, z, 1,
                        0, 0, 0, 0);

                if (chargeTime > STAGE_2 && i % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            x, y + 0.1, z, 1,
                            0, 0.01, 0, 0);
                }
                if (chargeTime > STAGE_3 / 2 && i % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.LAVA,
                            x, y - 0.1, z, 1,
                            0, 0.01, 0, 0);
                }
            }
        }

        if (chargeTime == STAGE_1) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.0F, 1.4F);
        } else if (chargeTime == STAGE_2) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.2F, 1.0F);
        } else if (chargeTime == STAGE_3) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.5F, 0.6F);
        }

        if (chargeTime >= STAGE_3 + 20) {
            player.releaseUsingItem();
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);
        isCharging = true;
        chargeTime = 0;

        if (!world.isClientSide) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.7F, 1.0F);
            if (world instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 10; i++) {
                    serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                            player.getX(), player.getEyeY(), player.getZ(),
                            1,
                            (player.getRandom().nextDouble() - 0.5) * 0.4,
                            player.getRandom().nextDouble() * 0.3,
                            (player.getRandom().nextDouble() - 0.5) * 0.4, 0);
                }
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int timeCharged) {
        if (!(user instanceof Player player)) return;
        if (world.isClientSide) {
            isCharging = false;
            chargeTime = 0;
            return;
        }

        int currentCharge = chargeTime;
        isCharging = false;
        chargeTime = 0;

        if (currentCharge < STAGE_1) {
            unleashTapAttack(world, player, stack);
        } else if (currentCharge < STAGE_2) {
            unleashStage1Attack(world, player, stack);
        } else if (currentCharge < STAGE_3) {
            unleashStage2Attack(world, player, stack);
        } else {
            unleashUltimateAttack(world, player, stack);
        }
    }

    /* ========= ATTACKS ========= */

    // kurzer Klick – ein Schuss nach vorne + Wirbel
    private void unleashTapAttack(Level world, Player player, ItemStack stack) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.2F, 1.3F);

        if (world instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();

            // Hauptfeuerball nach vorne
            SmallFireball forward = new SmallFireball(world, player, look.x * 1.5, look.y * 1.5, look.z * 1.5);
            forward.setPos(player.getX() + look.x, player.getEyeY() - 0.2 + look.y, player.getZ() + look.z);
            world.addFreshEntity(forward);

            // kleiner Wirbel um den Spieler
            for (int i = 0; i < 5; i++) {
                double angle = (i * Math.PI * 2 / 5) + (player.getYRot() * Math.PI / 180);
                double radius = 1.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                SmallFireball fireball = new SmallFireball(world, player, x * 0.5, 0.1, z * 0.5);
                fireball.setPos(player.getX() + x, player.getEyeY() - 0.5, player.getZ() + z);
                world.addFreshEntity(fireball);

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        player.getX() + x, player.getEyeY() - 0.2, player.getZ() + z,
                        2, 0, 0, 0, 0);
            }
        }

        for (int i = 0; i < 15; i++) {
            world.addParticle(ParticleTypes.FLAME,
                    player.getX(), player.getEyeY(), player.getZ(),
                    (player.getRandom().nextDouble() - 0.5) * 0.6,
                    player.getRandom().nextDouble() * 0.4,
                    (player.getRandom().nextDouble() - 0.5) * 0.6);
        }

        stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
        player.getCooldowns().addCooldown(this, 10);
    }

    // Stufe 1 – Laser dahin, wo du hinschaust (Raycast)
    private void unleashStage1Attack(Level world, Player player, ItemStack stack) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.8F, 1.0F);

        BlockHitResult hitResult = raycast(player, world, 40.0D);
        Vec3 start = player.getEyePosition();
        Vec3 end = hitResult.getLocation();

        if (world instanceof ServerLevel serverLevel) {
            int steps = 40;
            Vec3 dir = end.subtract(start).scale(1.0 / steps);
            Vec3 current = start;

            for (int i = 0; i < steps; i++) {
                current = current.add(dir);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        current.x, current.y, current.z,
                        3, 0, 0, 0, 0.01);
                if (i % 5 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            current.x, current.y, current.z,
                            1, 0, 0.01, 0, 0.01);
                }
            }
        }

        double radius = 2.5;
        AABB box = new AABB(
                end.x - radius, end.y - 1, end.z - radius,
                end.x + radius, end.y + 1, end.z + radius
        );
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : entities) {
            if (entity == player) continue;
            entity.hurt(DamageSource.indirectMagic(player, player), 10.0F);
            entity.setSecondsOnFire(6);
        }

        burnGroundAt(world, new BlockPos(end.x, end.y - 1, end.z), 1);

        stack.hurtAndBreak(2, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
        player.getCooldowns().addCooldown(this, 25);
    }

    // Stufe 2 – Feuerkegel + TNT in Blickrichtung
    private void unleashStage2Attack(Level world, Player player, ItemStack stack) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 2.5F, 1.0F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.9F);

        Vec3 lookVec = player.getLookAngle();

        if (world instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double dist = 1.0 + i * 0.5;
                double spread = 0.6 + i * 0.03;
                double ox = (random.nextDouble() - 0.5) * spread;
                double oy = (random.nextDouble() - 0.5) * spread * 0.5;
                double oz = (random.nextDouble() - 0.5) * spread;

                double x = player.getX() + lookVec.x * dist + ox;
                double y = player.getEyeY() - 0.2 + lookVec.y * dist + oy;
                double z = player.getZ() + lookVec.z * dist + oz;

                serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z,
                        6, 0, 0, 0, 0.02);

                if (i % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            x, y + 0.2, z,
                            2, 0, 0.02, 0, 0.01);
                }
            }
        }

        double radius = 6.0;
        AABB box = new AABB(
                player.getX() - radius, player.getY() - 2, player.getZ() - radius,
                player.getX() + radius, player.getY() + 2, player.getZ() + radius
        );
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box);

        for (LivingEntity entity : entities) {
            if (entity == player) continue;
            Vec3 toEntity = entity.position().subtract(player.position());
            if (toEntity.dot(lookVec) <= 0) continue;

            float damage = 10.0F + (player.isOnFire() ? 5.0F : 0.0F);
            entity.hurt(DamageSource.mobAttack(player), damage);
            entity.setSecondsOnFire(8);

            Vec3 push = toEntity.normalize().scale(2.5);
            entity.push(push.x, 0.8, push.z);
        }

        // TNT-Salve in Blickrichtung
        if (!world.isClientSide) {
            for (int i = 0; i < 3; i++) {
                double off = (i - 1) * 0.8;
                Vec3 side = new Vec3(-lookVec.z, 0, lookVec.x).normalize().scale(off);

                PrimedTnt tnt = new PrimedTnt(world,
                        player.getX() + lookVec.x + side.x,
                        player.getY() + 1.0,
                        player.getZ() + lookVec.z + side.z,
                        player);

                tnt.setDeltaMovement(lookVec.scale(0.7).add(0, 0.2, 0));
                tnt.setFuse(40);
                world.addFreshEntity(tnt);
            }
        }

        burnGroundAroundPlayer(world, player, 3, 1);

        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 8, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 5, 1));

        stack.hurtAndBreak(6, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
        player.getCooldowns().addCooldown(this, 60);
    }

    // Stufe 3 – Ultimate, großer AoE um dich herum
    private void unleashUltimateAttack(Level world, Player player, ItemStack stack) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0F, 0.7F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 3.0F, 0.8F);

        if (world instanceof ServerLevel serverLevel) {
            for (int ring = 0; ring < 4; ring++) {
                double ringRadius = 2.0 + ring * 1.8;
                int points = 28 + ring * 10;
                for (int i = 0; i < points; i++) {
                    double angle = (Math.PI * 2 / points) * i;
                    double xOff = Math.cos(angle) * ringRadius;
                    double zOff = Math.sin(angle) * ringRadius;

                    double x = player.getX() + xOff;
                    double y = player.getY() + 0.5 + ring * 0.3;
                    double z = player.getZ() + zOff;

                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            x, y, z,
                            4, 0, 0, 0, 0.03);

                    if (i % 3 == 0) {
                        serverLevel.sendParticles(ParticleTypes.LAVA,
                                x, y + 0.1, z,
                                1, 0, 0, 0, 0.01);
                    }
                }
            }

            for (int i = 0; i < 200; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = random.nextDouble() * 5;
                double height = 2 + random.nextDouble() * 8;

                double x = player.getX() + Math.cos(angle) * r;
                double z = player.getZ() + Math.sin(angle) * r;
                double y = player.getY() + height;

                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        x, y, z,
                        3, 0.1, 0.1, 0.1, 0.03);

                if (i % 5 == 0) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            x, y - 1, z,
                            2, 0, 0, 0, 0.02);
                }
            }
        }

        double radius = 10.0;
        AABB box = new AABB(
                player.getX() - radius, player.getY() - 4, player.getZ() - radius,
                player.getX() + radius, player.getY() + 4, player.getZ() + radius
        );
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box);

        int hitCount = 0;
        for (LivingEntity entity : entities) {
            if (entity == player) continue;

            entity.hurt(DamageSource.mobAttack(player), 25.0F);
            entity.setSecondsOnFire(15);

            Vec3 push = entity.position().subtract(player.position()).normalize().scale(4.0);
            entity.push(push.x, 1.5, push.z);
            hitCount++;
        }

        burnGroundAroundPlayer(world, player, 5, 2);

        if (hitCount > 0) {
            float healAmount = 6.0F + hitCount * 3.0F;
            player.heal(healAmount);

            if (world instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 25; i++) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            player.getX() + (random.nextDouble() - 0.5),
                            player.getY() + 1 + random.nextDouble(),
                            player.getZ() + (random.nextDouble() - 0.5),
                            1, 0, 0.1, 0, 0);
                }
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 20, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 10, 2));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 1));

        stack.hurtAndBreak(15, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
        player.getCooldowns().addCooldown(this, 20 * 15);
    }

    /* ========= RAYCAST ========= */

    private BlockHitResult raycast(Player player, Level level, double distance) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.x * distance, lookVec.y * distance, lookVec.z * distance);
        return level.clip(new ClipContext(
                eyePos,
                reachVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
    }

    /* ========= FLAMMEN AUF BODEN ========= */

    private void burnGroundAroundPlayer(Level world, Player player, int radius, int yOffset) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos basePos = new BlockPos(
                        player.getX() + x,
                        player.getY() - yOffset,
                        player.getZ() + z
                );
                BlockPos firePos = basePos.above();

                if (world.isEmptyBlock(firePos) && world.getBlockState(basePos).isSolidRender(world, basePos)) {
                    world.setBlockAndUpdate(firePos, BaseFireBlock.getState(world, firePos));
                }
            }
        }
    }

    private void burnGroundAt(Level world, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos basePos = center.offset(x, 0, z);
                BlockPos firePos = basePos.above();

                if (world.isEmptyBlock(firePos) && world.getBlockState(basePos).isSolidRender(world, basePos)) {
                    world.setBlockAndUpdate(firePos, BaseFireBlock.getState(world, firePos));
                }
            }
        }
    }

    /* ========= MELEE ========= */

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level.isClientSide) return true;

        attacker.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.6F, 0.9F);
        attacker.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.2F);

        float missing = target.getMaxHealth() - target.getHealth();
        float damageBonus = 1.0F + (missing / target.getMaxHealth());
        float damage = 16.0F * damageBonus;

        target.hurt(DamageSource.mobAttack(attacker), damage);
        target.setSecondsOnFire(10);

        Vec3 pushVec = target.position().subtract(attacker.position()).normalize();
        target.push(pushVec.x * 4.5, 1.4, pushVec.z * 4.5);

        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 10, 0));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 6, 0));

        if (attacker.level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 35; i++) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1,
                        (attacker.getRandom().nextDouble() - 0.5) * 0.5,
                        attacker.getRandom().nextDouble() * 0.5,
                        (attacker.getRandom().nextDouble() - 0.5) * 0.5, 0);

                if (i % 4 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            target.getX(), target.getY() + 1, target.getZ(), 1,
                            (attacker.getRandom().nextDouble() - 0.5) * 0.25,
                            0.1,
                            (attacker.getRandom().nextDouble() - 0.5) * 0.25, 0);
                }
            }
        }

        if (attacker instanceof Player player) {
            float heal = 4.0F + (damage / 10.0F);
            player.heal(heal);

            if (attacker.level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 12; i++) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            player.getX() + (attacker.getRandom().nextDouble() - 0.5),
                            player.getY() + 1 + attacker.getRandom().nextDouble(),
                            player.getZ() + (attacker.getRandom().nextDouble() - 0.5),
                            1, 0, 0.1, 0, 0);
                }
            }
        }

        stack.hurtAndBreak(2, attacker, (p) -> p.broadcastBreakEvent(attacker.getUsedItemHand()));
        return true;
    }

    /* ========= ITEM BASICS ========= */

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4§lINFERNO APOKALYPSE STAB"));
        tooltip.add(Component.literal("§6» Kurzer Rechtsklick: §7Vorwärtsschuss + Feuerwirbel"));
        tooltip.add(Component.literal("§6» Halten (Stufe I): §eLaserstrahl dahin, wo du hinschaust"));
        tooltip.add(Component.literal("§6» Halten (Stufe II): §cFeuerwelle + TNT-Salve nach vorne"));
        tooltip.add(Component.literal("§6» Halten (Stufe III): §4§lINFERNO-ULTIMATE (krasser AoE)"));
        tooltip.add(Component.literal("§6» Linksklick: §cSkalierender Schaden + Lifesteal"));
        tooltip.add(Component.literal("§6» Passive: §dStarker Lebensraub bei Treffern"));
        tooltip.add(Component.literal("§4\"Ich zünde die Welt, wenn ich nur will.\""));
        super.appendHoverText(stack, world, tooltip, flag);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF5500;
    }
}
