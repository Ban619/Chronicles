package com.abyss.utils;

import com.abyss.entities.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.*;
import java.util.logging.Logger;

// ════════════════════════════════════════════════════════════════════════════
// ReputationHandler
// ════════════════════════════════════════════════════════════════════════════
/**
 * ReputationHandler — Tracks and reports on player reputation.
 *
 * Reputation score: -100 (Villain) … 0 (Neutral) … +100 (Hero)
 *
 * Actions that change reputation (examples):
 *   Complete a good-alignment quest  → +10
 *   Help an NPC in distress          → +5
 *   Attack an innocent NPC           → -20
 *   Steal from a shop                → -10
 *   Defeat a boss that threatened the village → +25
 *
 * Reputation affects:
 *   - NPC dialogue branches
 *   - Shop prices (hero → 15% discount, villain → 10% surcharge)
 *   - Quest availability (some quests locked at low reputation)
 *   - Guard aggression (very low rep → guards become hostile)
 */
class ReputationHandler {

    private static final Logger LOG = Logger.getLogger(ReputationHandler.class.getName());

    /** Named reputation tiers. */
    public enum Tier {
        REVILED(-100, -60, "Reviled"),
        DISLIKED(-60, -20, "Disliked"),
        NEUTRAL(-20, 20, "Neutral"),
        RESPECTED(20, 60, "Respected"),
        REVERED(60, 100, "Revered");

        final int min, max;
        final String label;
        Tier(int min, int max, String label) { this.min = min; this.max = max; this.label = label; }
    }

    // ── Event log ─────────────────────────────────────────────────────────
    private final List<String> log = new ArrayList<>();

    public void applyEvent(Player player, String eventId, int delta) {
        player.adjustReputation(delta);
        String msg = "[Rep] " + eventId + " → " + (delta >= 0 ? "+" : "") + delta
                     + " (now " + player.getReputation() + ")";
        log.add(msg);
        LOG.info(msg);
    }

    public Tier getTier(int reputation) {
        for (Tier t : Tier.values()) {
            if (reputation >= t.min && reputation <= t.max) return t;
        }
        return Tier.NEUTRAL;
    }

    public String getTierLabel(int rep) { return getTier(rep).label; }

    public double getPriceModifier(int rep) {
        if (rep >= 50)  return 0.85;  // 15% discount
        if (rep >= 20)  return 0.95;  // 5% discount
        if (rep <= -50) return 1.15;  // 15% surcharge
        if (rep <= -20) return 1.08;  // 8% surcharge
        return 1.0;
    }

    public List<String> getLog() { return Collections.unmodifiableList(log); }
}

// ════════════════════════════════════════════════════════════════════════════
// AnimationHandler
// ════════════════════════════════════════════════════════════════════════════
/**
 * AnimationHandler — Manages sprite sheet animations for a single entity.
 *
 * Each animation is defined by:
 *   name       – unique key ("walk_down", "attack", "hurt")
 *   row        – sprite sheet row index
 *   frameCount – number of frames in this animation
 *   fps        – playback speed
 *   loop       – whether to loop or play once then hold last frame
 *
 * Usage:
 *   handler.addAnim("walk_down",  0, 4, 8,  true);
 *   handler.addAnim("walk_left",  1, 4, 8,  true);
 *   handler.addAnim("attack",     4, 5, 12, false);
 *   handler.play("attack");       // switches to attack
 *   handler.update(dt);
 *   handler.render(gc, sheet, x, y, 32, 32);
 */
class AnimationHandler {

    private record AnimDef(String name, int row, int frames, double fps, boolean loop) {}

    private final Map<String, AnimDef> anims = new LinkedHashMap<>();
    private AnimDef current  = null;
    private int    frame     = 0;
    private double timer     = 0;
    private boolean finished = false;

    public void addAnim(String name, int row, int frames, double fps, boolean loop) {
        anims.put(name, new AnimDef(name, row, frames, fps, loop));
        if (current == null) current = anims.get(name);
    }

    public void play(String name) {
        AnimDef def = anims.get(name);
        if (def == null || def.equals(current)) return;
        current  = def;
        frame    = 0;
        timer    = 0;
        finished = false;
    }

    public void update(double dt) {
        if (current == null || finished) return;
        timer += dt;
        double frameDuration = 1.0 / current.fps;
        while (timer >= frameDuration) {
            timer -= frameDuration;
            frame++;
            if (frame >= current.frames) {
                if (current.loop) {
                    frame = 0;
                } else {
                    frame    = current.frames - 1;
                    finished = true;
                    break;
                }
            }
        }
    }

    public void render(GraphicsContext gc, Image sheet, double x, double y, int fw, int fh) {
        if (current == null || sheet == null) return;
        int srcX = frame * fw;
        int srcY = current.row * fh;
        gc.drawImage(sheet, srcX, srcY, fw, fh, x, y, fw, fh);
    }

    public boolean isFinished() { return finished; }
    public String  getCurrent() { return current != null ? current.name : null; }
    public int     getFrame()   { return frame; }
}
