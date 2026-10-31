package com.abyss.items;

import com.abyss.entities.Player;

/**
 * Consumable — Single-use item that applies an effect to the player or a target.
 *
 * Effects (at most one each, 0 = no effect):
 *  hpRestore  – heals hp
 *  mpRestore  – restores mp
 *  xpBonus    – grants bonus XP
 *  statBuff   – temporarily boosts a stat (via StatusEffect on player)
 */
public class Consumable extends Item {

    private final int     hpRestore;
    private final int     mpRestore;
    private final int     xpBonus;
    private       boolean consumed = false;

    // ─────────────────────────────────────────────────────────────────────
    /** Convenience constructor for simple potions. */
    public Consumable(String displayName, String description, String iconKey,
                      int hpRestore, int mpRestore, int goldValue) {
        super(iconKey, displayName, description, iconKey,
              1, 99, goldValue, Rarity.COMMON);
        this.hpRestore = hpRestore;
        this.mpRestore = mpRestore;
        this.xpBonus   = 0;
    }

    /** Full constructor. */
    public Consumable(String id, String displayName, String description,
                      String iconKey, int count, int hpRestore, int mpRestore,
                      int xpBonus, int goldValue, Rarity rarity) {
        super(id, displayName, description, iconKey, count, 99, goldValue, rarity);
        this.hpRestore = hpRestore;
        this.mpRestore = mpRestore;
        this.xpBonus   = xpBonus;
    }

    // ── Use ───────────────────────────────────────────────────────────────
    public boolean use(Player player) {
        if (count <= 0) { consumed = true; return false; }
        boolean useful = (hpRestore > 0 && player.getHp() < player.getMaxHp())
                || (mpRestore > 0 && player.getMp() < player.getMaxMp()) || xpBonus > 0;
        if (!useful) return false;
        if (hpRestore > 0) player.heal(hpRestore);
        if (mpRestore > 0) player.restoreMp(mpRestore);
        if (xpBonus   > 0) player.gainXp(xpBonus);
        count--;
        if (count <= 0) consumed = true;
        return true;
    }

    public boolean isConsumed() { return consumed; }

    // ── Getters ───────────────────────────────────────────────────────────
    public int getHpRestore() { return hpRestore; }
    public int getMpRestore() { return mpRestore; }
    public int getXpBonus()   { return xpBonus; }

    @Override
    public Item clone(int newCount) {
        return new Consumable(id, displayName, description, iconKey,
                              newCount, hpRestore, mpRestore, xpBonus,
                              goldValue, rarity);
    }
}
