package unicornEntity;

import net.minecraft.world.entity.ai.goal.Goal;

import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FollowPlayerGoal extends Goal {

    private final UnicornEntity unicorn;

    private final double speed;

    private final float stopDistance;

    private final float startDistance;

    public FollowPlayerGoal(UnicornEntity unicorn, double speed, float startDistance, float stopDistance) {

        this.unicorn = unicorn;

        this.speed = speed;

        this.startDistance = startDistance;

        this.stopDistance = stopDistance;

        this.setFlags(EnumSet.of(Flag.MOVE));

    }

    @Override

    public boolean canUse() {

        Player player = unicorn.level.getNearestPlayer(unicorn, 10);

        if (player == null) return false;

        unicorn.setOwner(player);

        return unicorn.distanceTo(player) > startDistance;

    }

    @Override

    public void tick() {

        Player player = unicorn.getOwner();

        if (player != null && unicorn.distanceTo(player) > stopDistance) {

            unicorn.getNavigation().moveTo(player, speed);

        }

    }

}
