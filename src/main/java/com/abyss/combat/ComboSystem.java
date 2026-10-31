package com.abyss.combat;

/**
 * ComboSystem — Tracks consecutive hit sequences and returns bonus messages.
 *
 * Combo tiers:
 *   2 hits → "DOUBLE HIT! +10%"
 *   3 hits → "TRIPLE COMBO! +20%"
 *   4 hits → "QUAD STRIKE! +30% + status"
 *   5+     → "UNSTOPPABLE! +50%"
 *
 * A miss or enemy turn resets the combo counter.
 */
public class ComboSystem {

    private int    comboCount = 0;
    private String lastMoveId = null;

    public void reset() { comboCount = 0; lastMoveId = null; }

    /**
     * Called after a successful hit.
     * @return bonus message string, or null if no combo threshold reached.
     */
    public String registerHit(Move move) {
        comboCount++;
        lastMoveId = move.getName();

        return switch (comboCount) {
            case 2 -> "✦ DOUBLE HIT!";
            case 3 -> "✦ TRIPLE COMBO!";
            case 4 -> "✦ QUAD STRIKE!";
            default -> comboCount >= 5 ? "✦ UNSTOPPABLE!" : null;
        };
    }

    public void registerMiss() { comboCount = 0; }
    public int  getCount()     { return comboCount; }
}
