package com.plainston.gtqualityneo.client;

import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureMulti;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;

public final class PipeFlowTexture {

    // World directions at each face's top, bottom, left and right texture border.
    // NeoForge maps the bottom face directly to X/Z UVs.
    private static final byte[][] BORDER_SIDES = { { 2, 3, 4, 5 }, { 2, 3, 4, 5 }, { 1, 0, 5, 4 }, { 1, 0, 4, 5 },
        { 1, 0, 2, 3 }, { 1, 0, 3, 2 } };

    private static final IIconContainer[] ARROWS = { new Textures.BlockIcons.CustomIcon("gtqualityneo:block/pipe/flow_up"),
        new Textures.BlockIcons.CustomIcon("gtqualityneo:block/pipe/flow_down"),
        new Textures.BlockIcons.CustomIcon("gtqualityneo:block/pipe/flow_left"),
        new Textures.BlockIcons.CustomIcon("gtqualityneo:block/pipe/flow_right") };

    private PipeFlowTexture() {}

    public static ITexture withArrows(ITexture base, byte face, int disabledInputs) {
        ITexture[] layers = new ITexture[5];
        layers[0] = base;
        for (int border = 0; border < 4; border++) {
            if ((disabledInputs & (1 << BORDER_SIDES[face][border])) != 0)
                layers[border + 1] = new BlockTextureDefault(ARROWS[border], new short[] {255,255,255,255}, true, false, false, true);
        }
        return new BlockTextureMulti(layers);
    }

}
