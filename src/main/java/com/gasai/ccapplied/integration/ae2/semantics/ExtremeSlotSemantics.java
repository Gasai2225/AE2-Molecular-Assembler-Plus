package com.gasai.ccapplied.integration.ae2.semantics;

/**
 * Слот семантики для экстремального терминала кодирования паттернов.
 * Определяет назначение различных слотов в GUI.
 */
public final class ExtremeSlotSemantics {
    
    private ExtremeSlotSemantics() {}
    
    // Семантики для экстремальной сетки 9x9
    public static final String ULTIMATE_CRAFTING_GRID = "ultimate_crafting_grid";
    public static final String ULTIMATE_CRAFTING_RESULT = "ultimate_crafting_result";
    
    // Семантики для паттернов
    public static final String BLANK_PATTERN = "blank_pattern";
    public static final String ENCODED_PATTERN = "encoded_pattern";
    
    // Семантики для кнопок
    public static final String ENCODE_BUTTON = "ext_encode";
    public static final String CLEAR_BUTTON = "ext_clear";
    
    // Семантики для ME сети
    public static final String ME_STORAGE_PANEL = "me_storage_panel";
    public static final String NETWORK_STATUS = "network_status";
}
