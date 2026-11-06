package com.alikuxac.alchemy2048.menu.client;

import com.alikuxac.alchemy2048.config.ClientConfig;
import com.alikuxac.alchemy2048.core.Direction2048;
import com.alikuxac.alchemy2048.core.Grid2048Logic;
import com.alikuxac.alchemy2048.menu.AlchemyMatrixMenu;
import com.alikuxac.alchemy2048.network.ClaimPayload;
import com.alikuxac.alchemy2048.network.MoveGridPayload;
import com.alikuxac.alchemy2048.network.SelectPresetPayload;
import com.alikuxac.alchemy2048.recipe.EvolutionTreeRecipe;
import com.alikuxac.alchemy2048.recipe.ModRecipeTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlchemyMatrixScreen extends AbstractContainerScreen<AlchemyMatrixMenu> {
    private static final int BOARD_X = 8;
    private static final int BOARD_Y = 16;
    private static final int BOARD_WIDTH = 92;
    private static final int BOARD_HEIGHT = 92;

    private static final int CELL_SIZE = 20;
    private static final int CELL_GAP = 2;
    private static final int GRID_START_X = BOARD_X + 3;
    private static final int GRID_START_Y = BOARD_Y + 3;

    private static final int FE_BAR_X = 106;
    private static final int FE_BAR_Y = 16;
    private static final int FE_BAR_WIDTH = 62;
    private static final int FE_BAR_HEIGHT = 6;

    private static final int OUTPUT_SLOT_X = 126;
    private static final int OUTPUT_SLOT_Y = 26;
    private static final int OUTPUT_SLOT_SIZE = 22;

    private static final int BUTTON_X = 106;
    private static final int BUTTON_WIDTH = 62;
    private static final int BUTTON_HEIGHT = 16;
    private static final int EXTRACT_BUTTON_Y = 52;
    private static final int RESET_BUTTON_Y = 70;
    private static final int FE_TEXT_Y = 90;

    private static final int CONTAINER_BG = 0xFF1E2024;
    private static final int CONTAINER_BORDER = 0xFF2D3239;
    private static final int SUNKEN_SLOT_BG = 0xFF141619;
    private static final int SUNKEN_SLOT_BORDER = 0xFF2A2E33;
    private static final int FE_ACTIVE_COLOR = 0xFF00F0FF;
    private static final int FE_EMPTY_COLOR = 0xFFC0392B;

    private static final Component TITLE_COMP = Component.translatable("gui.alchemy2048.title");
    private static final Component GAME_OVER_COMP = Component.translatable("gui.alchemy2048.game_over");
    private static final Component CLAIM_REWARDS_COMP = Component.translatable("gui.alchemy2048.claim_rewards");
    private static final Component SELECT_PRESET_COMP = Component.translatable("gui.alchemy2048.select_preset");
    private static final Component TO_START_COMP = Component.translatable("gui.alchemy2048.to_start");
    private static final Component NO_PRESETS_1_COMP = Component.translatable("gui.alchemy2048.no_presets_1");
    private static final Component NO_PRESETS_2_COMP = Component.translatable("gui.alchemy2048.no_presets_2");
    private static final Component OVER_COMP = Component.translatable("gui.alchemy2048.over");
    private static final Component EMPTY_COMP = Component.translatable("gui.alchemy2048.empty");
    private static final Component EXTRACT_COMP = Component.translatable("gui.alchemy2048.extract");
    private static final Component RESET_COMP = Component.translatable("gui.alchemy2048.reset");

    private static final String[] EXPONENTIAL_STRINGS = new String[16];
    private static final String[] INDEX_STRINGS = new String[16];

    static {
        for (int i = 1; i < 16; i++) {
            EXPONENTIAL_STRINGS[i] = String.valueOf(1L << i);
            INDEX_STRINGS[i] = "T" + i;
        }
    }

    public record AvailablePreset(
            RecipeHolder<EvolutionTreeRecipe> recipeHolder,
            ItemStack displayStack,
            int count,
            int inventorySlot
    ) {}

    private final List<AvailablePreset> availablePresets = new ArrayList<>();
    private final TileInterpolationEngine interpolationEngine = new TileInterpolationEngine();
    private final byte[] currentGridBuffer = new byte[Grid2048Logic.CELL_COUNT];
    private final List<Component> reusableTooltipList = new ArrayList<>(16);

    private Button extractButton;
    private Button resetButton;
    private Button themeButton;
    private Button displayModeButton;

    private ItemStack cachedStarter = ItemStack.EMPTY;
    @Nullable
    private EvolutionTreeRecipe cachedRecipe = null;
    private final Map<Byte, ItemStack> tierStackCache = new HashMap<>();

    private long lastFrameTimeMs = 0;
    private int cachedCostValue = -1;
    private String cachedCostString = "";
    private int presetPage = 0;

    public AlchemyMatrixScreen(AlchemyMatrixMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 108;
    }

    @Override
    protected void init() {
        super.init();

        this.extractButton = this.addRenderableWidget(Button.builder(
                EXTRACT_COMP,
                btn -> sendPayload(new ClaimPayload(ClaimPayload.MODE_HIGHEST))
        ).bounds(leftPos + BUTTON_X, topPos + EXTRACT_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.extractButton.setTooltip(Tooltip.create(Component.translatable("gui.alchemy2048.extract.tooltip")));

        this.resetButton = this.addRenderableWidget(Button.builder(
                RESET_COMP,
                btn -> sendPayload(new ClaimPayload(ClaimPayload.MODE_ALL))
        ).bounds(leftPos + BUTTON_X, topPos + RESET_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.resetButton.setTooltip(Tooltip.create(Component.translatable("gui.alchemy2048.reset.tooltip")));

        this.themeButton = this.addRenderableWidget(Button.builder(
                Component.literal("🎨"),
                btn -> {
                    ClientConfig.theme = ClientConfig.theme.next();
                    updateThemeTooltip();
                    playUiClick();
                }
        ).bounds(leftPos + imageWidth - 17, topPos + 4, 12, 12).build());
        updateThemeTooltip();

        this.displayModeButton = this.addRenderableWidget(Button.builder(
                Component.literal("#"),
                btn -> {
                    ClientConfig.numberDisplay = ClientConfig.numberDisplay.next();
                    updateDisplayModeTooltip();
                    playUiClick();
                }
        ).bounds(leftPos + imageWidth - 31, topPos + 4, 12, 12).build());
        updateDisplayModeTooltip();

        updateAvailablePresets();
    }

    @Override
    protected void setInitialFocus() {
        setFocused(null);
    }

    private void updateThemeTooltip() {
        if (themeButton != null) {
            themeButton.setTooltip(Tooltip.create(Component.translatable(
                    "gui.alchemy2048.theme.tooltip",
                    Component.translatable(ClientConfig.theme.getDisplayNameKey())
            )));
        }
    }

    private void updateDisplayModeTooltip() {
        if (displayModeButton != null) {
            displayModeButton.setTooltip(Tooltip.create(Component.translatable(
                    "gui.alchemy2048.display.tooltip",
                    Component.translatable(ClientConfig.numberDisplay.getTranslationKey())
            )));
        }
    }

    private void playUiClick() {
        if (ClientConfig.playUiSounds && minecraft != null) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
        }
    }

    private void playSound(SoundEvent sound, float pitch, float volume) {
        if (ClientConfig.playUiSounds && minecraft != null) {
            minecraft.getSoundManager().play(
                    new SimpleSoundInstance(
                            sound.getLocation(),
                            SoundSource.MASTER,
                            volume,
                            pitch,
                            SoundInstance.createUnseededRandom(),
                            false,
                            0,
                            SoundInstance.Attenuation.NONE,
                            0.0,
                            0.0,
                            0.0,
                            true
                    )
            );
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.isGridEmpty()) {
            updateAvailablePresets();
        }
    }

    private void checkRecipeCache() {
        ItemStack starter = menu.getStarterStack();
        boolean starterChanged = !ItemStack.matches(starter, cachedStarter);
        if (starterChanged || (cachedRecipe == null && !starter.isEmpty())) {
            cachedStarter = starter.copy();
            tierStackCache.clear();
            cachedRecipe = resolveRecipe(cachedStarter);
        }
    }

    @Nullable
    private EvolutionTreeRecipe resolveRecipe(ItemStack starter) {
        if (minecraft == null || minecraft.level == null || starter.isEmpty()) {
            return null;
        }
        return minecraft.level.getRecipeManager()
                .getRecipesFor(ModRecipeTypes.EVOLUTION_TREE_TYPE.get(), new SingleRecipeInput(starter), minecraft.level)
                .stream()
                .filter(r -> r.value().isValid())
                .max(Comparator.comparingInt(a -> a.value().priority()))
                .map(RecipeHolder::value)
                .orElse(null);
    }

    private ItemStack getItemStackForTier(byte tier) {
        if (tier <= 0) return ItemStack.EMPTY;
        return tierStackCache.computeIfAbsent(tier, t -> {
            if (cachedRecipe != null) {
                Item item = cachedRecipe.getItemForTier(t);
                if (item != null) {
                    return new ItemStack(item);
                }
            }
            return ItemStack.EMPTY;
        });
    }

    private String getTierLabel(byte tier) {
        if (tier <= 0 || tier >= 16) return "";
        return switch (ClientConfig.numberDisplay) {
            case EXPONENTIAL -> EXPONENTIAL_STRINGS[tier];
            case TIER_INDEX -> INDEX_STRINGS[tier];
            case NONE -> "";
        };
    }

    private void updateAvailablePresets() {
        availablePresets.clear();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }

        Collection<RecipeHolder<EvolutionTreeRecipe>> allRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.EVOLUTION_TREE_TYPE.get());
        Inventory inv = minecraft.player.getInventory();

        for (RecipeHolder<EvolutionTreeRecipe> holder : allRecipes) {
            EvolutionTreeRecipe recipe = holder.value();
            if (!recipe.isValid()) continue;

            int totalCount = 0;
            int firstSlot = -1;
            ItemStack sample = ItemStack.EMPTY;

            for (int i = 0; i < 36; i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && recipe.starter().test(stack)) {
                    totalCount += stack.getCount();
                    if (firstSlot == -1) {
                        firstSlot = i;
                        sample = stack.copy();
                    }
                }
            }

            if (sample.isEmpty()) {
                ItemStack[] matching = recipe.starter().getItems();
                if (matching.length > 0) {
                    sample = matching[0].copy();
                }
            }
            if (!sample.isEmpty()) {
                sample.setCount(1);
                availablePresets.add(new AvailablePreset(holder, sample, totalCount, firstSlot));
            }
        }

        availablePresets.sort((a, b) -> Integer.compare(b.count(), a.count()));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        checkRecipeCache();

        long now = Util.getMillis();
        long elapsedMs = lastFrameTimeMs > 0 ? (now - lastFrameTimeMs) : 16;
        lastFrameTimeMs = now;
        if (elapsedMs > 100) elapsedMs = 16;
        float delta = elapsedMs / TileInterpolationEngine.ANIM_DURATION_MS;
        interpolationEngine.update(delta);

        for (int i = 0; i < Grid2048Logic.CELL_COUNT; i++) {
            currentGridBuffer[i] = menu.getGridCell(i);
        }

        if (!interpolationEngine.isAnimating()) {
            interpolationEngine.syncFromGrid(currentGridBuffer, leftPos + GRID_START_X, topPos + GRID_START_Y, CELL_SIZE, CELL_GAP);
        }

        GuiTheme theme = ClientConfig.theme != null ? ClientConfig.theme : GuiTheme.DARK;
        int x = leftPos;
        int y = topPos;

        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, CONTAINER_BG);
        guiGraphics.renderOutline(x, y, imageWidth, imageHeight, CONTAINER_BORDER);

        int titleW = font.width(TITLE_COMP);
        guiGraphics.drawString(font, TITLE_COMP, x + (imageWidth - titleW) / 2, y + 5, theme.getTitleColor(), false);

        int boardX = x + BOARD_X;
        int boardY = y + BOARD_Y;
        guiGraphics.fill(boardX, boardY, boardX + BOARD_WIDTH, boardY + BOARD_HEIGHT, SUNKEN_SLOT_BG);
        guiGraphics.renderOutline(boardX, boardY, BOARD_WIDTH, BOARD_HEIGHT, SUNKEN_SLOT_BORDER);

        for (int row = 0; row < Grid2048Logic.SIZE; row++) {
            for (int col = 0; col < Grid2048Logic.SIZE; col++) {
                int cx = x + GRID_START_X + col * (CELL_SIZE + CELL_GAP);
                int cy = y + GRID_START_Y + row * (CELL_SIZE + CELL_GAP);
                guiGraphics.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, SUNKEN_SLOT_BG);
                guiGraphics.renderOutline(cx, cy, CELL_SIZE, CELL_SIZE, SUNKEN_SLOT_BORDER);
            }
        }

        boolean isEmpty = menu.isGridEmpty();
        boolean isGameOver = menu.isGameOver();

        if (isEmpty) {
            renderPresetsOnBoard(guiGraphics, x, y, mouseX, mouseY);
        } else if (isGameOver) {
            renderGameOverOverlay(guiGraphics, boardX, boardY);
        } else {
            renderAnimatedTiles(guiGraphics, theme);
        }

        renderRightColumn(guiGraphics, x, y, theme);
    }

    private void renderPresetsOnBoard(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (availablePresets.isEmpty()) {
            int boardCenterX = x + BOARD_X + BOARD_WIDTH / 2;
            int boardCenterY = y + BOARD_Y + BOARD_HEIGHT / 2;
            int w1 = font.width(SELECT_PRESET_COMP);
            int w2 = font.width(TO_START_COMP);
            guiGraphics.drawString(font, SELECT_PRESET_COMP, boardCenterX - w1 / 2, boardCenterY - 10, 0xFF888888, false);
            guiGraphics.drawString(font, TO_START_COMP, boardCenterX - w2 / 2, boardCenterY + 2, 0xFF666666, false);
            return;
        }

        int pageSize = 16;
        int startIndex = presetPage * pageSize;
        int countOnPage = Math.min(pageSize, availablePresets.size() - startIndex);

        for (int i = 0; i < countOnPage; i++) {
            int col = i % Grid2048Logic.SIZE;
            int row = i / Grid2048Logic.SIZE;
            int cx = x + GRID_START_X + col * (CELL_SIZE + CELL_GAP);
            int cy = y + GRID_START_Y + row * (CELL_SIZE + CELL_GAP);

            AvailablePreset preset = availablePresets.get(startIndex + i);
            boolean canPlay = preset.count() > 0;
            boolean hovered = mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE;

            if (hovered) {
                guiGraphics.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, canPlay ? 0xFF2A3A2A : 0xFF3A2020);
                guiGraphics.renderOutline(cx, cy, CELL_SIZE, CELL_SIZE, canPlay ? 0xFF00FF88 : 0xFFFF4444);
            }

            guiGraphics.renderItem(preset.displayStack(), cx + 2, cy + 2);

            String countStr = String.valueOf(preset.count());
            int cw = font.width(countStr);
            guiGraphics.drawString(font, countStr, cx + CELL_SIZE - cw - 1, cy + CELL_SIZE - font.lineHeight + 1, canPlay ? 0xFF55FF55 : 0xFFFF5555, true);
        }
    }

    private void renderGameOverOverlay(GuiGraphics guiGraphics, int boardX, int boardY) {
        guiGraphics.fill(boardX, boardY, boardX + BOARD_WIDTH, boardY + BOARD_HEIGHT, 0xEE1E1414);
        guiGraphics.renderOutline(boardX, boardY, BOARD_WIDTH, BOARD_HEIGHT, 0xFFFF4444);

        int titleW = font.width(GAME_OVER_COMP);
        guiGraphics.drawString(font, GAME_OVER_COMP, boardX + (BOARD_WIDTH - titleW) / 2, boardY + 16, 0xFFFF4444, true);

        int subW = font.width(CLAIM_REWARDS_COMP);
        guiGraphics.drawString(font, CLAIM_REWARDS_COMP, boardX + (BOARD_WIDTH - subW) / 2, boardY + 34, 0xFFCCCCCC, true);
    }

    private void renderAnimatedTiles(GuiGraphics guiGraphics, GuiTheme theme) {
        TileInterpolationEngine.TileRenderState[] pool = interpolationEngine.getPool();

        for (TileInterpolationEngine.TileRenderState tile : pool) {
            if (!tile.active || tile.tier <= 0) continue;

            int cellColor = theme.getTierColor(tile.tier);
            float scale = tile.getScale();

            if (scale != 1.0f) {
                guiGraphics.pose().pushPose();
                float centerX = tile.currentX + CELL_SIZE / 2.0f;
                float centerY = tile.currentY + CELL_SIZE / 2.0f;
                guiGraphics.pose().translate(centerX, centerY, 0);
                guiGraphics.pose().scale(scale, scale, 1.0f);
                guiGraphics.pose().translate(-centerX, -centerY, 0);
            }

            int tx = (int) tile.currentX;
            int ty = (int) tile.currentY;

            guiGraphics.fill(tx, ty, tx + CELL_SIZE, ty + CELL_SIZE, cellColor);
            guiGraphics.renderOutline(tx, ty, CELL_SIZE, CELL_SIZE, SUNKEN_SLOT_BORDER);

            ItemStack stack = getItemStackForTier(tile.tier);
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, tx + 2, ty + 2);
            }

            String label = getTierLabel(tile.tier);
            if (!label.isEmpty()) {
                int tw = font.width(label);
                guiGraphics.drawString(font, label, tx + CELL_SIZE - tw - 1, ty + CELL_SIZE - font.lineHeight + 1, theme.getTileTextColor(tile.tier), true);
            }

            if (scale != 1.0f) {
                guiGraphics.pose().popPose();
            }
        }
    }

    private void renderRightColumn(GuiGraphics guiGraphics, int x, int y, GuiTheme theme) {
        int barX = x + FE_BAR_X;
        int barY = y + FE_BAR_Y;
        guiGraphics.fill(barX, barY, barX + FE_BAR_WIDTH, barY + FE_BAR_HEIGHT, SUNKEN_SLOT_BG);
        guiGraphics.renderOutline(barX, barY, FE_BAR_WIDTH, FE_BAR_HEIGHT, SUNKEN_SLOT_BORDER);

        int maxEnergy = menu.getMaxEnergy();
        int currentEnergy = menu.getEnergy();
        boolean enoughEnergy = menu.hasEnoughEnergy();

        if (maxEnergy > 0 && currentEnergy > 0) {
            int fillWidth = (int) ((long) currentEnergy * (FE_BAR_WIDTH - 2) / maxEnergy);
            fillWidth = Math.clamp(fillWidth, 1, FE_BAR_WIDTH - 2);
            int fillColor = enoughEnergy ? FE_ACTIVE_COLOR : FE_EMPTY_COLOR;
            guiGraphics.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + FE_BAR_HEIGHT - 1, fillColor);
        }

        int outX = x + OUTPUT_SLOT_X;
        int outY = y + OUTPUT_SLOT_Y;
        guiGraphics.fill(outX, outY, outX + OUTPUT_SLOT_SIZE, outY + OUTPUT_SLOT_SIZE, SUNKEN_SLOT_BG);
        guiGraphics.renderOutline(outX, outY, OUTPUT_SLOT_SIZE, OUTPUT_SLOT_SIZE, SUNKEN_SLOT_BORDER);

        byte maxTier = Grid2048Logic.getMaxTier(currentGridBuffer);
        if (maxTier > 0) {
            ItemStack outputStack = getItemStackForTier(maxTier);
            if (!outputStack.isEmpty()) {
                guiGraphics.renderItem(outputStack, outX + 3, outY + 3);
            }
        }

        int textY = y + FE_TEXT_Y;
        if (menu.isGameOver()) {
            int ow = font.width(OVER_COMP);
            guiGraphics.drawString(font, OVER_COMP, barX + (FE_BAR_WIDTH - ow) / 2, textY, 0xFFFF4444, false);
        } else {
            int moveCost = menu.getNextMoveCost();
            if (moveCost != cachedCostValue) {
                cachedCostValue = moveCost;
                cachedCostString = moveCost + " FE/m";
            }
            if (enoughEnergy) {
                int cw = font.width(cachedCostString);
                guiGraphics.drawString(font, cachedCostString, barX + (FE_BAR_WIDTH - cw) / 2, textY, 0xFFAAAAAA, false);
            } else {
                int ew = font.width(EMPTY_COMP);
                guiGraphics.drawString(font, EMPTY_COMP, barX + (FE_BAR_WIDTH - ew) / 2, textY, FE_EMPTY_COLOR, false);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xA0A0A0, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean isPlaying = !menu.isGridEmpty();

        if (extractButton != null && resetButton != null) {
            extractButton.visible = true;
            extractButton.active = isPlaying;
            resetButton.visible = true;
            resetButton.active = isPlaying;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        renderCustomTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        int outX = x + OUTPUT_SLOT_X;
        int outY = y + OUTPUT_SLOT_Y;
        if (mouseX >= outX && mouseX < outX + OUTPUT_SLOT_SIZE && mouseY >= outY && mouseY < outY + OUTPUT_SLOT_SIZE) {
            byte maxTier = Grid2048Logic.getMaxTier(currentGridBuffer);
            reusableTooltipList.clear();
            if (maxTier > 0) {
                ItemStack stack = getItemStackForTier(maxTier);
                reusableTooltipList.add(Component.translatable("gui.alchemy2048.output_buffer").withStyle(ChatFormatting.GOLD));
                if (!stack.isEmpty()) {
                    reusableTooltipList.add(stack.getHoverName().copy().withStyle(ChatFormatting.AQUA));
                }
                reusableTooltipList.add(Component.translatable("gui.alchemy2048.tier_tooltip", maxTier, 1L << maxTier).withStyle(ChatFormatting.GRAY));
            } else {
                reusableTooltipList.add(Component.translatable("gui.alchemy2048.output_buffer_empty").withStyle(ChatFormatting.GRAY));
            }
            guiGraphics.renderComponentTooltip(font, reusableTooltipList, mouseX, mouseY);
            return;
        }

        int barX = x + FE_BAR_X;
        int barY = y + FE_BAR_Y;
        if (mouseX >= barX && mouseX <= barX + FE_BAR_WIDTH && mouseY >= barY && mouseY <= barY + FE_BAR_HEIGHT) {
            reusableTooltipList.clear();
            reusableTooltipList.add(Component.translatable("gui.alchemy2048.energy_tooltip", menu.getEnergy(), menu.getMaxEnergy()).withStyle(ChatFormatting.AQUA));
            reusableTooltipList.add(Component.translatable("gui.alchemy2048.next_move_cost", menu.getNextMoveCost()).withStyle(ChatFormatting.GRAY));
            guiGraphics.renderComponentTooltip(font, reusableTooltipList, mouseX, mouseY);
            return;
        }

        if (menu.isGridEmpty() && !availablePresets.isEmpty()) {
            int pageSize = 16;
            int startIndex = presetPage * pageSize;
            int countOnPage = Math.min(pageSize, availablePresets.size() - startIndex);

            for (int i = 0; i < countOnPage; i++) {
                int col = i % Grid2048Logic.SIZE;
                int row = i / Grid2048Logic.SIZE;
                int cx = x + GRID_START_X + col * (CELL_SIZE + CELL_GAP);
                int cy = y + GRID_START_Y + row * (CELL_SIZE + CELL_GAP);

                if (mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE) {
                    AvailablePreset p = availablePresets.get(startIndex + i);
                    reusableTooltipList.clear();
                    reusableTooltipList.add(p.displayStack().getHoverName().copy().withStyle(ChatFormatting.GOLD));
                    if (p.count() > 0) {
                        reusableTooltipList.add(Component.translatable("gui.alchemy2048.inventory_count", p.count()).withStyle(ChatFormatting.GREEN));
                    } else {
                        reusableTooltipList.add(Component.translatable("gui.alchemy2048.missing_starter").withStyle(ChatFormatting.RED));
                    }
                    reusableTooltipList.add(Component.translatable("gui.alchemy2048.progression_ladder").withStyle(ChatFormatting.YELLOW));
                    EvolutionTreeRecipe r = p.recipeHolder().value();
                    int maxDisplay = Math.min(4, r.activeLadder().size());
                    for (int t = 1; t <= maxDisplay; t++) {
                        Item it = r.getItemForTier(t);
                        if (it != null) {
                            reusableTooltipList.add(Component.literal(" " + t + ". " + it.getName(new ItemStack(it)).getString()).withStyle(ChatFormatting.GRAY));
                        }
                    }
                    if (r.activeLadder().size() > maxDisplay) {
                        reusableTooltipList.add(Component.translatable("gui.alchemy2048.max_result", r.getResultItem(null).getHoverName().getString()).withStyle(ChatFormatting.AQUA));
                    }
                    if (p.count() > 0) {
                        if (menu.hasEnoughEnergy()) {
                            reusableTooltipList.add(Component.translatable("gui.alchemy2048.click_to_start").withStyle(ChatFormatting.GREEN));
                        } else {
                            reusableTooltipList.add(Component.translatable("gui.alchemy2048.requires_energy", menu.getNextMoveCost()).withStyle(ChatFormatting.RED));
                        }
                    } else {
                        reusableTooltipList.add(Component.translatable("gui.alchemy2048.requires_starter", p.displayStack().getHoverName().getString()).withStyle(ChatFormatting.DARK_RED));
                    }
                    guiGraphics.renderComponentTooltip(font, reusableTooltipList, mouseX, mouseY);
                    return;
                }
            }
        }

        if (!menu.isGridEmpty() && !menu.isGameOver() && ClientConfig.showTileTooltip) {
            for (int row = 0; row < Grid2048Logic.SIZE; row++) {
                for (int col = 0; col < Grid2048Logic.SIZE; col++) {
                    int cx = x + GRID_START_X + col * (CELL_SIZE + CELL_GAP);
                    int cy = y + GRID_START_Y + row * (CELL_SIZE + CELL_GAP);
                    if (mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE) {
                        int cellIdx = row * Grid2048Logic.SIZE + col;
                        byte tier = menu.getGridCell(cellIdx);
                        if (tier > 0) {
                            ItemStack stack = getItemStackForTier(tier);
                            reusableTooltipList.clear();
                            if (!stack.isEmpty()) {
                                reusableTooltipList.add(stack.getHoverName().copy().withStyle(ChatFormatting.GOLD));
                            }
                            reusableTooltipList.add(Component.translatable("gui.alchemy2048.tier_tooltip", tier, 1L << tier).withStyle(ChatFormatting.AQUA));
                            guiGraphics.renderComponentTooltip(font, reusableTooltipList, mouseX, mouseY);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.isGridEmpty() && !availablePresets.isEmpty()) {
            int pageSize = 16;
            int startIndex = presetPage * pageSize;
            int countOnPage = Math.min(pageSize, availablePresets.size() - startIndex);

            for (int i = 0; i < countOnPage; i++) {
                int col = i % Grid2048Logic.SIZE;
                int row = i / Grid2048Logic.SIZE;
                int cx = leftPos + GRID_START_X + col * (CELL_SIZE + CELL_GAP);
                int cy = topPos + GRID_START_Y + row * (CELL_SIZE + CELL_GAP);

                if (mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE) {
                    AvailablePreset p = availablePresets.get(startIndex + i);
                    if (p.count() > 0 && p.inventorySlot() != -1) {
                        sendPayload(new SelectPresetPayload(p.recipeHolder().id(), p.inventorySlot()));
                        playUiClick();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.isGridEmpty() && availablePresets.size() > 16) {
            int maxPages = (availablePresets.size() + 15) / 16;
            if (scrollY < 0 && presetPage < maxPages - 1) {
                presetPage++;
                return true;
            } else if (scrollY > 0 && presetPage > 0) {
                presetPage--;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Direction2048 dir = switch (keyCode) {
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> Direction2048.UP;
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> Direction2048.DOWN;
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> Direction2048.LEFT;
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> Direction2048.RIGHT;
            default -> null;
        };

        if (dir != null) {
            if (!menu.isGameOver() && !menu.isGridEmpty()) {
                if (!menu.hasEnoughEnergy()) {
                    playSound(SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.5f);
                    return true;
                }

                for (int i = 0; i < Grid2048Logic.CELL_COUNT; i++) {
                    currentGridBuffer[i] = menu.getGridCell(i);
                }

                TileInterpolationEngine.MoveResult result = interpolationEngine.onMove(
                        currentGridBuffer,
                        dir,
                        leftPos + GRID_START_X,
                        topPos + GRID_START_Y,
                        CELL_SIZE,
                        CELL_GAP
                );

                if (!result.moved()) {
                    playSound(SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.5f);
                } else {
                    playSound(SoundEvents.IRON_TRAPDOOR_CLOSE, 1.6f, 0.25f);

                    if (result.maxMergedTier() > 0) {
                        float pitch = Math.min(2.0f, 0.8f + result.maxMergedTier() * 0.1f);
                        playSound(SoundEvents.COPPER_HIT, pitch, 0.5f);
                    }

                    setFocused(null);
                    sendPayload(new MoveGridPayload(dir.getId()));
                }
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void sendPayload(CustomPacketPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundCustomPayloadPacket(payload));
        }
    }
}
