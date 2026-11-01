package com.abyss.items;

import com.abyss.entities.Player;
import com.abyss.utils.AssetLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MerchantTransactionsTest {

    @Test
    void purchaseChargesOnlyAfterItemIsDelivered() {
        Player player = new Player("Tester", 0, 0, AssetLoader.getInstance());
        Consumable potion = new Consumable("Health Potion", "Restores health.",
                "potion_hp", 50, 0, 25);

        assertTrue(MerchantTransactions.purchase(player, potion, 25).startsWith("Purchased"));
        assertEquals(125, player.getGold());
        assertEquals(2, player.countItems("potion_hp"));
    }

    @Test
    void insufficientGoldOrFullInventoryDoesNotCharge() {
        Player player = new Player("Tester", 0, 0, AssetLoader.getInstance());
        Item expensive = new Material("expensive", "Expensive", "", "expensive",
                1, 1, 200, Item.Rarity.COMMON);

        assertTrue(MerchantTransactions.purchase(player, expensive, 200).contains("need 200"));
        assertEquals(150, player.getGold());

        for (int i = 0; i < Player.INVENTORY_SIZE; i++) {
            player.getInventory()[i] = new Material("item-" + i, "Item", "",
                    "item", 1, 1, 0, Item.Rarity.COMMON);
        }
        assertEquals("Your inventory is full.",
                MerchantTransactions.purchase(player, expensive, 25));
        assertEquals(150, player.getGold());
    }
}
