package com.example.examplemod;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(ExampleMod.MODID) // Verwende die MODID Konstante
public class ExampleMod {

    // MODID muss hier definiert werden (außerhalb des Konstruktors!)
    public static final String MODID = "meinemod";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID); // Verwende MODID Konstante

    public static final RegistryObject<Item> SUPER_FEUER_WAFFE = ITEMS.register("super_feuer_waffe",
            () -> new SuperFeuerItem());

    public static final RegistryObject<Item> ZEIT_ITEM = ITEMS.register("zeit_item",
            () -> new ZeitItem());

    public static final RegistryObject<Item> EIS_ITEM = ITEMS.register("eis_item",
            () -> new EisItem());

    public ExampleMod() {
        System.out.println("💥 MOD GELADEN! 💥");
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
    }
}