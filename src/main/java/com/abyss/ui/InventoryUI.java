package com.abyss.ui;

import com.abyss.entities.Player;
import com.abyss.items.CraftingRecipe;
import com.abyss.items.CraftingSystem;
import com.abyss.items.Equipment;
import com.abyss.items.Item;
import com.abyss.world.GameMap;
import com.abyss.utils.InputHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/** Inventory, equipment, and crafting overlay. */
public final class InventoryUI {

    private static final int COLS = 6;
    private static final int ROWS = 4;

    private boolean open;
    private int activeTab;
    private int cursorRow;
    private int cursorCol;
    private int equipmentCursor;
    private int recipeCursor;

    public void open() { open = true; }
    public void close() { open = false; }
    public void toggle() { open = !open; }
    public boolean isOpen() { return open; }

    public void render(GraphicsContext gc, double width, double height, Player player) {
        if (!open) return;
        gc.setFill(Color.color(0, 0, 0, 0.76));
        gc.fillRect(0, 0, width, height);

        double panelW = Math.min(width - 32, 780);
        double panelH = Math.min(height - 32, 520);
        double x = (width - panelW) / 2;
        double y = (height - panelH) / 2;

        gc.setFill(Color.color(0.05, 0.05, 0.1, 0.98));
        gc.fillRoundRect(x, y, panelW, panelH, 10, 10);
        gc.setStroke(Color.web("#4b4233"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, panelW, panelH, 10, 10);

        gc.setFill(Color.web("#c8a96e"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        gc.fillText("INVENTORY", x + 20, y + 30);
        gc.setFill(Color.web("#8d846f"));
        gc.setFont(Font.font("Serif", 11));
        gc.fillText("[I] or [ESC] Close", x + panelW - 132, y + 29);
        renderTabs(gc, x + 12, y + 42, panelW - 24);

        double contentX = x + 18;
        double contentY = y + 84;
        double contentW = panelW - 36;
        double contentH = panelH - 96;
        switch (activeTab) {
            case 0 -> renderItems(gc, contentX, contentY, contentW, contentH, player);
            case 1 -> renderEquipment(gc, contentX, contentY, contentW, contentH, player);
            case 2 -> renderCrafting(gc, contentX, contentY, contentW, contentH, player);
        }
    }

    private void renderTabs(GraphicsContext gc, double x, double y, double width) {
        String[] names = {"Items", "Equipment", "Crafting"};
        double tabW = width / names.length;
        for (int i = 0; i < names.length; i++) {
            boolean selected = activeTab == i;
            gc.setFill(selected ? Color.web("#c8a96e") : Color.web("#191927"));
            gc.fillRoundRect(x + tabW * i, y, tabW - 3, 28, 5, 5);
            gc.setFill(selected ? Color.web("#11111a") : Color.web("#c4b99f"));
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 12));
            gc.fillText(names[i], x + tabW * i + tabW / 2 - names[i].length() * 3.3, y + 18);
        }
    }

    private void renderItems(GraphicsContext gc, double x, double y, double width,
                             double height, Player player) {
        double gap = 4;
        double cell = Math.min(50, Math.max(28, Math.min(
                (width * 0.56 - (COLS - 1) * gap) / COLS,
                (height - (ROWS - 1) * gap - 24) / ROWS)));
        double detailX = x + COLS * (cell + gap) + 18;
        Item selected = null;
        Item[] inventory = player.getInventory();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                double sx = x + col * (cell + gap);
                double sy = y + row * (cell + gap);
                boolean focused = row == cursorRow && col == cursorCol;
                gc.setFill(focused ? Color.color(0.5, 0.42, 0.27, 0.3) : Color.color(0, 0, 0, 0.36));
                gc.fillRoundRect(sx, sy, cell, cell, 5, 5);
                gc.setStroke(focused ? Color.web("#c8a96e") : Color.web("#363445"));
                gc.setLineWidth(focused ? 1.7 : 1);
                gc.strokeRoundRect(sx, sy, cell, cell, 5, 5);
                if (index >= inventory.length || inventory[index] == null) continue;
                Item item = inventory[index];
                if (focused) selected = item;
                gc.setFill(Color.web(item.getRarityHex()));
                gc.setFont(Font.font("Serif", FontWeight.BOLD, 18));
                gc.fillText(item.getDisplayName().substring(0, 1).toUpperCase(), sx + cell / 2 - 5, sy + cell / 2 + 6);
                if (item.getCount() > 1) {
                    gc.setFill(Color.web("#f0c040"));
                    gc.setFont(Font.font("Serif", 9));
                    gc.fillText("x" + item.getCount(), sx + cell - 22, sy + cell - 4);
                }
            }
        }

        if (selected != null) {
            gc.setFill(Color.web(selected.getRarityHex()));
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 14));
            gc.fillText(selected.getDisplayName(), detailX, y + 18);
            gc.setFill(Color.web("#a69b85"));
            gc.setFont(Font.font("Serif", 11));
            gc.fillText(selected.getRarity().name(), detailX, y + 36);
            drawWrapped(gc, selected.getDescription(), detailX, y + 56,
                    Math.max(90, x + width - detailX - 8), 15, Color.web("#bdb39e"));
            gc.setFill(Color.web("#f0c040"));
            gc.fillText("Value: " + selected.getGoldValue() + "g", detailX, y + 105);
            gc.setFill(Color.web("#88806d"));
            gc.setFont(Font.font("Serif", 10));
            gc.fillText("[ENTER/U] Use   [E] Equip   [D] Drop", detailX, y + 132);
        } else {
            gc.setFill(Color.web("#88806d"));
            gc.setFont(Font.font("Serif", 12));
            gc.fillText("Choose an item", detailX, y + 20);
        }

        gc.setFill(Color.web("#f0c040"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 12));
        gc.fillText("Gold: " + player.getGold(), x, y + height - 8);
        gc.setFill(Color.web("#88806d"));
        gc.setFont(Font.font("Serif", 10));
        gc.fillText("Items stack automatically when collected.", x + 110, y + height - 8);
    }

    private void renderEquipment(GraphicsContext gc, double x, double y, double width,
                                 double height, Player player) {
        Equipment.Slot[] slots = Equipment.Slot.values();
        double slotW = width / 3;
        double rowPitch = Math.min(54, Math.max(36, (height - 74) / 3));
        for (int i = 0; i < slots.length; i++) {
            int col = i % 3;
            int row = i / 3;
            double sx = x + col * slotW;
            double sy = y + row * rowPitch;
            boolean focused = equipmentCursor == i;
            gc.setFill(Color.color(0, 0, 0, 0.35));
            gc.fillRoundRect(sx + 4, sy, slotW - 10, rowPitch - 8, 5, 5);
            gc.setStroke(focused ? Color.web("#c8a96e") : Color.web("#363445"));
            gc.setLineWidth(focused ? 1.6 : 1);
            gc.strokeRoundRect(sx + 4, sy, slotW - 10, rowPitch - 8, 5, 5);
            gc.setFill(Color.web("#94866c"));
            gc.setFont(Font.font("Serif", 10));
            gc.fillText(prettySlot(slots[i]), sx + 12, sy + 15);
            Equipment item = player.getEquipment().getSlot(slots[i]);
            gc.setFill(item == null ? Color.web("#565262") : Color.web(item.getRarityHex()));
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 11));
            gc.fillText(item == null ? "Empty" : item.getDisplayName(), sx + 12, sy + rowPitch - 15);
        }
        Equipment equipment = player.getEquipment();
        double statsY = y + height - 58;
        gc.setFill(Color.web("#c8a96e"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 12));
        gc.fillText("Equipment Bonuses", x + 6, statsY);
        gc.setFill(Color.web("#c3b99f"));
        gc.setFont(Font.font("Serif", 11));
        gc.fillText("ATK +" + equipment.getTotalAttack() + "   DEF +" + (equipment.getTotalBonusDef() + equipment.getTotalArmour())
                + "   STR +" + equipment.getTotalBonusStr() + "   HP +" + equipment.getTotalBonusHp()
                + "   MP +" + equipment.getTotalBonusMp(), x + 6, statsY + 19);
        gc.setFill(Color.web("#88806d"));
        gc.setFont(Font.font("Serif", 10));
        gc.fillText("[ENTER/E] Unequip selected   Equip items from the Items tab with [E].", x + 6, statsY + 40);
    }

    private void renderCrafting(GraphicsContext gc, double x, double y, double width,
                                double height, Player player) {
        List<CraftingRecipe> recipes = CraftingSystem.getRecipes();
        double rowPitch = Math.min(58, (height - 24) / recipes.size());
        double cardHeight = Math.max(32, rowPitch - 4);
        for (int i = 0; i < recipes.size(); i++) {
            CraftingRecipe recipe = recipes.get(i);
            double ry = y + 4 + i * rowPitch;
            boolean selected = i == recipeCursor;
            boolean craftable = recipe.canCraft(player);
            gc.setFill(selected ? Color.color(0.5, 0.42, 0.27, 0.24) : Color.color(0, 0, 0, 0.3));
            gc.fillRoundRect(x + 4, ry, width - 8, cardHeight, 5, 5);
            gc.setStroke(selected ? Color.web("#c8a96e") : Color.web("#363445"));
            gc.setLineWidth(selected ? 1.6 : 1);
            gc.strokeRoundRect(x + 4, ry, width - 8, cardHeight, 5, 5);
            gc.setFill(Color.web("#e1d4b7"));
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 12));
            gc.fillText(recipe.getName(), x + 14, ry + 18);
            gc.setFill(Color.web("#a69b85"));
            gc.setFont(Font.font("Serif", 10));
            gc.fillText("Needs: " + recipe.requirementsText(player), x + 14, ry + 36);
            Item output = recipe.createOutput();
            gc.setFill(Color.web(output.getRarityHex()));
            gc.setFont(Font.font("Serif", 11));
            gc.fillText("Makes: " + output.getDisplayName(), x + width * 0.56, ry + 18);
            gc.setFill(craftable ? Color.web("#68c58a") : Color.web("#7b5660"));
            gc.setFont(Font.font("Serif", 10));
            gc.fillText(craftable ? "Ready" : "Missing materials or space", x + width * 0.56, ry + 36);
        }
        gc.setFill(Color.web("#88806d"));
        gc.setFont(Font.font("Serif", 10));
        gc.fillText("Defeat enemies and collect their drops to find crafting materials. [ENTER/C] Craft.",
                x + 6, y + height - 8);
    }

    private void drawWrapped(GraphicsContext gc, String text, double x, double y,
                             double maxWidth, double lineHeight, Color color) {
        gc.setFill(color);
        gc.setFont(Font.font("Serif", 11));
        StringBuilder line = new StringBuilder();
        double baseline = y;
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && gc.getFont().getSize() * candidate.length() * 0.55 > maxWidth) {
                gc.fillText(line.toString(), x, baseline);
                baseline += lineHeight;
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) gc.fillText(line.toString(), x, baseline);
    }

    private String prettySlot(Equipment.Slot slot) {
        String name = slot.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** Returns a short notification when an action succeeds or needs attention. */
    public String handleInput(InputHandler input, Player player, GameMap map) {
        if (!open) return "";
        if (input.isJustPressed("tab")) {
            activeTab = (activeTab + 1) % 3;
            return "";
        }
        if (activeTab == 0) return handleItemInput(input, player, map);
        if (activeTab == 1) return handleEquipmentInput(input, player);
        return handleCraftingInput(input, player);
    }

    private String handleItemInput(InputHandler input, Player player, GameMap map) {
        if (input.isJustPressed("up")) cursorRow = Math.max(0, cursorRow - 1);
        if (input.isJustPressed("down")) cursorRow = Math.min(ROWS - 1, cursorRow + 1);
        if (input.isJustPressed("left")) cursorCol = Math.max(0, cursorCol - 1);
        if (input.isJustPressed("right")) cursorCol = Math.min(COLS - 1, cursorCol + 1);
        int slot = cursorRow * COLS + cursorCol;
        Item item = player.getInventory()[slot];
        if (item == null) return "";
        if (input.isJustPressed("u") || input.isJustPressed("enter")) {
            return player.useInventoryItem(slot) ? "Used " + item.getDisplayName() + "."
                    : item instanceof com.abyss.items.Consumable ? "That item cannot help you right now." : "That item cannot be used.";
        }
        if (input.isJustPressed("e")) {
            return player.equipInventoryItem(slot) ? "Equipped " + item.getDisplayName() + "."
                    : "Select an equipment item to equip it.";
        }
        if (input.isJustPressed("d")) {
            if (player.removeItem(slot)) {
                map.dropItem(item, player.getX() + player.getWidth() + 4, player.getY());
                return "Dropped " + item + ".";
            }
        }
        return "";
    }

    private String handleEquipmentInput(InputHandler input, Player player) {
        if (input.isJustPressed("up")) equipmentCursor = Math.max(0, equipmentCursor - 3);
        if (input.isJustPressed("down")) equipmentCursor = Math.min(Equipment.Slot.values().length - 1, equipmentCursor + 3);
        if (input.isJustPressed("left")) equipmentCursor = Math.max(0, equipmentCursor - 1);
        if (input.isJustPressed("right")) equipmentCursor = Math.min(Equipment.Slot.values().length - 1, equipmentCursor + 1);
        if (input.isJustPressed("enter") || input.isJustPressed("e")) {
            Equipment.Slot slot = Equipment.Slot.values()[equipmentCursor];
            Equipment item = player.getEquipment().getSlot(slot);
            if (item == null) return "Nothing is equipped in that slot.";
            return player.unequipItem(slot) ? "Unequipped " + item.getDisplayName() + "." : "Inventory is full.";
        }
        return "";
    }

    private String handleCraftingInput(InputHandler input, Player player) {
        List<CraftingRecipe> recipes = CraftingSystem.getRecipes();
        if (input.isJustPressed("up")) recipeCursor = Math.max(0, recipeCursor - 1);
        if (input.isJustPressed("down")) recipeCursor = Math.min(recipes.size() - 1, recipeCursor + 1);
        if (input.isJustPressed("enter") || input.isJustPressed("c")) {
            CraftingRecipe recipe = recipes.get(recipeCursor);
            if (recipe.craft(player)) return "Crafted " + recipe.getName() + ".";
            return recipe.canCraft(player) ? "Crafting failed." : "Need more materials or inventory space.";
        }
        return "";
    }
}
