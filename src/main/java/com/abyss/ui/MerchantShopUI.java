package com.abyss.ui;

import com.abyss.entities.Player;
import com.abyss.items.Consumable;
import com.abyss.items.Equipment;
import com.abyss.items.Item;
import com.abyss.utils.InputHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Small keyboard-driven shop for the village and forest merchants. */
public final class MerchantShopUI {

    private boolean open;

    public boolean isOpen() { return open; }
    public void open() { open = true; }
    public void close() { open = false; }

    public String handleInput(InputHandler input, Player player) {
        if (!open) return "";
        if (input.isJustPressed("escape") || input.isJustPressed("e")) {
            close();
            return "You leave the shop.";
        }
        if (input.isJustPressed("1")) {
            return purchase(player, new Consumable("Health Potion", "Restores 50 HP.",
                    "potion_hp", 50, 0, 25), 25);
        }
        if (input.isJustPressed("2")) {
            return purchase(player, new Consumable("Mana Potion", "Restores 30 MP.",
                    "potion_mp", 0, 30, 20), 20);
        }
        if (input.isJustPressed("3")) {
            return purchase(player, new Equipment("iron_sword", "Iron Sword",
                    "A dependable blade. +3 attack and +2 strength.", "iron_sword",
                    Equipment.Slot.WEAPON, 0, 0, 2, 0, 0, 0,
                    3, 0, null, 85, Item.Rarity.UNCOMMON), 85);
        }
        return "";
    }

    private String purchase(Player player, Item item, int cost) {
        if (player.getGold() < cost) return "You need " + cost + " gold for that.";
        if (!player.canAddItem(item)) return "Your inventory is full.";
        if (!player.addItem(item)) return "The purchase could not be added to your inventory.";
        player.spendGold(cost);
        return "Purchased " + item.getDisplayName() + " for " + cost + " gold.";
    }

    public void render(GraphicsContext gc, double width, double height, Player player) {
        if (!open) return;
        double panelWidth = Math.min(520, width - 32);
        double panelHeight = 300;
        double x = (width - panelWidth) / 2;
        double y = (height - panelHeight) / 2;

        gc.setFill(Color.color(0, 0, 0, 0.72));
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.web("#11131b", 0.98));
        gc.fillRoundRect(x, y, panelWidth, panelHeight, 14, 14);
        gc.setStroke(Color.web("#c8a96e"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, panelWidth, panelHeight, 14, 14);

        gc.setFill(Color.web("#ead4a4"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.fillText("MERCHANT", x + 24, y + 38);
        gc.setFill(Color.web("#f0c040"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        gc.fillText("Gold: " + player.getGold(), x + panelWidth - 105, y + 38);

        String[] offers = {
                "[1] Health Potion     25g     Restore 50 HP",
                "[2] Mana Potion       20g     Restore 30 MP",
                "[3] Iron Sword        85g     +3 attack, +2 strength"
        };
        gc.setFont(Font.font("Serif", 15));
        for (int i = 0; i < offers.length; i++) {
            gc.setFill(i == 2 ? Color.web("#d8c3ff") : Color.web("#d4c5a9"));
            gc.fillText(offers[i], x + 24, y + 92 + i * 44);
        }
        gc.setFill(Color.web("#aaa28e"));
        gc.setFont(Font.font("Serif", 12));
        gc.fillText("[1–3] Buy     [E / ESC] Leave", x + 24, y + panelHeight - 22);
    }
}
