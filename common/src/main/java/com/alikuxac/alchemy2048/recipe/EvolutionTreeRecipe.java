package com.alikuxac.alchemy2048.recipe;

import com.alikuxac.alchemy2048.core.PresetMigrationLogic;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class EvolutionTreeRecipe implements Recipe<SingleRecipeInput> {
    public static final int REQUIRED_LADDER_SIZE = PresetMigrationLogic.REQUIRED_LADDER_SIZE;
    public static final int MIN_LADDER_SIZE = PresetMigrationLogic.MIN_LADDER_SIZE;
    public static final TagKey<Item> BANNED_OUTPUTS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("alchemy2048", "banned_outputs")
    );

    private final Ingredient starter;
    private final List<ResourceLocation> rawLadder;
    private final List<Item> activeLadder;
    private final int priority;
    private final boolean valid;
    private final int[] remapTable;

    public EvolutionTreeRecipe(Ingredient starter, List<ResourceLocation> rawLadder) {
        this(starter, rawLadder, 0);
    }

    public EvolutionTreeRecipe(Ingredient starter, List<ResourceLocation> rawLadder, int priority) {
        this.starter = starter;
        this.rawLadder = List.copyOf(rawLadder);
        this.priority = priority;

        boolean starterValid = false;
        if (!starter.isEmpty()) {
            ItemStack[] items = starter.getItems();
            if (items.length > 0) {
                starterValid = Arrays.stream(items).anyMatch(stack -> !stack.isEmpty() && !stack.is(Items.AIR));
            }
        }

        PresetMigrationLogic.CompactionResult<ResourceLocation> result = PresetMigrationLogic.compactAndValidate(
                starterValid,
                this.rawLadder,
                id -> {
                    if (!BuiltInRegistries.ITEM.containsKey(id)) return false;
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item == Items.AIR) return false;
                    return !BuiltInRegistries.ITEM.wrapAsHolder(item).is(BANNED_OUTPUTS);
                }
        );

        this.valid = result.isValid();
        this.remapTable = result.getRemapTable();

        List<Item> resolved = new ArrayList<>();
        for (ResourceLocation id : result.getActiveLadder()) {
            resolved.add(BuiltInRegistries.ITEM.get(id));
        }
        this.activeLadder = Collections.unmodifiableList(resolved);
    }

    public Ingredient starter() {
        return starter;
    }

    public List<ResourceLocation> rawLadder() {
        return rawLadder;
    }

    public List<Item> ladder() {
        return activeLadder;
    }

    public List<Item> activeLadder() {
        return activeLadder;
    }

    public int priority() {
        return priority;
    }

    public boolean isValid() {
        return valid;
    }

    public int remapTier(int oldTier) {
        if (oldTier <= 0) return 0;
        if (oldTier < remapTable.length) {
            return remapTable[oldTier];
        }
        return activeLadder.size();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        if (!this.valid) {
            return false;
        }
        return this.starter.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        if (this.valid && !activeLadder.isEmpty()) {
            return new ItemStack(activeLadder.getFirst());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        if (this.valid && !activeLadder.isEmpty()) {
            return new ItemStack(activeLadder.getLast());
        }
        return ItemStack.EMPTY;
    }

    public Item getItemForTier(int tier) {
        if (!valid || tier <= 0 || activeLadder.isEmpty()) {
            return null;
        }
        int index = tier - 1;
        if (index < activeLadder.size()) {
            return activeLadder.get(index);
        }
        return activeLadder.getLast();
    }

    public ItemStack getItemStackForTier(int tier) {
        Item item = getItemForTier(tier);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.EVOLUTION_TREE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.EVOLUTION_TREE_TYPE.get();
    }

    public static boolean remapGrid(byte[] grid, List<ResourceLocation> savedLadder, EvolutionTreeRecipe currentRecipe) {
        if (grid == null || currentRecipe == null || !currentRecipe.isValid()) {
            return false;
        }
        List<ResourceLocation> activeIds = new ArrayList<>();
        for (Item item : currentRecipe.activeLadder()) {
            activeIds.add(BuiltInRegistries.ITEM.getKey(item));
        }
        return PresetMigrationLogic.remapGrid(grid, savedLadder, activeIds, currentRecipe.remapTable);
    }

    public static class Serializer implements RecipeSerializer<EvolutionTreeRecipe> {
        public static final MapCodec<EvolutionTreeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("starter").forGetter(EvolutionTreeRecipe::starter),
                        ResourceLocation.CODEC.listOf().validate(list -> {
                            if (list.size() != REQUIRED_LADDER_SIZE) {
                                return DataResult.error(() -> "Preset ladder must configure exactly " + REQUIRED_LADDER_SIZE + " items for highest value 2048, found: " + list.size());
                            }
                            return DataResult.success(list);
                        }).fieldOf("ladder").forGetter(EvolutionTreeRecipe::rawLadder),
                        Codec.INT.optionalFieldOf("priority", 0).forGetter(EvolutionTreeRecipe::priority)
                ).apply(instance, EvolutionTreeRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, EvolutionTreeRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC,
                EvolutionTreeRecipe::starter,
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
                EvolutionTreeRecipe::rawLadder,
                ByteBufCodecs.VAR_INT,
                EvolutionTreeRecipe::priority,
                EvolutionTreeRecipe::new
        );

        @Override
        public MapCodec<EvolutionTreeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EvolutionTreeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
