package screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import screen.ModMenuTypes;

public class SkillTreeMenu extends AbstractContainerMenu {

    // 1. The constructor for the Server (Manual creation)
    public SkillTreeMenu(int containerId, Inventory inv) {
        this(containerId, inv, null);
    }

    // 2. The constructor for the Network/Registry (The one IForgeMenuType wants)
    public SkillTreeMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.SKILL_TREE_MENU.get(), containerId);
        // You can read extraData here if you sent any (like player XP or skill levels)
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}