// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import com.plainston.gtqualityneo.client.ui.MachineProgress;
import gregapi.data.LH;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared machine recipe layout, texture and stats for both recipe browsers. */
public final class MachineRecipeVisuals {
    private MachineRecipeVisuals() {}

    public static RecipeLayout layout(RecipeMap map) {
        int lines = 2 + (map.mShowVoltageAmperageInNEI ? 2 : 0) + (map.mCombinePower ? 0 : 1);
        if (UT.Code.stringValid(map.mNEISpecialValuePre) || UT.Code.stringValid(map.mNEISpecialValuePost)) lines += 2;
        return new RecipeLayout(map.mInputItemsCount, map.mOutputItemsCount, map.mInputFluidCount, map.mOutputFluidCount,
            lines * 10 + 4);
    }

    public static void draw(RecipeMap map, Recipe recipe, GuiGraphicsExtractor graphics) {
        var layout = layout(map);
        var texture = Identifier.parse(map.mGUIPath.toLowerCase(Locale.ROOT));
        for (boolean output : new boolean[]{false, true}) {
            int items = output ? map.mOutputItemsCount : map.mInputItemsCount;
            int fluids = output ? map.mOutputFluidCount : map.mInputFluidCount;
            for (int i = 0; i < items; i++)
                slotTexture(graphics, texture, layout.slot(output, false, i), RecipeTextures.item(output, items, fluids, i));
            for (int i = 0; i < fluids; i++)
                slotTexture(graphics, texture, layout.slot(output, true, i), RecipeTextures.fluid(output, i));
        }
        boolean special = recipe.mSpecialItems instanceof ItemStack stack && ST.valid(stack);
        if (special) slotTexture(graphics, texture, layout.special(), new RecipeTextures.Source(79, 42));
        int y = layout.arrowY(special);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, RecipeLayout.ARROW_X, y, 78, 24, 20, 18, 256, 256);
        int phase = (int) (System.nanoTime() / 1_000_000L % 2000 * Short.MAX_VALUE / 1999);
        var frame = MachineProgress.frame(phase, map.mProgressBarDirection, Byte.toUnsignedInt(map.mProgressBarAmount));
        if (frame.width() > 0 && frame.height() > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, RecipeLayout.ARROW_X + frame.x(), y + frame.y(),
                176 + frame.x(), frame.y(), frame.width(), frame.height(), 256, 256);
        graphics.fill(5, layout.infoY() - 4, RecipeLayout.WIDTH - 5, layout.infoY() - 3, 0x30707070);
    }

    private static void slotTexture(GuiGraphicsExtractor graphics, Identifier texture, RecipeLayout.Position position,
                                    RecipeTextures.Source source) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, position.x(), position.y(), source.x(), source.y(), 18, 18, 256, 256);
    }

    public static List<Component> info(RecipeMap map, Recipe recipe) {
        List<Component> lines = new ArrayList<>();
        long duration = recipe.mDuration;
        if (duration > 0) lines.add(Component.literal(LH.tt("Time: ") + (duration < 1200
            ? UT.Code.makeString(duration) + LH.tt(" ticks") : duration < 36000
            ? UT.Code.makeString(duration / 20) + LH.tt(" secs") : UT.Code.makeString(duration / 1200) + LH.tt(" mins"))));
        long energy = recipe.mEUt;
        if (energy == 0) {
            if (map.mShowVoltageAmperageInNEI) lines.add(Component.literal(LH.tt("Tier: unspecified")));
        } else {
            long amount = Math.abs(energy);
            lines.add(Component.literal(LH.tt(energy > 0 ? "Costs: " : "Gain: ") + UT.Code.makeString(amount * duration) + " GU"));
            if (!map.mCombinePower && (map.mShowVoltageAmperageInNEI || amount != 1))
                lines.add(Component.literal(LH.tt(energy > 0 ? "Usage: " : "Output: ") + UT.Code.makeString(amount) + " GU/t"));
            if (map.mShowVoltageAmperageInNEI) {
                lines.add(Component.literal(LH.tt("Tier: ") + UT.Code.makeString(amount / map.mPower) + " GU"));
                lines.add(Component.literal(LH.tt("Power: ") + UT.Code.makeString(map.mPower)));
            }
        }
        if (UT.Code.stringValid(map.mNEISpecialValuePre) || UT.Code.stringValid(map.mNEISpecialValuePost))
            lines.add(Component.literal(map.mNEISpecialValuePre + UT.Code.makeString(recipe.mSpecialValue * map.mNEISpecialValueMultiplier)
                + map.mNEISpecialValuePost));
        return lines;
    }
}
