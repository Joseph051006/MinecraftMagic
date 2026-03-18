package com.example.examplemod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

public class BlitzZauberstabItem extends Item {
    private static final double REICHWEITE = 50.0;
    private static final double SCHADEN = 12.0F;
    private static final int BRENNZEIT = 4;
    private static final double RADIUS = 6.0;
    private static final int COOLDOWN = 30;
    private static final int HALTBARKEITSVERLUST = 3;

    public BlitzZauberstabItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(75)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
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

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.setPos(hitPos.x, hitPos.y, hitPos.z);
                lightning.setVisualOnly(false);
                world.addFreshEntity(lightning);
            }

            world.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5F, 0.8F);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.5F);

            List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                    new AABB(hitPos.x - RADIUS, hitPos.y - RADIUS, hitPos.z - RADIUS,
                            hitPos.x + RADIUS, hitPos.y + RADIUS, hitPos.z + RADIUS),
                    e -> e != player && e.isAlive());

            for (LivingEntity entity : entities) {
                entity.hurt(DamageSource.LIGHTNING_BOLT, (float) SCHADEN);
                entity.setSecondsOnFire(BRENNZEIT);
            }

            stack.hurtAndBreak(HALTBARKEITSVERLUST, player, (p) -> p.broadcastBreakEvent(hand));
            player.getCooldowns().addCooldown(this, COOLDOWN);

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
