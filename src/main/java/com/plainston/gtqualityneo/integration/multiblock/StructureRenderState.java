package com.plainston.gtqualityneo.integration.multiblock;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.List;

public record StructureRenderState(List<Cell> cells, Vector3f center, Quaternionf rotation,
    int x0, int y0, int x1, int y1, float scale, Matrix3x2f pose,
    ScreenRectangle scissorArea, ScreenRectangle bounds) implements PictureInPictureRenderState {
    public record Cell(BlockPos pos, List<BakedQuad> quads) {}
}
