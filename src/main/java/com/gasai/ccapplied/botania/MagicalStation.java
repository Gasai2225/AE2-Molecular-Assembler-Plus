package com.gasai.ccapplied.botania;

public enum MagicalStation {
    POOL("pool"), RUNIC_ALTAR("runic_altar"), APOTHECARY("apothecary"),
    TERRA_PLATE("terra_plate"), ELVEN_TRADE("elven_trade"), BREWERY("brewery"), PURE_DAISY("pure_daisy");
    private final String id;
    MagicalStation(String id) { this.id = id; }
    public String id() { return id; }
    public boolean supportsSubstitutions() { return this == PURE_DAISY || this == APOTHECARY; }
    public int inputSlots() {
        return switch (this) {
            case POOL, PURE_DAISY -> 1;
            case ELVEN_TRADE -> 5;
            case TERRA_PLATE -> 3;
            case BREWERY -> 6;
            default -> 16;
        };
    }
    public boolean hasReagent() {
        return this == APOTHECARY || this == RUNIC_ALTAR || this == BREWERY;
    }
    public static MagicalStation fromId(String id) {
        for (var station : values()) if (station.id.equals(id)) return station;
        throw new IllegalArgumentException("Unknown magical station: " + id);
    }
}
