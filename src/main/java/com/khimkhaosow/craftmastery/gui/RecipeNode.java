package com.khimkhaosow.craftmastery.gui;

import com.khimkhaosow.craftmastery.recipe.RecipeEntry;

/**
 * Узел рецепта в древе рецептов
 */
public class RecipeNode {
    public final RecipeEntry recipe;
    public final float x, y;
    public NodeState state;

    public RecipeNode(RecipeEntry recipe, float x, float y) {
        this.recipe = recipe;
        this.x = x;
        this.y = y;
        this.state = NodeState.LOCKED;
    }
}
