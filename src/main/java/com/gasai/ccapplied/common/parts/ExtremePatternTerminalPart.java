package com.gasai.ccapplied.common.parts;

import com.gasai.ccapplied.CCApplied;
import com.gasai.ccapplied.common.logic.ExtremePatternEncodingLogic;
import com.gasai.ccapplied.common.menu.ExtremePatternTerminalMenu;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import appeng.parts.reporting.AbstractTerminalPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;

/**
 * Part экстремального терминала кодирования паттернов
 * Основан на PatternEncodingTerminalPart из AE2
 */
public class ExtremePatternTerminalPart extends AbstractTerminalPart
        implements ExtremePatternEncodingLogic.IExtremePatternTerminalLogicHost, IExtremePatternTerminalMenuHost {

    @PartModels
    public static final ResourceLocation MODEL_BASE_OFF = new ResourceLocation(CCApplied.MODID,
            "part/extreme_pattern_terminal_base");

    // Используем стандартные модели от PatternEncodingTerminalPart
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE_OFF, 
            PatternEncodingTerminalPart.MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE_OFF,
            PatternEncodingTerminalPart.MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE_OFF,
            PatternEncodingTerminalPart.MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final ExtremePatternEncodingLogic logic = new ExtremePatternEncodingLogic(this);

    public ExtremePatternTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
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
    public MenuType<?> getMenuType(Player p) {
        return ExtremePatternTerminalMenu.TYPE;
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public ExtremePatternEncodingLogic getLogic() {
        return logic;
    }

    @Override
    public void markForSave() {
        getHost().markForSave();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.of(() -> logic.getBlankPatternInv().toItemHandler()).cast();
        }
        return super.getCapability(cap);
    }

}

