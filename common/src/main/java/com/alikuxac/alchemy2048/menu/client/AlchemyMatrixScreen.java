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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
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
    private static final int BOARD_X = 76;
    private static final int BOARD_Y = 18;
    private static final int BOARD_WIDTH = 92;
    private static final int BOARD_HEIGHT = 92;

    private static final int CELL_SIZE = 20;
    private static final int CELL_GAP = 2;
    private static final int GRID_START_X = BOARD_X + 3;
    private static final int GRID_START_Y = BOARD_Y + 3;

    private static final int BUTTON_X = 10;
    private static final int BUTTON_WIDTH = 62;
    private static final int BUTTON_HEIGHT = 14;

    private static final int PRESET_AREA_X = 11;
    private static final int PRESET_SLOT_SIZE = 18;
    private static final int PRESET_GAP = 3;

    private static final int ENERGY_BAR_X = 10;
    private static final int ENERGY_BAR_Y = 83;
    private static final int ENERGY_BAR_WIDTH = 62;
    private static final int ENERGY_BAR_HEIGHT = 10;

    public record AvailablePreset(
            RecipeHolder<EvolutionTreeRecipe> recipeHolder,
            ItemStack displayStack,
            int count,
            int inventorySlot
    ) {}

    private final List<AvailablePreset> availablePresets = new ArrayList<>();
    private int presetPage = 0;

    private ThemedButton claimHighestButton;
    private ThemedButton claimAllButton;
    private ThemedButton themeButton;
    private ThemedButton displayModeButton;

    private ItemStack cachedStarter = ItemStack.EMPTY;
    @Nullable
    private EvolutionTreeRecipe cachedRecipe = null;
    private final Map<Byte, ItemStack> tierStackCache = new HashMap<>();

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

        this.claimHighestButton = this.addRenderableWidget(new ThemedButton(
                leftPos + BUTTON_X, topPos + 50, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.alchemy2048.claim_max"),
                btn -> sendPayload(new ClaimPayload(ClaimPayload.MODE_HIGHEST)),
                0xFFD4AF37, 0xFFFFDF00, 0xFFFFD700
        ));
        this.claimHighestButton.setTooltip(Tooltip.create(Component.translatable("gui.alchemy2048.claim_max.tooltip")));

        this.claimAllButton = this.addRenderableWidget(new ThemedButton(
                leftPos + BUTTON_X, topPos + 66, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.alchemy2048.claim_all"),
                btn -> sendPayload(new ClaimPayload(ClaimPayload.MODE_ALL)),
                0xFF2E8B57, 0xFF3CB371, 0xFF85E3B3
        ));
        this.claimAllButton.setTooltip(Tooltip.create(Component.translatable("gui.alchemy2048.claim_all.tooltip")));

        this.themeButton = this.addRenderableWidget(new ThemedButton(
                leftPos + imageWidth - 17, topPos + 4, 12, 12,
                Component.literal("🎨"),
                btn -> {
                    ClientConfig.theme = ClientConfig.theme.next();
                    updateThemeTooltip();
                    playUiClick();
                },
                0, 0xFFAAAAAA, 0xFFFFFFFF
        ));
        updateThemeTooltip();

        this.displayModeButton = this.addRenderableWidget(new ThemedButton(
                leftPos + imageWidth - 31, topPos + 4, 12, 12,
                Component.literal("#"),
                btn -> {
                    ClientConfig.numberDisplay = ClientConfig.numberDisplay.next();
                    updateDisplayModeTooltip();
                    playUiClick();
                },
                0, 0xFFAAAAAA, 0xFFFFFFFF
        ));
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
        if (ClientConfig.playUiSounds) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
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

    private int getPresetX(int slotOnPage) {
        int col = slotOnPage % 3;
        return leftPos + PRESET_AREA_X + col * (PRESET_SLOT_SIZE + PRESET_GAP);
    }

    private int getPresetY(int slotOnPage) {
        int row = slotOnPage / 3;
        return topPos + 33 + row * (PRESET_SLOT_SIZE + 4);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        checkRecipeCache();

        GuiTheme theme = ClientConfig.theme != null ? ClientConfig.theme : GuiTheme.DARK;
        int x = leftPos;
        int y = topPos;

        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, theme.getBgColor());
        guiGraphics.renderOutline(x, y, imageWidth, imageHeight, theme.getBgOutlineColor());

        String titleStr = Component.translatable("gui.alchemy2048.title").getString();
        int titleW = font.width(titleStr);
        guiGraphics.drawString(font, titleStr, x + (imageWidth - titleW) / 2, y + 6, theme.getTitleColor(), false);

        if (menu.isGridEmpty()) {
            renderPlayablePresets(guiGraphics, x, y, mouseX, mouseY, theme);
        } else {
            boolean isOver = menu.isGameOver();
            String playTitle = isOver
                    ? Component.translatable("gui.alchemy2048.game_over").getString()
                    : Component.translatable("gui.alchemy2048.playing").getString();
            int pw = font.width(playTitle);
            int titleColor = isOver ? 0xFFFF4444 : theme.getTitleColor();
            guiGraphics.drawString(font, playTitle, x + 10 + (62 - pw) / 2, y + 20, titleColor, false);

            ItemStack starter = menu.getStarterStack();
            if (!starter.isEmpty()) {
                int sx = x + 32;
                int sy = y + 30;
                guiGraphics.fill(sx, sy, sx + 18, sy + 18, theme.getBoardBgColor());
                guiGraphics.renderOutline(sx, sy, 18, 18, theme.getBoardOutlineColor());
                guiGraphics.renderItem(starter, sx + 1, sy + 1);
            }

            if (isOver) {
                byte maxTier = 0;
                for (int i = 0; i < 16; i++) {
                    byte t = menu.getGridCell(i);
                    if (t > maxTier) maxTier = t;
                }
                if (maxTier > 0) {
                    ItemStack maxStack = getItemStackForTier(maxTier);
                    if (!maxStack.isEmpty()) {
                        int mx = x + 32;
                        int my = y + 54;
                        guiGraphics.fill(mx, my, mx + 18, my + 18, theme.getBoardBgColor());
                        guiGraphics.renderOutline(mx, my, 18, 18, 0xFFFFD700);
                        guiGraphics.renderItem(maxStack, mx + 1, my + 1);
                    }
                }
            }
        }

        int boardX = x + BOARD_X;
        int boardY = y + BOARD_Y;
        guiGraphics.fill(boardX, boardY, boardX + BOARD_WIDTH, boardY + BOARD_HEIGHT, theme.getBoardBgColor());
        guiGraphics.renderOutline(boardX, boardY, BOARD_WIDTH, BOARD_HEIGHT, theme.getBoardOutlineColor());

        if (menu.isGridEmpty()) {
            String hint1 = Component.translatable("gui.alchemy2048.select_preset").getString();
            String hint2 = Component.translatable("gui.alchemy2048.to_start").getString();
            guiGraphics.drawString(font, hint1, boardX + (BOARD_WIDTH - font.width(hint1)) / 2, boardY + 38, theme.getTitleColor(), true);
            guiGraphics.drawString(font, hint2, boardX + (BOARD_WIDTH - font.width(hint2)) / 2, boardY + 50, theme.getSubTitleColor(), true);
        } else if (menu.isGameOver()) {
            guiGraphics.fill(boardX, boardY, boardX + BOARD_WIDTH, boardY + BOARD_HEIGHT, theme.getBoardBgColor());
            guiGraphics.renderOutline(boardX, boardY, BOARD_WIDTH, BOARD_HEIGHT, 0xFFFF4444);
            String overTitle = Component.translatable("gui.alchemy2048.game_over").getString();
            int overW = font.width(overTitle);
            guiGraphics.drawString(font, overTitle, boardX + (BOARD_WIDTH - overW) / 2, boardY + 12, 0xFFFF4444, true);
            String overSub = Component.translatable("gui.alchemy2048.claim_rewards").getString();
            int subW = font.width(overSub);
            guiGraphics.drawString(font, overSub, boardX + (BOARD_WIDTH - subW) / 2, boardY + 26, 0xFFCCCCCC, true);
        } else {
            for (int row = 0; row < Grid2048Logic.SIZE; row++) {
                for (int col = 0; col < Grid2048Logic.SIZE; col++) {
                    int cellIdx = row * Grid2048Logic.SIZE + col;
                    byte tier = menu.getGridCell(cellIdx);
                    int cx = x + GRID_START_X + col * (CELL_SIZE + CELL_GAP);
                    int cy = y + GRID_START_Y + row * (CELL_SIZE + CELL_GAP);

                    int color = theme.getTierColor(tier);
                    guiGraphics.fill(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, color);
                    guiGraphics.renderOutline(cx, cy, CELL_SIZE, CELL_SIZE, theme.getEmptyCellBorder());

                    if (tier > 0) {
                        ItemStack stack = getItemStackForTier(tier);
                        if (!stack.isEmpty()) {
                            guiGraphics.renderItem(stack, cx + 2, cy + 2);
                        }

                        String label = ClientConfig.numberDisplay.format(tier);
                        if (!label.isEmpty()) {
                            int tw = font.width(label);
                            int tx = cx + CELL_SIZE - tw - 1;
                            int ty = cy + CELL_SIZE - font.lineHeight + 1;
                            guiGraphics.drawString(font, label, tx, ty, theme.getTileTextColor(tier), true);
                        }
                    }
                }
            }
        }

        int energyBarX = x + ENERGY_BAR_X;
        int energyBarY = y + ENERGY_BAR_Y;
        guiGraphics.fill(energyBarX, energyBarY, energyBarX + ENERGY_BAR_WIDTH, energyBarY + ENERGY_BAR_HEIGHT, theme.getEnergyBarBg());
        guiGraphics.renderOutline(energyBarX, energyBarY, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, theme.getBoardOutlineColor());

        int maxEnergy = menu.getMaxEnergy();
        int currentEnergy = menu.getEnergy();
        if (maxEnergy > 0 && currentEnergy > 0) {
            int fillWidth = (int) ((long) currentEnergy * (ENERGY_BAR_WIDTH - 2) / maxEnergy);
            fillWidth = Math.clamp(fillWidth, 1, ENERGY_BAR_WIDTH - 2);
            guiGraphics.fill(energyBarX + 1, energyBarY + 1, energyBarX + 1 + fillWidth, energyBarY + ENERGY_BAR_HEIGHT - 1, theme.getEnergyBarFill());
        }

        if (menu.isGameOver()) {
            String overStr = Component.translatable("gui.alchemy2048.over").getString();
            int ow = font.width(overStr);
            guiGraphics.drawString(font, overStr, energyBarX + (ENERGY_BAR_WIDTH - ow) / 2, y + 96, 0xFFFF4444, false);
        } else {
            String costText = Component.translatable("gui.alchemy2048.cost_fe", menu.getNextMoveCost()).getString();
            boolean enoughEnergy = menu.hasEnoughEnergy();
            guiGraphics.drawString(font, costText, energyBarX, y + 96, enoughEnergy ? theme.getSubTitleColor() : 0xFFFF4444, false);
            if (!enoughEnergy) {
                String emptyText = Component.translatable("gui.alchemy2048.empty").getString();
                int costW = font.width(costText);
                guiGraphics.drawString(font, emptyText, energyBarX + costW + 4, y + 96, 0xFFFF3333, false);
            }
        }
    }

    private void renderPlayablePresets(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, GuiTheme theme) {
        String title = Component.translatable("gui.alchemy2048.presets").getString();
        int tw = font.width(title);
        guiGraphics.drawString(font, title, x + 10 + (62 - tw) / 2, y + 20, theme.getTitleColor(), false);

        if (availablePresets.isEmpty()) {
            String line1 = Component.translatable("gui.alchemy2048.no_presets_1").getString();
            String line2 = Component.translatable("gui.alchemy2048.no_presets_2").getString();
            guiGraphics.drawString(font, line1, x + 10 + (62 - font.width(line1)) / 2, y + 42, 0xFF888888, false);
            guiGraphics.drawString(font, line2, x + 10 + (62 - font.width(line2)) / 2, y + 54, 0xFF666666, false);
            return;
        }

        int pageSize = 6;
        int startIndex = presetPage * pageSize;
        int countOnPage = Math.min(pageSize, availablePresets.size() - startIndex);

        for (int i = 0; i < countOnPage; i++) {
            int presetIdx = startIndex + i;
            AvailablePreset p = availablePresets.get(presetIdx);
            int bx = getPresetX(i);
            int by = getPresetY(i);

            boolean canPlay = p.count() > 0;
            boolean hovered = mouseX >= bx && mouseX < bx + PRESET_SLOT_SIZE && mouseY >= by && mouseY < by + PRESET_SLOT_SIZE;

            int bg = hovered ? (canPlay ? 0xFF3D3D3D : 0xFF2A2020) : (canPlay ? theme.getBoardBgColor() : 0xFF141414);
            int border = hovered ? (canPlay ? 0xFF888888 : 0xFF663333) : (canPlay ? 0xFF555555 : 0xFF333333);

            guiGraphics.fill(bx, by, bx + PRESET_SLOT_SIZE, by + PRESET_SLOT_SIZE, bg);
            guiGraphics.renderOutline(bx, by, PRESET_SLOT_SIZE, PRESET_SLOT_SIZE, border);

            guiGraphics.renderItem(p.displayStack(), bx + 1, by + 1);

            String countStr = String.valueOf(p.count());
            int cw = font.width(countStr);
            int color = canPlay ? 0xFF55FF55 : 0xFFFF5555;
            guiGraphics.drawString(font, countStr, bx + PRESET_SLOT_SIZE - cw, by + PRESET_SLOT_SIZE - font.lineHeight, color, true);
        }

        if (availablePresets.size() > 6) {
            int totalPages = (availablePresets.size() + 5) / 6;
            String pageStr = (presetPage + 1) + "/" + totalPages;
            int pw = font.width(pageStr);
            guiGraphics.drawString(font, pageStr, x + 10 + (62 - pw) / 2, y + 74, theme.getSubTitleColor(), false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xA0A0A0, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean isPlaying = !menu.isGridEmpty();
        boolean isGameOver = menu.isGameOver();

        if (claimHighestButton != null && claimAllButton != null) {
            claimHighestButton.visible = isPlaying;
            claimHighestButton.active = isPlaying;
            claimAllButton.visible = isPlaying;
            claimAllButton.active = isPlaying;

            if (isGameOver) {
                int btnW = 76;
                int btnH = 18;
                int btnX = leftPos + BOARD_X + (BOARD_WIDTH - btnW) / 2;
                claimHighestButton.setPosition(btnX, topPos + BOARD_Y + 42);
                claimHighestButton.setDimensions(btnW, btnH);

                claimAllButton.setPosition(btnX, topPos + BOARD_Y + 65);
                claimAllButton.setDimensions(btnW, btnH);
            } else {
                claimHighestButton.setPosition(leftPos + BUTTON_X, topPos + 50);
                claimHighestButton.setDimensions(BUTTON_WIDTH, BUTTON_HEIGHT);

                claimAllButton.setPosition(leftPos + BUTTON_X, topPos + 66);
                claimAllButton.setDimensions(BUTTON_WIDTH, BUTTON_HEIGHT);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = leftPos;
        int y = topPos;

        if (menu.isGridEmpty() && !availablePresets.isEmpty()) {
            int pageSize = 6;
            int startIndex = presetPage * pageSize;
            int countOnPage = Math.min(pageSize, availablePresets.size() - startIndex);

            for (int i = 0; i < countOnPage; i++) {
                int presetIdx = startIndex + i;
                AvailablePreset p = availablePresets.get(presetIdx);
                int bx = getPresetX(i);
                int by = getPresetY(i);

                if (mouseX >= bx && mouseX < bx + PRESET_SLOT_SIZE && mouseY >= by && mouseY < by + PRESET_SLOT_SIZE) {
                    List<Component> tip = new ArrayList<>();
                    tip.add(p.displayStack().getHoverName().copy().withStyle(ChatFormatting.GOLD));
                    if (p.count() > 0) {
                        tip.add(Component.translatable("gui.alchemy2048.inventory_count", p.count()).withStyle(ChatFormatting.GREEN));
                    } else {
                        tip.add(Component.translatable("gui.alchemy2048.missing_starter").withStyle(ChatFormatting.RED));
                    }
                    tip.add(Component.translatable("gui.alchemy2048.progression_ladder").withStyle(ChatFormatting.YELLOW));
                    EvolutionTreeRecipe r = p.recipeHolder().value();
                    int maxDisplay = Math.min(4, r.activeLadder().size());
                    for (int t = 1; t <= maxDisplay; t++) {
                        Item it = r.getItemForTier(t);
                        if (it != null) {
                            tip.add(Component.literal(" " + t + ". " + it.getName(new ItemStack(it)).getString()).withStyle(ChatFormatting.GRAY));
                        }
                    }
                    if (r.activeLadder().size() > maxDisplay) {
                        tip.add(Component.translatable("gui.alchemy2048.max_result", r.getResultItem(null).getHoverName().getString()).withStyle(ChatFormatting.AQUA));
                    }
                    if (p.count() > 0) {
                        if (menu.hasEnoughEnergy()) {
                            tip.add(Component.translatable("gui.alchemy2048.click_to_start").withStyle(ChatFormatting.GREEN));
                        } else {
                            tip.add(Component.translatable("gui.alchemy2048.requires_energy", menu.getNextMoveCost()).withStyle(ChatFormatting.RED));
                        }
                    } else {
                        tip.add(Component.translatable("gui.alchemy2048.requires_starter", p.displayStack().getHoverName().getString()).withStyle(ChatFormatting.DARK_RED));
                    }
                    guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
                    break;
                }
            }
        } else if (!menu.isGridEmpty()) {
            ItemStack starter = menu.getStarterStack();
            if (!starter.isEmpty()) {
                int sx = x + 32;
                int sy = y + 30;
                if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                    List<Component> tip = new ArrayList<>();
                    tip.add(starter.getHoverName().copy().withStyle(ChatFormatting.GOLD));
                    tip.add(Component.translatable("gui.alchemy2048.current_preset").withStyle(ChatFormatting.GREEN));
                    if (cachedRecipe != null) {
                        tip.add(Component.translatable("gui.alchemy2048.progression_ladder").withStyle(ChatFormatting.YELLOW));
                        int maxDisplay = Math.min(4, cachedRecipe.activeLadder().size());
                        for (int t = 1; t <= maxDisplay; t++) {
                            Item it = cachedRecipe.getItemForTier(t);
                            if (it != null) {
                                tip.add(Component.literal(" " + t + ". " + it.getName(new ItemStack(it)).getString()).withStyle(ChatFormatting.GRAY));
                            }
                        }
                        if (cachedRecipe.activeLadder().size() > maxDisplay) {
                            tip.add(Component.translatable("gui.alchemy2048.max_result", cachedRecipe.getResultItem(null).getHoverName().getString()).withStyle(ChatFormatting.AQUA));
                        }
                    }
                    guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
                }
            }

            if (menu.isGameOver()) {
                int mx = x + 32;
                int my = y + 54;
                if (mouseX >= mx && mouseX < mx + 18 && mouseY >= my && mouseY < my + 18) {
                    byte maxTier = 0;
                    for (int i = 0; i < 16; i++) {
                        byte t = menu.getGridCell(i);
                        if (t > maxTier) maxTier = t;
                    }
                    if (maxTier > 0) {
                        ItemStack maxStack = getItemStackForTier(maxTier);
                        if (!maxStack.isEmpty()) {
                            guiGraphics.renderTooltip(font, maxStack, mouseX, mouseY);
                        }
                    }
                }
            }
        }

        if (!menu.isGridEmpty() && !menu.isGameOver()) {
            for (int row = 0; row < Grid2048Logic.SIZE; row++) {
                for (int col = 0; col < Grid2048Logic.SIZE; col++) {
                    int cx = x + GRID_START_X + col * (CELL_SIZE + CELL_GAP);
                    int cy = y + GRID_START_Y + row * (CELL_SIZE + CELL_GAP);
                    if (mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE) {
                        int cellIdx = row * Grid2048Logic.SIZE + col;
                        byte tier = menu.getGridCell(cellIdx);
                        if (tier > 0 && ClientConfig.showTileTooltip) {
                            ItemStack stack = getItemStackForTier(tier);
                            List<Component> tip = new ArrayList<>();
                            if (!stack.isEmpty()) {
                                tip.add(stack.getHoverName().copy().withStyle(ChatFormatting.GOLD));
                            }
                            tip.add(Component.translatable("gui.alchemy2048.tier_tooltip", tier, 1L << tier).withStyle(ChatFormatting.AQUA));
                            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
                        }
                    }
                }
            }
        }

        int energyBarX = x + ENERGY_BAR_X;
        int energyBarY = y + ENERGY_BAR_Y;
        if (mouseX >= energyBarX && mouseX <= energyBarX + ENERGY_BAR_WIDTH &&
            mouseY >= energyBarY && mouseY <= energyBarY + ENERGY_BAR_HEIGHT) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.alchemy2048.energy_tooltip", menu.getEnergy(), menu.getMaxEnergy()).withStyle(ChatFormatting.RED));
            tip.add(Component.translatable("gui.alchemy2048.next_move_cost", menu.getNextMoveCost()).withStyle(ChatFormatting.GRAY));
            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.isGridEmpty() && availablePresets.size() > 6) {
            int maxPages = (availablePresets.size() + 5) / 6;
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.isGridEmpty() && !availablePresets.isEmpty()) {
            int pageSize = 6;
            int startIndex = presetPage * pageSize;
            int countOnPage = Math.min(pageSize, availablePresets.size() - startIndex);

            for (int i = 0; i < countOnPage; i++) {
                int presetIdx = startIndex + i;
                AvailablePreset p = availablePresets.get(presetIdx);
                int bx = getPresetX(i);
                int by = getPresetY(i);

                if (mouseX >= bx && mouseX < bx + PRESET_SLOT_SIZE && mouseY >= by && mouseY < by + PRESET_SLOT_SIZE) {
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Direction2048 dir = switch (keyCode) {
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> Direction2048.UP;
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> Direction2048.DOWN;
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> Direction2048.LEFT;
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> Direction2048.RIGHT;
            default -> null;
        };

        if (dir != null) {
            if (!menu.isGameOver()) {
                setFocused(null);
                sendPayload(new MoveGridPayload(dir.getId()));
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

    private static class ThemedButton extends Button {
        private final int normalBorder;
        private final int hoverBorder;
        private final int textColor;

        ThemedButton(int x, int y, int width, int height, Component message, OnPress onPress,
                     int normalBorder, int hoverBorder, int textColor) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.normalBorder = normalBorder;
            this.hoverBorder = hoverBorder;
            this.textColor = textColor;
        }

        public void setDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            GuiTheme theme = ClientConfig.theme != null ? ClientConfig.theme : GuiTheme.DARK;
            int bg = !this.active ? 0xFF181818 : (this.isHoveredOrFocused() ? 0xFF3D3D3D : theme.getBoardBgColor());
            int border = !this.active ? 0xFF333333 : (this.isHoveredOrFocused() ? hoverBorder : (normalBorder != 0 ? normalBorder : theme.getBoardOutlineColor()));
            int txt = !this.active ? 0xFF666666 : (this.isHoveredOrFocused() ? 0xFFFFFFFF : textColor);

            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            int textW = font.width(this.getMessage());
            int textX = this.getX() + (this.width - textW) / 2;
            int textY = this.getY() + (this.height - font.lineHeight) / 2 + 1;
            guiGraphics.drawString(font, this.getMessage(), textX, textY, txt, false);
        }
    }
}
