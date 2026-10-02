package com.plainston.gtqualityneo.qol;

import gregapi.data.CS;
import gregapi.tileentity.base.TileEntityBase09FacingSingle;
import gregapi.tileentity.connectors.MultiTileEntityPipeFluid;
import gregapi.tileentity.connectors.MultiTileEntityWireRedstone;
import gregapi.tileentity.connectors.TileEntityBase10ConnectorRendered;
import gregapi.cover.ICover;
import gregapi.cover.covers.AbstractCoverAttachmentTorch;
import gregapi.cover.covers.CoverPressureValve;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CollisionShapes {
    private CollisionShapes() {}

    public static VoxelShape hopper(TileEntityBase09FacingSingle hopper) {
        VoxelShape result = Shapes.or(box(0, 10, 0, 16, 16, 16), box(4, 4, 4, 12, 10, 12));
        result = Shapes.or(result, switch (hopper.mFacing) {
            case 0 -> box(6, 0, 6, 10, 4, 10);
            case 2 -> box(6, 4, 0, 10, 8, 4);
            case 3 -> box(6, 4, 12, 10, 8, 16);
            case 4 -> box(0, 4, 6, 4, 8, 10);
            case 5 -> box(12, 4, 6, 16, 8, 10);
            default -> Shapes.empty();
        });
        if (hopper.hasCovers()) for (byte side = 0; side < 6; side++) {
            ICover cover = hopper.mCovers.mBehaviours[side];
            if (cover != null) result = Shapes.or(result, coverBox(cover.getHolderBounds(side, hopper.mCovers)),
                coverBox(cover.getCoverBounds(side, hopper.mCovers)));
        }
        return result;
    }

    private static VoxelShape box(int x, int y, int z, int xx, int yy, int zz) {
        return Shapes.create(new AABB(x / 16.0, y / 16.0, z / 16.0, xx / 16.0, yy / 16.0, zz / 16.0));
    }

    private static VoxelShape coverBox(float[] b) {
        return b == null ? Shapes.empty() : Shapes.create(new AABB(b[0], b[1], b[2], b[3], b[4], b[5]));
    }

    public static float[] smallCoverBounds(TileEntityBase10ConnectorRendered connector) {
        if (!(connector instanceof MultiTileEntityPipeFluid || connector instanceof MultiTileEntityWireRedstone
            || connector instanceof gregapi.tileentity.connectors.MultiTileEntityWireRedstoneInsulated)
            || !connector.hasCovers() || connector.mFoam || connector.mDiameter >= 1) return null;
        for (ICover cover : connector.mCovers.mBehaviours)
            if (cover != null && !(cover instanceof AbstractCoverAttachmentTorch) && !(cover instanceof CoverPressureValve)) return null;
        float half = (1 - connector.mDiameter) / 2;
        float[] bounds = {connector.connected(CS.SIDE_X_NEG) ? 0 : half, connector.connected(CS.SIDE_Y_NEG) ? 0 : half,
            connector.connected(CS.SIDE_Z_NEG) ? 0 : half, connector.connected(CS.SIDE_X_POS) ? 1 : 1 - half,
            connector.connected(CS.SIDE_Y_POS) ? 1 : 1 - half, connector.connected(CS.SIDE_Z_POS) ? 1 : 1 - half};
        for (byte side = 0; side < 6; side++) {
            ICover cover = connector.mCovers.mBehaviours[side];
            if (cover != null) {
                include(bounds, cover.getHolderBounds(side, connector.mCovers));
                include(bounds, cover.getCoverBounds(side, connector.mCovers));
            }
        }
        return bounds;
    }

    private static void include(float[] bounds, float[] part) {
        if (part == null) return;
        for (int axis = 0; axis < 3; axis++) {
            bounds[axis] = Math.min(bounds[axis], part[axis]);
            bounds[axis + 3] = Math.max(bounds[axis + 3], part[axis + 3]);
        }
    }
}
