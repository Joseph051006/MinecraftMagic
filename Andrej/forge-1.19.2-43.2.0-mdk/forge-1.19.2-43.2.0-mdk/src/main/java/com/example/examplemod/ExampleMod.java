package com.example.examplemod;

import net.minecraft.client.model.geom.ModelLayerLocation;
import unicornEntity.UnicornRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraft.world.level.block.RenderShape;
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
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

import screen.ModMenuTypes;
import screen.SkillTreeMenu;
import screen.SkillTreeScreen;
import unicornEntity.UnicornEntity;
import unicornEntity.UnicornModel;

import java.util.List;

@Mod("meinemod")
public class ExampleMod {

    // ============= 1. ITEM REGISTRY =============
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "meinemod");

    // ✅ ALL ITEMS NOW IMPORT FROM SEPARATE FILES
    public static final RegistryObject<Item> SUPER_FEUER_WAFFE = ITEMS.register("super_feuer_waffe",
            () -> new SuperFeuerItem());

    public static final RegistryObject<Item> BLITZ_ZAUBERSTAB = ITEMS.register("blitz_zauberstab",
            () -> new BlitzZauberstabItem());

    public static final RegistryObject<Item> CREATIVE_SPITZHACKE = ITEMS.register("creative_spitzhacke",
            () -> new CreativeDestructionPickaxe());

    public static final RegistryObject<Item> SKILL_TREE_BOOK = ITEMS.register("skill_tree_book",
            () -> new SkillTreeBookItem());

    public static final RegistryObject<Item> ZEIT_ITEM = ITEMS.register("zeit_item",
            () -> new ZeitItem());

    public static final RegistryObject<Item> EIS_ITEM = ITEMS.register("eis_item",
            () -> new EisItem());

    // ============= 2. CONSTRUCTOR =============
    public ExampleMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register everything to the bus
        ITEMS.register(bus);
        ModMenuTypes.MENUS.register(bus);
        ENTITIES.register(bus);

        // Listen for the client setup event
        bus.addListener(this::clientSetup);

        System.out.println("💥 MOD GELADEN! 💥");
    }

    @Mod.EventBusSubscriber(modid = "meinemod", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistrationEvents {
        @SubscribeEvent
        public static void onEntityAttributes(EntityAttributeCreationEvent event) {
            // This gives the Unicorn its 'soul' (Health and Speed)
            event.put(UNICORN.get(), UnicornEntity.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, 2000.0D)
                    .add(Attributes.MOVEMENT_SPEED, 0.3D)
                    .add(Attributes.ATTACK_DAMAGE, 150.0D)
                    .add(Attributes.FOLLOW_RANGE, 24.0D)
                    .build());
        }
    }

    @Mod.EventBusSubscriber(modid = "meinemod", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEntityEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // This links the Entity Type to the Renderer
            event.registerEntityRenderer(UNICORN.get(), UnicornRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ExampleMod.UNICORN_LAYER, UnicornModel::createBodyLayer);
        }
    }

    // ============= 3. CLIENT SETUP =============
    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Links the Menu to the Screen
            MenuScreens.register(ModMenuTypes.SKILL_TREE_MENU.get(), SkillTreeScreen::new);
        });
    }

    // ============= 4. ITEM CLASSES (INNER STUBS - CONSIDER MOVING TO SEPARATE FILES) =============

    // --- Creative Destruction Pickaxe ---
    public static class CreativeDestructionPickaxe extends Item {
        public CreativeDestructionPickaxe() {
            super(new Item.Properties().tab(CreativeModeTab.TAB_TOOLS).stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        }
        // TODO: Add mineBlock and shootTNT logic here
    }

    // --- Skill Tree Book ---
    public static class SkillTreeBookItem extends Item {
        public SkillTreeBookItem() {
            super(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_COMBAT).rarity(Rarity.UNCOMMON));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);

            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Skill Tree");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new SkillTreeMenu(id, inv);
                    }
                });
                player.sendSystemMessage(Component.literal("Opening Skill Tree..."));
            }
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        }
    }

    // --- Zeit Item ---
    public static class ZeitItem extends Item {
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
            var entities = world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50));
            for (LivingEntity entity : entities) {
                if (entity == player) continue;

                entity.setDeltaMovement(Vec3.ZERO);
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999999, 255));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 999999, 255));
            }
        }

        private void unfreezeAlleMobs(Level world, Player player) {
            var entities = world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50));
            for (LivingEntity entity : entities) {
                if (entity == player) continue;

                entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                entity.removeEffect(MobEffects.DIG_SLOWDOWN);
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

    // ============= 5. UNICORN ENTITY =============
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "meinemod");

    public static final RegistryObject<EntityType<UnicornEntity>> UNICORN =
            ENTITIES.register("unicorn", () -> EntityType.Builder.of(UnicornEntity::new, MobCategory.CREATURE)
                    .sized(1.2f, 1.5f)
                    .build("unicorn"));

    public static final ModelLayerLocation UNICORN_LAYER =
            new ModelLayerLocation(new ResourceLocation("meinemod", "unicorn"), "main");

} // End of ExampleMod Class