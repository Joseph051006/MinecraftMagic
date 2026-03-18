package com.example.examplemod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;

public class ExplodingSheep extends Sheep {
    private static final int EXPLODE_DELAY = 40;
    private int delayTimer = 0;

    public ExplodingSheep(EntityType<? extends Sheep> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            delayTimer++;
            if (delayTimer >= EXPLODE_DELAY) {
                level.explode(this, getX(), getY(0.0625), getZ(), 3.0F, true, Explosion.BlockInteraction.DESTROY);
                discard();
            }
        }
    }
}
