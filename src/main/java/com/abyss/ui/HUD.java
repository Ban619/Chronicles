package com.abyss.ui;

import com.abyss.entities.Player;
import com.abyss.items.Item;
import com.abyss.world.DayNightCycle;
import com.abyss.world.WeatherSystem;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * HUD — Heads-up display rendered every frame in screen space.
 *
 * Elements:
 *  ┌──────────────────────────────────────────────────────────┐
 *  │ [Player Panel]          [Clock | Weather | Location]     │
 *  │  Name  Lv.5                  14:32  ⛈Storm              │
 *  │  HP ████████░░   100/120     Ashenveil                   │
 *  │  MP ████░░░░░░    40/50                                   │
 *  │  XP ██░░░░░░░░   250/400                                  │
 *  │                                                          │
 *  │              [Hotbar]                  [Mini-Map]         │
 *  │  [1][2][3][4][5][6][7][8][9]                             │
 *  └──────────────────────────────────────────────────────────┘
 *
 * HUD auto-scales with canvas size (responsive).
 */
public class HUD {

    // ── References ────────────────────────────────────────────────────────
    private final Player        player;
    private final DayNightCycle dayNight;
    private final WeatherSystem weather;

    // ── Notification system ───────────────────────────────────────────────
    private String  notification    = "";
    private double  notifTimer      = 0;
    private Color   notifColor      = Color.web("#c8a96e");
    private String location = "Ashenveil";

    // ─────────────────────────────────────────────────────────────────────
    public HUD(Player player, DayNightCycle dayNight, WeatherSystem weather) {
        this.player   = player;
        this.dayNight = dayNight;
        this.weather  = weather;
    }

    // ── Notification ──────────────────────────────────────────────────────
    public void showNotification(String msg, Color color) {
        notification = msg;
        notifTimer   = 3.0;
        notifColor   = color;
    }

    public void setLocation(String location) {
        this.location = location == null || location.isBlank() ? "Unknown" : location;
    }

    // ── Main render ───────────────────────────────────────────────────────
    public void render(GraphicsContext gc, double w, double h, int fps) {
        renderPlayerPanel(gc, w, h);
        renderInfoPanel(gc, w, h);
        renderHotbar(gc, w, h);
        renderNotification(gc, w, h);
        renderFPS(gc, w, h, fps);
    }

    // ── Player stat panel (top-left) ──────────────────────────────────────
    private void renderPlayerPanel(GraphicsContext gc, double w, double h) {
        double px = 10, py = 10;
        double pw = 210, ph = 90;

        // Panel background
        gc.setFill(Color.color(0.04, 0.04, 0.08, 0.82));
        gc.fillRoundRect(px, py, pw, ph, 6, 6);
        gc.setStroke(Color.web("#2a2a3a"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(px, py, pw, ph, 6, 6);

        // Name + Level
        gc.setFill(Color.web("#c8a96e"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        gc.fillText(player.getName(), px + 10, py + 18);
        gc.setFill(Color.web("#7a6e5a"));
        gc.setFont(Font.font("Serif", 11));
        gc.fillText("Lv." + player.getLevel(), px + pw - 45, py + 18);

        // HP bar
        renderBar(gc, px + 10, py + 24, pw - 20, 9,
                  player.getHp(), player.getMaxHp(),
                  Color.web("#7f0000"), Color.web("#c0392b"), "HP");

        // MP bar
        renderBar(gc, px + 10, py + 42, pw - 20, 9,
                  player.getMp(), player.getMaxMp(),
                  Color.web("#00237f"), Color.web("#2980b9"), "MP");

        // XP bar
        renderBar(gc, px + 10, py + 60, pw - 20, 9,
                  player.getXp(), player.getXpToNext(),
                  Color.web("#3d007f"), Color.web("#8e44ad"), "XP");

        // Gold
        gc.setFill(Color.web("#f0c040"));
        gc.setFont(Font.font("Serif", 11));
        gc.fillText("✦ " + player.getGold() + " gold", px + 10, py + 82);
    }

    private void renderBar(GraphicsContext gc,
                           double x, double y, double bw, double bh,
                           int cur, int max,
                           Color bgColor, Color fgColor, String label) {
        double pct = max == 0 ? 0 : Math.max(0, Math.min(1.0, (double) cur / max));

        // BG track
        gc.setFill(Color.color(0, 0, 0, 0.5));
        gc.fillRect(x + 22, y, bw - 22, bh);

        // Fill
        gc.setFill(bgColor);
        gc.fillRect(x + 22, y, (bw - 22) * pct, bh);
        // Highlight
        gc.setFill(Color.color(1, 1, 1, 0.1));
        gc.fillRect(x + 22, y, (bw - 22) * pct, bh / 2.0);

        // Label
        gc.setFill(Color.web("#7a6e5a"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 9));
        gc.fillText(label, x, y + bh - 1);

        // Values
        gc.setFill(Color.web("#d4c5a9"));
        gc.setFont(Font.font("Serif", 9));
        gc.fillText(cur + "/" + max, x + 22 + 2, y + bh - 1);
    }

    // ── Info panel (top-right) ────────────────────────────────────────────
    private void renderInfoPanel(GraphicsContext gc, double w, double h) {
        double pw = 200, ph = 76;
        double px = w - pw - 10, py = 10;

        gc.setFill(Color.color(0.04, 0.04, 0.08, 0.82));
        gc.fillRoundRect(px, py, pw, ph, 6, 6);
        gc.setStroke(Color.web("#2a2a3a"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(px, py, pw, ph, 6, 6);

        // Day/night
        gc.setFill(Color.web("#c8a96e"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 12));
        gc.fillText(dayNight.getTimeString() + "  " + dayNight.getPhaseLabel(), px + 10, py + 18);

        // Weather
        gc.setFill(Color.web("#7a6e5a"));
        gc.setFont(Font.font("Serif", 11));
        gc.fillText(weather.getLabel(), px + 10, py + 34);

        // Location (from map name - simplified)
        gc.setFill(Color.web("#555566"));
        gc.setFont(Font.font("Serif", 10));
        gc.fillText(location, px + 10, py + 50);
        if (player.getRoadQuestStage() > 0) {
            gc.setFill(Color.web("#f0c040"));
            gc.setFont(Font.font("Serif", 10));
            String questStatus = player.getRoadQuestStage() == 2 ? "Road secured"
                    : "Road threat: " + player.getRoadQuestKills() + "/3";
            gc.fillText(questStatus, px + 10, py + 66);
        }
    }

    // ── Hotbar (bottom-centre) ────────────────────────────────────────────
    private void renderHotbar(GraphicsContext gc, double w, double h) {
        int slots = 9;
        double sw  = 46, sh = 46, gap = 4;
        double totalW = slots * (sw + gap) - gap;
        double hx = w / 2 - totalW / 2;
        double hy = h - sh - 10;

        // Panel bg
        gc.setFill(Color.color(0.04, 0.04, 0.08, 0.85));
        gc.fillRoundRect(hx - 6, hy - 6, totalW + 12, sh + 12, 6, 6);
        gc.setStroke(Color.web("#2a2a3a"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(hx - 6, hy - 6, totalW + 12, sh + 12, 6, 6);

        Item[] inv = player.getInventory();
        for (int i = 0; i < slots; i++) {
            double sx = hx + i * (sw + gap);
            boolean active = i == player.getHotbarSlot();

            // Slot bg
            gc.setFill(active ? Color.color(0.5, 0.42, 0.27, 0.3) : Color.color(0, 0, 0, 0.4));
            gc.fillRect(sx, hy, sw, sh);
            gc.setStroke(active ? Color.web("#c8a96e") : Color.web("#2a2a3a"));
            gc.setLineWidth(active ? 1.5 : 1);
            gc.strokeRect(sx, hy, sw, sh);

            // Item icon (emoji fallback)
            if (inv[i] != null) {
                gc.setFill(Color.web(inv[i].getRarityHex()));
                gc.setFont(Font.font("Serif", 18));
                gc.fillText(getItemEmoji(inv[i]), sx + 12, hy + 28);

                if (inv[i].getCount() > 1) {
                    gc.setFill(Color.web("#f0c040"));
                    gc.setFont(Font.font("Serif", 9));
                    gc.fillText("" + inv[i].getCount(), sx + sw - 14, hy + sh - 4);
                }
            }

            // Slot number
            gc.setFill(Color.web("#444"));
            gc.setFont(Font.font("Serif", 8));
            gc.fillText(String.valueOf(i + 1), sx + 3, hy + 11);
        }
    }

    private String getItemEmoji(Item item) {
        String id = item.getId().toLowerCase();
        if (id.contains("hp") || id.contains("potion"))   return "⚗";
        if (id.contains("mp"))                             return "✦";
        if (id.contains("sword") || id.contains("weapon"))return "⚔";
        if (id.contains("shield") || id.contains("armor")) return "🛡";
        if (id.contains("food")  || id.contains("bread")) return "🍞";
        if (id.contains("key"))                            return "🗝";
        if (id.contains("scroll"))                         return "📜";
        return "◈";
    }

    // ── Notification bar ──────────────────────────────────────────────────
    private void renderNotification(GraphicsContext gc, double w, double h) {
        if (notifTimer <= 0 || notification.isEmpty()) return;
        double alpha = Math.min(1.0, notifTimer);
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.color(0.04, 0.04, 0.08, 0.85));
        double nw = notification.length() * 8 + 30;
        gc.fillRoundRect(w / 2 - nw / 2, h / 2 - 40, nw, 30, 6, 6);
        gc.setFill(notifColor);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        gc.fillText(notification, w / 2 - nw / 2 + 10, h / 2 - 20);
        gc.setGlobalAlpha(1.0);
        notifTimer -= 1.0 / 60.0; // approximate; real dt injected via update()
    }

    // ── FPS counter (dev mode) ────────────────────────────────────────────
    private void renderFPS(GraphicsContext gc, double w, double h, int fps) {
        gc.setFill(Color.color(0.5, 0.5, 0.5, 0.5));
        gc.setFont(Font.font("Monospaced", 10));
        gc.fillText("FPS: " + fps, w - 60, h - 14);
    }
}
