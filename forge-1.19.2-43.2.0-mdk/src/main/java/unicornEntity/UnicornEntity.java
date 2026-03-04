package unicornEntity;


import net.minecraft.world.entity.*;

 import net.minecraft.world.entity.ai.goal.*;

 import net.minecraft.world.entity.player.Player;

 import net.minecraft.world.level.Level;

public class UnicornEntity extends PathfinderMob {
    private Player owner;

    // Must be public for the EntityType.Builder to see it!
    public UnicornEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override

     protected void registerGoals() {

         this.goalSelector.addGoal(1, new FloatGoal(this));

         this.goalSelector.addGoal(2, new FollowPlayerGoal(this, 1.2D, 3.0F, 1.0F));

         this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

     }

    public void setOwner(Player player) {

         this.owner = player;

     }

    public Player getOwner() {

         return this.owner;

     }

 }