package screen;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, "meinemod");

    public static final RegistryObject<MenuType<SkillTreeMenu>> SKILL_TREE_MENU =
        MENUS.register("skill_tree_menu", () -> IForgeMenuType.create(SkillTreeMenu::new));
}