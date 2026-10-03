package com.gasai.ccapplied.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractTerminalPart;
import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.integration.ae2.api.IExtremePatternTerminalMenuHost;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DraconicPatternEncodingTerminalPart extends AbstractTerminalPart implements IExtremePatternTerminalMenuHost {
    public static final ResourceLocation MODEL_OFF = ResourceLocation.fromNamespaceAndPath(CCApplied.MODID,
            "part/draconic_pattern_encoding_terminal_off");
    public static final ResourceLocation MODEL_ON = ResourceLocation.fromNamespaceAndPath(CCApplied.MODID,
            "part/draconic_pattern_encoding_terminal_on");
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final ExtremePatternEncodingLogic logic = new ExtremePatternEncodingLogic(this);

    @Override
    public int maxEncodedInputAmount(int slot) {
        return slot == com.gasai.ccapplied.patterns.DraconicFusionPattern.OUTER_SLOTS ? 64 : 1;
    }

    public DraconicPatternEncodingTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        for (var is : this.logic.getBlankPatternInv()) drops.add(is);
        for (var is : this.logic.getEncodedPatternInv()) drops.add(is);
    }

    public void clearContent() {
        super.clearContent();
        this.logic.getBlankPatternInv().clear();
        this.logic.getEncodedPatternInv().clear();
    }

    public void readFromNBT(CompoundTag data, Provider provider) {
        super.readFromNBT(data, provider);
        logic.readFromNBT(data, provider);
    }

    public void writeToNBT(CompoundTag data, Provider provider) {
        super.writeToNBT(data, provider);
        logic.writeToNBT(data, provider);
    }

    public MenuType<?> getMenuType(Player p) {
        return com.gasai.ccapplied.core.registry.CCMenuTypes.DRACONIC_PATTERN_TERM.get();
    }

    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    public ExtremePatternEncodingLogic getLogic() {
        return logic;
    }

    public net.minecraft.world.level.Level getLevel() {
        var be = getHost().getBlockEntity();
        return be != null ? be.getLevel() : null;
    }

    public void markForSave() {
        getHost().markForSave();
    }

    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (player.level().isClientSide()) return true;
        var type = getMenuType(player);
        if (type != null) {
            appeng.menu.MenuOpener.open(type, player, appeng.menu.locator.MenuLocators.forPart(this));
            return true;
        }
        return false;
    }
}
