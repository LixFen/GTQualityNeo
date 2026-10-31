package com.plainston.gtqualityneo.integration.jei;

import com.plainston.gtqualityneo.client.MachineProgress;
import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.jei.GT6_JEI_RecipeCategory;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import gregapi.util.UT;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Keeps GT6's recipe ingredients and semantics while replacing the old NEI/GUI background layers. */
public final class MachineRecipeDisplay {
    private MachineRecipeDisplay() {}

    public static RecipeLayout layout(RecipeMap map) {
        int lines = 2 + (map.mShowVoltageAmperageInNEI ? 2 : 0) + (map.mCombinePower ? 0 : 1);
        if (UT.Code.stringValid(map.mNEISpecialValuePre) || UT.Code.stringValid(map.mNEISpecialValuePost)) lines += 2;
        return new RecipeLayout(map.mInputItemsCount, map.mOutputItemsCount, map.mInputFluidCount, map.mOutputFluidCount,
            lines * 10 + 4);
    }

    public static void setRecipe(RecipeMap map, IRecipeLayoutBuilder builder, Recipe recipe) {
        var layout = layout(map);
        for (int i = 0; i < map.mInputItemsCount; i++) {
            var slot = slot(builder, false, layout.slot(false, false, i), "input/item/" + i);
            var stack = recipe.getRepresentativeInput(i);
            if (stack == null) continue;
            slot.add(GT6_JEI_RecipeCategory.shown(stack));
            if (ST.size(stack) == 0)
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(
                    LH.Chat.BLINKING_CYAN + LH.tt("Does not get consumed in the process"))));
        }
        for (int i = 0; i < map.mOutputItemsCount; i++) {
            var slot = slot(builder, true, layout.slot(true, false, i), "output/item/" + i);
            var stack = recipe.getOutput(i);
            if (stack == null) continue;
            slot.add(stack);
            int chance = recipe.getOutputChance(i), maximum = recipe.getMaxChance(i);
            if (chance > 0 && chance != maximum) {
                long percent = UT.Code.units(chance, maximum, 10000, false);
                String text = "%d.%02d%%".formatted(percent / 100, percent % 100);
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                    stack.getCount() > 1 ? "gtqualityneo.jei.machine.chance_each" : "gtqualityneo.jei.machine.chance", text)));
            }
        }
        fluids(builder, layout, recipe.mFluidInputs, false, map.mUseBucketSizeIn);
        fluids(builder, layout, recipe.mFluidOutputs, true, map.mUseBucketSizeOut);
        if (recipe.mSpecialItems instanceof ItemStack special && ST.valid(special))
            slot(builder, false, layout.special(), "special").add(special.copy());
    }

    private static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, boolean output,
                                          RecipeLayout.Position position, String name) {
        return builder.addSlot(output ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT,
            position.x() + 1, position.y() + 1).setSlotName(name);
    }

    private static void fluids(IRecipeLayoutBuilder builder, RecipeLayout layout, FluidStack[] fluids,
                               boolean output, boolean bucketScale) {
        int count = output ? layout.outputFluids() : layout.inputFluids();
        var role = output ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT;
        for (int i = 0; i < count; i++) {
            var slot = slot(builder, output, layout.slot(output, true, i), (output ? "output" : "input") + "/fluid/" + i);
            if (i >= fluids.length || fluids[i] == null || fluids[i].isEmpty()) continue;
            // GT6's display stack carries amount, bucket scaling and the full material/fluid tooltip.
            var display = FL.display(fluids[i], true, false, bucketScale);
            if (display != null) slot.add(display);
            builder.addInvisibleIngredients(role).add(NeoForgeTypes.FLUID_STACK, fluids[i].copy());
        }
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

    public static void extras(RecipeMap map, Recipe recipe, IRecipeExtrasBuilder builder) {
        var layout = layout(map);
        builder.addText(info(map, recipe), RecipeLayout.WIDTH - 10, layout.infoHeight())
            .setPosition(5, layout.infoY()).setColor(0xff404040).setShadow(false).setLineSpacing(1);
    }

    public static List<FormattedText> info(RecipeMap map, Recipe recipe) {
        List<FormattedText> lines = new ArrayList<>();
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
