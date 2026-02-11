package com.example.examplemod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

import org.checkerframework.checker.units.qual.s;

import com.example.examplemod.ExampleMod.BlitzZauberstabItem;
import com.example.examplemod.ExampleMod.CreativeDestructionPickaxe;
import com.example.examplemod.ExampleMod.SkillTreeBookItem;
import com.example.examplemod.ExampleMod.SuperFeuerItem;

import java.util.List;

@Mod("meinemod")
public class ExampleMod {

    // ============= 1. SUPER FEUER WAFFE / FEUERZAUBERSTAB =============
    public static class SuperFeuerItem extends Item {
        public SuperFeuerItem() {
            super(new Item.Properties()
                    .tab(CreativeModeTab.TAB_COMBAT)
                    .stacksTo(1)
                    .durability(100)
                    .rarity(Rarity.RARE)  // Blauer Text im Inventar
                    .fireResistant());
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);

            if (!world.isClientSide) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 2.0F, 0.5F);

                // 3 kleine Feuerbälle
                for (int i = 0; i < 3; i++) {
                    SmallFireball fireball = new SmallFireball(world, player,
                            player.getLookAngle().x * 2,
                            player.getLookAngle().y * 2,
                            player.getLookAngle().z * 2);

                    fireball.setPos(player.getX(), player.getEyeY() - 0.5, player.getZ());
                    world.addFreshEntity(fireball);

                    // Partikel-Effekt beim Schießen
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
                // 15 Schaden
                target.hurt(DamageSource.mobAttack(attacker), 15.0F);

                // Knockback
                target.push(
                        attacker.getLookAngle().x * 3,
                        2.0,
                        attacker.getLookAngle().z * 3
                );

                // Setze Gegner in Brand
                target.setSecondsOnFire(5);

                attacker.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);

                // Feuer-Partikel
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
            return true; // Immer glitzern
        }

        // Tooltip mit Infos in rot/orange
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

    // ============= 2. BLITZ ZAUBERSTAB =============
    public static class BlitzZauberstabItem extends Item {
        // KONFIGURATION
        private static final double REICHWEITE = 50.0;
        private static final double SCHADEN = 12.0F;
        private static final int BRENNZEIT = 4;
        private static final double RADIUS = 6.0;
        private static final int COOLDOWN = 30;
        private static final int HALTBARKEITSVERLUST = 3;

        public BlitzZauberstabItem() {
            super(new Item.Properties()
                    .tab(CreativeModeTab.TAB_COMBAT)
                    .stacksTo(1)
                    .durability(75)
                    .rarity(Rarity.RARE)
                    .fireResistant());
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);

            if (!world.isClientSide) {
                // Berechnung mit konfigurierter Reichweite
                Vec3 start = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                Vec3 end = start.add(look.scale(REICHWEITE));

                BlockHitResult rayTrace = world.clip(new ClipContext(
                        start, end,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                ));

                Vec3 hitPos = rayTrace.getType() == HitResult.Type.BLOCK ?
                        rayTrace.getLocation() : end;

                // Blitz mit Effekten
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
                if (lightning != null) {
                    lightning.setPos(hitPos.x, hitPos.y, hitPos.z);
                    lightning.setVisualOnly(false);
                    world.addFreshEntity(lightning);
                }

                // Mehr Sounds
                world.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5F, 0.8F);
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.5F);

                // Effekt auf alle in Reichweite
                List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                        new AABB(hitPos.x - RADIUS, hitPos.y - RADIUS, hitPos.z - RADIUS,
                                hitPos.x + RADIUS, hitPos.y + RADIUS, hitPos.z + RADIUS),
                        e -> e != player && e.isAlive());

                for (LivingEntity entity : entities) {
                    entity.hurt(DamageSource.LIGHTNING_BOLT, (float) SCHADEN);
                    entity.setSecondsOnFire(BRENNZEIT);
                }

                // Verbrauch
                stack.hurtAndBreak(HALTBARKEITSVERLUST, player, (p) -> p.broadcastBreakEvent(hand));
                player.getCooldowns().addCooldown(this, COOLDOWN);

                // Erfolgs-Sound für Spieler
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.5F, 1.2F);
            }

            player.swing(hand, true);
            return InteractionResultHolder.success(stack);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("§bReichweite: " + REICHWEITE + " Blöcke"));
            tooltip.add(Component.literal("§cSchaden: " + SCHADEN / 2 + " Herzen"));
            tooltip.add(Component.literal("§6Brennt: " + BRENNZEIT + "s"));
            super.appendHoverText(stack, world, tooltip, flag);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }
    }

    // ============= 3. CREATIVE ZERSTÖRUNGS-SPITZHACKE =============
    public static class CreativeDestructionPickaxe extends Item {
        public CreativeDestructionPickaxe() {
            super(new Item.Properties()
                    .tab(CreativeModeTab.TAB_TOOLS)
                    .stacksTo(1)
                    .durability(0)
                    .rarity(Rarity.EPIC)
                    .fireResistant());
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level world, BlockState state,
                                 BlockPos pos, LivingEntity miner) {
            if (!world.isClientSide && miner instanceof Player player) {
                if (player.isCrouching()) {
                    return false;
                }

                if (!player.isCreative()) {
                    world.destroyBlock(pos, true);

                    world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                            0.8F, 1.5F);
                }
            }
            return true;
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return 100.0F;
        }

        @Override
        public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
            return true;
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            if (!attacker.level.isClientSide && attacker instanceof Player player) {
                if (player.isCrouching()) {
                    shootTNT(attacker.level, attacker);
                    return true;
                }

                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(attacker.level);
                if (lightning != null) {
                    lightning.setPos(target.getX(), target.getY(), target.getZ());
                    lightning.setVisualOnly(false);
                    attacker.level.addFreshEntity(lightning);
                }

                target.hurt(DamageSource.LIGHTNING_BOLT, 20.0F);
                target.setSecondsOnFire(10);

                attacker.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                        2.0F, 0.8F);
            }
            return true;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);

            if (player.isCrouching()) {
                if (!world.isClientSide) {
                    shootTNT(world, player);
                    player.getCooldowns().addCooldown(this, 10);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TNT_PRIMED, SoundSource.PLAYERS,
                            2.0F, 1.0F);
                }
                player.swing(hand, true);
                return InteractionResultHolder.success(stack);
            }

            if (!world.isClientSide) {
                BlockHitResult rayTrace = getPlayerPOVHitResult(world, player, ClipContext.Fluid.NONE);

                if (rayTrace.getType() == HitResult.Type.BLOCK) {
                    BlockPos placePos = rayTrace.getBlockPos().relative(rayTrace.getDirection());
                    BlockState blockToPlace = Blocks.STONE.defaultBlockState();

                    if (world.isEmptyBlock(placePos)) {
                        world.setBlock(placePos, blockToPlace, 3);

                        world.playSound(null, placePos.getX(), placePos.getY(), placePos.getZ(),
                                SoundEvents.STONE_PLACE, SoundSource.PLAYERS,
                                1.0F, 1.0F);
                    }
                }
            }

            return InteractionResultHolder.success(stack);
        }

        private void shootTNT(Level world, LivingEntity shooter) {
            if (!world.isClientSide) {
                PrimedTnt tnt = new PrimedTnt(world,
                        shooter.getX(),
                        shooter.getEyeY(),
                        shooter.getZ(),
                        shooter);

                Vec3 look = shooter.getLookAngle();
                tnt.setDeltaMovement(
                        look.x * 2.0,
                        look.y * 2.0 + 0.5,
                        look.z * 2.0
                );

                tnt.setFuse(80);
                world.addFreshEntity(tnt);

                world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                        SoundEvents.TNT_PRIMED, SoundSource.PLAYERS,
                        1.0F, 1.0F);

                world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                        0.5F, 2.0F);
            }
        }

        @Override
        public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
            return true;
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }

        @Override
        public boolean isEnchantable(ItemStack stack) {
            return true;
        }

        @Override
        public int getEnchantmentValue() {
            return 30;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level world,
                                    List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("§5§lGÖTTLICHE SPITZHACKE"));
            tooltip.add(Component.literal("§aLinksklick: §7Creative-Mode Abbau"));
            tooltip.add(Component.literal("§aRechtsklick: §7Creative-Mode Platzieren"));
            tooltip.add(Component.literal("§c[SHIFT] + Rechtsklick: §7TNT schießen"));
            tooltip.add(Component.literal("§6Angriff: §7Blitze beschwören"));
            tooltip.add(Component.literal("§eUnzerstörbar - Episch!"));
            super.appendHoverText(stack, world, tooltip, flag);
        }
    }

        
// ============= 4. SKILL TREE BOOK =============
public static class SkillTreeBookItem extends Item {
    public SkillTreeBookItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }
   
    

    @Override
public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (!world.isClientSide()) {
        
        player.sendSystemMessage(Component.literal("You used the custom item!"));
        
    }
    
    if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
        NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Skill Tree");
                
            }

            @Override
            public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
            
                return new MyMenu(pContainerId, pPlayerInventory);
            }
        });
    }


    return InteractionResultHolder.success(stack);
}

       


    
}







    // ============= ITEM REGISTRIERUNG =============
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "meinemod");

    public static final RegistryObject<Item> SUPER_FEUER_WAFFE = ITEMS.register("super_feuer_waffe",
            () -> new SuperFeuerItem());

    public static final RegistryObject<Item> BLITZ_ZAUBERSTAB = ITEMS.register("blitz_zauberstab",
            () -> new BlitzZauberstabItem());

    public static final RegistryObject<Item> CREATIVE_SPITZHACKE = ITEMS.register("creative_spitzhacke",
            () -> new CreativeDestructionPickaxe());
    public static final RegistryObject<Item> SKILL_TREE_BOOK = ITEMS.register("skill_tree_book",
            () -> new SkillTreeBookItem());

    public ExampleMod() {
        System.out.println("💥 MOD GELADEN! 💥");
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
       
        SkillTree.REGISTER.register(bus);
        ITEMS.register(bus);


    }




}