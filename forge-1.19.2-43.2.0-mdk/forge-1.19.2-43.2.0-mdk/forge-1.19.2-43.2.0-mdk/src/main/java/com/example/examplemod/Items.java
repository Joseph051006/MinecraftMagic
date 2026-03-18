package com.example.examplemod;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Items {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "meinemod");

    public static final RegistryObject<Item> CATALYST_ITEM = ITEMS.register("catalyst_item", () ->
            new Catalyst(new Item.Properties()
                    .stacksTo(1)
                    .rarity(net.minecraft.world.item.Rarity.EPIC)
                    .tab(CreativeModeTab.TAB_MISC)));  // <- 1.19.2 TAB!
}
