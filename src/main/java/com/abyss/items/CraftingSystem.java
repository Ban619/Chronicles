package com.abyss.items;

import java.util.List;
import java.util.Map;

/** The recipes currently available to the player. */
public final class CraftingSystem {

    private static final List<CraftingRecipe> RECIPES = List.of(
            new CraftingRecipe("Health Potion", Map.of("herb", 2, "flask", 1),
                    () -> new Consumable("Health Potion", "Restores 50 HP", "potion_hp", 50, 0, 25)),
            new CraftingRecipe("Mana Potion", Map.of("mushroom", 2, "flask", 1),
                    () -> new Consumable("Mana Potion", "Restores 30 MP", "potion_mp", 0, 30, 20)),
            new CraftingRecipe("Iron Sword", Map.of("iron_ore", 3, "wood", 1),
                    () -> new Equipment("iron_sword", "Iron Sword", "A sturdy sword forged from local ore.",
                            "iron_sword", Equipment.Slot.WEAPON, 0, 0, 0, 0, 0, 0,
                            12, 0, null, 90, Item.Rarity.UNCOMMON)),
            new CraftingRecipe("Leather Armour", Map.of("hide", 4, "thread", 2),
                    () -> new Equipment("leather_armour", "Leather Armour", "Layered hide reinforced with moon thread.",
                            "leather_armour", Equipment.Slot.CHEST, 0, 0, 0, 8, 0, 0,
                            0, 0, null, 75, Item.Rarity.UNCOMMON))
    );

    private CraftingSystem() {}

    public static List<CraftingRecipe> getRecipes() { return RECIPES; }
}
