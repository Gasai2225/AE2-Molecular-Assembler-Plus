package com.gasai.ccapplied.botania;

/** Coordinates inside the 126 x 154 station panel, shared with layout regression tests. */
public final class MagicalStationLayout {
    public static final int WIDTH = 126, HEIGHT = 154, OUTPUTS_PER_PAGE = 6;
    public record Point(int x, int y) {}
    private MagicalStationLayout() {}

    public static Point input(MagicalStation station, int index) {
        if (index < 0 || index >= station.inputSlots()) return null;
        return switch (station) {
            case POOL, PURE_DAISY -> new Point(16, 70);
            case ELVEN_TRADE -> new Point(new int[] {16, 34, 52, 34, 34}[index],
                    new int[] {70, 70, 70, 52, 88}[index]);
            case TERRA_PLATE -> new Point(22 + index * 24, 86);
            case BREWERY -> new Point(new int[] {22, 44, 66, 66, 44, 22}[index],
                    new int[] {38, 26, 38, 74, 86, 74}[index]);
            case APOTHECARY -> {
                // Sixteen ingredients surround the seed, like petals around the basin.
                int[][] ring = {{16,16},{38,16},{60,16},{82,16},{104,16},{104,38},{104,60},{104,82},
                        {104,104},{82,104},{60,104},{38,104},{16,104},{16,82},{16,60},{16,38}};
                yield new Point(ring[index][0], ring[index][1]);
            }
            case RUNIC_ALTAR -> new Point(6 + index % 4 * 18, 18 + index / 4 * 18);
        };
    }

    public static Point reagent(MagicalStation station) {
        return switch (station) {
            case RUNIC_ALTAR -> new Point(102, 48);
            case APOTHECARY -> new Point(60, 60);
            case BREWERY -> new Point(44, 56);
            default -> null;
        };
    }

    public static Point output(MagicalStation station, int index) {
        return switch (station) {
            case POOL, PURE_DAISY, ELVEN_TRADE -> new Point(100, 70);
            case TERRA_PLATE -> new Point(46, 22);
            case BREWERY -> new Point(102, 56);
            case APOTHECARY -> new Point(60, 130);
            case RUNIC_ALTAR -> new Point(48 + new int[] {0, -18, 18}[index % 3], 112 + index / 3 * 18);
        };
    }

    public static Point emblem(MagicalStation station) {
        return switch (station) {
            case POOL, PURE_DAISY -> new Point(58, 42);
            case ELVEN_TRADE -> new Point(58, 24);
            case TERRA_PLATE -> new Point(96, 60);
            case BREWERY -> new Point(44, 116);
            case APOTHECARY -> new Point(60, 38);
            case RUNIC_ALTAR -> new Point(102, 20);
        };
    }

    public static boolean multipleOutputs(MagicalStation station) {
        return station == MagicalStation.RUNIC_ALTAR || station == MagicalStation.ELVEN_TRADE;
    }
    public static int outputsPerPage(MagicalStation station) {
        return station == MagicalStation.RUNIC_ALTAR ? OUTPUTS_PER_PAGE : 1;
    }
}
