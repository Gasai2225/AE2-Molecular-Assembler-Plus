package com.gasai.ccapplied.core.registry;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;

public final class CCOptionalMods {
    public static final String DRACONIC_EVOLUTION = "draconicevolution";

    private static final boolean DRACONIC_EVOLUTION_LOADED = isLoaded(DRACONIC_EVOLUTION);

    private CCOptionalMods() {
    }

    public static boolean isBotaniaLoaded() {
        return isLoaded("botania");
    }

    public static boolean isDraconicEvolutionLoaded() {
        return DRACONIC_EVOLUTION_LOADED;
    }

    private static boolean isLoaded(String modId) {
        try {
            var modList = ModList.get();
            if (modList != null) {
                return modList.isLoaded(modId);
            }
        } catch (Throwable ignored) {
        }

        try {
            var loadingModList = LoadingModList.get();
            return loadingModList != null && loadingModList.getModFileById(modId) != null;
        } catch (Throwable ignored) {
        }

        return false;
    }
}
