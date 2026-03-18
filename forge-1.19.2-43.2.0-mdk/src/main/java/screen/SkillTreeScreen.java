package screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;


public class SkillTreeScreen extends AbstractContainerScreen<SkillTreeMenu> {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation("meinemod", "textures/gui/skilltree_selector.png");

    public SkillTreeScreen(SkillTreeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Adjust these offsets to line up with your three parchment tiles
        addRenderableWidget(invisibleButton(x + 50,  y + 80, 64, 64, btn -> onSkillClick(0)));
        addRenderableWidget(invisibleButton(x + 96,  y + 80, 64, 64, btn -> onSkillClick(1)));
        addRenderableWidget(invisibleButton(x + 142, y + 80, 64, 64, btn -> onSkillClick(2)));
    }

    private void onSkillClick(int skillIndex) {
        // TODO: send a packet to the server here
        System.out.println("Clicked skill: " + skillIndex);
        switch (skillIndex){
            case 0: giveStrength(minecraft.player, 0);
            case 1: giveRegenaration(minecraft.player, 0);
            case 2: FlightManager.giveFlight(minecraft.player, 60);
        }
    }


    private Button invisibleButton(int x, int y, int w, int h, Button.OnPress onPress) {
    return new Button(x, y, w, h, Component.empty(), onPress) {
        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float delta) {
            // Don't render anything — the background already shows the icon
        }
    };
}

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.blit(poseStack, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, delta);
        renderTooltip(poseStack, mouseX, mouseY);
    }

    private void giveStrength(Player player, int amplifier) {
    player.addEffect(new MobEffectInstance(
        MobEffects.DAMAGE_BOOST,
        Integer.MAX_VALUE,  // ~68 years, effectively permanent
        amplifier,
        false,
        true
    ));
}
  private void giveRegenaration(Player player, int amplifier) {
    player.addEffect(new MobEffectInstance(
        MobEffects.REGENERATION,
        Integer.MAX_VALUE,  // ~68 years, effectively permanent
        amplifier,
        false,
        true
    ));
}



}