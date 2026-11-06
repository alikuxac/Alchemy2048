package com.alikuxac.alchemy2048.menu.client;

public enum GuiTheme {
    DARK(
            "dark",
            "gui.alchemy2048.theme.dark",
            0xFF1E2024,
            0xFF2D3239,
            0xFF141619,
            0xFF2A2E33,
            0xFFFFD700,
            0xFFAAAAAA,
            0xFF141619,
            0xFF2A2E33,
            0xFFFFFFFF,
            0xFF141619,
            0xFF00F0FF,
            new int[]{
                    0xFF2A2A2A,
                    0xFF8C7B70,
                    0xFFA69385,
                    0xFFD98244,
                    0xFFE66B38,
                    0xFFE64E33,
                    0xFFE63321,
                    0xFFE8C846,
                    0xFFE8C22E,
                    0xFFE8BD15,
                    0xFFC29B0E,
                    0xFF9E7C00
            }
    ),
    STEAMPUNK(
            "steampunk",
            "gui.alchemy2048.theme.steampunk",
            0xFF2D2319,
            0xFF6B533E,
            0xFF1B1510,
            0xFF4A3828,
            0xFFE5A65D,
            0xFFC69E7B,
            0xFF32261A,
            0xFF251A10,
            0xFFFFF3E0,
            0xFF19120B,
            0xFFD97724,
            new int[]{
                    0xFF32261A,
                    0xFF8D6E63,
                    0xFFA1887F,
                    0xFFBCAAA4,
                    0xFFD7CCC8,
                    0xFFCD853F,
                    0xFFD2691E,
                    0xFFB8860B,
                    0xFFDAA520,
                    0xFFFFD700,
                    0xFFFF8C00,
                    0xFFFF4500
            }
    ),
    MINIMAL(
            "minimal",
            "gui.alchemy2048.theme.minimal",
            0xFF1E222A,
            0xFF3E4451,
            0xFF16181D,
            0xFF282C34,
            0xFF61AFEF,
            0xFFABB2BF,
            0xFF21252B,
            0xFF1B1D23,
            0xFFFFFFFF,
            0xFF121418,
            0xFF4FA6ED,
            new int[]{
                    0xFF21252B,
                    0xFF3F4451,
                    0xFF4B5263,
                    0xFF5C6370,
                    0xFF61AFEF,
                    0xFF528BFF,
                    0xFF98C379,
                    0xFFE5C07B,
                    0xFFE06C75,
                    0xFFC678DD,
                    0xFFBE5046,
                    0xFF00E676
            }
    ),
    RETRO_2048(
            "retro_2048",
            "gui.alchemy2048.theme.retro_2048",
            0xFFFAF8EF,
            0xFFBBADA0,
            0xFFBBADA0,
            0xFF9E8E80,
            0xFF776E65,
            0xFF8F7A66,
            0xFFCDC1B4,
            0xFFB5A799,
            0xFFFFFFFF,
            0xFFCDC1B4,
            0xFFE76F51,
            new int[]{
                    0xFFCDC1B4,
                    0xFFEEE4DA,
                    0xFFEDE0C8,
                    0xFFF2B179,
                    0xFFF59563,
                    0xFFF67C5F,
                    0xFFF65E3B,
                    0xFFEDCF72,
                    0xFFEDCC61,
                    0xFFEDC850,
                    0xFFEDC53F,
                    0xFFEDC22E
            }
    );

    private final String id;
    private final String displayNameKey;
    private final int bgColor;
    private final int bgOutlineColor;
    private final int boardBgColor;
    private final int boardOutlineColor;
    private final int titleColor;
    private final int subTitleColor;
    private final int emptyCellColor;
    private final int emptyCellBorder;
    private final int defaultTextColor;
    private final int energyBarBg;
    private final int energyBarFill;
    private final int[] tierColors;

    GuiTheme(
            String id,
            String displayNameKey,
            int bgColor,
            int bgOutlineColor,
            int boardBgColor,
            int boardOutlineColor,
            int titleColor,
            int subTitleColor,
            int emptyCellColor,
            int emptyCellBorder,
            int defaultTextColor,
            int energyBarBg,
            int energyBarFill,
            int[] tierColors
    ) {
        this.id = id;
        this.displayNameKey = displayNameKey;
        this.bgColor = bgColor;
        this.bgOutlineColor = bgOutlineColor;
        this.boardBgColor = boardBgColor;
        this.boardOutlineColor = boardOutlineColor;
        this.titleColor = titleColor;
        this.subTitleColor = subTitleColor;
        this.emptyCellColor = emptyCellColor;
        this.emptyCellBorder = emptyCellBorder;
        this.defaultTextColor = defaultTextColor;
        this.energyBarBg = energyBarBg;
        this.energyBarFill = energyBarFill;
        this.tierColors = tierColors;
    }

    public String getId() {
        return id;
    }

    public String getDisplayNameKey() {
        return displayNameKey;
    }

    public int getBgColor() {
        return bgColor;
    }

    public int getBgOutlineColor() {
        return bgOutlineColor;
    }

    public int getBoardBgColor() {
        return boardBgColor;
    }

    public int getBoardOutlineColor() {
        return boardOutlineColor;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public int getSubTitleColor() {
        return subTitleColor;
    }

    public int getEmptyCellColor() {
        return emptyCellColor;
    }

    public int getEmptyCellBorder() {
        return emptyCellBorder;
    }

    public int getDefaultTextColor() {
        return defaultTextColor;
    }

    public int getEnergyBarBg() {
        return energyBarBg;
    }

    public int getEnergyBarFill() {
        return energyBarFill;
    }

    public int getTierColor(byte tier) {
        if (tier <= 0) return emptyCellColor;
        if (tier < tierColors.length) {
            return tierColors[tier];
        }
        return tierColors[tierColors.length - 1];
    }

    public int getTileTextColor(byte tier) {
        if (this == RETRO_2048 && (tier == 1 || tier == 2)) {
            return 0xFF776E65;
        }
        return defaultTextColor;
    }

    public GuiTheme next() {
        GuiTheme[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static GuiTheme byId(String id) {
        for (GuiTheme theme : values()) {
            if (theme.id.equalsIgnoreCase(id)) {
                return theme;
            }
        }
        return DARK;
    }
}

