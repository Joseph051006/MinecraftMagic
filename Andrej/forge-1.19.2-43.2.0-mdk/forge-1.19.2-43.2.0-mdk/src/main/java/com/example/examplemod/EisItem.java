package com.example.examplemod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EisItem extends Item {

    private static final UUID ARMOR_MODIFIER_ID        = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ATTACK_SPEED_MODIFIER_ID = UUID.fromString("ffffffff-1111-2222-3333-444444444444");
    private static final UUID FREEZE_MOVE_ID           = UUID.fromString("55555555-6666-7777-8888-999999999999");
    private static final UUID FREEZE_ATTACK_ID         = UUID.fromString("00000000-1234-5678-9abc-def012345678");

    public static final Map<UUID, Integer> blizzardTickMap = new HashMap<>();

    // ─── Inner Forge Event Handler ────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "meinemod", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class EventHandler {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (blizzardTickMap.isEmpty()) return;

            java.util.Iterator<Map.Entry<UUID, Integer>> iter = blizzardTickMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<UUID, Integer> entry = iter.next();
                if (entry.getValue() <= 0) { iter.remove(); continue; }

                for (net.minecraft.server.level.ServerLevel level : event.getServer().getAllLevels()) {
                    net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());
                    if (entity instanceof LivingEntity target && target.isAlive()) {
                        target.hurt(DamageSource.MAGIC, 7.5F);

                        RandomSource random = level.getRandom();
                        for (int i = 0; i < 6; i++) {
                            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                    target.getX() + random.nextGaussian() * 0.4,
                                    target.getY() + target.getBbHeight() * 0.3 + random.nextDouble() * 0.5,
                                    target.getZ() + random.nextGaussian() * 0.4,
                                    1, random.nextGaussian() * 0.02,
                                    0.05 + random.nextFloat() * 0.04,
                                    random.nextGaussian() * 0.02, 0);
                        }
                        for (int i = 0; i < 6; i++) {
                            level.sendParticles(ParticleTypes.ASH,
                                    target.getX() + random.nextGaussian() * 0.5,
                                    target.getY() + target.getBbHeight() + random.nextDouble() * 0.3,
                                    target.getZ() + random.nextGaussian() * 0.5,
                                    1, random.nextGaussian() * 0.01, -0.02,
                                    random.nextGaussian() * 0.01, 0);
                        }
                        for (int i = 0; i < 8; i++) {
                            double a = i * (Math.PI / 4);
                            level.sendParticles(ParticleTypes.SNOWFLAKE,
                                    target.getX() + Math.cos(a) * 0.6,
                                    target.getY() + 0.05,
                                    target.getZ() + Math.sin(a) * 0.6,
                                    1, 0, 0.02, 0, 0);
                        }
                    }
                }
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    // ─── Constructor ──────────────────────────────────────────────────────────

    public EisItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .durability(512));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;  // Trident animation
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    // ─── Charge Visuals ───────────────────────────────────────────────────────

    @Override
    public void onUseTick(Level world, LivingEntity livingEntity, ItemStack stack, int remainingTicks) {
        if (!(livingEntity instanceof Player player)) return;
        int usedTicks = getUseDuration(stack) - remainingTicks;

        if (world.isClientSide) {
            RandomSource random = world.getRandom();

            if (usedTicks < 15) {
                for (int i = 0; i < 2; i++) {
                    world.addParticle(ParticleTypes.SNOWFLAKE,
                            player.getX() + random.nextGaussian() * 0.2,
                            player.getEyeY() - 0.15,
                            player.getZ() + random.nextGaussian() * 0.2,
                            0, 0.03, 0);
                }
            } else if (usedTicks < 50) {
                for (int i = 0; i < 3; i++) {
                    world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX() + random.nextGaussian() * 0.3,
                            player.getEyeY() - 0.1,
                            player.getZ() + random.nextGaussian() * 0.3,
                            0, 0.04, 0);
                }
                for (int i = 0; i < 2; i++) {
                    world.addParticle(ParticleTypes.ASH,
                            player.getX() + random.nextGaussian() * 0.4,
                            player.getEyeY() + 0.1,
                            player.getZ() + random.nextGaussian() * 0.4,
                            random.nextGaussian() * 0.01, -0.01, random.nextGaussian() * 0.01);
                }
            } else {
                double angle = (world.getGameTime() % 20) / 20.0 * Math.PI * 2;
                for (int i = 0; i < 8; i++) {
                    double a = angle + i * (Math.PI / 4);
                    world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX() + Math.cos(a) * 1.4,
                            player.getY() + 1.1,
                            player.getZ() + Math.sin(a) * 1.4,
                            -Math.sin(a) * 0.08, 0.01, Math.cos(a) * 0.08);
                }
                for (int i = 0; i < 4; i++) {
                    double r = 0.8 + random.nextDouble() * 1.2;
                    double a = random.nextDouble() * Math.PI * 2;
                    world.addParticle(ParticleTypes.SCULK_SOUL,
                            player.getX() + Math.cos(a) * r,
                            player.getY() + 0.1,
                            player.getZ() + Math.sin(a) * r,
                            0, 0.06 + random.nextFloat() * 0.04, 0);
                }
                for (int i = 0; i < 5; i++) {
                    world.addParticle(ParticleTypes.ASH,
                            player.getX() + random.nextGaussian() * 1.5,
                            player.getY() + 2.5 + random.nextDouble() * 0.5,
                            player.getZ() + random.nextGaussian() * 1.5,
                            random.nextGaussian() * 0.01, -0.03, random.nextGaussian() * 0.01);
                }
                for (int i = 0; i < 3; i++) {
                    world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                            player.getX() + random.nextGaussian() * 1.0,
                            player.getY() + 0.05,
                            player.getZ() + random.nextGaussian() * 1.0,
                            random.nextGaussian() * 0.05, 0.04, random.nextGaussian() * 0.05);
                }
            }
        }
    }

    // ─── Release ──────────────────────────────────────────────────────────────

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) return;
        if (world.isClientSide) return;

        int usedTicks = getUseDuration(stack) - timeLeft;

        if (usedTicks < 15) {
            performIceBolt(world, player);
        } else if (usedTicks < 50) {
            performIceRupture(world, player);
        } else {
            performDeathFrost(world, player);
        }

        InteractionHand usedHand = player.getUsedItemHand();
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(usedHand));
        }
    }

    // ─── ABILITY 1: Ice Bolt (<0.75s) ─────────────────────────────────────────

    private void performIceBolt(Level world, Player player) {
        double range = 35.0;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        HitResult blockHit = world.clip(new ClipContext(
                eyePos, endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));
        Vec3 effectiveEnd = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation()
                : endPos;

        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);
        LivingEntity hitTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && !(e instanceof Player p && p.isCreative()) && e.isAlive())) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = entityBox.clip(eyePos, effectiveEnd);
            if (hit.isPresent()) {
                double dist = eyePos.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    hitTarget = entity;
                }
            }
        }

        if (hitTarget != null) {
            hitTarget.hurt(DamageSource.playerAttack(player), 12.0F);
            applyPassiveOnHitEffects(player, hitTarget);
        }

        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Vec3 trailEnd = hitTarget != null
                    ? hitTarget.position().add(0, hitTarget.getBbHeight() * 0.5, 0)
                    : effectiveEnd;
            double travelDist = eyePos.distanceTo(trailEnd);
            int steps = (int)(travelDist * 4);
            RandomSource random = world.getRandom();

            for (int i = 0; i < steps; i++) {
                double t = (double) i / steps;
                Vec3 p = eyePos.lerp(trailEnd, t);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        p.x, p.y, p.z, 1,
                        random.nextGaussian() * 0.02,
                        random.nextGaussian() * 0.02,
                        random.nextGaussian() * 0.02, 0);
                if (i % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                            p.x + random.nextGaussian() * 0.1,
                            p.y + random.nextGaussian() * 0.1,
                            p.z + random.nextGaussian() * 0.1,
                            1, 0, 0.01, 0, 0);
                }
            }
            if (hitTarget != null) {
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()),
                        hitTarget.getX(), hitTarget.getY() + hitTarget.getBbHeight() * 0.5, hitTarget.getZ(),
                        14, 0.3, 0.3, 0.3, 0.35);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        hitTarget.getX(), hitTarget.getY() + hitTarget.getBbHeight() * 0.5, hitTarget.getZ(),
                        8, 0.25, 0.25, 0.25, 0.1);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    // ─── ABILITY 2: Ice Rupture (0.75–2.5s) ──────────────────────────────────

    private void performIceRupture(Level world, Player player) {
        Vec3 look = player.getLookAngle();
        double rangeH = 12.0, rangeV = 3.5;

        AABB area = player.getBoundingBox()
                .inflate(rangeH, rangeV, rangeH)
                .move(look.x * rangeH * 0.5, 0, look.z * rangeH * 0.5);

        List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e != player && !(e instanceof Player p && p.isCreative()));

        for (LivingEntity target : targets) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            if (dx * look.x + dz * look.z <= 0) continue;

            target.hurt(DamageSource.playerAttack(player), 36.0F);
            applyPassiveOnHitEffects(player, target);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.5F, 0.6F);

        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            RandomSource random = world.getRandom();

            for (int i = 1; i <= 12; i++) {
                double d = i * 1.0;
                for (int j = 0; j < 10; j++) {
                    double spread = d * 0.35;
                    double ox = random.nextGaussian() * spread;
                    double oz = random.nextGaussian() * spread;
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX() + look.x * d + ox,
                            player.getEyeY() - 0.3 + random.nextGaussian() * 0.25,
                            player.getZ() + look.z * d + oz,
                            1, look.x * 0.12, 0.01, look.z * 0.12, 0);
                    if (j % 3 == 0) {
                        serverLevel.sendParticles(
                                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                                player.getX() + look.x * d + ox * 0.6,
                                player.getY() + 0.1,
                                player.getZ() + look.z * d + oz * 0.6,
                                1, look.x * 0.1, 0.06, look.z * 0.1, 0);
                    }
                    if (j % 2 == 0) {
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                                player.getX() + look.x * d + ox * 0.5,
                                player.getEyeY() - 0.2,
                                player.getZ() + look.z * d + oz * 0.5,
                                1, look.x * 0.06, 0.005, look.z * 0.06, 0);
                    }
                }
            }
            // Ash dust along cone
            for (int i = 1; i <= 12; i++) {
                double d = i * 1.0;
                serverLevel.sendParticles(ParticleTypes.ASH,
                        player.getX() + look.x * d + random.nextGaussian() * 0.4,
                        player.getY() + 0.2,
                        player.getZ() + look.z * d + random.nextGaussian() * 0.4,
                        1, look.x * 0.05, 0.02, look.z * 0.05, 0);
            }
            // Blue ice shockwave ring at cone tip
            double ringDist = 12.0;
            for (int i = 0; i < 16; i++) {
                double a = i * (Math.PI / 8);
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()),
                        player.getX() + look.x * ringDist + Math.cos(a) * 1.0,
                        player.getEyeY() - 0.4,
                        player.getZ() + look.z * ringDist + Math.sin(a) * 1.0,
                        1, random.nextGaussian() * 0.15, 0.08, random.nextGaussian() * 0.15, 0);
            }
        }
    }

    // ─── ABILITY 3: Death Frost (≥4s) ────────────────────────────────────────

    private void performDeathFrost(Level world, Player player) {
        double radius = 12.0;
        AABB frostBox = new AABB(
                player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius);

        List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class, frostBox,
                e -> e != player && !(e instanceof Player p && p.isCreative()));

        for (LivingEntity target : targets) {
            float damage = (float)(target.getMaxHealth() * 0.12) + 20.0F;
            target.hurt(DamageSource.playerAttack(player), damage);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 4));
            MobEffectInstance existingSlow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            int slowAmp = Math.min((existingSlow != null ? existingSlow.getAmplifier() : -1) + 2, 5);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, slowAmp));
            applyPassiveOnHitEffects(player, target);
            blizzardTickMap.put(target.getUUID(), 8);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 2.0F, 0.3F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1.8F, 0.5F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 1.5F, 1.6F);

        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            RandomSource random = world.getRandom();

            for (int i = 0; i < 60; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = 1.0 + random.nextDouble() * (radius - 1.0);
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                        player.getX() + Math.cos(angle) * r, player.getY() + 0.1,
                        player.getZ() + Math.sin(angle) * r,
                        1, Math.cos(angle) * 0.02, 0.05 + random.nextFloat() * 0.04, Math.sin(angle) * 0.02, 0);
            }
            for (int i = 0; i < 80; i++) {
                double h = random.nextDouble() * 5.0;
                double r = random.nextDouble() * 3.0;
                double angle = random.nextDouble() * Math.PI * 2;
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX() + Math.cos(angle) * r, player.getY() + h,
                        player.getZ() + Math.sin(angle) * r,
                        1, Math.cos(angle) * -0.03, 0.03, Math.sin(angle) * -0.03, 0);
            }
            for (int i = 0; i < 60; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = random.nextDouble() * radius;
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                        player.getX() + Math.cos(angle) * r, player.getY() + 0.1,
                        player.getZ() + Math.sin(angle) * r,
                        1, Math.cos(angle) * 0.08, 0.15 + random.nextFloat() * 0.15, Math.sin(angle) * 0.08, 0);
            }
            for (int i = 0; i < 40; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = radius * 0.4 + random.nextDouble() * radius * 0.5;
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()),
                        player.getX() + Math.cos(angle) * r,
                        player.getY() + 0.2 + random.nextFloat() * 1.5,
                        player.getZ() + Math.sin(angle) * r,
                        1, random.nextGaussian() * 0.1, 0.1 + random.nextFloat() * 0.1, random.nextGaussian() * 0.1, 0);
            }
            for (int i = 0; i < 80; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = random.nextDouble() * radius;
                serverLevel.sendParticles(ParticleTypes.ASH,
                        player.getX() + Math.cos(angle) * r,
                        player.getY() + 3.0 + random.nextDouble() * 2.0,
                        player.getZ() + Math.sin(angle) * r,
                        1, random.nextGaussian() * 0.01, -0.04, random.nextGaussian() * 0.01, 0);
            }
            for (int i = 0; i < 50; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = radius * 0.7 + random.nextDouble() * radius * 0.3;
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        player.getX() + Math.cos(angle) * r,
                        player.getY() + 0.1 + random.nextFloat() * 2.0,
                        player.getZ() + Math.sin(angle) * r,
                        1, Math.cos(angle) * -0.05, 0.04 + random.nextFloat() * 0.03, Math.sin(angle) * -0.05, 0);
            }
        }
    }

    // ─── Passive On-Hit ───────────────────────────────────────────────────────

    public static void applyPassiveOnHitEffects(Player attacker, LivingEntity target) {
        if (target.level.isClientSide) return;

        MobEffectInstance existing = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        int newAmp = Math.min((existing != null ? existing.getAmplifier() : -1) + 1, 3);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, newAmp));

        AttributeInstance atkSpeedAttr = target.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeedAttr != null) {
            atkSpeedAttr.removeModifier(ATTACK_SPEED_MODIFIER_ID);
            atkSpeedAttr.addTransientModifier(new AttributeModifier(
                    ATTACK_SPEED_MODIFIER_ID, "eisitem_attack_speed_debuff",
                    -atkSpeedAttr.getBaseValue() * 0.3, AttributeModifier.Operation.ADDITION));
        }

        AttributeInstance armorAttr = target.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_MODIFIER_ID);
            armorAttr.addTransientModifier(new AttributeModifier(
                    ARMOR_MODIFIER_ID, "eisitem_armor_reduction",
                    -armorAttr.getBaseValue() * 0.4, AttributeModifier.Operation.ADDITION));
        }

        Level level = target.level;
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SNOW_HIT, SoundSource.PLAYERS, 0.8F, 1.2F);

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    5, 0.25, 0.25, 0.25, 0.04);
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    5, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b§lIce Wand"));
        tooltip.add(Component.literal("§6<0.75s:    §bIce bolt 12DMG"));
        tooltip.add(Component.literal("§60.75–2.5s: §bIce Rupture 36 DMG"));
        tooltip.add(Component.literal("§6≥4s:       §3§lPermafrost, crush everything in your way (12% max HP + 30DMG + wither effect"));
        tooltip.add(Component.literal("§7Passive: slowness & armor Reduction "));
        super.appendHoverText(stack, world, tooltip, flag);
    }
}