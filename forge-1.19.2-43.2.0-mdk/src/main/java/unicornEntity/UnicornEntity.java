package unicornEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.EnumSet;

public class UnicornEntity extends PathfinderMob {
    private Player owner;

    public UnicornEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // This makes the unicorn invincible as per your request
        this.setInvulnerable(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));

        // Attack Goal: Attacks nearby enemies
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, true));

        // Follow Goal: Uses the inner class at the bottom of this file
        this.goalSelector.addGoal(3, new UnicornFollowOwnerGoal(this, 1.2D, 6.0F, 2.0F));

        // Looking around
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // Target Selectors: Who should the unicorn fight?
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    public void setOwner(Player player) {
        this.owner = player;
    }

    public Player getOwner() {
        return this.owner;
    }

    // This defines the HP, Speed, and Damage
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    // --- INNER CLASS: Follow Logic (Saves you from creating a separate file) ---
    static class UnicornFollowOwnerGoal extends Goal {
        private final UnicornEntity unicorn;
        private final double speed;
        private final float startDistance;
        private final float stopDistance;

        public UnicornFollowOwnerGoal(UnicornEntity unicorn, double speed, float startDistance, float stopDistance) {
            this.unicorn = unicorn;
            this.speed = speed;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Automatically find a player to follow if no owner is set
            Player player = this.unicorn.level.getNearestPlayer(this.unicorn, 10);
            if (player == null) return false;

            this.unicorn.setOwner(player);
            return this.unicorn.distanceToSqr(player) > (double)(startDistance * startDistance);
        }

        @Override
        public void tick() {
            Player owner = this.unicorn.getOwner();
            if (owner != null && this.unicorn.distanceToSqr(owner) > (double)(stopDistance * stopDistance)) {
                this.unicorn.getNavigation().moveTo(owner, this.speed);
            }
        }
    }
}