package com.abyss.skills;

import com.abyss.entities.Player;
import com.abyss.combat.Move;

import java.util.List;

/**
 * SkillEffect — Defines what a SkillNode does when unlocked or upgraded.
 *
 * Effects are categorised as:
 *   PASSIVE  – permanent stat modifier applied to the Player immediately
 *   ACTIVE   – grants a new Move that appears in the combat Move Select menu
 *   AURA     – persistent per-turn effect (regeneration, thorns, lifesteal)
 *   MODIFIER – multiplies an existing stat or move power
 *
 * The onRankUp(int rank) callback is called for rank 1 (first unlock)
 * and each subsequent rank. It receives the new rank so scaling can be done.
 *
 * All passive effects store their delta so they can be reapplied cleanly
 * from save data without double-applying on re-load.
 */
public class SkillEffect {

    public enum EffectType { PASSIVE, ACTIVE, AURA, MODIFIER }

    // ── Identity ──────────────────────────────────────────────────────────
    private final EffectType type;
    private final String     description; // one-line per-rank description template

    // ── Stat deltas (PASSIVE) ─────────────────────────────────────────────
    private int hpPerRank  = 0;
    private int mpPerRank  = 0;
    private int strPerRank = 0;
    private int defPerRank = 0;
    private int spdPerRank = 0;
    private int lckPerRank = 0;

    // ── Active move (ACTIVE) ──────────────────────────────────────────────
    private Move grantedMove = null;

    // ── Modifier (MODIFIER) ───────────────────────────────────────────────
    private String modTarget   = null;  // e.g. "crit_chance", "move_power_physical"
    private double modPerRank  = 0;

    // ── Callback reference (set after construction via setPlayer) ─────────
    private Player playerRef = null;
    private int    rankApplied = 0;

    // ─────────────────────────────────────────────────────────────────────
    private SkillEffect(EffectType type, String description) {
        this.type        = type;
        this.description = description;
    }

    // ── Factory methods ───────────────────────────────────────────────────

    /** Passive: increases HP each rank. */
    public static SkillEffect passiveHp(int perRank) {
        SkillEffect e = new SkillEffect(EffectType.PASSIVE, "+" + perRank + " Max HP per rank");
        e.hpPerRank = perRank;
        return e;
    }

    public static SkillEffect passiveStr(int perRank) {
        SkillEffect e = new SkillEffect(EffectType.PASSIVE, "+" + perRank + " Strength per rank");
        e.strPerRank = perRank;
        return e;
    }

    public static SkillEffect passiveDef(int perRank) {
        SkillEffect e = new SkillEffect(EffectType.PASSIVE, "+" + perRank + " Defense per rank");
        e.defPerRank = perRank;
        return e;
    }

    public static SkillEffect passiveMp(int perRank) {
        SkillEffect e = new SkillEffect(EffectType.PASSIVE, "+" + perRank + " Max MP per rank");
        e.mpPerRank = perRank;
        return e;
    }

    public static SkillEffect passiveMpWithModifier(int perRank, String target, double modifierPerRank) {
        SkillEffect e = passiveMp(perRank);
        e.modTarget = target;
        e.modPerRank = modifierPerRank;
        return e;
    }

    public static SkillEffect passiveSpd(int perRank) {
        SkillEffect e = new SkillEffect(EffectType.PASSIVE, "+" + perRank + " Speed per rank");
        e.spdPerRank = perRank;
        return e;
    }

    public static SkillEffect passiveSpdWithDodge(int speedPerRank, double dodgePerRank) {
        SkillEffect e = passiveSpd(speedPerRank);
        e.modTarget = "dodge";
        e.modPerRank = dodgePerRank;
        return e;
    }

    public static SkillEffect passiveLck(int perRank) {
        SkillEffect e = new SkillEffect(EffectType.PASSIVE, "+" + perRank + " Luck per rank");
        e.lckPerRank = perRank;
        return e;
    }

    /** Passive multi-stat bundle. */
    public static SkillEffect passiveBundle(int hp, int mp, int str, int def, int spd) {
        SkillEffect e = new SkillEffect(EffectType.PASSIVE, "All stats per rank");
        e.hpPerRank=hp; e.mpPerRank=mp; e.strPerRank=str; e.defPerRank=def; e.spdPerRank=spd;
        return e;
    }

    /** Active: grants a Move to the player's move pool at rank 1. */
    public static SkillEffect activeMove(Move move) {
        SkillEffect e = new SkillEffect(EffectType.ACTIVE, "Grants: " + move.getName());
        e.grantedMove = move;
        return e;
    }

    /** Modifier: multiplies crit chance, move power, etc. */
    public static SkillEffect modifier(String target, double perRank) {
        SkillEffect e = new SkillEffect(EffectType.MODIFIER, "+" + (int)(perRank*100) + "% " + target + " per rank");
        e.modTarget  = target;
        e.modPerRank = perRank;
        return e;
    }

    // ── Callback ──────────────────────────────────────────────────────────
    /** Called each time this node's rank increases. rank=1 is first unlock. */
    public void onRankUp(int rank) {
        rankApplied = rank;
        if (playerRef == null) return;
        applyToPlayer(playerRef, rank);
    }

    public void bindPlayer(Player p) {
        this.playerRef = p;
    }

    private void applyToPlayer(Player p, int rank) {
        // Only apply the DELTA for this rank (not cumulative to avoid double-apply on load)
        switch (type) {
            case PASSIVE -> {
                p.applyStatDelta(hpPerRank, mpPerRank, strPerRank, defPerRank, spdPerRank, lckPerRank);
                if (modTarget != null) p.applyModifier(modTarget, modPerRank);
            }
            case ACTIVE -> {
                if (rank == 1 && grantedMove != null) {
                    p.learnMove(grantedMove);
                }
            }
            case MODIFIER -> {
                p.applyModifier(modTarget, modPerRank);
            }
            case AURA -> {
                // handled by CombatManager checking player's active auras
            }
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public EffectType getType()        { return type; }
    public String     getDescription() { return description; }
    public Move       getGrantedMove() { return grantedMove; }
    public String     getModTarget()   { return modTarget; }
    public double     getModPerRank()  { return modPerRank; }
    public int        getHpPerRank()   { return hpPerRank; }
    public int        getMpPerRank()   { return mpPerRank; }
    public int        getStrPerRank()  { return strPerRank; }
    public int        getDefPerRank()  { return defPerRank; }
}
