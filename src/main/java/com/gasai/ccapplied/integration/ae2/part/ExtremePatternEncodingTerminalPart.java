package com.gasai.ccapplied.integration.ae2.part;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractTerminalPart;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;


public class ExtremePatternEncodingTerminalPart extends AbstractTerminalPart
        implements com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost{

    @PartModels
    public static final ResourceLocation MODEL_OFF = ResourceLocation.fromNamespaceAndPath(AppEng.MOD_ID,
            "part/pattern_encoding_terminal_off"); // можно сделать свой ассет позже
    @PartModels
    public static final ResourceLocation MODEL_ON = ResourceLocation.fromNamespaceAndPath(AppEng.MOD_ID,
            "part/pattern_encoding_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final ExtremePatternEncodingLogic logic = new ExtremePatternEncodingLogic(this);

    public ExtremePatternEncodingTerminalPart(IPartItem<?> partItem) {
        super(partItem);
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] ctor created part {}", partItem.asItem());
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] addAdditionalDrops wrenched={}", wrenched);
        for (var is : this.logic.getBlankPatternInv()) {
            drops.add(is);
        }
        for (var is : this.logic.getEncodedPatternInv()) {
            drops.add(is);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.logic.getBlankPatternInv().clear();
        this.logic.getEncodedPatternInv().clear();
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        logic.readFromNBT(data);
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        logic.writeToNBT(data);
    }

    @Override
    public MenuType<?> getMenuType(net.minecraft.world.entity.player.Player p) {
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] getMenuType for {} return {}", p.getGameProfile().getName(), com.gasai.ccapplied.core.registry.CCMenuTypes.EXTREME_PATTERN_TERM.getId());
        return com.gasai.ccapplied.core.registry.CCMenuTypes.EXTREME_PATTERN_TERM.get();
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public ExtremePatternEncodingLogic getLogic() { return logic; }

    @Override
    public net.minecraft.world.level.Level getLevel() {
    var be = getHost().getBlockEntity();
    com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] getLevel host={} beLevel={}", getHost(), be != null ? be.getLevel() : null);
    return be != null ? be.getLevel() : null;
    }

    @Override
    public void markForSave() {
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] markForSave");
        getHost().markForSave();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap) {
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] getCapability {}", cap);
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.of(() -> logic.getBlankPatternInv().toItemHandler()).cast();
        }
        return super.getCapability(cap);
    }
    
    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] onPartActivate by {} on {}", player.getGameProfile().getName(), player.level().isClientSide() ? "CLIENT" : "SERVER");
        if (player.level().isClientSide()) {
            com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] Client-side activation, returning true");
            return true;
        }
        var type = getMenuType(player);
        if (type != null) {
            com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] Opening menu with type: {}", type);
            try {
                appeng.menu.MenuOpener.open(type, player, appeng.menu.locator.MenuLocators.forPart(this));
                com.gasai.ccapplied.CCApplied.LOG.info("[ExtremePart] Menu opened successfully");
                return true;
            } catch (Exception e) {
                com.gasai.ccapplied.CCApplied.LOG.error("[ExtremePart] Failed to open menu", e);
                return false;
            }
        }
        com.gasai.ccapplied.CCApplied.LOG.warn("[ExtremePart] MenuType is null");
        return false;
    }

}
