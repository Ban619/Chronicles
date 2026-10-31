package com.abyss.items;

/** Stackable crafting material collected from the world. */
public final class Material extends Item {

    public Material(String id, String displayName, String description, String iconKey,
                    int count, int maxStack, int goldValue, Rarity rarity) {
        super(id, displayName, description, iconKey, count, maxStack, goldValue, rarity);
    }

    @Override
    public Item clone(int newCount) {
        return new Material(id, displayName, description, iconKey, newCount,
                maxStack, goldValue, rarity);
    }
}
