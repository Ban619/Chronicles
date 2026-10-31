package com.abyss.world;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * DayNightCycle — Controls the passage of time and applies a full-screen
 * ambient overlay that tints the scene accordingly.
 *
 * Time is normalised to [0.0, 1.0] representing a full in-game day:
 *   0.00 – 0.25 : Dawn     (orange → bright)
 *   0.25 – 0.50 : Day      (clear, no overlay)
 *   0.50 – 0.75 : Dusk     (amber / red tint)
 *   0.75 – 1.00 : Night    (deep blue overlay, reduced visibility)
 *
 * One real-world minute = one full in-game day by default (configurable).
 */
public class DayNightCycle {

    // ── Time ──────────────────────────────────────────────────────────────
    private double normalizedTime = 0.25; // start at dawn
    private double dayDuration    = 300.0; // seconds per full cycle (5 min)

    // ── Phases ────────────────────────────────────────────────────────────
    public enum Phase { DAWN, DAY, DUSK, NIGHT }

    // ─────────────────────────────────────────────────────────────────────
    public void update(double dt) {
        if (!Double.isFinite(dt) || dt <= 0 || dayDuration <= 0) return;
        normalizedTime = wrapTime(normalizedTime + dt / dayDuration);
    }

    // ── Overlay rendering ─────────────────────────────────────────────────
    public void renderOverlay(GraphicsContext gc, double w, double h) {
        Color overlay = computeOverlayColor();
        if (overlay == null) return;
        gc.setFill(overlay);
        gc.fillRect(0, 0, w, h);
    }

    private Color computeOverlayColor() {
        double t = normalizedTime;

        if (t < 0.20) {
            // Night → Dawn: fade from deep blue to transparent
            double pct = t / 0.20;
            return Color.color(0.04, 0.06, 0.22, lerp(0.72, 0.0, pct));
        } else if (t < 0.28) {
            // Dawn: warm orange tint
            double pct = (t - 0.20) / 0.08;
            return Color.color(lerp(0.9,0.0,pct), lerp(0.4,0.0,pct), 0.0, lerp(0.25, 0.0, pct));
        } else if (t < 0.65) {
            // Day: no overlay
            return null;
        } else if (t < 0.72) {
            // Dusk begin: amber tint builds
            double pct = (t - 0.65) / 0.07;
            return Color.color(0.6, 0.2, 0.0, lerp(0.0, 0.2, pct));
        } else if (t < 0.78) {
            // Dusk → Night transition
            double pct = (t - 0.72) / 0.06;
            return Color.color(lerp(0.6, 0.04, pct), lerp(0.2, 0.06, pct), lerp(0.0, 0.22, pct),
                               lerp(0.2, 0.72, pct));
        } else {
            // Full night
            return Color.color(0.04, 0.06, 0.22, 0.72);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private double lerp(double a, double b, double t) { return a + (b - a) * Math.min(1, Math.max(0, t)); }

    public Phase getPhase() {
        if (normalizedTime < 0.20 || normalizedTime >= 0.78) return Phase.NIGHT;
        if (normalizedTime < 0.28) return Phase.DAWN;
        if (normalizedTime < 0.65) return Phase.DAY;
        return Phase.DUSK;
    }

    public String getPhaseLabel() {
        return switch (getPhase()) {
            case DAWN  -> "Dawn";
            case DAY   -> "Day";
            case DUSK  -> "Dusk";
            case NIGHT -> "Night";
        };
    }

    /** Human-readable in-game time (HH:MM). */
    public String getTimeString() {
        int totalMinutes = (int)(normalizedTime * 24 * 60);
        int hours   = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
    }

    public double getNormalizedTime() { return normalizedTime; }

    /** Smooth daylight strength: low at night, rising at dawn and fading at dusk. */
    public double getDaylightLevel() {
        if (normalizedTime < 0.20 || normalizedTime >= 0.78) return 0.12;
        if (normalizedTime < 0.28) {
            return lerp(0.12, 1.0, smoothStep((normalizedTime - 0.20) / 0.08));
        }
        if (normalizedTime < 0.65) return 1.0;
        return lerp(1.0, 0.12, smoothStep((normalizedTime - 0.65) / 0.13));
    }

    public void setDayDuration(double seconds) {
        if (Double.isFinite(seconds) && seconds > 0) dayDuration = seconds;
    }

    private double smoothStep(double value) {
        double t = Math.min(1, Math.max(0, value));
        return t * t * (3 - 2 * t);
    }

    private double wrapTime(double value) {
        return ((value % 1.0) + 1.0) % 1.0;
    }
}
