package com.abyss.entities;

import com.abyss.combat.Move;
import com.abyss.items.Item;
import com.abyss.utils.AssetLoader;
import com.abyss.world.GameMap;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/**
 * PartyMember — A companion that travels with and fights alongside the player.
 *
 * Party members follow the player on the map (they are rendered as a trailing
 * formation) and participate in turn-based combat with their own move sets.
 *
 * Formation offsets (pixels behind the player, staggered):
 *   Slot 0: 32px behind
 *   Slot 1: 32px behind + 16px right
 *   Slot 2: 48px behind
 */
public class PartyMember extends Entity {

    // ── Stats ─────────────────────────────────────────────────────────────
    private int    level    = 1;
    private int    xp       = 0;
    private int    xpToNext = 100;
    private int    maxHp;
    private int    hp;
    private int    maxMp;
    private int    mp;
    private int    strength;
    private int    defense;
    private int    speed;

    // ── Move set ──────────────────────────────────────────────────────────
    private final List<Move> moves = new ArrayList<>();

    // ── Follow formation ─────────────────────────────────────────────────
    private float targetX, targetY;
    private int   formationSlot; // 0, 1, 2

    // ── Animation ─────────────────────────────────────────────────────────
    private int    direction = 0;
    private boolean moving   = false;

    // ── Assets ────────────────────────────────────────────────────────────

    // ── Class archetype ──────────────────────────────────────────────────
    public enum Archetype { WARRIOR, MAGE, ROGUE, CLERIC, RANGER }
    private final Archetype archetype;

    // ─────────────────────────────────────────────────────────────────────
    public PartyMember(String name, Archetype archetype, float x, float y,
                       int formationSlot, AssetLoader loader) {
        super(name, x, y, 32, 32);
        this.archetype     = archetype;
        this.formationSlot = formationSlot;
        initStats(archetype);
        initMoves(archetype);
    }

    // ── Stat initialisation per archetype ────────────────────────────────
    private void initStats(Archetype a) {
        switch (a) {
            case WARRIOR -> { maxHp=120; hp=120; maxMp=20;  mp=20;  strength=14; defense=10; speed=4; }
            case MAGE    -> { maxHp=60;  hp=60;  maxMp=100; mp=100; strength=5;  defense=3;  speed=5; }
            case ROGUE   -> { maxHp=80;  hp=80;  maxMp=40;  mp=40;  strength=12; defense=6;  speed=8; }
            case CLERIC  -> { maxHp=90;  hp=90;  maxMp=80;  mp=80;  strength=7;  defense=8;  speed=4; }
            case RANGER  -> { maxHp=85;  hp=85;  maxMp=30;  mp=30;  strength=11; defense=7;  speed=7; }
        }
    }

    private void initMoves(Archetype a) {
        switch (a) {
            case WARRIOR -> {
                moves.add(new Move("Slash",        Move.Type.PHYSICAL, 25, 5,  95, null));
                moves.add(new Move("Shield Bash",  Move.Type.PHYSICAL, 15, 8,  90, "stun"));
                moves.add(new Move("War Cry",      Move.Type.BUFF,      0, 12, 100, "atk_up"));
                moves.add(new Move("Earthquake",   Move.Type.PHYSICAL, 40, 20, 75, null));
            }
            case MAGE -> {
                moves.add(new Move("Fireball",     Move.Type.MAGICAL,  35, 15, 90, "burn"));
                moves.add(new Move("Ice Shard",    Move.Type.MAGICAL,  28, 12, 95, "freeze"));
                moves.add(new Move("Thunder",      Move.Type.MAGICAL,  32, 18, 85, "paralyze"));
                moves.add(new Move("Arcane Blast", Move.Type.MAGICAL,  50, 30, 80, null));
            }
            case ROGUE -> {
                moves.add(new Move("Backstab",     Move.Type.PHYSICAL, 30, 0,  85, "poison"));
                moves.add(new Move("Smoke Bomb",   Move.Type.DEBUFF,    0, 8,  100,"blind"));
                moves.add(new Move("Twin Strike",  Move.Type.PHYSICAL, 20, 5,  90, null));
                moves.add(new Move("Assassinate",  Move.Type.PHYSICAL, 60, 25, 70, null));
            }
            case CLERIC -> {
                moves.add(new Move("Holy Strike",  Move.Type.MAGICAL,  20, 8,  95, null));
                moves.add(new Move("Heal",         Move.Type.HEAL,     35, 10, 100, null));
                moves.add(new Move("Blessing",     Move.Type.BUFF,      0, 15, 100,"def_up"));
                moves.add(new Move("Holy Nova",    Move.Type.MAGICAL,  40, 20, 85, "blind"));
            }
            case RANGER -> {
                moves.add(new Move("Arrow Shot",   Move.Type.PHYSICAL, 25, 5,  95, null));
                moves.add(new Move("Volley",       Move.Type.PHYSICAL, 15, 10, 90, null));
                moves.add(new Move("Trap",         Move.Type.DEBUFF,    0, 12, 80, "slow"));
                moves.add(new Move("Piercing Shot",Move.Type.PHYSICAL, 45, 20, 85, "bleed"));
            }
        }
    }

    // ── Update ────────────────────────────────────────────────────────────
    @Override
    public void update(double dt, GameMap map) {
        // Follow the player (target set by GameLoop each frame)
        float dx   = targetX - x;
        float dy   = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 8) {
            float speed = 110f;
            float nx = dx / dist;
            float ny = dy / dist;
            float newX = x + nx * speed * (float) dt;
            float newY = y + ny * speed * (float) dt;
            if (!map.isBlocked(newX, y, width, height)) x = newX;
            if (!map.isBlocked(x, newY, width, height)) y = newY;
            moving = true;
            direction = Math.abs(nx) > Math.abs(ny) ? (nx < 0 ? 1 : 2) : (ny < 0 ? 3 : 0);
        } else {
            moving = false;
        }

    }

    /** Called by GameLoop to update the follow target. */
    public void setFormationTarget(Player player) {
        float behind = 36f + formationSlot * 16f;
        float side   = (formationSlot % 2 == 1) ? 20f : 0f;
        float px     = player.getX();
        float py     = player.getY();
        float vx     = player.getVelX();
        float vy     = player.getVelY();
        // If player moving, trail behind; else spread out
        float len = (float) Math.sqrt(vx * vx + vy * vy);
        if (len > 0) {
            targetX = px - (vx / len) * behind + side;
            targetY = py - (vy / len) * behind;
        } else {
            targetX = px + (formationSlot - 1) * 34f;
            targetY = py + 36f;
        }
    }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void render(GraphicsContext gc) {
        if (!visible) return;
        CharacterPainter.draw(gc, x, y, width, height,
                CharacterPainter.Style.valueOf(archetype.name()), name, direction, moving);
    }

    // ── Combat ────────────────────────────────────────────────────────────
    public void takeDamage(int amount) {
        int mitigated = Math.max(1, amount - defense);
        hp = Math.max(0, hp - mitigated);
    }

    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }
    public void restoreMp(int amount) { mp = Math.min(maxMp, mp + amount); }

    public void gainXp(int amount) {
        xp += amount;
        while (xp >= xpToNext) { xp -= xpToNext; levelUp(); }
    }

    private void levelUp() {
        level++; xpToNext = (int)(xpToNext * 1.4);
        maxHp += 10; hp = maxHp; maxMp += 5; mp = maxMp;
        strength += 2; defense += 2; speed += 1;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public int    getLevel()       { return level; }
    public int    getHp()          { return hp; }
    public int    getMaxHp()       { return maxHp; }
    public int    getMp()          { return mp; }
    public int    getMaxMp()       { return maxMp; }
    public int    getStrength()    { return strength; }
    public int    getDefense()     { return defense; }
    public int    getSpeed()       { return speed; }
    public boolean isAlive()       { return hp > 0; }
    public List<Move> getMoves()   { return moves; }
    public Archetype  getArchetype(){ return archetype; }
    public int    getFormationSlot(){ return formationSlot; }
}
