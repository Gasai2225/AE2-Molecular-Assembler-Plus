package com.gasai.ccapplied.botania;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MagicalPattern implements IPatternDetails {
    private final AEItemKey definition;
    private final MagicalStation station;
    private final PoolCatalyst catalyst;
    private final List<ItemStack> ingredients;
    private final MagicalRecipeResolver.Result recipe;
    private final IInput[] inputs;
    private final GenericStack[] outputs;

    public MagicalPattern(ItemStack encoded, MagicalStation station, PoolCatalyst catalyst,
            List<ItemStack> ingredients, MagicalRecipeResolver.Result recipe) {
        this(encoded, station, catalyst, ingredients, recipe, List.of());
    }
    public MagicalPattern(ItemStack encoded, MagicalStation station, PoolCatalyst catalyst,
            List<ItemStack> ingredients, MagicalRecipeResolver.Result recipe, List<ItemStack> alternatives) {
        this.definition = AEItemKey.of(encoded);
        this.station = station;
        this.catalyst = catalyst;
        this.ingredients = ingredients.stream().map(ItemStack::copy).toList();
        this.recipe = recipe;
        var required = count(ingredients);
        if (recipe.mana() > 0) required.put(ManaKey.INSTANCE, recipe.mana());
        inputs = required.entrySet().stream().map(entry -> new ExactInput(entry.getKey(), entry.getValue()))
                .toArray(IInput[]::new);
        if (station == MagicalStation.PURE_DAISY && ingredients.size() == 1 && !alternatives.isEmpty())
            inputs[0] = new AlternativeInput(alternatives.stream().map(AEItemKey::of).distinct().toList());
        outputs = count(recipe.outputs()).entrySet().stream().map(e -> new GenericStack(e.getKey(), e.getValue()))
                .toArray(GenericStack[]::new);
    }
    private static Map<AEKey, Long> count(List<ItemStack> stacks) {
        var result = new LinkedHashMap<AEKey, Long>();
        for (var stack : stacks) if (!stack.isEmpty()) result.merge(AEItemKey.of(stack), (long) stack.getCount(), Long::sum);
        return result;
    }
    public MagicalStation station() { return station; }
    public PoolCatalyst catalyst() { return catalyst; }
    public List<ItemStack> ingredients() { return ingredients.stream().map(ItemStack::copy).toList(); }
    public MagicalRecipeResolver.Result recipe() { return recipe; }
    @Override public AEItemKey getDefinition() { return definition; }
    @Override public IInput[] getInputs() { return inputs; }
    @Override public List<GenericStack> getOutputs() { return List.of(outputs); }
    @Override public boolean supportsPushInputsToExternalInventory() { return false; }
    @Override public boolean equals(Object other) { return other instanceof MagicalPattern p && definition.equals(p.definition); }
    @Override public int hashCode() { return definition.hashCode(); }

    private record AlternativeInput(List<AEItemKey> keys) implements IInput {
        @Override public GenericStack[] getPossibleInputs() {
            return keys.stream().map(key -> new GenericStack(key, 1)).toArray(GenericStack[]::new);
        }
        @Override public long getMultiplier() { return 1; }
        @Override public boolean isValid(AEKey input, Level level) { return keys.contains(input); }
        @Override public AEKey getRemainingKey(AEKey template) { return null; }
    }

    private record ExactInput(AEKey key, long amount) implements IInput {
        @Override public GenericStack[] getPossibleInputs() { return new GenericStack[] {new GenericStack(key, 1)}; }
        @Override public long getMultiplier() { return amount; }
        @Override public boolean isValid(AEKey input, Level level) { return key.equals(input); }
        // Returned runes/catalysts are explicit recipe outputs, not implicit crafting remainders.
        @Override public AEKey getRemainingKey(AEKey template) { return null; }
    }
}
