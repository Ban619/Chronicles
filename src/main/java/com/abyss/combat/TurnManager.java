package com.abyss.combat;

import com.abyss.entities.Enemy;
import com.abyss.entities.Player;

/**
 * TurnManager — Resolves damage calculations for player and enemy turns.
 *
 * Damage formula (physical):
 *   base = (attacker.strength + move.power) * 0.5
 *   modified = base * (1 - defender.defense * 0.02)
 *   critical = 1.5× if luck roll succeeds
 *   final = max(1, (int) modified)
 *
 * Damage formula (magical):
 *   base = move.power * 1.4
 *   ignores 50% of physical defence
 */
public class TurnManager {

    private Player player;
    private Enemy  enemy;

    public void setup(Player p, Enemy e) {
        this.player = p;
        this.enemy  = e;
    }

    // ── Player deals damage to enemy ──────────────────────────────────────
    public int resolvePlayerDamage(Player p, Move move, Enemy e) {
        double base;
        if (move.getType() == Move.Type.PHYSICAL) {
            base = (p.getStrength() + move.getPower()) * 0.55;
            base *= Math.max(0.1, 1.0 - e.getDefense() * 0.018);
        } else if (move.getType() == Move.Type.MAGICAL) {
            base = move.getPower() * 1.35;
            base *= Math.max(0.3, 1.0 - e.getDefense() * 0.008);
        } else {
            return 0; // heals / buffs do no damage
        }

        // Critical hit
        boolean crit = Math.random() < 0.05 + p.getLuck() * 0.01;
        if (crit) base *= 1.5;

        // Random variance ±10%
        base *= 0.9 + Math.random() * 0.2;

        return Math.max(1, (int) base);
    }

    // ── Enemy deals damage to player ──────────────────────────────────────
    public int resolveEnemyDamage(Enemy e, Move move, Player p) {
        double base = (e.getStrength() + move.getPower()) * 0.5;
        base *= Math.max(0.1, 1.0 - p.getDefense() * 0.02);
        boolean crit = Math.random() < 0.04;
        if (crit) base *= 1.5;
        base *= 0.9 + Math.random() * 0.2;
        return Math.max(1, (int) base);
    }
}
