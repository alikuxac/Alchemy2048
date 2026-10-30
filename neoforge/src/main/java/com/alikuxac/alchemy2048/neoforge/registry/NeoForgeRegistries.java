package com.alikuxac.alchemy2048.neoforge.registry;

import com.alikuxac.alchemy2048.Alchemy2048Common;
import com.alikuxac.alchemy2048.block.AlchemyMatrixBlock;
import com.alikuxac.alchemy2048.block.entity.AlchemyMatrixBlockEntity;
import com.alikuxac.alchemy2048.menu.AlchemyMatrixMenu;
import com.alikuxac.alchemy2048.recipe.EvolutionTreeRecipe;
import com.alikuxac.alchemy2048.recipe.ModRecipeTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeRegistries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Alchemy2048Common.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Alchemy2048Common.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Alchemy2048Common.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, Alchemy2048Common.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Alchemy2048Common.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Alchemy2048Common.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alchemy2048Common.MOD_ID);

    public static final DeferredHolder<Block, AlchemyMatrixBlock> ALCHEMY_MATRIX_BLOCK = BLOCKS.register("alchemy_matrix", () -> new AlchemyMatrixBlock());

    public static final DeferredHolder<Item, BlockItem> ALCHEMY_MATRIX_ITEM = ITEMS.register("alchemy_matrix",
            () -> new BlockItem(ALCHEMY_MATRIX_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlchemyMatrixBlockEntity>> ALCHEMY_MATRIX_BE = BLOCK_ENTITY_TYPES.register("alchemy_matrix",
            () -> BlockEntityType.Builder.of(AlchemyMatrixBlockEntity::new, ALCHEMY_MATRIX_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<AlchemyMatrixMenu>> ALCHEMY_MATRIX_MENU = MENU_TYPES.register("alchemy_matrix",
            () -> IMenuTypeExtension.create((containerId, playerInv, extraData) -> new AlchemyMatrixMenu(containerId, playerInv)));

    public static final DeferredHolder<RecipeType<?>, RecipeType<EvolutionTreeRecipe>> EVOLUTION_TREE_TYPE = RECIPE_TYPES.register(
            ModRecipeTypes.EVOLUTION_TREE_ID, ModRecipeTypes::createType);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EvolutionTreeRecipe>> EVOLUTION_TREE_SERIALIZER = RECIPE_SERIALIZERS.register(
            ModRecipeTypes.EVOLUTION_TREE_ID, ModRecipeTypes::createSerializer);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("alchemy2048_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.alchemy2048"))
                    .icon(() -> new ItemStack(ALCHEMY_MATRIX_ITEM.get()))
                    .displayItems((params, output) -> output.accept(ALCHEMY_MATRIX_ITEM.get()))
                    .build());

    private NeoForgeRegistries() {}

    public static void register(IEventBus modBus) {
        AlchemyMatrixBlock.BLOCK = ALCHEMY_MATRIX_BLOCK;
        AlchemyMatrixBlockEntity.TYPE = ALCHEMY_MATRIX_BE;
        AlchemyMatrixMenu.TYPE = ALCHEMY_MATRIX_MENU;
        ModRecipeTypes.EVOLUTION_TREE_TYPE = EVOLUTION_TREE_TYPE;
        ModRecipeTypes.EVOLUTION_TREE_SERIALIZER = EVOLUTION_TREE_SERIALIZER;

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
