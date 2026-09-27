package com.gasai.ccapplied.botania;

import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.*;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class MagicalTerminalMenu extends MEStorageMenu {
    public static final MenuType<MagicalTerminalMenu> TYPE = appeng.menu.implementations.MenuTypeBuilder.create(MagicalTerminalMenu::new, MagicalTerminalPart.class).buildUnregistered(com.gasai.ccapplied.CCApplied.makeId("magical_terminal"));
    public static final SlotSemantic[] INGREDIENTS = semantics("MAGIC_INPUT_", 16);
    public static final SlotSemantic REAGENT = SlotSemantics.register("MAGIC_REAGENT", false);
    public static final SlotSemantic[] RESULTS = semantics("MAGIC_OUTPUT_", 18);
    private static SlotSemantic[] semantics(String prefix, int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> SlotSemantics.register(prefix + i, false))
                .toArray(SlotSemantic[]::new);
    }
    public java.util.List<net.minecraft.world.inventory.Slot> ingredientSlots() {
        return java.util.Arrays.stream(INGREDIENTS).flatMap(s -> getSlots(s).stream()).toList();
    }
    public java.util.List<net.minecraft.world.inventory.Slot> resultSlots() {
        return java.util.Arrays.stream(RESULTS).flatMap(s -> getSlots(s).stream()).toList();
    }
    private final MagicalTerminalPart host;
    private final AppEngInternalInventory preview = new AppEngInternalInventory(18);
    private int lastRevision = -1;
    @GuiSync(40) public int station;
    @GuiSync(41) public int catalyst;
    @GuiSync(42) public long mana;
    @GuiSync(43) public boolean valid;
    @GuiSync(44) public boolean substitutions;
    @GuiSync(45) public int resultCount;

    public MagicalTerminalMenu(int id, Inventory playerInventory, MagicalTerminalPart host) {
        super(TYPE, id, playerInventory, host, true);
        this.host = host;
        for (int i = 0; i < 17; i++) addSlot(new FakeSlot(host.ghosts, i) {
            @Override public boolean isActive() { return acceptsInput(getSlotIndex()); }
            @Override public boolean mayPlace(ItemStack stack) { return isActive(); }
            @Override public void set(ItemStack stack) {
                if (isActive()) super.set(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            }
        }, i == 16 ? REAGENT : INGREDIENTS[i]);
        for (int i = 0; i < preview.size(); i++) addSlot(new AppEngSlot(preview, i) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        }, RESULTS[i]);
        addSlot(new AppEngSlot(host.patterns, 0) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(BotaniaContent.MAGICAL_PATTERN.get()) && !MagicalPatternItem.isEncoded(stack);
            }
        }, SlotSemantics.BLANK_PATTERN);
        addSlot(new OutputSlot(host.patterns, 1, null), SlotSemantics.ENCODED_PATTERN);
        registerClientAction("magicStation", this::cycleStation);
        registerClientAction("magicSelectStation", Integer.class, this::selectStation);
        registerClientAction("magicCatalyst", this::cycleCatalyst);
        registerClientAction("magicSelectCatalyst", Integer.class, this::selectCatalyst);
        registerClientAction("magicSubstitutions", Boolean.class, this::setSubstitutions);
        registerClientAction("magicClear", this::clearInputs);
        registerClientAction("magicEncode", this::encode);
        registerClientAction("magicJeiRecipe", String.class, this::applyJeiRecipe);
    }
    public void cycleStation() {
        if (isClientSide()) sendClientAction("magicStation");
        else host.cycleStation();
    }
    public void selectStation(int index) {
        if (index < 0 || index >= MagicalStation.values().length) return;
        if (isClientSide()) sendClientAction("magicSelectStation", index);
        else host.selectStation(MagicalStation.values()[index]);
    }
    public void cycleCatalyst() {
        if (isClientSide()) sendClientAction("magicCatalyst");
        else host.cycleCatalyst();
    }
    public MagicalStation selectedStation() {
        return isServerSide() ? host.station() : MagicalStation.values()[Math.floorMod(station, MagicalStation.values().length)];
    }
    private boolean acceptsInput(int slot) {
        var selected = selectedStation();
        return slot == 16 ? selected.hasReagent() : slot < selected.inputSlots();
    }
    public void selectCatalyst(int index) {
        if (index < 0 || index >= PoolCatalyst.values().length) return;
        if (isClientSide()) sendClientAction("magicSelectCatalyst", index);
        else host.selectCatalyst(PoolCatalyst.values()[index]);
    }
    public void setSubstitutions(boolean enabled) {
        if (isClientSide()) sendClientAction("magicSubstitutions", enabled);
        else host.setSubstitutions(enabled);
    }
    public void clearInputs() {
        if (isClientSide()) sendClientAction("magicClear");
        else host.ghosts.clear();
    }
    public void applyJeiRecipe(String recipeId) {
        if (recipeId == null || recipeId.length() > 256) return;
        var id = net.minecraft.resources.ResourceLocation.tryParse(recipeId);
        var transfer = MagicalRecipeTransfer.prepare(host.getLevel(), id);
        if (transfer == null) return;
        if (isClientSide()) { sendClientAction("magicJeiRecipe", recipeId); return; }
        if (!host.isActive()) return;
        host.selectStation(transfer.station());
        host.selectCatalyst(transfer.catalyst());
        host.ghosts.clear();
        var inputs = transfer.inputs();
        int first = transfer.station() == MagicalStation.BREWERY ? 1 : 0;
        int end = inputs.size();
        if (transfer.station().hasReagent()) {
            int reagent = first == 1 ? 0 : --end;
            host.ghosts.setItemDirect(16, inputs.get(reagent).copy());
        }
        for (int i = first; i < end; i++) host.ghosts.setItemDirect(i - first, inputs.get(i).copy());
    }
    public void encode() {
        if (isClientSide()) { sendClientAction("magicEncode"); return; }
        var blank = host.patterns.getStackInSlot(0);
        if (!host.isActive() || !host.patterns.getStackInSlot(1).isEmpty()
                || !blank.is(BotaniaContent.MAGICAL_PATTERN.get()) || MagicalPatternItem.isEncoded(blank)) return;
        var inputs = host.inputs();
        var result = MagicalRecipeResolver.find(host.getLevel(), host.station(), host.catalyst(), inputs);
        if (result == null) return;
        var encoded = MagicalPatternItem.encode(host.getLevel(), host.station(), host.catalyst(), result.recipeId(), inputs, host.substitutions());
        if (encoded.isEmpty()) return;
        host.patterns.extractItem(0, 1, false);
        host.patterns.setItemDirect(1, encoded);
    }
    @Override public void broadcastChanges() {
        if (isServerSide() && lastRevision != host.revision()) {
            lastRevision = host.revision();
            station = host.station().ordinal();
            catalyst = host.catalyst().ordinal();
            substitutions = host.substitutions();
            var inputs = host.inputs();
            var recipe = MagicalRecipeResolver.find(host.getLevel(), host.station(), host.catalyst(), inputs);
            valid = recipe != null;
            resultCount = valid ? Math.min(preview.size(), recipe.outputs().size()) : 0;
            mana = valid ? recipe.mana() : 0;
            preview.clear();
            if (valid) for (int i = 0; i < Math.min(preview.size(), recipe.outputs().size()); i++)
                preview.setItemDirect(i, recipe.outputs().get(i).copy());
        }
        super.broadcastChanges();
    }
}
