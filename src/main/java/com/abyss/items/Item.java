package com.abyss.items;

/**
 * Item — Abstract base for every object the player can carry.
 *
 * Item hierarchy:
 *   Item
 *   ├── Consumable   (potions, food, scrolls)
 *   ├── Equipment    (weapons, armour, accessories)
 *   └── BattleItem   (throwable, usable in combat without consuming turn)
 *
 * Each item has:
 *  - A unique string ID (matches asset/data keys)
 *  - Display name and description
 *  - Stack size and current count
 *  - Gold value (buy price = value * 1.5, sell = value * 0.5)
 *  - Rarity tier affecting UI colour
 */
public abstract class Item {

    // ── Rarity ────────────────────────────────────────────────────────────
    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }

    // ── Fields ────────────────────────────────────────────────────────────
    protected final String  id;
    protected final String  displayName;
    protected final String  description;
    protected final String  iconKey;       // asset key for the icon sprite
    protected       int     count;
    protected final int     maxStack;
    protected final int     goldValue;
    protected final Rarity  rarity;
    protected       boolean equipped = false;

    // ─────────────────────────────────────────────────────────────────────
    protected Item(String id, String displayName, String description,
                   String iconKey, int count, int maxStack, int goldValue, Rarity rarity) {
        this.id          = id;
        this.displayName = displayName;
        this.description = description;
        this.iconKey     = iconKey;
        this.count       = count;
        this.maxStack    = maxStack;
        this.goldValue   = goldValue;
        this.rarity      = rarity;
    }

    // ── Stack management ──────────────────────────────────────────────────
    public boolean canStack(Item other) {
        return other.id.equals(this.id) && count < maxStack;
    }

    /** Adds up to amount items and returns the number added. */
    public int addToStack(int amount) {
        int space = maxStack - count;
        int added = Math.min(space, amount);
        count += added;
        return added;
    }

    public Item splitStack(int amount) {
        if (amount >= count || amount <= 0) return null;
        count -= amount;
        return clone(amount);
    }

    /** Removes up to {@code amount} units and returns how many were removed. */
    public int removeFromStack(int amount) {
        int removed = Math.min(Math.max(0, amount), count);
        count -= removed;
        return removed;
    }

    /** Clone with a given count — subclasses must implement. */
    public abstract Item clone(int newCount);

    // ── Identification ────────────────────────────────────────────────────
    public String  getId()          { return id; }
    public String  getDisplayName() { return displayName; }
    public String  getDescription() { return description; }
    public String  getIconKey()     { return iconKey; }
    public int     getCount()       { return count; }
    public int     getMaxStack()    { return maxStack; }
    public int     getGoldValue()   { return goldValue; }
    public int     getBuyPrice()    { return (int)(goldValue * 1.5); }
    public int     getSellPrice()   { return (int)(goldValue * 0.5); }
    public Rarity  getRarity()      { return rarity; }
    public boolean isEquipped()     { return equipped; }
    public void    setEquipped(boolean e){ equipped = e; }

    public String getRarityHex() {
        return switch (rarity) {
            case COMMON    -> "#d4c5a9";
            case UNCOMMON  -> "#27ae60";
            case RARE      -> "#2980b9";
            case EPIC      -> "#8e44ad";
            case LEGENDARY -> "#c8a96e";
        };
    }

    @Override
    public String toString() {
        return displayName + (count > 1 ? " x" + count : "");
    }
}
