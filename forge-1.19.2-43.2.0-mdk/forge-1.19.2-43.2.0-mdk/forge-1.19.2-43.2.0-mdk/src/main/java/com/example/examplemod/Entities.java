package com.example.examplemod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Entities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "meinemod");

    public static final RegistryObject<EntityType<ExplodingSheep>> EXPLODING_SHEEP =
            ENTITIES.register("exploding_sheep", () ->
                    EntityType.Builder.<ExplodingSheep>of(ExplodingSheep::new, MobCategory.MISC)
                            .sized(0.9F, 1.3F).build("exploding_sheep"));
}
