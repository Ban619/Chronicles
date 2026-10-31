package com.abyss.entities;

import com.abyss.combat.Move;
import com.abyss.combat.StatusEffect;
import com.abyss.utils.AssetLoader;
import com.abyss.world.GameMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Enemy — Hostile entity that patrols, pursues nearby players, and attacks in real time.
 *
 * AI states:
 *   IDLE     – standing still / on-spot idle animation
 *   PATROL   – following a waypoint loop
 *   ALERT    – player detected, approaching and attacking at close range
 *   STUNNED  – temporarily disabled (trap, status effect)
 *
 * Combat attributes:
 *   - Move pool (used by CombatManager during turn resolution)
 *   - XP reward on defeat
 *   - Item drop table (id → weight)
 */
public class Enemy extends Entity {

    // ── AI state ──────────────────────────────────────────────────────────
    public enum AIState { IDLE, PATROL, ALERT, STUNNED }
    private AIState state = AIState.IDLE;

    // ── Stats ─────────────────────────────────────────────────────────────
    private int maxHp;
    private int hp;
    private int mp;
    private int maxMp;
    private int strength;
    private final int baseStrength;
    private int defense;
    private int speed;
    private int xpReward;
    private int goldReward;
    private boolean isBoss;
    private int bossPhase = 1;
    private boolean defeated;

    // ── Combat ────────────────────────────────────────────────────────────
    private final List<Move>         moves         = new ArrayList<>();
    private final List<StatusEffect> activeEffects = new ArrayList<>();

    // Drop table: item-id → drop-weight
    private final java.util.Map<String, Integer> dropTable = new java.util.LinkedHashMap<>();

    // ── Patrol ────────────────────────────────────────────────────────────
    private final List<float[]> waypoints    = new ArrayList<>();
    private int     waypointIdx  = 0;
    private double  waitTimer    = 0;
    private boolean waiting      = false;
    private static final float PATROL_SPEED  = 35f;
    private static final float AGGRO_RANGE   = 160f;
    private static final float ATTACK_RANGE  = 38f;
    private static final float ALERT_SPEED   = 60f;
    private double attackCooldown;

    // ── Animation ─────────────────────────────────────────────────────────
    private int    direction = 0;
    private int    animFrame = 0;
    private double animTimer = 0;
    private boolean moving   = false;
    private double  flashTimer = 0;   // flashes red on hit

    // ── Stun ──────────────────────────────────────────────────────────────
    private double stunTimer = 0;

    // ── Assets ────────────────────────────────────────────────────────────
    private final String spriteKey;
    private final AssetLoader assetLoader;
    private Image sprite;

    // ─────────────────────────────────────────────────────────────────────
    public Enemy(String name, float x, float y,
                 int maxHp, int strength, int defense, int speed,
                 int xpReward, int goldReward, boolean isBoss,
                 String spriteKey, AssetLoader loader) {
        super(name, x, y, 32, 32);
        this.maxHp     = maxHp;
        this.hp        = maxHp;
        this.mp        = 20;
        this.maxMp     = 20;
        this.strength  = strength;
        this.baseStrength = strength;
        this.defense   = defense;
        this.speed     = speed;
        this.xpReward  = xpReward;
        this.goldReward= goldReward;
        this.isBoss    = isBoss;
        this.spriteKey = spriteKey;
        this.assetLoader = loader;
        this.sprite    = loader.getImage("sprites/" + spriteKey + ".png");
    }

    // ── Move pool ─────────────────────────────────────────────────────────
    public void addMove(Move m)                  { moves.add(m); }
    public void addDrop(String itemId, int weight){ dropTable.put(itemId, weight); }
    public void addWaypoint(float wx, float wy)  { waypoints.add(new float[]{wx, wy}); }

    // ── Update ────────────────────────────────────────────────────────────
    @Override
    public void update(double dt, GameMap map) {
        if (!active) return;
        tickTimers(dt);
        if (!isAlive()) return;
        if (state != AIState.STUNNED && state != AIState.ALERT) patrol(dt, map);
    }

    /** Update with player position for pursuit and real-time contact attacks. */
    public void update(double dt, GameMap map, Player player) {
        if (!active) return;
        tickTimers(dt);
        if (!active || !isAlive() || state == AIState.STUNNED) return;

        float dist = distanceTo(player);
        if (dist < AGGRO_RANGE && state != AIState.ALERT) {
            state = AIState.ALERT;
        }
        if (state == AIState.ALERT) {
            moving = false;
            if (dist <= ATTACK_RANGE) {
                if (attackCooldown == 0) {
                    player.takeDamage(strength);
                    map.spawnDamageNumber(player.getX() + 16, player.getY(), strength, false);
                    attackCooldown = isBoss ? (bossPhase == 3 ? 0.45 : 0.8) : 1.1;
                }
            } else {
                moveTowards(player.getX(), player.getY(), ALERT_SPEED, dt, map);
            }
        } else {
            patrol(dt, map);
        }
    }

    private void tickTimers(double dt) {
        if (flashTimer > 0) flashTimer = Math.max(0, flashTimer - dt);
        attackCooldown = Math.max(0, attackCooldown - dt);
        activeEffects.removeIf(effect -> !effect.tick(this, dt));
        if (state == AIState.STUNNED) {
            stunTimer -= dt;
            if (stunTimer <= 0) state = AIState.PATROL;
            return;
        }
    }

    private void patrol(double dt, GameMap map) {
        if (waypoints.isEmpty() || state == AIState.ALERT) return;
        state = AIState.PATROL;

        if (waiting) {
            waitTimer -= dt;
            moving = false;
            if (waitTimer <= 0) { waiting = false; waypointIdx = (waypointIdx + 1) % waypoints.size(); }
            return;
        }

        float[] target = waypoints.get(waypointIdx);
        float dx = target[0] - x, dy = target[1] - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 4f) {
            waiting = true;
            waitTimer = 1.0 + Math.random() * 1.5;
            return;
        }
        moveTowards(target[0], target[1], PATROL_SPEED, dt, map);
    }

    private void moveTowards(float tx, float ty, float spd, double dt, GameMap map) {
        float dx   = tx - x, dy = ty - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist == 0) return;
        float nx = dx / dist, ny = dy / dist;
        float newX = x + nx * spd * (float) dt;
        float newY = y + ny * spd * (float) dt;
        if (!map.isBlocked(newX, y, width, height)) x = newX;
        if (!map.isBlocked(x, newY, width, height)) y = newY;
        moving = true;
        direction = Math.abs(nx) > Math.abs(ny) ? (nx < 0 ? 1 : 2) : (ny < 0 ? 3 : 0);

        animTimer += dt;
        if (animTimer >= 0.15) { animTimer = 0; animFrame = (animFrame + 1) % 4; }
    }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void render(GraphicsContext gc) {
        if (!visible || !active) return;

        if (flashTimer > 0) {
            // Red flash on damage
            gc.setGlobalAlpha(0.5);
            gc.setFill(Color.RED);
            gc.fillRect(x, y, width, height);
            gc.setGlobalAlpha(1.0);
        }

        if (sprite != null) {
            int frame = moving ? animFrame : 0;
            gc.drawImage(sprite, frame * 32, direction * 32, 32, 32, x, y, 32, 32);
        } else {
            gc.setFill(isBoss ? Color.web("#8b0000") : Color.web("#c0392b"));
            gc.fillRect(x, y, width, height);
            gc.setFill(Color.web("#f39c12"));
            gc.fillOval(x + 8, y + 4, 16, 16);
        }

        // HP bar above enemy
        float pct = (float) hp / maxHp;
        double bw = isBoss ? 48 : 32;
        double bx = x + (width - bw) / 2.0;
        double by = y - 10;
        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillRect(bx, by, bw, 5);
        gc.setFill(pct > 0.5 ? Color.web("#27ae60") : pct > 0.25 ? Color.web("#f39c12") : Color.web("#c0392b"));
        gc.fillRect(bx, by, bw * pct, 5);

        // Boss label
        if (isBoss) {
            gc.setFill(Color.web("#c8a96e"));
            gc.setFont(javafx.scene.text.Font.font("Serif", 9));
            gc.fillText("★ " + name, x - 4, y - 13);
        }

        // Shadow
        gc.setFill(Color.color(0, 0, 0, 0.2));
        gc.fillOval(x + 4, y + height - 6, 24, 8);
    }

    // ── Combat ────────────────────────────────────────────────────────────
    public int takeDamage(int amount) {
        int mitigated = Math.max(1, amount - defense);
        double vulnerability = activeEffects.stream()
            .mapToDouble(StatusEffect::getDamageTakenModifier).max().orElse(1.0);
        int dealt = Math.max(1, (int) Math.ceil(mitigated * vulnerability));
        hp = Math.max(0, hp - dealt);
        if (isBoss) {
            double hpRatio = (double) hp / maxHp;
            int nextPhase = hpRatio <= 0.30 ? 3 : hpRatio <= 0.60 ? 2 : 1;
            if (nextPhase > bossPhase) setBossPhase(nextPhase);
        }
        flashTimer = 0.1;
        return dealt;
    }

    public void stun(double duration) {
        state     = AIState.STUNNED;
        stunTimer = duration;
    }

    public void setBossPhase(int phase) {
        if (!isBoss) return;
        int nextPhase = Math.max(bossPhase, Math.max(1, Math.min(3, phase)));
        if (nextPhase == bossPhase) return;
        bossPhase = nextPhase;
        strength = switch (bossPhase) {
            case 2 -> (int) Math.ceil(baseStrength * 1.25);
            case 3 -> (int) Math.ceil(baseStrength * 1.65);
            default -> baseStrength;
        };
        attackCooldown = 0;
    }

    public void returnToWorldAfterBattle() {
        if (!isAlive()) return;
        active = true;
        visible = true;
        state = AIState.PATROL;
        waiting = false;
        waitTimer = 0;
    }

    public void applyEffect(StatusEffect effect) {
        activeEffects.add(effect);
        if (effect.disablesMovement()) stun(effect.getDurationRemaining());
        else if (effect.getType().equals("paralyze") && Math.random() < 0.3) stun(0.6);
    }

    public boolean markDefeated() {
        if (defeated || isAlive()) return false;
        defeated = true;
        setActive(false);
        setVisible(false);
        return true;
    }

    /** Weighted random move selection. */
    public Move selectMove() {
        if (moves.isEmpty()) return Move.basicAttack(strength);
        int idx = (int)(Math.random() * moves.size());
        return moves.get(idx);
    }

    /** Roll the drop table. Returns item ID or null. */
    public String rollDrop() {
        int total = dropTable.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return null;
        int roll = (int)(Math.random() * total);
        int cum  = 0;
        for (java.util.Map.Entry<String, Integer> e : dropTable.entrySet()) {
            cum += e.getValue();
            if (roll < cum) return e.getKey();
        }
        return null;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public int     getHp()          { return hp; }
    public int     getMaxHp()       { return maxHp; }
    public int     getMp()          { return mp; }
    public int     getMaxMp()       { return maxMp; }
    public int     getStrength()    { return strength; }
    public int     getDefense()     { return defense; }
    public int     getSpeed()       { return speed; }
    public int     getXpReward()    { return xpReward; }
    public int     getGoldReward()  { return goldReward; }
    public boolean isBoss()         { return isBoss; }
    public boolean isAlive()        { return hp > 0; }
    public int     getBossPhase()   { return bossPhase; }
    public AIState getState()       { return state; }
    public List<Move> getMoves()    { return moves; }
    public List<StatusEffect> getActiveEffects() { return activeEffects; }
}
