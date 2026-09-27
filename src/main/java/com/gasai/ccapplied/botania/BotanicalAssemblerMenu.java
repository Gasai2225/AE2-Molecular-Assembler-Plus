package com.gasai.ccapplied.botania;

import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.AppEngSlot;
import appeng.util.inv.AppEngInternalInventory;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BotanicalAssemblerMenu extends UpgradeableMenu<BotanicalAssemblerBlockEntity> {
    public static final MenuType<BotanicalAssemblerMenu> TYPE = MenuTypeBuilder
            .create(BotanicalAssemblerMenu::new, BotanicalAssemblerBlockEntity.class)
            .buildUnregistered(com.gasai.ccapplied.CCApplied.makeId("botanical_assembler"));
    public static final SlotSemantic[] INPUTS = semantics("BOTANICAL_INPUT_");
    public static final SlotSemantic[] OUTPUTS = semantics("BOTANICAL_OUTPUT_");
    private static final SlotSemantic JOB_PATTERN = SlotSemantics.register("BOTANICAL_JOB_PATTERN", false);
    private AppEngInternalInventory displayedPlan;
    @GuiSync(20) public int progress;
    @GuiSync(21) public long mana;
    @GuiSync(22) public int status;
    @GuiSync(23) public long requiredMana;
    @GuiSync(24) public int ingredientCount;
    @GuiSync(25) public int resultCount;

    private static SlotSemantic[] semantics(String prefix) {
        return java.util.stream.IntStream.range(0, 18)
                .mapToObj(i -> SlotSemantics.register(prefix + i, false)).toArray(SlotSemantic[]::new);
    }
    public BotanicalAssemblerMenu(int id, Inventory player, BotanicalAssemblerBlockEntity host) {
        super(TYPE, id, player, host);
    }
    public MagicalStation station() { return getHost().station(); }
    public List<Slot> inputSlots() { return java.util.Arrays.stream(INPUTS).flatMap(s -> getSlots(s).stream()).toList(); }
    public List<Slot> outputSlots() { return java.util.Arrays.stream(OUTPUTS).flatMap(s -> getSlots(s).stream()).toList(); }
    public MagicalPattern displayedPattern() {
        return getHost().decodeTemplate(displayedPlan.getStackInSlot(0));
    }
    public boolean inputEnabled(int slot) {
        var pattern = displayedPattern();
        return pattern != null && slot < pattern.ingredients().size();
    }
    public boolean manualMode() { return !getHost().templateInventory().isEmpty(); }

    @Override protected void setupConfig() {
        displayedPlan = new AppEngInternalInventory(1);
        addSlot(new AppEngSlot(displayedPlan, 0) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        }, JOB_PATTERN);
        var inputs = getHost().menuInputs();
        var outputs = getHost().menuOutputs();
        for (int i = 0; i < 18; i++) {
            final int inputIndex = i;
            addSlot(new AppEngSlot(inputs, i) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return status != 2 && getHost().isValidManualInput(inputIndex, stack);
                }
                @Override public boolean mayPickup(Player player) {
                    return status != 2 && getHost().manualEditable();
                }
            }, INPUTS[i]);
            addSlot(new AppEngSlot(outputs, i) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) {
                    return status != 2 && getHost().activePattern().isEmpty();
                }
                @Override public ItemStack getDisplayStack() {
                    var real = getItem();
                    var pattern = displayedPattern();
                    return !real.isEmpty() || pattern == null || inputIndex >= pattern.recipe().outputs().size()
                            ? real : pattern.recipe().outputs().get(inputIndex);
                }
            }, OUTPUTS[i]);
        }
        addSlot(new AppEngSlot(getHost().templateInventory(), 0) {
            @Override public boolean mayPlace(ItemStack stack) {
                return status != 2 && getHost().templateInventory().isItemValid(0, stack);
            }
            @Override public boolean mayPickup(Player player) {
                return status != 2 && getHost().canChangeTemplate();
            }
            @Override public ItemStack getDisplayStack() {
                return getItem().isEmpty() ? displayedPlan.getStackInSlot(0) : getItem();
            }
        }, SlotSemantics.ENCODED_PATTERN);
    }

    @Override public void broadcastChanges() {
        if (isServerSide()) {
            progress = getHost().craftProgress();
            mana = getHost().reservedMana();
            status = getHost().status();
            var pattern = getHost().displayedPattern();
            displayedPlan.setItemDirect(0, pattern == null ? ItemStack.EMPTY : pattern.getDefinition().toStack());
            requiredMana = pattern == null ? 0 : pattern.recipe().mana();
            ingredientCount = pattern == null ? 0 : pattern.ingredients().size();
            resultCount = pattern == null ? 0 : pattern.recipe().outputs().size();
            for (int i = 0; i < 18; i++)
                if (!getHost().menuOutputs().getStackInSlot(i).isEmpty()) resultCount = Math.max(resultCount, i + 1);
        }
        super.broadcastChanges();
    }
}
