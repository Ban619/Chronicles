package com.abyss.items;

/**
 * Equipment — Wearable/equippable item with stat modifiers.
 *
 * Slot types:
 *   WEAPON, OFFHAND, HEAD, CHEST, LEGS, BOOTS, GLOVES, RING, AMULET
 *
 * Stat bonuses are additive and applied when the equipment
 * is equipped via the Equipment container.
 */
public class Equipment extends Item {

    // ── Slot ──────────────────────────────────────────────────────────────
    public enum Slot {
        WEAPON, OFFHAND, HEAD, CHEST, LEGS, BOOTS, GLOVES, RING, AMULET
    }

    // ── Held equipment per slot ───────────────────────────────────────────
    // (Used as a container object for the Player's loadout)
    private final java.util.EnumMap<Slot, Equipment> slots =
            new java.util.EnumMap<>(Slot.class);

    // ── Stat bonuses ──────────────────────────────────────────────────────
    private final Slot slot;
    private final int  bonusHp;
    private final int  bonusMp;
    private final int  bonusStr;
    private final int  bonusDef;
    private final int  bonusSpd;
    private final int  bonusLck;
    private final String specialEffect; // e.g. "lifesteal", "firethorn", "dodge"

    // ── Attack / defence values ───────────────────────────────────────────
    private final int attackPower;     // weapons
    private final int armourRating;    // armour pieces

    // ─────────────────────────────────────────────────────────────────────
    /** Minimal constructor – used as slot-container on Player. */
    public Equipment() {
        super("none", "None", "", "none", 1, 1, 0, Rarity.COMMON);
        this.slot          = null;
        this.bonusHp = this.bonusMp = this.bonusStr = this.bonusDef =
            this.bonusSpd = this.bonusLck = 0;
        this.attackPower   = 0;
        this.armourRating  = 0;
        this.specialEffect = null;
    }

    /** Full item constructor. */
    public Equipment(String id, String displayName, String description,
                     String iconKey, Slot slot,
                     int bonusHp, int bonusMp, int bonusStr, int bonusDef,
                     int bonusSpd, int bonusLck,
                     int attackPower, int armourRating,
                     String specialEffect, int goldValue, Rarity rarity) {
        super(id, displayName, description, iconKey, 1, 1, goldValue, rarity);
        this.slot          = slot;
        this.bonusHp       = bonusHp;
        this.bonusMp       = bonusMp;
        this.bonusStr      = bonusStr;
        this.bonusDef      = bonusDef;
        this.bonusSpd      = bonusSpd;
        this.bonusLck      = bonusLck;
        this.attackPower   = attackPower;
        this.armourRating  = armourRating;
        this.specialEffect = specialEffect;
    }

    // ── Container operations (used as player's equipment set) ─────────────
    /** Equip an item into its slot. Returns previously equipped item or null. */
    public Equipment equip(Equipment item) {
        if (item.slot == null) return null;
        Equipment prev = slots.get(item.slot);
        slots.put(item.slot, item);
        item.setEquipped(true);
        if (prev != null) prev.setEquipped(false);
        return prev;
    }

    public Equipment unequip(Slot s) {
        Equipment item = slots.remove(s);
        if (item != null) item.setEquipped(false);
        return item;
    }

    public Equipment getSlot(Slot s) { return slots.get(s); }

    // ── Aggregate bonuses across all equipped slots ───────────────────────
    public int getTotalBonusHp()  { return slots.values().stream().mapToInt(e -> e.bonusHp).sum(); }
    public int getTotalBonusMp()  { return slots.values().stream().mapToInt(e -> e.bonusMp).sum(); }
    public int getTotalBonusStr() { return slots.values().stream().mapToInt(e -> e.bonusStr).sum(); }
    public int getTotalBonusDef() { return slots.values().stream().mapToInt(e -> e.bonusDef).sum(); }
    public int getTotalBonusSpd() { return slots.values().stream().mapToInt(e -> e.bonusSpd).sum(); }
    public int getTotalBonusLck() { return slots.values().stream().mapToInt(e -> e.bonusLck).sum(); }
    public int getTotalAttack()   { return slots.values().stream().mapToInt(e -> e.attackPower).sum(); }
    public int getTotalArmour()   { return slots.values().stream().mapToInt(e -> e.armourRating).sum(); }

    // ── Getters ───────────────────────────────────────────────────────────
    public Slot   getSlot()          { return slot; }
    public int    getBonusStr()      { return bonusStr; }
    public int    getBonusDef()      { return bonusDef; }
    public int    getBonusHp()       { return bonusHp; }
    public int    getBonusMp()       { return bonusMp; }
    public int    getBonusSpd()      { return bonusSpd; }
    public int    getBonusLck()      { return bonusLck; }
    public int    getAttackPower()   { return attackPower; }
    public int    getArmourRating()  { return armourRating; }
    public String getSpecialEffect() { return specialEffect; }

    @Override
    public Item clone(int newCount) {
        return new Equipment(id, displayName, description, iconKey, slot,
                bonusHp, bonusMp, bonusStr, bonusDef, bonusSpd, bonusLck,
                attackPower, armourRating, specialEffect, goldValue, rarity);
    }
}
