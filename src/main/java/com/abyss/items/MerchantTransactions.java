package com.abyss.items;

import com.abyss.entities.Player;

/** Validates and applies a merchant purchase without charging for failed deliveries. */
public final class MerchantTransactions {

    private MerchantTransactions() {}

    public static String purchase(Player player, Item item, int cost) {
        if (cost < 0 || item == null) return "That offer is unavailable.";
        if (player.getGold() < cost) return "You need " + cost + " gold for that.";
        if (!player.canAddItem(item)) return "Your inventory is full.";
        if (!player.addItem(item)) return "The purchase could not be added to your inventory.";
        player.spendGold(cost);
        return "Purchased " + item.getDisplayName() + " for " + cost + " gold.";
    }
}
