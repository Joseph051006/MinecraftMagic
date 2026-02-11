package com.example.examplemod;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ZeitItem extends Item {

    private static final String NBT_FROZEN = "Frozen";
    private static final String NBT_RADIUS = "Radius";
    private static final String NBT_ARROW_FROZEN = "FrozenByTime";
    private static final String NBT_FROZEN_BY_TIME = "FrozenByTime";
    private static final String NBT_ANIMATION_TICK = "AnimationTick";
    private static final String NBT_ANIMATION_FRAME = "AnimationFrame";
    private static final String NBT_CUSTOM_MODEL_DATA = "CustomModelData";
    private static final String NBT_PROJECTILE_VELOCITY = "StoredVelocity";

    private static final int DEFAULT_RADIUS = 50;
    private static final int MAX_RADIUS = 128;
    private static final int ANIMATION_FRAMES = 4;

    // Speichert die ursprünglichen Geschwindigkeiten
    private static final Map<UUID, Vec3> projectileVelocities = new HashMap<>();
    private static final Map<UUID, Vec3> entityVelocities = new HashMap<>();

    public ZeitItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .durability(256)
                .setNoRepair()
        );
    }

    // ======================================================
    // ANIMATION & VISUAL EFFECTS
    // ======================================================

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!world.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();

            // Schnellere Animation (alle 10 Ticks = 0.5 Sekunden)
            int tick = tag.getInt(NBT_ANIMATION_TICK);
            tick = (tick + 1) % 40;

            tag.putInt(NBT_ANIMATION_TICK, tick);

            // Smooth Animation mit 4 Frames
            int frame = (tick / 10) % ANIMATION_FRAMES;
            tag.putInt(NBT_ANIMATION_FRAME, frame);
            tag.putInt(NBT_CUSTOM_MODEL_DATA, frame);

            // Visual Effect wenn Zeit gefroren ist
            if (tag.getBoolean(NBT_FROZEN) && selected && world instanceof ServerLevel serverLevel) {
                spawnTimeDistortionParticles(serverLevel, entity);
            }
        }
    }

    private void spawnTimeDistortionParticles(ServerLevel world, Entity entity) {
        for (int i = 0; i < 3; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 0.5 + world.random.nextDouble() * 0.5;
            double x = entity.getX() + Math.cos(angle) * radius;
            double y = entity.getY() + entity.getBbHeight() * 0.5 + world.random.nextDouble() * 0.3;
            double z = entity.getZ() + Math.sin(angle) * radius;

            world.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0.1);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(NBT_FROZEN);
    }

    // ======================================================
    // HAUPTFUNKTION - ZEIT KONTROLLE
    // ======================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            boolean frozen = !tag.getBoolean(NBT_FROZEN);
            tag.putBoolean(NBT_FROZEN, frozen);

            int radius = getRadius(stack);

            if (frozen) {
                // SPEICHERE alle Geschwindigkeiten VOR dem Freeze
                saveAllVelocities(world, player, radius);

                // Dann freeze
                freezeAll(world, player, radius);
                spawnFreezeEffect(world, player);
                player.sendSystemMessage(Component.literal("⏸ ZEIT GEFROREN")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

                tag.putInt(NBT_ANIMATION_TICK, 0);
                tag.putInt(NBT_CUSTOM_MODEL_DATA, 0);
            } else {
                // Unfreeze mit WIEDERHERGESTELLTEN Geschwindigkeiten
                unfreezeAll(world, player, radius);
                spawnUnfreezeEffect(world, player);
                player.sendSystemMessage(Component.literal("▶ ZEIT LÄUFT WIEDER")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                tag.putInt(NBT_ANIMATION_TICK, 10);
                tag.putInt(NBT_CUSTOM_MODEL_DATA, 1);
            }
        }

        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
    }

    // ======================================================
    // GESCHWINDIGKEITEN SPEICHERN & WIEDERHERSTELLEN
    // ======================================================

    private void saveAllVelocities(Level world, Player player, int radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        List<Entity> allEntities = world.getEntitiesOfClass(Entity.class, box);

        // Speichere Entity-Geschwindigkeiten
        List<LivingEntity> livingEntities = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ArmorStand));

        for (LivingEntity entity : livingEntities) {
            if (entity instanceof Player || entity instanceof ArmorStand) continue;
            entityVelocities.put(entity.getUUID(), entity.getDeltaMovement());
        }

        // Speichere ALLE Projektil-Geschwindigkeiten
        for (Entity entity : allEntities) {
            if (isProjectile(entity)) {
                projectileVelocities.put(entity.getUUID(), entity.getDeltaMovement());

                // Speichere auch in NBT für Persistenz
                CompoundTag tag = entity.getPersistentData();
                CompoundTag velocityTag = new CompoundTag();
                Vec3 velocity = entity.getDeltaMovement();
                velocityTag.putDouble("X", velocity.x);
                velocityTag.putDouble("Y", velocity.y);
                velocityTag.putDouble("Z", velocity.z);
                tag.put(NBT_PROJECTILE_VELOCITY, velocityTag);
            }
        }

        // Speichere fallende Entities (Sand, Kies, etc.)
        for (Entity entity : allEntities) {
            if (isFallingEntity(entity)) {
                Vec3 velocity = entity.getDeltaMovement();
                entityVelocities.put(entity.getUUID(), velocity);

                // Speichere in NBT für Persistenz
                CompoundTag tag = entity.getPersistentData();
                CompoundTag velocityTag = new CompoundTag();
                velocityTag.putDouble("X", velocity.x);
                velocityTag.putDouble("Y", velocity.y);
                velocityTag.putDouble("Z", velocity.z);
                tag.put(NBT_PROJECTILE_VELOCITY, velocityTag);
            }
        }
    }

    private boolean isProjectile(Entity entity) {
        return entity instanceof AbstractArrow ||
                entity instanceof Snowball ||
                entity instanceof ThrownExperienceBottle ||
                entity instanceof FireworkRocketEntity ||
                entity instanceof LlamaSpit ||
                entity instanceof ShulkerBullet ||
                entity instanceof EvokerFangs ||
                entity instanceof EyeOfEnder ||
                entity instanceof FishingHook ||
                entity instanceof Fireball ||
                entity instanceof WitherSkull ||
                entity instanceof DragonFireball ||
                entity instanceof LargeFireball ||
                entity instanceof SmallFireball ||
                entity instanceof ThrownTrident ||
                entity instanceof ThrowableItemProjectile;
    }

    private boolean isFallingEntity(Entity entity) {
        return entity instanceof FallingBlockEntity ||
                entity instanceof ItemEntity;
    }

    // ======================================================
    // FREEZE MIT GESCHWINDIGKEITS-SPEICHERUNG
    // ======================================================

    private void freezeAll(Level world, Player player, int radius) {
        AABB box = player.getBoundingBox().inflate(radius);

        // Lebewesen einfrieren
        List<LivingEntity> mobs = world.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e != player && e.isAlive() && !(e instanceof ArmorStand)
        );

        for (LivingEntity e : mobs) {
            if (e instanceof Player || e instanceof ArmorStand) continue;

            CompoundTag persistentData = e.getPersistentData();
            persistentData.putBoolean(NBT_FROZEN_BY_TIME, true);

            if (e instanceof Mob mob) {
                mob.setNoAi(true);
                mob.getNavigation().stop();
                mob.setTarget(null);
                mob.setAggressive(false);
            }

            // Bewegung stoppen
            e.setDeltaMovement(Vec3.ZERO);
            e.hasImpulse = false;
            e.yBodyRot = e.yBodyRotO;
            e.yHeadRot = e.yHeadRotO;

            if (!(e instanceof Player)) {
                e.setNoGravity(true);
            }

            // Time-Stop Effekt
            e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            e.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    1000000,
                    255,
                    false,
                    false,
                    false));
        }

        // ALLE Projektile einfrieren
        List<Entity> allEntities = world.getEntitiesOfClass(Entity.class, box);

        for (Entity entity : allEntities) {
            if (isProjectile(entity)) {
                CompoundTag tag = entity.getPersistentData();
                tag.putBoolean(NBT_ARROW_FROZEN, true);

                // Bewegung stoppen
                entity.setDeltaMovement(Vec3.ZERO);
                entity.hasImpulse = false;

                if (entity instanceof AbstractArrow arrow) {
                    arrow.setNoGravity(true);
                    arrow.setNoPhysics(true);
                }

                // Rotation einfrieren
                entity.setYRot(entity.yRotO);
                entity.setXRot(entity.xRotO);
            }
        }

        // Fallende Blöcke einfrieren (Sand, Kies, etc.)
        List<FallingBlockEntity> fallingBlocks = world.getEntitiesOfClass(
                FallingBlockEntity.class,
                box
        );

        for (FallingBlockEntity fallingBlock : fallingBlocks) {
            CompoundTag tag = fallingBlock.getPersistentData();
            tag.putBoolean(NBT_FROZEN_BY_TIME, true);

            // Bewegung stoppen
            fallingBlock.setDeltaMovement(Vec3.ZERO);
            fallingBlock.hasImpulse = false;

            // FallingBlock Timer einfrieren
            fallingBlock.time = fallingBlock.time - 1;

            // Schaden auf 0 setzen
            fallingBlock.setHurtsEntities(false);

            // Custom NBT für Wiederherstellung
            CompoundTag motionTag = new CompoundTag();
            motionTag.putDouble("motionX", 0);
            motionTag.putDouble("motionY", 0);
            motionTag.putDouble("motionZ", 0);
            tag.put("FrozenMotion", motionTag);
        }

        // Item Entities einfrieren (herumliegende Items)
        List<ItemEntity> itemEntities = world.getEntitiesOfClass(
                ItemEntity.class,
                box
        );

        for (ItemEntity itemEntity : itemEntities) {
            CompoundTag tag = itemEntity.getPersistentData();
            tag.putBoolean(NBT_FROZEN_BY_TIME, true);

            // Bewegung stoppen
            itemEntity.setDeltaMovement(Vec3.ZERO);
            itemEntity.hasImpulse = false;
            itemEntity.setNoGravity(true);

            // Pickup-Delay erhöhen (verhindert Aufsammeln während Freeze)
            if (itemEntity.pickupDelay < 32767) {
                tag.putInt("OriginalPickupDelay", itemEntity.pickupDelay);
                itemEntity.setPickUpDelay(32767); // Maximaler Delay
            }
        }
    }

    // ======================================================
    // UNFREEZE MIT GESCHWINDIGKEITS-WIEDERHERSTELLUNG
    // ======================================================

    private void unfreezeAll(Level world, Player player, int radius) {
        AABB box = player.getBoundingBox().inflate(radius);

        // Lebewesen entfrieren
        List<LivingEntity> mobs = world.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e != player && e.isAlive() && !(e instanceof ArmorStand)
        );

        for (LivingEntity e : mobs) {
            if (e instanceof Player || e instanceof ArmorStand) continue;

            CompoundTag persistentData = e.getPersistentData();
            if (!persistentData.getBoolean(NBT_FROZEN_BY_TIME)) continue;

            persistentData.remove(NBT_FROZEN_BY_TIME);

            if (e instanceof Mob mob) {
                mob.setNoAi(false);
            }

            e.setNoGravity(false);

            // WIEDERHERSTELLE Geschwindigkeit
            Vec3 originalVelocity = entityVelocities.get(e.getUUID());
            if (originalVelocity != null) {
                e.setDeltaMovement(originalVelocity);
                entityVelocities.remove(e.getUUID());
            }

            MobEffectInstance effect = e.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (effect != null && effect.getAmplifier() == 255) {
                e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }

        // ALLE Projektile entfrieren mit WIEDERHERGESTELLTER Geschwindigkeit
        List<Entity> allEntities = world.getEntitiesOfClass(Entity.class, box);

        for (Entity entity : allEntities) {
            if (!isProjectile(entity)) continue;

            CompoundTag tag = entity.getPersistentData();
            if (!tag.getBoolean(NBT_ARROW_FROZEN)) continue;

            tag.remove(NBT_ARROW_FROZEN);

            // Hole gespeicherte Geschwindigkeit
            Vec3 originalVelocity = projectileVelocities.get(entity.getUUID());
            if (originalVelocity == null) {
                // Fallback: Versuche aus NBT zu laden
                CompoundTag velocityTag = tag.getCompound(NBT_PROJECTILE_VELOCITY);
                if (!velocityTag.isEmpty()) {
                    originalVelocity = new Vec3(
                            velocityTag.getDouble("X"),
                            velocityTag.getDouble("Y"),
                            velocityTag.getDouble("Z")
                    );
                }
            }

            // WIEDERHERSTELLE Geschwindigkeit
            if (originalVelocity != null) {
                entity.setDeltaMovement(originalVelocity);
                projectileVelocities.remove(entity.getUUID());

                // Spawn Geschwindigkeits-Partikel
                if (world instanceof ServerLevel serverLevel) {
                    spawnVelocityParticles(serverLevel, entity, originalVelocity);
                }
            }

            tag.remove(NBT_PROJECTILE_VELOCITY);

            if (entity instanceof AbstractArrow arrow) {
                arrow.setNoGravity(false);
                arrow.setNoPhysics(false);
            }
        }

        // Fallende Blöcke entfrieren
        List<FallingBlockEntity> fallingBlocks = world.getEntitiesOfClass(
                FallingBlockEntity.class,
                box
        );

        for (FallingBlockEntity fallingBlock : fallingBlocks) {
            CompoundTag tag = fallingBlock.getPersistentData();
            if (!tag.getBoolean(NBT_FROZEN_BY_TIME)) continue;

            tag.remove(NBT_FROZEN_BY_TIME);

            // Geschwindigkeit wiederherstellen
            Vec3 originalVelocity = entityVelocities.get(fallingBlock.getUUID());
            if (originalVelocity != null) {
                fallingBlock.setDeltaMovement(originalVelocity);
                entityVelocities.remove(fallingBlock.getUUID());
            }

            // FallingBlock Timer wieder normalisieren
            fallingBlock.time = fallingBlock.time + 1;

            tag.remove("FrozenMotion");
        }

        // Item Entities entfrieren
        List<ItemEntity> itemEntities = world.getEntitiesOfClass(
                ItemEntity.class,
                box
        );

        for (ItemEntity itemEntity : itemEntities) {
            CompoundTag tag = itemEntity.getPersistentData();
            if (!tag.getBoolean(NBT_FROZEN_BY_TIME)) continue;

            tag.remove(NBT_FROZEN_BY_TIME);

            // Geschwindigkeit wiederherstellen
            Vec3 originalVelocity = entityVelocities.get(itemEntity.getUUID());
            if (originalVelocity != null) {
                itemEntity.setDeltaMovement(originalVelocity);
                entityVelocities.remove(itemEntity.getUUID());
            }

            itemEntity.setNoGravity(false);

            // Pickup-Delay wiederherstellen
            if (tag.contains("OriginalPickupDelay")) {
                itemEntity.setPickUpDelay(tag.getInt("OriginalPickupDelay"));
                tag.remove("OriginalPickupDelay");
            } else {
                itemEntity.setPickUpDelay(0);
            }
        }
    }

    private void spawnVelocityParticles(ServerLevel world, Entity entity, Vec3 velocity) {
        Vec3 direction = velocity.normalize();
        for (int i = 0; i < 5; i++) {
            double offset = world.random.nextDouble() * 0.3;
            world.sendParticles(ParticleTypes.END_ROD,
                    entity.getX() + direction.x * offset,
                    entity.getY() + direction.y * offset + entity.getBbHeight() * 0.5,
                    entity.getZ() + direction.z * offset,
                    1,
                    0, 0, 0,
                    0);
        }
    }

    // ======================================================
    // TICK HANDLER für kontinuierlichen Freeze
    // ======================================================

    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class FreezeTicker {

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity e = event.getEntity();
            Level world = e.getCommandSenderWorld();

            if (world.isClientSide) return;

            CompoundTag persistentData = e.getPersistentData();
            if (persistentData.getBoolean(NBT_FROZEN_BY_TIME)) {
                // Lebewesen einfrieren
                e.setDeltaMovement(Vec3.ZERO);
                e.hasImpulse = false;
                e.yBodyRot = e.yBodyRotO;
                e.yHeadRot = e.yHeadRotO;

                if (e instanceof Mob mob) {
                    mob.getNavigation().stop();
                    mob.setTarget(null);
                    mob.setAggressive(false);
                }

                MobEffectInstance effect = e.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                if (effect == null || effect.getAmplifier() != 255 || effect.getDuration() < 100) {
                    e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    e.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN,
                            200,
                            255,
                            false,
                            false,
                            false));
                }
            }
        }

        @SubscribeEvent
        public static void onWorldTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;

            Level world = event.level;

            if (world instanceof ServerLevel serverLevel) {
                // Alle Entities durchgehen
                for (Entity entity : serverLevel.getAllEntities()) {
                    CompoundTag tag = entity.getPersistentData();

                    if (tag.getBoolean(NBT_FROZEN_BY_TIME)) {
                        // Alle eingefrorenen Entities behandeln
                        if (entity instanceof FallingBlockEntity ||
                                entity instanceof ItemEntity ||
                                isProjectileType(entity)) {

                            // Bewegung stoppen
                            entity.setDeltaMovement(Vec3.ZERO);
                            entity.hasImpulse = false;
                            entity.setPos(entity.xOld, entity.yOld, entity.zOld);

                            // Für FallingBlockEntity speziell
                            if (entity instanceof FallingBlockEntity fallingBlock) {
                                // Verhindere das Aufprallen/Block-Erstellen
                                fallingBlock.time = fallingBlock.time - 1; // Zähler einfrieren
                                // Stelle sicher, dass kein Schaden verursacht wird (FLOAT Wert)
                                fallingBlock.setHurtsEntities(false);
                            }

                            // Visual Feedback
                            if (world.getGameTime() % 10 == 0 && entity.isAlive()) {
                                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                                        entity.getX(),
                                        entity.getY() + entity.getBbHeight() * 0.5,
                                        entity.getZ(),
                                        1,
                                        0.1, 0.1, 0.1,
                                        0);
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onEntityJoin(EntityJoinLevelEvent event) {
            Entity entity = event.getEntity();

            if (isProjectileType(entity)) {
                CompoundTag tag = entity.getPersistentData();
                if (tag.getBoolean(NBT_ARROW_FROZEN)) {
                    entity.setDeltaMovement(Vec3.ZERO);
                    entity.hasImpulse = false;

                    if (entity instanceof AbstractArrow arrow) {
                        arrow.setNoGravity(true);
                        arrow.setNoPhysics(true);
                    }
                }
            }
        }

        private static boolean isProjectileType(Entity entity) {
            return entity instanceof AbstractArrow ||
                    entity instanceof Snowball ||
                    entity instanceof Fireball ||
                    entity instanceof ThrownTrident ||
                    entity instanceof ThrowableItemProjectile;
        }
    }

    // ======================================================
    // VISUELLE EFFEKTE
    // ======================================================

    private void spawnFreezeEffect(Level world, Player player) {
        if (world.isClientSide) return;

        world.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 1.5F, 0.3F);

        if (world instanceof ServerLevel serverWorld) {
            // Zeit-Welle Effekt
            for (int r = 1; r <= 5; r++) {
                double radius = r * 2.0;
                for (int i = 0; i < 18; i++) {
                    double angle = Math.toRadians(i * 20);
                    double x = player.getX() + Math.cos(angle) * radius;
                    double z = player.getZ() + Math.sin(angle) * radius;

                    serverWorld.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            x, player.getY() + 1, z,
                            1,
                            0, 0.5, 0,
                            0.1);
                }
            }

            // Zentraler Explosionseffekt
            serverWorld.sendParticles(ParticleTypes.DRAGON_BREATH,
                    player.getX(), player.getY() + 2, player.getZ(),
                    50,
                    2.0, 1.5, 2.0,
                    0.2);
        }
    }

    private void spawnUnfreezeEffect(Level world, Player player) {
        if (world.isClientSide) return;

        world.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.PLAYERS, 1.0F, 1.5F);

        if (world instanceof ServerLevel serverWorld) {
            // Explosionsartiger Effekt
            serverWorld.sendParticles(ParticleTypes.FLASH,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    1,
                    0, 0, 0,
                    0);

            serverWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    100,
                    3.0, 2.0, 3.0,
                    0.3);

            // Geschwindigkeitslinien
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(i * 30);
                double speed = 1.0 + serverWorld.random.nextDouble();
                serverWorld.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        3,
                        Math.cos(angle) * speed,
                        serverWorld.random.nextDouble() * 0.3,
                        Math.sin(angle) * speed,
                        0.1);
            }
        }
    }

    // ======================================================
    // TOOLTIP & INFO
    // ======================================================

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4§l⏳ ZEITMANIPULATOR"));
        tooltip.add(Component.literal("§7Rechtsklick: §fZeit anhalten/fortsetzen"));
        tooltip.add(Component.literal("§7Radius: §e" + getRadius(stack) + " Blöcke"));

        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.getBoolean(NBT_FROZEN)) {
                tooltip.add(Component.literal("§c§l⏸ ZEIT GEFROREN"));
                tooltip.add(Component.literal("§7Alle Bewegungen sind angehalten"));
            } else {
                tooltip.add(Component.literal("§a§l▶ ZEIT LÄUFT"));
            }
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§6✧ Besondere Eigenschaften:"));
        tooltip.add(Component.literal("§7• Projektil-Geschwindigkeiten werden gespeichert"));
        tooltip.add(Component.literal("§7• Fallende Blöcke (Sand/Kies) werden angehalten"));
        tooltip.add(Component.literal("§7• Herumliegende Items werden eingefroren"));
        tooltip.add(Component.literal("§7• Bei Fortsetzung fliegen sie weiter"));
        tooltip.add(Component.literal("§7• Funktioniert mit allen Wurfgeschossen"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§d⚡ Legendäres Zeit-Artefakt"));
    }

    // ======================================================
    // RADIUS MANAGEMENT
    // ======================================================

    private int getRadius(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int r = tag.getInt(NBT_RADIUS);

        if (r <= 0) r = DEFAULT_RADIUS;
        if (r > MAX_RADIUS) r = MAX_RADIUS;

        return r;
    }

    public void setRadius(ItemStack stack, int radius) {
        stack.getOrCreateTag().putInt(
                NBT_RADIUS,
                Mth.clamp(radius, 5, MAX_RADIUS));
    }

    // ======================================================
    // CLIENT-SIDE HELPER
    // ======================================================

    @OnlyIn(Dist.CLIENT)
    public static float getAnimationProgress(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_ANIMATION_FRAME)) {
            int frame = tag.getInt(NBT_ANIMATION_FRAME);
            return frame / (float) ANIMATION_FRAMES;
        }
        return 0.0f;
    }
}