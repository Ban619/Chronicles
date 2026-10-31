package com.abyss.skills;

import com.abyss.combat.Move;
import com.abyss.entities.Player;

import java.util.*;
import java.util.logging.Logger;

/**
 * SkillTree — Holds the complete skill graph, manages skill points,
 * and applies effects to the player when nodes are unlocked.
 *
 * Three branches, 5 tiers each (15 nodes per branch = 45 total):
 *
 * ── WARRIOR branch (left, red) ─────────────────────────────────────────
 *   T0: Iron Skin        (passive DEF)
 *   T1: Heavy Strike     (active PHYSICAL 35 dmg)
 *   T1: Fortify          (passive HP + DEF)
 *   T2: Cleave           (active AoE physical 25 dmg)
 *   T2: Battle Hardened  (passive DEF)
 *   T3: Warlord's Stance (passive STR + DEF)
 *   T3: Earthquake       (active massive physical 55 dmg)
 *   T4: Unstoppable      (passive all stats)
 *
 * ── ARCANE branch (centre, blue) ───────────────────────────────────────
 *   T0: Mana Font        (passive MP)
 *   T1: Fireball         (active MAGICAL 35 dmg, burn)
 *   T1: Frost Shield     (passive DEF + SPD)
 *   T2: Chain Lightning  (active MAGICAL 30 dmg, paralyze)
 *   T2: Arcane Mastery   (modifier spell_power +15%)
 *   T3: Void Rift        (active MAGICAL 55 dmg)
 *   T3: Mana Surge       (passive MP + modifier crit_chance)
 *   T4: Archmage         (passive all stats + move)
 *
 * ── SHADOW branch (right, green) ───────────────────────────────────────
 *   T0: Shadow Step      (passive SPD + LCK)
 *   T1: Poison Blade     (active PHYSICAL 20 dmg + poison)
 *   T1: Evasion          (passive SPD + modifier dodge)
 *   T2: Shadow Strike    (active PHYSICAL 45 dmg high crit)
 *   T2: Venomous         (modifier poison_duration +1)
 *   T3: Death Mark       (active DEBUFF, increases all damage enemy takes)
 *   T3: Ghost Walk       (passive all speed stats)
 *   T4: Assassin's Creed (active massive + status combo)
 */
public class SkillTree {

    private static final Logger LOG = Logger.getLogger(SkillTree.class.getName());

    private final Map<String, SkillNode> nodes = new LinkedHashMap<>();
    private int    skillPoints        = 0;
    private int    totalPointsSpent   = 0;
    private Player player;

    // ─────────────────────────────────────────────────────────────────────
    public SkillTree() {
        buildWarriorBranch();
        buildArcaneBranch();
        buildShadowBranch();
    }

    // ── Branch builders ───────────────────────────────────────────────────

    private void buildWarriorBranch() {
        // Tier 0 — root
        add(new SkillNode("w_iron_skin", "Iron Skin", "Harden yourself — +6 DEF per rank.",
                "🛡", 0, 0, SkillNode.Branch.WARRIOR, 1, 3,
                SkillEffect.passiveDef(6)));

        // Tier 1
        add(node("w_heavy_strike", "Heavy Strike",
                "A crushing blow. Physical attack, 35 power, ignores 30% DEF.",
                "⚔", 1, 0, SkillNode.Branch.WARRIOR, 2, 1,
                SkillEffect.activeMove(new Move("Heavy Strike", Move.Type.PHYSICAL, 35, 8, 90, null)),
                "w_iron_skin"));
        add(node("w_fortify", "Fortify",
                "+20 HP and +4 DEF per rank.",
                "🏰", 1, 1, SkillNode.Branch.WARRIOR, 1, 3,
                SkillEffect.passiveBundle(20, 0, 0, 4, 0),
                "w_iron_skin"));

        // Tier 2
        add(node("w_cleave", "Cleave",
                "Wide swing. Physical 25 power — hits all enemies in range.",
                "⚔", 2, 0, SkillNode.Branch.WARRIOR, 2, 1,
                SkillEffect.activeMove(new Move("Cleave", Move.Type.PHYSICAL, 25, 12, 92, null)),
                "w_heavy_strike"));
        add(node("w_battle_hardened", "Battle Hardened",
                "Experience tempers you. +5 DEF and +8 HP per rank.",
                "💪", 2, 1, SkillNode.Branch.WARRIOR, 1, 3,
                SkillEffect.passiveBundle(8, 0, 0, 5, 0),
                "w_fortify"));

        // Tier 3
        add(node("w_warlord_stance", "Warlord's Stance",
                "Offensive stance. +8 STR and +5 DEF per rank.",
                "⚡", 3, 0, SkillNode.Branch.WARRIOR, 2, 2,
                SkillEffect.passiveBundle(0, 0, 8, 5, 0),
                "w_cleave", "w_battle_hardened"));
        add(node("w_earthquake", "Earthquake",
                "Slam the ground. Physical 55 power, chance to stun.",
                "🌋", 3, 1, SkillNode.Branch.WARRIOR, 3, 1,
                SkillEffect.activeMove(new Move("Earthquake", Move.Type.PHYSICAL, 55, 25, 80, "stun")),
                "w_warlord_stance"));

        // Tier 4 — capstone
        add(node("w_unstoppable", "Unstoppable",
                "Peak warrior. +30 HP, +10 STR, +10 DEF, +2 SPD.",
                "👑", 4, 0, SkillNode.Branch.WARRIOR, 4, 1,
                SkillEffect.passiveBundle(30, 0, 10, 10, 2),
                "w_earthquake", "w_warlord_stance"));
    }

    private void buildArcaneBranch() {
        add(new SkillNode("a_mana_font", "Mana Font", "+15 Max MP per rank.",
                "✦", 0, 2, SkillNode.Branch.ARCANE, 1, 4,
                SkillEffect.passiveMp(15)));

        add(node("a_fireball", "Fireball",
                "Classic fire bolt. Magical 35 power, applies Burn.",
                "🔥", 1, 2, SkillNode.Branch.ARCANE, 2, 1,
                SkillEffect.activeMove(new Move("Fireball", Move.Type.MAGICAL, 35, 15, 90, "burn")),
                "a_mana_font"));
        add(node("a_frost_shield", "Frost Shield",
                "Ice-laced armor. +6 DEF and +2 SPD per rank.",
                "❄", 1, 3, SkillNode.Branch.ARCANE, 1, 3,
                SkillEffect.passiveBundle(0, 0, 0, 6, 2),
                "a_mana_font"));

        add(node("a_chain_lightning", "Chain Lightning",
                "Arc that bounces. Magical 30 power, paralyzes target.",
                "⚡", 2, 2, SkillNode.Branch.ARCANE, 2, 1,
                SkillEffect.activeMove(new Move("Chain Lightning", Move.Type.MAGICAL, 30, 18, 88, "paralyze")),
                "a_fireball"));
        add(node("a_arcane_mastery", "Arcane Mastery",
                "+15% spell power per rank (all magical moves).",
                "🔮", 2, 3, SkillNode.Branch.ARCANE, 2, 3,
                SkillEffect.modifier("spell_power", 0.15),
                "a_frost_shield"));

        add(node("a_void_rift", "Void Rift",
                "Tear reality apart. Magical 55 power, confuses target.",
                "🌀", 3, 2, SkillNode.Branch.ARCANE, 3, 1,
                SkillEffect.activeMove(new Move("Void Rift", Move.Type.MAGICAL, 55, 30, 82, "confusion")),
                "a_chain_lightning", "a_arcane_mastery"));
        add(node("a_mana_surge", "Mana Surge",
                "+20 MP and +12% crit chance per rank.",
                "💧", 3, 3, SkillNode.Branch.ARCANE, 2, 2,
                SkillEffect.passiveMp(20),
                "a_void_rift"));

        add(node("a_archmage", "Archmage",
                "Peak caster. +50 MP, +15 STR, +5 DEF. Grants: Meteor.",
                "🌟", 4, 2, SkillNode.Branch.ARCANE, 5, 1,
                SkillEffect.activeMove(new Move("Meteor", Move.Type.MAGICAL, 80, 40, 78, "burn")),
                "a_void_rift", "a_mana_surge"));
    }

    private void buildShadowBranch() {
        add(new SkillNode("s_shadow_step", "Shadow Step", "+2 SPD and +2 LCK per rank.",
                "👣", 0, 4, SkillNode.Branch.SHADOW, 1, 4,
                SkillEffect.passiveBundle(0, 0, 0, 0, 2)));

        add(node("s_poison_blade", "Poison Blade",
                "Coated strike. Physical 20 power + Poison.",
                "🗡", 1, 4, SkillNode.Branch.SHADOW, 2, 1,
                SkillEffect.activeMove(new Move("Poison Blade", Move.Type.PHYSICAL, 20, 6, 95, "poison")),
                "s_shadow_step"));
        add(node("s_evasion", "Evasion",
                "+3 SPD and +10% dodge per rank.",
                "💨", 1, 5, SkillNode.Branch.SHADOW, 1, 3,
                SkillEffect.passiveSpd(3),
                "s_shadow_step"));

        add(node("s_shadow_strike", "Shadow Strike",
                "Instant kill setup. Physical 45 power, high crit rate.",
                "⚡", 2, 4, SkillNode.Branch.SHADOW, 2, 1,
                SkillEffect.activeMove(new Move("Shadow Strike", Move.Type.PHYSICAL, 45, 15, 88, null)),
                "s_poison_blade"));
        add(node("s_venomous", "Venomous",
                "Your poisons linger. +1 poison duration per rank.",
                "☠", 2, 5, SkillNode.Branch.SHADOW, 1, 3,
                SkillEffect.modifier("poison_duration", 1.0),
                "s_evasion"));

        add(node("s_death_mark", "Death Mark",
                "Mark an enemy. All damage +30% for 3 seconds. Debuff move.",
                "💀", 3, 4, SkillNode.Branch.SHADOW, 2, 1,
                SkillEffect.activeMove(new Move("Death Mark", Move.Type.DEBUFF, 0, 12, 100, "marked")),
                "s_shadow_strike", "s_venomous"));
        add(node("s_ghost_walk", "Ghost Walk",
                "+5 SPD and +3 LCK per rank.",
                "👻", 3, 5, SkillNode.Branch.SHADOW, 2, 2,
                SkillEffect.passiveBundle(0, 0, 0, 0, 5),
                "s_death_mark"));

        add(node("s_assassins_creed", "Assassin's Creed",
                "Peak shadow. Physical 70 power + Poison + Blind. Instant.",
                "🗡", 4, 4, SkillNode.Branch.SHADOW, 5, 1,
                SkillEffect.activeMove(new Move("Assassin's Creed", Move.Type.PHYSICAL, 70, 35, 92, "poison")),
                "s_death_mark", "s_ghost_walk"));
    }

    // ── Node factory helpers ──────────────────────────────────────────────
    private void add(SkillNode node) { nodes.put(node.getId(), node); }

    private SkillNode node(String id, String name, String desc, String icon,
                           int tier, int col, SkillNode.Branch branch,
                           int cost, int maxRank, SkillEffect effect,
                           String... prereqs) {
        SkillNode n = new SkillNode(id, name, desc, icon, tier, col, branch, cost, maxRank, effect);
        for (String p : prereqs) n.addPrerequisite(p);
        return n;
    }

    // ── Player binding ────────────────────────────────────────────────────
    /** Must be called once the player is available so effects can be applied. */
    public void bindPlayer(Player p) {
        this.player = p;
        // Bind all effect references
        nodes.values().forEach(n -> {
            if (n.getEffect() != null) n.getEffect().bindPlayer(p);
        });
        LOG.info("[SkillTree] Bound to player " + p.getName());
    }

    // ── Skill point management ────────────────────────────────────────────
    public void addSkillPoints(int pts) { skillPoints += pts; }

    /**
     * Attempt to unlock/upgrade a node.
     * @return true if successful.
     */
    public boolean unlock(String nodeId) {
        SkillNode node = nodes.get(nodeId);
        if (node == null) return false;
        int cost = node.upgrade(skillPoints, this);
        if (cost < 0) return false;
        skillPoints     -= cost;
        totalPointsSpent+= cost;
        LOG.info("[SkillTree] Unlocked " + node.getName() + " (rank " + node.getCurrentRank() + ")");
        return true;
    }

    // ── Queries ───────────────────────────────────────────────────────────
    public SkillNode getNode(String id) { return nodes.get(id); }
    public Collection<SkillNode> getAllNodes() { return nodes.values(); }
    public List<SkillNode> getBranch(SkillNode.Branch b) {
        return nodes.values().stream().filter(n -> n.getBranch() == b).toList();
    }

    public int  getSkillPoints()       { return skillPoints; }
    public int  getTotalPointsSpent()  { return totalPointsSpent; }
    public boolean hasNode(String id)  { return nodes.containsKey(id); }

    /** Grant 1 skill point per level-up — called from Player.levelUp(). */
    public void onLevelUp() {
        skillPoints++;
        LOG.info("[SkillTree] +1 skill point. Available: " + skillPoints);
    }

    /** Serialise all unlock states for saving. */
    public Map<String, Integer> toSaveMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        nodes.forEach((id, node) -> {
            if (node.getCurrentRank() > 0) m.put(id, node.getCurrentRank());
        });
        return m;
    }

    /** Restore from save data. */
    public void fromSaveMap(Map<String, Integer> data, int savedPoints) {
        skillPoints = savedPoints;
        data.forEach((id, rank) -> {
            SkillNode node = nodes.get(id);
            if (node != null) node.setCurrentRank(rank);
        });
    }
}
