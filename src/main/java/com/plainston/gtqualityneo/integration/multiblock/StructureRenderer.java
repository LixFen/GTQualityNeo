package com.plainston.gtqualityneo.integration.multiblock;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Native depth-buffered GUI scene, independent of GTM's UI library. */
public final class StructureRenderer extends PictureInPictureRenderer<StructureRenderState> {
    public StructureRenderer(MultiBufferSource.BufferSource buffers) { super(buffers); }
    @Override public Class<StructureRenderState> getRenderStateClass() { return StructureRenderState.class; }
    @Override protected String getTextureLabel() { return "gtqualityneo_multiblock"; }
    @Override protected float getTranslateY(int height, int guiScale) { return height / 2f; }
    @Override protected void renderToTexture(StructureRenderState state, PoseStack pose) {
        var minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        var features = minecraft.gameRenderer.getFeatureRenderDispatcher();
        applyCamera(pose, state.rotation(), state.center());
        for (var cell : state.cells()) {
            pose.pushPose();
            pose.translate(cell.pos().getX(), cell.pos().getY(), cell.pos().getZ());
            features.getSubmitNodeStorage().submitItem(pose, ItemDisplayContext.NONE, 15728880,
                OverlayTexture.NO_OVERLAY, 0, new int[0], cell.quads(), ItemStackRenderState.FoilType.NONE);
            pose.popPose();
        }
        features.renderAllFeatures();
    }

    static void applyCamera(PoseStack pose, Quaternionf rotation, Vector3f center) {
        // PiP supplies +Y down and reflects Z. Restore +Y up and +Z towards the viewer
        // so the orthographic depth buffer and Scene.hit() agree on the nearest face.
        pose.scale(1, -1, -1);
        pose.mulPose(rotation);
        pose.translate(-center.x, -center.y, -center.z);
    }

    static float frontYaw(byte facing) {
        // Keep the controller face visible, angled 45 degrees towards screen-right.
        return 45 + switch (facing) {
            case 3 -> 0; // south
            case 4 -> 90; // west
            case 5 -> -90; // east
            default -> 180; // north, also used for vertically facing controllers
        };
    }
}
