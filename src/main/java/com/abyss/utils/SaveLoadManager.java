package com.abyss.utils;

import com.abyss.entities.Player;
import com.abyss.items.Consumable;
import com.abyss.items.Equipment;
import com.abyss.items.Item;
import com.abyss.items.Material;
import com.abyss.world.WorldManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**                     
 * SaveLoadManager — Serialises and deserialises the complete game state.
 *
 * Save slots: 3 named slots + 1 auto-save slot.
 * Format: JSON (human-readable, easy to inspect/mod).
 * Location: System user home / .chronicles_abyss / saves /
 *
 * Save structure (JSON):
 * {
 *   "version":   "1.0.0",
 *   "timestamp": "2025-08-01T14:32:00",
 *   "player":    { ...Player stats... },
 *   "map":       { "name": "Ashenveil", "x": 320, "y": 256 },
 *   "quests":    [ { "id": "q001", "complete": false } ],
 *   "settings":  { "masterVol": 80, "difficulty": "Normal" }
 * }
 */
public class SaveLoadManager {

    private static final Logger LOG    = Logger.getLogger(SaveLoadManager.class.getName());
    private static final String SAVE_DIR = System.getProperty("user.home")
                                           + File.separator + ".chronicles_abyss"
                                           + File.separator + "saves";
    private static final int   SLOTS  = 3;

    private final ObjectMapper mapper;

    // ─────────────────────────────────────────────────────────────────────
    public SaveLoadManager() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ensureSaveDir();
    }

    private void ensureSaveDir() {
        File dir = new File(SAVE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            LOG.warning("[Save] Could not create save directory: " + SAVE_DIR);
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────
    /**
     * Saves the current game state to the specified slot (1–3) or 0 for auto-save.
     */
    public boolean save(int slot, Player player, String mapName) {
        return save(slot, player, mapName, null);
    }

    public boolean save(int slot, Player player, String mapName, String mapPath) {
        if (!validSlot(slot)) return false;
        Map<String, Object> data = buildSaveData(player, mapName, mapPath);
        String filename = slotFilename(slot);
        try {
            mapper.writeValue(new File(SAVE_DIR + File.separator + filename), data);
            LOG.info("[Save] Game saved to slot " + slot + " (" + filename + ")");
            return true;
        } catch (IOException e) {
            LOG.severe("[Save] Failed to save: " + e.getMessage());
            return false;
        }
    }

    /** Auto-save (slot 0). */
    public void autoSave(Player player, String mapName) {
        save(0, player, mapName);
    }

    // ── Load ──────────────────────────────────────────────────────────────
    /**
     * Loads a save file and applies it to the player.
     * @return true on success, false if file missing or corrupt.
     */
    @SuppressWarnings("unchecked")
    public boolean load(int slot, Player player) {
        if (!validSlot(slot)) return false;
        String path = SAVE_DIR + File.separator + slotFilename(slot);
        File file = new File(path);
        if (!file.exists()) {
            LOG.info("[Save] No save in slot " + slot);
            return false;
        }
        try {
            Map<String, Object> data = mapper.readValue(file, Map.class);
            Map<String, Object> playerData = (Map<String, Object>) data.get("player");
            applyPlayerData(player, playerData);
            restoreInventory(player, playerData);
            LOG.info("[Save] Loaded slot " + slot);
            return true;
        } catch (Exception e) {
            LOG.severe("[Save] Failed to load slot " + slot + ": " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean load(int slot, Player player, WorldManager world) {
        if (!validSlot(slot)) return false;
        File file = new File(SAVE_DIR + File.separator + slotFilename(slot));
        if (!file.exists()) return false;
        try {
            Map<String, Object> data = mapper.readValue(file, Map.class);
            Map<String, Object> playerData = (Map<String, Object>) data.get("player");
            Map<String, Object> mapData = (Map<String, Object>) data.get("map");
            if (playerData == null || mapData == null) return false;
            String mapPath = mapData.get("path") instanceof String path ? path : null;
            if (mapPath != null && !world.canLoadSavedLocation(mapPath)) return false;
            player.restoreFromSaveMap(playerData);
            restoreInventory(player, playerData);
            float x = player.getX();
            float y = player.getY();
            if (mapPath != null && !world.loadSavedLocation(mapPath, x, y, player)) return false;
            return true;
        } catch (Exception e) {
            LOG.severe("[Save] Failed to load slot " + slot + ": " + e.getMessage());
            return false;
        }
    }

    // ── Save data builder ─────────────────────────────────────────────────
    private Map<String, Object> buildSaveData(Player player, String mapName, String mapPath) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version",   "1.0.0");
        root.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Player stats
        Map<String, Object> playerData = new LinkedHashMap<>(player.toSaveMap());
        List<Object> inventory = new ArrayList<>(player.getInventory().length);
        for (Item item : player.getInventory()) inventory.add(serializeItem(item));
        playerData.put("inventory", inventory);

        Map<String, Object> equipped = new LinkedHashMap<>();
        for (Equipment.Slot slot : Equipment.Slot.values()) {
            equipped.put(slot.name(), serializeItem(player.getEquipment().getSlot(slot)));
        }
        playerData.put("equipment", equipped);
        root.put("player", playerData);

        // Map position
        Map<String, Object> mapData = new LinkedHashMap<>();
        mapData.put("name", mapName);
        if (mapPath != null) mapData.put("path", mapPath);
        mapData.put("x", player.getX());
        mapData.put("y", player.getY());
        root.put("map", mapData);

        return root;
    }

    private void applyPlayerData(Player player, Map<String, Object> data) {
        player.restoreFromSaveMap(data);
    }

    private Map<String, Object> serializeItem(Item item) {
        if (item == null) return null;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kind", item instanceof Consumable ? "CONSUMABLE"
                : item instanceof Equipment ? "EQUIPMENT"
                : item instanceof Material ? "MATERIAL" : "UNKNOWN");
        data.put("id", item.getId());
        data.put("name", item.getDisplayName());
        data.put("description", item.getDescription());
        data.put("iconKey", item.getIconKey());
        data.put("count", item.getCount());
        data.put("maxStack", item.getMaxStack());
        data.put("goldValue", item.getGoldValue());
        data.put("rarity", item.getRarity().name());
        if (item instanceof Consumable consumable) {
            data.put("hpRestore", consumable.getHpRestore());
            data.put("mpRestore", consumable.getMpRestore());
            data.put("xpBonus", consumable.getXpBonus());
        } else if (item instanceof Equipment equipment) {
            data.put("slot", equipment.getSlot() == null ? null : equipment.getSlot().name());
            data.put("bonusHp", equipment.getBonusHp());
            data.put("bonusMp", equipment.getBonusMp());
            data.put("bonusStr", equipment.getBonusStr());
            data.put("bonusDef", equipment.getBonusDef());
            data.put("bonusSpd", equipment.getBonusSpd());
            data.put("bonusLck", equipment.getBonusLck());
            data.put("attackPower", equipment.getAttackPower());
            data.put("armourRating", equipment.getArmourRating());
            data.put("specialEffect", equipment.getSpecialEffect());
        } else if (item instanceof Material material) {
            data.put("maxStack", material.getMaxStack());
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private void restoreInventory(Player player, Map<String, Object> data) {
        Object rawInventory = data.get("inventory");
        if (rawInventory instanceof List<?> entries) {
            Item[] inventory = player.getInventory();
            java.util.Arrays.fill(inventory, null);
            for (int index = 0; index < Math.min(entries.size(), inventory.length); index++) {
                if (entries.get(index) instanceof Map<?, ?> itemData) {
                    inventory[index] = deserializeItem((Map<String, Object>) itemData);
                }
            }
        }

        Object rawEquipment = data.get("equipment");
        if (rawEquipment instanceof Map<?, ?> equipmentData) {
            for (Equipment.Slot slot : Equipment.Slot.values()) player.getEquipment().unequip(slot);
            for (Object rawItem : equipmentData.values()) {
                if (rawItem instanceof Map<?, ?> itemData) {
                    Item item = deserializeItem((Map<String, Object>) itemData);
                    if (item instanceof Equipment equipment) player.getEquipment().equip(equipment);
                }
            }
        }
        player.clampVitalsToEquipment();
    }

    private Item deserializeItem(Map<String, Object> data) {
        String kind = stringValue(data, "kind", "UNKNOWN");
        String id = stringValue(data, "id", "unknown");
        String name = stringValue(data, "name", id);
        String description = stringValue(data, "description", "");
        String iconKey = stringValue(data, "iconKey", id);
        int count = Math.max(1, intValue(data, "count", 1));
        int gold = Math.max(0, intValue(data, "goldValue", 0));
        Item.Rarity rarity;
        try { rarity = Item.Rarity.valueOf(stringValue(data, "rarity", "COMMON")); }
        catch (IllegalArgumentException ex) { rarity = Item.Rarity.COMMON; }

        if (kind.equals("CONSUMABLE")) {
            return new Consumable(id, name, description, iconKey, count,
                    intValue(data, "hpRestore", 0), intValue(data, "mpRestore", 0),
                    intValue(data, "xpBonus", 0), gold, rarity);
        }
        if (kind.equals("EQUIPMENT")) {
            Equipment.Slot slot;
            try { slot = Equipment.Slot.valueOf(stringValue(data, "slot", "WEAPON")); }
            catch (IllegalArgumentException ex) { slot = Equipment.Slot.WEAPON; }
            return new Equipment(id, name, description, iconKey, slot,
                    intValue(data, "bonusHp", 0), intValue(data, "bonusMp", 0),
                    intValue(data, "bonusStr", 0), intValue(data, "bonusDef", 0),
                    intValue(data, "bonusSpd", 0), intValue(data, "bonusLck", 0),
                    intValue(data, "attackPower", 0), intValue(data, "armourRating", 0),
                    stringValue(data, "specialEffect", null), gold, rarity);
        }
        if (kind.equals("MATERIAL")) {
            return new Material(id, name, description, iconKey, count,
                    Math.max(1, intValue(data, "maxStack", 99)), gold, rarity);
        }
        return null;
    }

    private int intValue(Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String stringValue(Map<String, Object> data, String key, String fallback) {
        Object value = data.get(key);
        return value instanceof String string ? string : fallback;
    }

    // ── Slot info ─────────────────────────────────────────────────────────
    public boolean slotExists(int slot) {
        if (!validSlot(slot)) return false;
        return new File(SAVE_DIR + File.separator + slotFilename(slot)).exists();
    }

    public String slotSummary(int slot) {
        if (!slotExists(slot)) return "Empty";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = mapper.readValue(
                new File(SAVE_DIR + File.separator + slotFilename(slot)), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> p = (Map<String, Object>) data.get("player");
            return "Lv." + p.get("level") + " " + p.get("name")
                   + "  –  " + data.get("timestamp");
        } catch (Exception e) {
            return "Corrupt";
        }
    }

    public boolean deleteSlot(int slot) {
        if (!validSlot(slot)) return false;
        File f = new File(SAVE_DIR + File.separator + slotFilename(slot));
        if (f.exists()) return f.delete();
        return false;
    }

    private String slotFilename(int slot) {
        return slot == 0 ? "autosave.json" : "save_slot_" + slot + ".json";
    }

    private boolean validSlot(int slot) {
        return slot >= 0 && slot <= SLOTS;
    }
}
