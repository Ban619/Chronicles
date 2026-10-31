package com.abyss.items;

import com.abyss.entities.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.function.Supplier;

/** A recipe with item-ID based ingredients and a freshly created output item. */
public final class CraftingRecipe {

    private final String name;
    private final Map<String, Integer> ingredients;
    private final Supplier<Item> outputFactory;

    public CraftingRecipe(String name, Map<String, Integer> ingredients,
                          Supplier<Item> outputFactory) {
        this.name = name;
        this.ingredients = Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
        this.outputFactory = outputFactory;
    }

    public String getName() { return name; }
    public Map<String, Integer> getIngredients() { return ingredients; }
    public Item createOutput() { return outputFactory.get(); }

    public boolean canCraft(Player player) {
        for (Map.Entry<String, Integer> ingredient : ingredients.entrySet()) {
            if (player.countItems(ingredient.getKey()) < ingredient.getValue()) return false;
        }
        return player.canAddItemAfterRemoving(createOutput(), ingredients);
    }

    public String requirementsText() {
        return ingredients.entrySet().stream()
                .map(entry -> entry.getValue() + " " + materialName(entry.getKey()))
                .reduce((left, right) -> left + " + " + right)
                .orElse("No ingredients");
    }

    public String requirementsText(Player player) {
        return ingredients.entrySet().stream()
                .map(entry -> entry.getValue() + " " + materialName(entry.getKey())
                        + " (" + player.countItems(entry.getKey()) + ")")
                .reduce((left, right) -> left + " + " + right)
                .orElse("No ingredients");
    }

    private static String materialName(String id) {
        return switch (id) {
            case "herb" -> "Duskleaf Herb";
            case "flask" -> "Glass Flask";
            case "mushroom" -> "Gloomcap Mushroom";
            case "iron_ore" -> "Iron Ore";
            case "wood" -> "Ashwood";
            case "hide" -> "Beast Hide";
            case "thread" -> "Moon Thread";
            default -> id;
        };
    }

    /** Crafts only when both the ingredients and output slot are available. */
    public boolean craft(Player player) {
        Item output = createOutput();
        if (!player.canAddItemAfterRemoving(output, ingredients) || !player.removeItems(ingredients)) return false;
        return player.addItem(output);
    }
}
