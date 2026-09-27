package com.gasai.ccapplied.botania;

import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.guisync.GuiSync;
import appeng.api.util.IConfigManager;
import appeng.api.config.Settings;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public final class ManaBusMenu extends UpgradeableMenu<ManaBusPart> {
    public static final MenuType<ManaBusMenu> TYPE = appeng.menu.implementations.MenuTypeBuilder.create(ManaBusMenu::new, ManaBusPart.class).buildUnregistered(com.gasai.ccapplied.CCApplied.makeId("mana_bus"));
    @GuiSync(10) public long buffered;
    @GuiSync(11) public long transferred;
    @GuiSync(12) public long limit;

    public ManaBusMenu(int id, Inventory inventory, ManaBusPart host) {
        super(TYPE, id, inventory, host);
    }
    @Override protected void loadSettingsFromHost(IConfigManager config) {
        setRedStoneMode(config.getSetting(Settings.REDSTONE_CONTROLLED));
    }
    @Override public void broadcastChanges() {
        if (isServerSide()) {
            buffered = getHost().bufferedMana();
            transferred = getHost().lastTransfer();
            limit = getHost().transferLimit();
        }
        super.broadcastChanges();
    }
}
