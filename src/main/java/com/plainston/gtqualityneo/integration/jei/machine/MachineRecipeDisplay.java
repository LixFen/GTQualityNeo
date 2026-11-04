// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import gregapi.util.UT;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

/** Keeps GT6's recipe ingredients and semantics while replacing the old NEI/GUI background layers. */
public final class MachineRecipeDisplay {
    private MachineRecipeDisplay() {}

    public static void setRecipe(RecipeMap map, IRecipeLayoutBuilder builder, Recipe recipe) {
        var layout = MachineRecipeVisuals.layout(map);
        for (int i = 0; i < map.mInputItemsCount; i++) {
            var slot = slot(builder, false, layout.slot(false, false, i), "input/item/" + i);
            var options = RecipeVariants.itemOptions(recipe, i);
            if (options.isEmpty()) continue;
            var stack = options.getFirst();
            for (var option : options) slot.add(ST.zerosize(option) ? ST.amount(option.getCount(), option) : option);
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
        fluids(builder, layout, recipe, false, map.mUseBucketSizeIn);
        fluids(builder, layout, recipe, true, map.mUseBucketSizeOut);
        if (recipe.mSpecialItems instanceof ItemStack special && ST.valid(special))
            slot(builder, false, layout.special(), "special").add(special.copy());
    }

    private static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, boolean output,
                                          RecipeLayout.Position position, String name) {
        return builder.addSlot(output ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT,
            position.x() + 1, position.y() + 1).setSlotName(name);
    }

    private static void fluids(IRecipeLayoutBuilder builder, RecipeLayout layout, Recipe recipe,
                               boolean output, boolean bucketScale) {
        FluidStack[] fluids = output ? recipe.mFluidOutputs : recipe.mFluidInputs;
        int count = output ? layout.outputFluids() : layout.inputFluids();
        var role = output ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT;
        for (int i = 0; i < count; i++) {
            var slot = slot(builder, output, layout.slot(output, true, i), (output ? "output" : "input") + "/fluid/" + i);
            if (i >= fluids.length || fluids[i] == null || fluids[i].isEmpty()) continue;
            var options = output ? List.of(fluids[i]) : RecipeVariants.inputOptions(recipe, i);
            for (var fluid : options) {
                // Each option keeps GT6's amount, bucket scaling and full material/fluid tooltip.
                var display = FL.display(fluid, true, false, bucketScale);
                if (display != null) slot.add(display);
                builder.addInvisibleIngredients(role).add(NeoForgeTypes.FLUID_STACK, fluid.copy());
            }
        }
    }

    public static void extras(RecipeMap map, Recipe recipe, IRecipeExtrasBuilder builder) {
        var layout = MachineRecipeVisuals.layout(map);
        builder.addText(List.<FormattedText>copyOf(MachineRecipeVisuals.info(map, recipe)), RecipeLayout.WIDTH - 10, layout.infoHeight())
            .setPosition(5, layout.infoY()).setColor(0xff404040).setShadow(false).setLineSpacing(1);
    }
}
