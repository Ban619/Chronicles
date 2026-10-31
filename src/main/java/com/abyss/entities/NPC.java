package com.abyss.entities;

import com.abyss.utils.AssetLoader;
import com.abyss.utils.DialogueHandler;
import com.abyss.world.DayNightCycle;
import com.abyss.world.GameMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC — Non-player character with patrol AI, schedules, and reputation-aware dialogue.
 *
 * Patrol behaviour:
 *   NPCs follow a list of waypoints, pausing briefly at each.
 *   At night some NPCs retreat indoors (schedule-based).
 *
 * Dialogue:
 *   Each NPC holds a DialogueTree identified by an NPC-unique key.
 *   DialogueHandler resolves the correct branch based on:
 *     - Quest flags
 *     - Player reputation score
 *     - Time of day
 */
public class NPC extends Entity {

    // ── Types ──────────────────────────────────────────────────────────────
    public enum NPCType {
        VILLAGER, MERCHANT, BAKER, GUARD, QUEST_GIVER, HEALER, BLACKSMITH, COMPANION
    }

    // ── Fields ─────────────────────────────────────────────────────────────
    private final NPCType type;
    private final String  dialogueKey;

    // Patrol waypoints (pixel positions)
    private final List<float[]> waypoints = new ArrayList<>();
    private int     waypointIdx  = 0;
    private double  waypointWait = 0;         // seconds to pause at waypoint
    private boolean waiting      = false;

    private static final float PATROL_SPEED  = 40f;  // px/s
    private static final float WAYPOINT_DIST = 4f;   // snap radius

    // Schedule — indoor at night
    private boolean indoors     = false;
    private float   homeX, homeY;
    private boolean hasHome     = false;

    // Facing direction for sprite
    private int  direction = 0; // 0=down 1=left 2=right 3=up
    private boolean moving = false;

    // Name tag visibility distance
    private static final float NAME_DIST = 120f;

    // ─────────────────────────────────────────────────────────────────────
    public NPC(String name, NPCType type, float x, float y,
               String dialogueKey, AssetLoader loader) {
        super(name, x, y, 32, 32);
        this.type        = type;
        this.dialogueKey = dialogueKey;
    }

    // ── Patrol setup ──────────────────────────────────────────────────────
    public void addWaypoint(float wx, float wy) {
        waypoints.add(new float[]{wx, wy});
    }

    public void setHome(float hx, float hy) {
        this.homeX   = hx;
        this.homeY   = hy;
        this.hasHome = true;
    }

    // ── Update ────────────────────────────────────────────────────────────
    @Override
    public void update(double dt, GameMap map) {
        if (!active) return;
        // handled by overloaded update below
    }

    /** Extended update with day/night info and player for interaction range. */
    public void update(double dt, GameMap map, Player player, DayNightCycle dayNight) {
        handleSchedule(dayNight);
        if (!active) return;

        patrol(dt, map);
    }

    private void handleSchedule(DayNightCycle dn) {
        if (!hasHome) return;
        boolean nightTime = dn.getNormalizedTime() > 0.75 || dn.getNormalizedTime() < 0.15;
        if (nightTime && !indoors) {
            // Snap home
            x       = homeX;
            y       = homeY;
            indoors = true;
            active  = false; // hidden inside house
        } else if (!nightTime && indoors) {
            indoors = false;
            active  = true;
        }
    }

    private void patrol(double dt, GameMap map) {
        if (waypoints.isEmpty()) { moving = false; return; }

        if (waiting) {
            waypointWait -= dt;
            moving = false;
            if (waypointWait <= 0) {
                waiting = false;
                waypointIdx = (waypointIdx + 1) % waypoints.size();
            }
            return;
        }

        float[] target = waypoints.get(waypointIdx);
        float   dx     = target[0] - x;
        float   dy     = target[1] - y;
        float   dist   = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < WAYPOINT_DIST) {
            x = target[0];
            y = target[1];
            waiting      = true;
            waypointWait = 1.5 + Math.random() * 2.0;
            moving       = false;
            return;
        }

        float nx = dx / dist;
        float ny = dy / dist;

        float newX = x + nx * PATROL_SPEED * (float) dt;
        float newY = y + ny * PATROL_SPEED * (float) dt;

        if (!map.isBlocked(newX, y, width, height, this)) x = newX;
        if (!map.isBlocked(x, newY, width, height, this)) y = newY;

        moving = true;
        // Update facing
        if (Math.abs(nx) > Math.abs(ny)) direction = nx < 0 ? 1 : 2;
        else                              direction  = ny < 0 ? 3 : 0;
    }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void render(GraphicsContext gc) {
        if (!visible || !active) return;
        CharacterPainter.draw(gc, x, y, width, height,
                CharacterPainter.Style.valueOf(type == NPCType.BAKER ? NPCType.MERCHANT.name() : type.name()),
                name, direction, moving);

        // A bright speech bubble keeps quest-givers easy to spot at a glance.
        if (type == NPCType.QUEST_GIVER) {
            double bounce = Math.sin(System.currentTimeMillis() / 260.0) * 1.5;
            gc.save();
            gc.setFill(Color.web("#fff2b4"));
            gc.fillOval(x + 10, y - 12 + bounce, 12, 12);
            gc.setStroke(Color.web("#d99c48"));
            gc.setLineWidth(1.2);
            gc.strokeOval(x + 10, y - 12 + bounce, 12, 12);
            gc.setFill(Color.web("#b8753c"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 9));
            gc.fillText("!", x + 15, y - 3 + bounce);
            gc.restore();
        }
    }

    /** Render name tag — called after main render to allow distance check. */
    public void renderNameTag(GraphicsContext gc, Player player) {
        if (distanceTo(player) > NAME_DIST) return;
        gc.save();
        double tw = name.length() * 6.0;
        double tagX = x + width / 2.0 - tw / 2 - 7;
        gc.setFill(Color.color(0.12, 0.15, 0.23, 0.82));
        gc.fillRoundRect(tagX, y - 21, tw + 14, 15, 8, 8);
        gc.setStroke(Color.color(0.97, 0.83, 0.55, 0.72));
        gc.setLineWidth(0.8);
        gc.strokeRoundRect(tagX, y - 21, tw + 14, 15, 8, 8);
        gc.setFill(Color.web("#fff0cf"));
        gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, 10));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(name, x + width / 2.0, y - 10);
        gc.restore();
    }

    // ── Dialogue ──────────────────────────────────────────────────────────
    /**
     * Resolve dialogue text for the given player state.
     * Delegates to the DialogueHandler which reads from dialogue JSON files.
     */
    public String getDialogue(Player player, DialogueHandler dialogueHandler) {
        return dialogueHandler.resolve(dialogueKey, player.getReputation(),
                                       player.getLevel(), player.getName());
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public NPCType getType()        { return type; }
    public String  getDialogueKey() { return dialogueKey; }
}
