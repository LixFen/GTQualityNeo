// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import com.plainston.gtqualityneo.client.ui.MachineLayout;

/** A recipe template independent of the machine GUI texture and its pixel coordinates. */
public record RecipeLayout(int inputItems, int outputItems, int inputFluids, int outputFluids, int infoHeight,
                           boolean squareInputs) {
    public static final int WIDTH = 176, TOP = 4, ARROW_X = 78, INFO_HEIGHT = 54;
    public record Position(int x, int y) {}

    public RecipeLayout(int inputItems, int outputItems, int inputFluids, int outputFluids) {
        this(inputItems, outputItems, inputFluids, outputFluids, INFO_HEIGHT);
    }
    public RecipeLayout(int inputItems, int outputItems, int inputFluids, int outputFluids, int infoHeight) {
        this(inputItems, outputItems, inputFluids, outputFluids, infoHeight, false);
    }
    private MachineLayout machine() { return new MachineLayout(inputItems, outputItems, inputFluids, outputFluids, true); }
    public int contentHeight() { return machine().contentHeight(); }
    public int arrowY() { return machine().arrowY() - MachineLayout.TOP + TOP; }
    public int arrowY(boolean special) { return special ? arrowY() : TOP + (contentHeight() - 18) / 2; }
    public Position special() { return position(inputItems + outputItems); }
    public int infoY() { return TOP + contentHeight() + 8; }
    public int height() { return infoY() + infoHeight; }

    public Position slot(boolean output, boolean fluid, int index) {
        if (squareInputs && !output && !fluid)
            return new Position(ARROW_X - MachineLayout.GAP - 36 + index % 2 * 18,
                position(0).y() + index / 2 * 18);
        int offset = fluid ? inputItems + outputItems + 1 + (output ? inputFluids : 0) : output ? inputItems : 0;
        return position(offset + index);
    }

    private Position position(int index) {
        var position = machine().position(index);
        return new Position(position.x(), position.y() - MachineLayout.TOP + TOP);
    }
}
