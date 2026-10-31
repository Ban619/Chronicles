package com.abyss.skills;

import java.util.ArrayList;
import java.util.List;

/**
 * SkillNode — A single node in the player's skill tree.
 *
 * Each node has:
 *   id          – unique string key (used for save/load + prerequisite checks)
 *   name        – display name
 *   description – tooltip text
 *   icon        – emoji character for the node icon
 *   tier        – which row it sits on (0 = root, 1 = 2nd, 2 = advanced…)
 *   branch      – which of the 3 branches this node belongs to:
 *                   WARRIOR (red), ARCANE (blue), SHADOW (green)
 *   cost        – skill points required to unlock
 *   maxRank     – how many times this node can be upgraded (1 = toggle, 2–5 = ranks)
 *   currentRank – current upgrade level (0 = locked)
 *   prerequisites– list of node IDs that must be unlocked first
 *   effect      – SkillEffect that is applied/upgraded when rank increases
 *
 * Layout coordinates (for SkillTreeUI rendering):
 *   col, row – grid position in the visual tree (0-indexed)
 */
public class SkillNode {

    public enum Branch { WARRIOR, ARCANE, SHADOW }

    // ── Identity ──────────────────────────────────────────────────────────
    private final String id;
    private final String name;
    private final String description;
    private final String icon;

    // ── Tree position ─────────────────────────────────────────────────────
    private final int    tier;
    private final int    col;      // grid column for rendering
    private final Branch branch;

    // ── Unlock ────────────────────────────────────────────────────────────
    private final int    cost;
    private final int    maxRank;
    private       int    currentRank = 0;

    // ── Prerequisites ─────────────────────────────────────────────────────
    private final List<String> prerequisites = new ArrayList<>();

    // ── Effect ────────────────────────────────────────────────────────────
    private final SkillEffect effect;

    // ─────────────────────────────────────────────────────────────────────
    public SkillNode(String id, String name, String description, String icon,
                     int tier, int col, Branch branch,
                     int cost, int maxRank, SkillEffect effect) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.icon        = icon;
        this.tier        = tier;
        this.col         = col;
        this.branch      = branch;
        this.cost        = cost;
        this.maxRank     = maxRank;
        this.effect      = effect;
    }

    // ── Unlock logic ──────────────────────────────────────────────────────
    public boolean isUnlocked()         { return currentRank > 0; }
    public boolean isMaxRank()          { return currentRank >= maxRank; }
    public boolean canUpgrade(int skillPoints, SkillTree tree) {
        if (isMaxRank()) return false;
        if (skillPoints < cost) return false;
        for (String prereq : prerequisites) {
            SkillNode node = tree.getNode(prereq);
            if (node == null || !node.isUnlocked()) return false;
        }
        return true;
    }

    /** Increase rank by 1. Returns the cost if successful, -1 if not. */
    public int upgrade(int availablePoints, SkillTree tree) {
        if (!canUpgrade(availablePoints, tree)) return -1;
        currentRank++;
        if (effect != null) effect.onRankUp(currentRank);
        return cost;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public String       getId()           { return id; }
    public String       getName()         { return name; }
    public String       getDescription()  { return description; }
    public String       getIcon()         { return icon; }
    public int          getTier()         { return tier; }
    public int          getCol()          { return col; }
    public Branch       getBranch()       { return branch; }
    public int          getCost()         { return cost; }
    public int          getMaxRank()      { return maxRank; }
    public int          getCurrentRank()  { return currentRank; }
    public SkillEffect  getEffect()       { return effect; }
    public List<String> getPrerequisites(){ return prerequisites; }

    public void addPrerequisite(String nodeId) { prerequisites.add(nodeId); }

    /** Restore from save data. */
    public void setCurrentRank(int rank) {
        currentRank = Math.max(0, Math.min(maxRank, rank));
        if (currentRank > 0 && effect != null) {
            for (int i = 1; i <= currentRank; i++) effect.onRankUp(i);
        }
    }

    public String getBranchColorHex() {
        return switch (branch) {
            case WARRIOR -> "#c0392b";
            case ARCANE  -> "#2980b9";
            case SHADOW  -> "#27ae60";
        };
    }
}
