package screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SkillTreeScreen extends AbstractContainerScreen<SkillTreeMenu> {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation("meinemod", "textures/gui/skilltree_selector.png");

    public SkillTreeScreen(SkillTreeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;  // Width of your PNG
        this.imageHeight = 256; // Height of your PNG
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Centers the texture on any screen size
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Draws the texture (blit: stack, x, y, u, v, width, height)
        this.blit(poseStack, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        renderBackground(poseStack); // Darkens the game world behind the menu
        super.render(poseStack, mouseX, mouseY, delta);
        renderTooltip(poseStack, mouseX, mouseY);
    }
}