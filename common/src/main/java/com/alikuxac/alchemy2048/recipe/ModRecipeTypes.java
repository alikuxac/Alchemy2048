package com.alikuxac.alchemy2048.recipe;

import com.alikuxac.alchemy2048.Alchemy2048Common;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.Supplier;

public final class ModRecipeTypes {
    public static final String EVOLUTION_TREE_ID = "evolution_tree";

    public static Supplier<RecipeType<EvolutionTreeRecipe>> EVOLUTION_TREE_TYPE;
    public static Supplier<RecipeSerializer<EvolutionTreeRecipe>> EVOLUTION_TREE_SERIALIZER;

    private ModRecipeTypes() {}

    public static RecipeType<EvolutionTreeRecipe> createType() {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return Alchemy2048Common.id(EVOLUTION_TREE_ID).toString();
            }
        };
    }

    public static RecipeSerializer<EvolutionTreeRecipe> createSerializer() {
        return new EvolutionTreeRecipe.Serializer();
    }
}
