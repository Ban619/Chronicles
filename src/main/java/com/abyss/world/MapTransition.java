package com.abyss.world;

/**
 * MapTransition — A rectangular trigger zone on the map that teleports
 * the player to a target map when entered.
 *
 * Each transition stores:
 *   triggerX/Y/W/H  – pixel-space bounding box on the source map
 *   targetMapPath   – classpath resource key for the destination map JSON
 *   targetSpawnX/Y  – tile-space spawn point on the destination map
 *   direction       – cardinal direction the player exits (for arrival facing)
 *   label           – short human-readable name shown in the transition banner
 *   requiresKey     – optional item ID that must be in inventory to pass
 *
 * Transitions are checked each frame in GameMap.checkTransitions().
 * When triggered, WorldManager.loadMap() is called.
 */
public class MapTransition {

    public enum Direction { NORTH, SOUTH, EAST, WEST }

    // ── Trigger zone (pixels) ─────────────────────────────────────────────
    private final float triggerX, triggerY, triggerW, triggerH;

    // ── Destination ───────────────────────────────────────────────────────
    private final String targetMapPath;
    private final float  targetSpawnX;   // tile coordinates
    private final float  targetSpawnY;
    private final Direction direction;
    private final String label;
    private final String requiresKey;    // null = no gate

    // ── State ─────────────────────────────────────────────────────────────
    private double cooldown = 0;  // prevents re-triggering immediately on arrival

    // ─────────────────────────────────────────────────────────────────────
    public MapTransition(float triggerX, float triggerY, float triggerW, float triggerH,
                         String targetMapPath, float targetSpawnX, float targetSpawnY,
                         Direction direction, String label, String requiresKey) {
        this.triggerX      = triggerX;
        this.triggerY      = triggerY;
        this.triggerW      = triggerW;
        this.triggerH      = triggerH;
        this.targetMapPath = targetMapPath;
        this.targetSpawnX  = targetSpawnX;
        this.targetSpawnY  = targetSpawnY;
        this.direction     = direction;
        this.label         = label;
        this.requiresKey   = requiresKey;
    }

    /** Convenience constructor without item gate. */
    public MapTransition(float triggerX, float triggerY, float triggerW, float triggerH,
                         String targetMapPath, float targetSpawnX, float targetSpawnY,
                         Direction direction, String label) {
        this(triggerX, triggerY, triggerW, triggerH,
             targetMapPath, targetSpawnX, targetSpawnY, direction, label, null);
    }

    // ── Trigger check ─────────────────────────────────────────────────────
    /**
     * Tests whether the player's bounding box overlaps this trigger zone.
     * Cooldown prevents instant re-trigger after the player arrives on a new map.
     */
    public boolean isTriggered(float px, float py, int pw, int ph) {
        if (cooldown > 0) return false;
        return px < triggerX + triggerW && px + pw > triggerX
            && py < triggerY + triggerH && py + ph > triggerY;
    }

    public void update(double dt) {
        if (cooldown > 0) cooldown = Math.max(0, cooldown - dt);
    }

    /** Reset cooldown after a map load so the exit zone isn't re-triggered. */
    public void resetCooldown() { cooldown = 1.2; }
    public void setCooldown(double seconds) { cooldown = Math.max(cooldown, Math.max(0, seconds)); }

    // ── Getters ───────────────────────────────────────────────────────────
    public String    getTargetMapPath()  { return targetMapPath; }
    public float     getTargetSpawnX()   { return targetSpawnX; }
    public float     getTargetSpawnY()   { return targetSpawnY; }
    public Direction getDirection()      { return direction; }
    public String    getLabel()          { return label; }
    public String    getRequiresKey()    { return requiresKey; }
    public float     getTriggerX()       { return triggerX; }
    public float     getTriggerY()       { return triggerY; }
    public float     getTriggerW()       { return triggerW; }
    public float     getTriggerH()       { return triggerH; }
}
