package com.alikuxac.alchemy2048.block.entity;

import com.alikuxac.alchemy2048.config.ServerConfig;
import com.alikuxac.alchemy2048.core.Direction2048;
import com.alikuxac.alchemy2048.core.EnergyLogic;
import com.alikuxac.alchemy2048.core.Grid2048Logic;
import com.alikuxac.alchemy2048.menu.AlchemyMatrixMenu;
import com.alikuxac.alchemy2048.recipe.EvolutionTreeRecipe;
import com.alikuxac.alchemy2048.recipe.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class AlchemyMatrixBlockEntity extends BlockEntity implements MenuProvider {
    public static Supplier<BlockEntityType<AlchemyMatrixBlockEntity>> TYPE;

    public static BiFunction<BlockPos, BlockState, AlchemyMatrixBlockEntity> FACTORY = AlchemyMatrixBlockEntity::new;

    private final byte[] grid = new byte[Grid2048Logic.CELL_COUNT];
    private final List<ResourceLocation> savedLadder = new ArrayList<>();
    private ItemStack starterStack = ItemStack.EMPTY;
    private int score = 0;
    private boolean gameOver = false;
    private boolean safeEjectMode = false;
    @Nullable
    private UUID activePlayerUUID = null;
    @Nullable
    private UUID boardOwnerUUID = null;
    private final Map<UUID, Integer> playerMoves = new HashMap<>();
    private final Random random = new Random();
    private boolean isInitializing = false;

    private final AlchemyEnergyStorage energyStorage = new AlchemyEnergyStorage(
            EnergyLogic::getCapacity,
            EnergyLogic::getMaxReceive,
            this::onEnergyChanged
    );

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 0 && index < Grid2048Logic.CELL_COUNT) {
                return grid[index];
            } else if (index == 16) {
                return score;
            } else if (index == 17) {
                return gameOver ? 1 : 0;
            } else if (index == 18) {
                return energyStorage.getEnergyStored() & 0xFFFF;
            } else if (index == 19) {
                return (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
            } else if (index == 20) {
                return energyStorage.getMaxEnergyStored() & 0xFFFF;
            } else if (index == 21) {
                return (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
            } else if (index == 22) {
                return EnergyLogic.calculateMoveCost(Grid2048Logic.getMaxTier(grid));
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < Grid2048Logic.CELL_COUNT) {
                grid[index] = (byte) value;
            } else if (index == 16) {
                score = value;
            } else if (index == 17) {
                gameOver = (value != 0);
            }
        }

        @Override
        public int getCount() {
            return 23;
        }
    };

    public AlchemyMatrixBlockEntity(BlockPos pos, BlockState blockState) {
        super(TYPE.get(), pos, blockState);
    }

    private void onEnergyChanged() {
        setChanged();
        if (this.level != null && !this.level.isClientSide && !starterStack.isEmpty() && isGridEmpty() && !isInitializing) {
            initGridIfMatchingRecipe();
        }
    }

    public AlchemyEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public byte[] getGrid() {
        return grid;
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public ItemStack getStarterStack() {
        return starterStack;
    }

    public void setStarterStack(ItemStack stack) {
        this.starterStack = stack.copy();
        setChanged();

        if (this.level != null && !this.level.isClientSide) {
            if (!starterStack.isEmpty()) {
                if (isGridEmpty()) {
                    initGridIfMatchingRecipe();
                } else {
                    migrateGridIfNeeded();
                }
            }
        }
    }

    public boolean isGridEmpty() {
        for (byte b : grid) {
            if (b != 0) return false;
        }
        return true;
    }

    public boolean isSafeEjectMode() {
        return safeEjectMode;
    }

    public boolean canPlayerAccess(Player player) {
        if (player == null) return false;
        if (activePlayerUUID == null || activePlayerUUID.equals(player.getUUID())) {
            this.activePlayerUUID = player.getUUID();
            return true;
        }
        if (this.level instanceof ServerLevel serverLevel) {
            Player existing = serverLevel.getPlayerByUUID(activePlayerUUID);
            double maxDistSq = ServerConfig.mutexDistanceBlocks * ServerConfig.mutexDistanceBlocks;
            if (existing == null || !existing.isAlive() || existing.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) > maxDistSq) {
                this.activePlayerUUID = player.getUUID();
                return true;
            }
        }
        return false;
    }

    public void onPlayerClosedMenu(Player player) {
        if (player != null && player.getUUID().equals(this.activePlayerUUID)) {
            this.activePlayerUUID = null;
            setChanged();
        }
    }

    public Optional<RecipeHolder<EvolutionTreeRecipe>> getActiveRecipe() {
        if (this.level == null || this.starterStack.isEmpty()) {
            return Optional.empty();
        }
        List<RecipeHolder<EvolutionTreeRecipe>> recipes = this.level.getRecipeManager()
                .getRecipesFor(ModRecipeTypes.EVOLUTION_TREE_TYPE.get(), new SingleRecipeInput(this.starterStack), this.level);
        return recipes.stream()
                .filter(r -> r.value().isValid())
                .max(Comparator.comparingInt(a -> a.value().priority()));
    }

    public void migrateGridIfNeeded() {
        if (this.level == null || this.level.isClientSide || starterStack.isEmpty() || isGridEmpty()) {
            return;
        }

        Optional<RecipeHolder<EvolutionTreeRecipe>> match = getActiveRecipe();
        if (match.isEmpty() || !match.get().value().isValid()) {
            if (!isGridEmpty()) {
                this.safeEjectMode = true;
                setChanged();
            }
            return;
        }

        this.safeEjectMode = false;
        EvolutionTreeRecipe recipe = match.get().value();

        boolean remapped = EvolutionTreeRecipe.remapGrid(grid, savedLadder, recipe);
        if (remapped) {
            this.score = Grid2048Logic.calculateScore(grid);
            this.gameOver = !Grid2048Logic.canMove(grid);
            setChanged();
        }

        syncSavedLadder(recipe);
    }

    private void syncSavedLadder(EvolutionTreeRecipe recipe) {
        savedLadder.clear();
        for (Item item : recipe.activeLadder()) {
            savedLadder.add(BuiltInRegistries.ITEM.getKey(item));
        }
    }

    private void initGridIfMatchingRecipe() {
        if (this.level == null || isInitializing) return;

        Optional<RecipeHolder<EvolutionTreeRecipe>> match = getActiveRecipe();

        if (match.isPresent() && match.get().value().isValid()) {
            int initCost = EnergyLogic.getBaseCost();
            if (this.energyStorage.getEnergyStored() < initCost) {
                return;
            }
            isInitializing = true;
            try {
                if (!this.energyStorage.consume(initCost)) {
                    return;
                }
                Grid2048Logic.spawnRandomTile(grid, random);
                Grid2048Logic.spawnRandomTile(grid, random);
                this.gameOver = false;
                this.safeEjectMode = false;
                this.score = Grid2048Logic.calculateScore(grid);
                if (this.boardOwnerUUID == null && this.activePlayerUUID != null) {
                    this.boardOwnerUUID = this.activePlayerUUID;
                }
                syncSavedLadder(match.get().value());
                setChanged();
            } finally {
                isInitializing = false;
            }
        }
    }

    public boolean applyMove(Direction2048 direction) {
        Player player = null;
        if (this.activePlayerUUID != null && this.level instanceof ServerLevel sl) {
            player = sl.getPlayerByUUID(this.activePlayerUUID);
        }
        return applyMove(direction, player);
    }

    public boolean applyMove(Direction2048 direction, @Nullable Player player) {
        if (gameOver || starterStack.isEmpty() || safeEjectMode) {
            return false;
        }

        migrateGridIfNeeded();

        Optional<RecipeHolder<EvolutionTreeRecipe>> match = getActiveRecipe();
        if (match.isEmpty() || !match.get().value().isValid()) {
            return false;
        }

        int moveCost = EnergyLogic.calculateMoveCost(Grid2048Logic.getMaxTier(grid));
        if (this.energyStorage.getEnergyStored() < moveCost) {
            return false;
        }

        EvolutionTreeRecipe recipe = match.get().value();
        int starterSlot = -1;
        if (player != null && !player.getAbilities().instabuild) {
            var inv = player.getInventory();
            for (int i = 0; i < 36; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && recipe.starter().test(s)) {
                    starterSlot = i;
                    break;
                }
            }
            if (starterSlot == -1) {
                player.displayClientMessage(Component.translatable("gui.alchemy2048.missing_starter"), true);
                return false;
            }
        }

        boolean moved = Grid2048Logic.slideAndMerge(grid, direction);
        if (moved) {
            if (player != null && !player.getAbilities().instabuild && starterSlot != -1) {
                player.getInventory().removeItem(starterSlot, 1);
                if (player.containerMenu instanceof AlchemyMatrixMenu menu) {
                    int menuSlotIndex = (starterSlot < 9) ? (28 + starterSlot) : (starterSlot - 8);
                    if (menuSlotIndex >= 0 && menuSlotIndex < menu.slots.size()) {
                        menu.getSlot(menuSlotIndex).setChanged();
                    }
                    menu.broadcastChanges();
                }
                player.inventoryMenu.broadcastChanges();
            }

            if (this.activePlayerUUID != null) {
                playerMoves.merge(this.activePlayerUUID, 1, Integer::sum);
            }
            this.energyStorage.consume(moveCost);
            this.score = Grid2048Logic.calculateScore(grid);

            byte maxTier = Grid2048Logic.getMaxTier(grid);
            int winTier = Math.min((int) Grid2048Logic.WIN_TIER, match.get().value().activeLadder().size());

            if (maxTier >= winTier) {

                boolean fullyClaimed = claimAll(player, false);
                if (this.level instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                if (player != null) {
                    player.displayClientMessage(Component.translatable(fullyClaimed
                            ? "message.alchemy2048.victory"
                            : "message.alchemy2048.victory_partial"), true);
                }
                setChanged();
                return true;
            }

            Grid2048Logic.spawnRandomTile(grid, random);
            this.score = Grid2048Logic.calculateScore(grid);
            if (!Grid2048Logic.canMove(grid)) {
                this.gameOver = true;
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.alchemy2048.game_over"), true);
                }
                if (this.level instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, worldPosition, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0f, 0.8f);
                }
            }
            setChanged();
        }
        return moved;
    }

    public void claimHighest(@Nullable Player player) {
        if (level == null || level.isClientSide) return;
        if (isClaimBlockedByMissingRecipe(player)) return;

        migrateGridIfNeeded();

        byte maxTier = Grid2048Logic.getMaxTier(grid);
        if (maxTier <= 0) return;

        Optional<RecipeHolder<EvolutionTreeRecipe>> match = getActiveRecipe();

        ItemStack itemStack = ItemStack.EMPTY;
        if (match.isPresent() && match.get().value().isValid()) {
            itemStack = match.get().value().getItemStackForTier(maxTier);
        } else if (maxTier - 1 < savedLadder.size()) {
            ResourceLocation id = savedLadder.get(maxTier - 1);
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                itemStack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            }
        }

        if (!itemStack.isEmpty()) {
            if (player != null) {
                Component itemName = itemStack.getHoverName();
                if (!giveOrDropClaimedItem(player, itemStack)) {
                    return;
                }
                player.displayClientMessage(Component.translatable("message.alchemy2048.claimed_highest", itemName), true);
            } else {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), itemStack);
            }
            if (this.level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            for (int i = 0; i < grid.length; i++) {
                grid[i] = 0;
            }
            this.score = 0;
            this.gameOver = false;
            this.safeEjectMode = false;
            this.starterStack = ItemStack.EMPTY;
            this.savedLadder.clear();
            setChanged();
        }
    }

    private boolean isClaimBlockedByMissingRecipe(@Nullable Player player) {
        if (!safeEjectMode) {
            return false;
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.alchemy2048.recipe_missing"), true);
        }
        return true;
    }

    public boolean claimAll(@Nullable Player player) {
        return claimAll(player, true);
    }

    public boolean claimAll(@Nullable Player player, boolean applyGachaFail) {
        if (level == null || level.isClientSide) return false;
        if (isGridEmpty()) return false;
        if (isClaimBlockedByMissingRecipe(player)) return false;

        migrateGridIfNeeded();

        Optional<RecipeHolder<EvolutionTreeRecipe>> match = getActiveRecipe();
        EvolutionTreeRecipe recipe = match.map(RecipeHolder::value).orElse(null);

        boolean isCreative = player != null && player.getAbilities().instabuild;
        boolean shouldApplyGacha = applyGachaFail && !isCreative && ServerConfig.gachaFailRate > 0.0;

        int claimedCount = 0;
        int failedCount = 0;
        boolean allClaimed = true;

        for (int i = 0; i < grid.length; i++) {
            if (grid[i] > 0) {
                byte tier = grid[i];
                ItemStack stack = ItemStack.EMPTY;
                if (recipe != null && recipe.isValid()) {
                    stack = recipe.getItemStackForTier(tier);
                } else if (tier - 1 < savedLadder.size()) {
                    ResourceLocation id = savedLadder.get(tier - 1);
                    if (BuiltInRegistries.ITEM.containsKey(id)) {
                        stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                    }
                }

                if (stack.isEmpty()) {
                    allClaimed = false;
                    continue;
                }

                if (shouldApplyGacha && random.nextDouble() < ServerConfig.gachaFailRate) {
                    failedCount++;
                    grid[i] = 0;
                    continue;
                }

                if (player != null) {
                    if (!giveOrDropClaimedItem(player, stack)) {
                        allClaimed = false;
                        continue;
                    }
                } else {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
                claimedCount++;
                grid[i] = 0;
            }
        }

        if (failedCount > 0) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.alchemy2048.claim_all_gacha_fail", failedCount), true);
            }
            if (this.level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0f, 0.9f);
            }
        }

        if (claimedCount > 0) {
            if (failedCount == 0 && player != null) {
                player.displayClientMessage(Component.translatable("message.alchemy2048.all_claimed"), true);
            }
            if (this.level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }

        if (allClaimed) {
            this.score = 0;
            this.gameOver = false;
            this.safeEjectMode = false;
            this.starterStack = ItemStack.EMPTY;
            this.savedLadder.clear();
        }
        setChanged();
        return allClaimed;
    }

    private boolean giveOrDropClaimedItem(Player player, ItemStack stack) {
        boolean isOwner = (boardOwnerUUID == null || boardOwnerUUID.equals(player.getUUID()));
        if (!isOwner) {
            if (ServerConfig.enableContributionRatio) {
                int moves = playerMoves.getOrDefault(player.getUUID(), 0);
                int totalMoves = playerMoves.values().stream().mapToInt(Integer::intValue).sum();
                if (totalMoves > 0 && (double) moves / totalMoves < 0.2) {
                    player.displayClientMessage(Component.translatable("message.alchemy2048.low_contribution"), true);
                }
            } else {
                if (random.nextDouble() < ServerConfig.coopTaxRatio) {
                    player.displayClientMessage(Component.translatable("message.alchemy2048.coop_fee_applied"), true);
                    return false;
                }
            }
        }
        giveOrDropItem(player, stack);
        return true;
    }

    public void resetGrid() {
        for (int i = 0; i < grid.length; i++) {
            grid[i] = 0;
        }
        this.score = 0;
        this.gameOver = false;
        this.safeEjectMode = false;
        this.savedLadder.clear();
        initGridIfMatchingRecipe();
        setChanged();
    }

    private void giveOrDropItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public void dropInventoryAndRemnants(Level level, BlockPos pos) {
        if (!starterStack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), starterStack);
            starterStack = ItemStack.EMPTY;
        }
        EvolutionTreeRecipe recipe = null;
        if (savedLadder.isEmpty()) {
            recipe = getActiveRecipe().map(RecipeHolder::value).orElse(null);
        }
        for (int i = 0; i < grid.length; i++) {
            byte tier = grid[i];
            if (tier > 0) {
                ItemStack stack = ItemStack.EMPTY;
                if (tier - 1 < savedLadder.size()) {
                    ResourceLocation id = savedLadder.get(tier - 1);
                    if (BuiltInRegistries.ITEM.containsKey(id)) {
                        stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                    }
                } else if (recipe != null) {
                    stack = recipe.getItemStackForTier(tier);
                }
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
                grid[i] = 0;
            }
        }
        this.score = 0;
        this.gameOver = false;
        this.safeEjectMode = false;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByteArray("Grid", grid);
        tag.putInt("Score", score);
        tag.putBoolean("GameOver", gameOver);
        tag.putBoolean("SafeEject", safeEjectMode);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        if (boardOwnerUUID != null) {
            tag.putUUID("BoardOwner", boardOwnerUUID);
        }
        if (!starterStack.isEmpty()) {
            tag.put("Starter", starterStack.save(registries));
        }
        if (!savedLadder.isEmpty()) {
            ListTag ladderTag = new ListTag();
            for (ResourceLocation loc : savedLadder) {
                ladderTag.add(StringTag.valueOf(loc.toString()));
            }
            tag.put("Ladder", ladderTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isInitializing = true;
        try {
            if (tag.contains("Grid")) {
                byte[] loaded = tag.getByteArray("Grid");
                System.arraycopy(loaded, 0, grid, 0, Math.min(loaded.length, grid.length));
            }
            score = tag.getInt("Score");
            gameOver = tag.getBoolean("GameOver");
            safeEjectMode = tag.getBoolean("SafeEject");
            if (tag.hasUUID("BoardOwner")) {
                boardOwnerUUID = tag.getUUID("BoardOwner");
            }
            if (tag.contains("Energy")) {
                energyStorage.setEnergy(tag.getInt("Energy"));
            }
            if (tag.contains("Starter")) {
                starterStack = ItemStack.parseOptional(registries, tag.getCompound("Starter"));
            } else {
                starterStack = ItemStack.EMPTY;
            }
            savedLadder.clear();
            if (tag.contains("Ladder", Tag.TAG_LIST)) {
                ListTag ladderTag = tag.getList("Ladder", Tag.TAG_STRING);
                for (int i = 0; i < ladderTag.size(); i++) {
                    ResourceLocation loc = ResourceLocation.tryParse(ladderTag.getString(i));
                    if (loc != null) {
                        savedLadder.add(loc);
                    }
                }
            }
        } finally {
            isInitializing = false;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.alchemy2048.alchemy_matrix");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AlchemyMatrixMenu(containerId, playerInventory, this, this.dataAccess);
    }
}

