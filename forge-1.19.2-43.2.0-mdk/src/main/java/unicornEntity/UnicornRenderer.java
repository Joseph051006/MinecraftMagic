package unicornEntity;

import com.example.examplemod.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class UnicornRenderer extends MobRenderer<UnicornEntity, UnicornModel<UnicornEntity>> {

    // Ensure the file is exactly at: src/main/resources/assets/meinemod/textures/entity/abomination.png
    private static final ResourceLocation TEXTURE = new ResourceLocation("meinemod", "textures/entity/begleiter-1.png");

    public UnicornRenderer(EntityRendererProvider.Context context) {
        // This links the custom model and the unique layer we created to avoid the Horse crash
        super(context, new UnicornModel<>(context.bakeLayer(ExampleMod.UNICORN_LAYER)), 0.7f);
    }

    @Override
    public void render(UnicornEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // DEBUG: If you see this in your console, the renderer is working!
        // If the Unicorn is still invisible, the issue is the PNG file itself or its path.
        System.out.println("DEBUG: Rendering Unicorn Entity at " + entity.position());

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(UnicornEntity entity) {
        return TEXTURE;
    }
}