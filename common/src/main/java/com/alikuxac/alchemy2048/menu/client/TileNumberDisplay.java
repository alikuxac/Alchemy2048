package com.alikuxac.alchemy2048.menu.client;

public enum TileNumberDisplay {
    EXPONENTIAL("exponential", "gui.alchemy2048.display.exponential"),
    TIER_INDEX("tier_index", "gui.alchemy2048.display.tier_index"),
    NONE("none", "gui.alchemy2048.display.none");

    private final String id;
    private final String translationKey;

    TileNumberDisplay(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public TileNumberDisplay next() {
        TileNumberDisplay[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static TileNumberDisplay byId(String id) {
        for (TileNumberDisplay display : values()) {
            if (display.id.equalsIgnoreCase(id)) {
                return display;
            }
        }
        return EXPONENTIAL;
    }

    public String format(byte tier) {
        if (tier <= 0) return "";
        return switch (this) {
            case EXPONENTIAL -> String.valueOf(1 << tier);
            case TIER_INDEX -> "T" + tier;
            case NONE -> "";
        };
    }
}
