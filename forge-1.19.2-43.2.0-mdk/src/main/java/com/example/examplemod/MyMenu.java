package com.example.examplemod;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class MyMenu extends AbstractContainerMenu {
    public MyMenu(int containerId, Inventory inventory) {
    super(SkillTree.MY_MENU.get(), containerId);
    
}

    @Override
    public boolean stillValid(Player player) {
        
        return true;
        
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;  // Basic: no shift-click; implement slot transfers
    }
}