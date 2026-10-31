package com.abyss.world;

import javafx.scene.paint.Color;

/**
 * LightSource — A single point light in world space.
 *
 * Properties:
 *   x, y       – world-pixel centre of the light
 *   radius     – falloff distance in pixels (hard-edge is softened by the engine's radial gradient)
 *   color      – base RGBA tint of the light cone
 *   type       – animation behaviour:
 *       STEADY  – constant, no variation
 *       FLICKER – fast random intensity oscillation (torches, candles)
 *       PULSE   – slow sinusoidal breathing (lava, magic)
 *
 * The LightingEngine reads these each frame and composites them onto the
 * shadow mask canvas via radial gradients with multiply/screen blending.
 */
public class LightSource {

    public enum Type { STEADY, FLICKER, PULSE }

    // ── Position (world pixels) ───────────────────────────────────────────
    private float x, y;

    // ── Light properties ──────────────────────────────────────────────────
    private final float baseRadius;
    private float       currentRadius;
    private final Color baseColor;
    private       Color currentColor;
    private final Type  type;

    // ── Animation state ───────────────────────────────────────────────────
    private double phase       = Math.random() * Math.PI * 2; // offset so torches don't sync
    private double flickerSeed = Math.random();
    private long   lastUpdate  = 0;

    // ─────────────────────────────────────────────────────────────────────
    public LightSource(float x, float y, float radius, Color color, Type type) {
        this.x             = x;
        this.y             = y;
        this.baseRadius    = radius;
        this.currentRadius = radius;
        this.baseColor     = color;
        this.currentColor  = color;
        this.type          = type;
    }

    // ── Update animation ──────────────────────────────────────────────────
    public void update(long nowMs) {
        if (lastUpdate == 0) { lastUpdate = nowMs; return; }
        double dt = (nowMs - lastUpdate) / 1000.0;
        lastUpdate = nowMs;
        phase += dt;

        switch (type) {
            case STEADY -> {
                currentRadius = baseRadius;
                currentColor  = baseColor;
            }
            case FLICKER -> {
                // Fast noise: multiply two sine waves at different frequencies
                double noise = 0.85
                    + 0.08 * Math.sin(phase * 11.3 + flickerSeed * 6.28)
                    + 0.07 * Math.sin(phase * 17.7 + flickerSeed * 3.14);
                currentRadius = (float)(baseRadius * noise);
                // Slightly warmer on bright flickers
                double bright = Math.max(0, Math.min(1, noise));
                currentColor  = Color.color(
                    clamp(baseColor.getRed()   + bright * 0.08),
                    clamp(baseColor.getGreen() - bright * 0.04),
                    clamp(baseColor.getBlue()  - bright * 0.08),
                    baseColor.getOpacity());
            }
            case PULSE -> {
                double pulse  = 0.88 + 0.12 * Math.sin(phase * 1.8);
                currentRadius = (float)(baseRadius * pulse);
                currentColor  = Color.color(
                    clamp(baseColor.getRed()),
                    clamp(baseColor.getGreen() * 0.8),
                    clamp(baseColor.getBlue()),
                    baseColor.getOpacity() * pulse);
            }
        }
    }

    // ── Position setters (used for player light that follows the character) ─
    public void moveTo(float newX, float newY) { this.x = newX; this.y = newY; }

    // ── Getters ───────────────────────────────────────────────────────────
    public float  getX()             { return x; }
    public float  getY()             { return y; }
    public float  getRadius()        { return currentRadius; }
    public Color  getColor()         { return currentColor; }
    public Type   getType()          { return type; }
    public float  getBaseRadius()    { return baseRadius; }

    // ── Helper ────────────────────────────────────────────────────────────
    private double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}
