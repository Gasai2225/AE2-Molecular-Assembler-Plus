package com.gasai.ccapplied.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.MenuTypeBuilder.MenuFactory;
import appeng.menu.locator.MenuLocators;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;

/** Creates addon menus without adding them to AE2's own registration queue. */
public final class CCMenuBuilder {
    private CCMenuBuilder() {
    }

    public static <M extends AEBaseMenu, H> MenuType<M> create(MenuFactory<M, H> factory, Class<H> hostType) {
        MenuType<M> type = IForgeMenuType.create((id, inventory, buffer) -> {
            var locator = MenuLocators.readFromPacket(buffer);
            H host = locator.locate(inventory.player, hostType);
            if (host == null) {
                throw new IllegalStateException("Missing menu host at " + locator);
            }
            M menu = factory.create(id, inventory, host);
            menu.setReturnedFromSubScreen(buffer.readBoolean());
            return menu;
        });
        MenuOpener.addOpener(type, (player, locator, fromSubMenu) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return false;
            }
            H host = locator.locate(player, hostType);
            if (host == null) {
                return false;
            }
            Component title = host instanceof Nameable nameable && nameable.hasCustomName()
                    ? nameable.getCustomName() : Component.empty();
            var provider = new SimpleMenuProvider((id, inventory, ignored) -> {
                M menu = factory.create(id, inventory, host);
                menu.setLocator(locator);
                return menu;
            }, title);
            NetworkHooks.openScreen(serverPlayer, provider, buffer -> {
                MenuLocators.writeToPacket(buffer, locator);
                buffer.writeBoolean(fromSubMenu);
            });
            return true;
        });
        return type;
    }
}
