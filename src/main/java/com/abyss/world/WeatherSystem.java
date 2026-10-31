package com.abyss.world;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WeatherSystem — Dynamic weather that transitions probabilistically and
 * renders screen-space particle effects.
 *
 * Weather types:
 *   CLEAR, CLOUDY, RAIN, HEAVY_RAIN, SNOW, FOG, STORM
 *
 * Each weather type has:
 *  - A visual particle effect (rain streaks, snowflakes, fog overlay)
 *  - A tint / opacity modifier stacked on top of the day/night overlay
 *  - Ambient audio hint (returned as a string for AssetLoader to play)
 *  - Transition probabilities to other weather types
 *
 * Weather only changes during transitions (~every 3–7 game minutes).
 */
public class WeatherSystem {

    // ── Types ─────────────────────────────────────────────────────────────
    public enum WeatherType { CLEAR, CLOUDY, RAIN, HEAVY_RAIN, SNOW, FOG, STORM }

    // ── State ─────────────────────────────────────────────────────────────
    private WeatherType current     = WeatherType.CLEAR;
    private WeatherType next        = WeatherType.CLEAR;
    private double      transTimer  = 0;     // time until next weather check
    private double      transDelay  = 180.0; // seconds between weather changes
    private double      transProgress = 1.0; // 0..1 blend from current to next
    private static final double TRANS_SPEED = 0.005; // blend per second

    // ── Particles ─────────────────────────────────────────────────────────
    private final List<WeatherParticle> particles = new ArrayList<>();
    private final Random rng = new Random();

    // ── Screen size (updated from render call) ────────────────────────────
    private double screenW = 960, screenH = 640;

    // ─────────────────────────────────────────────────────────────────────
    public void update(double dt, DayNightCycle dayNight) {
        transTimer -= dt;
        if (transTimer <= 0) {
            transTimer = transDelay * (0.5 + rng.nextDouble());
            next       = rollNextWeather(dayNight);
        }

        // Blend towards next weather
        if (transProgress < 1.0) {
            transProgress = Math.min(1.0, transProgress + TRANS_SPEED * dt * 60);
            if (transProgress >= 1.0) { current = next; transProgress = 1.0; }
        } else if (!current.equals(next)) {
            transProgress = 0.0;
        }

        // Spawn and update particles
        spawnParticles(dt);
        particles.removeIf(p -> !p.update(dt, screenH));
    }

    private WeatherType rollNextWeather(DayNightCycle dn) {
        // Night → more likely storm/rain; Day → more likely clear
        boolean isNight = dn.getPhase() == DayNightCycle.Phase.NIGHT;
        double r = rng.nextDouble();
        return switch (current) {
            case CLEAR      -> r < 0.6 ? WeatherType.CLEAR : r < 0.8 ? WeatherType.CLOUDY : WeatherType.RAIN;
            case CLOUDY     -> r < 0.3 ? WeatherType.CLEAR : r < 0.7 ? WeatherType.RAIN   : WeatherType.FOG;
            case RAIN       -> r < 0.3 ? WeatherType.CLOUDY: r < 0.6 ? WeatherType.RAIN   : (isNight ? WeatherType.STORM : WeatherType.HEAVY_RAIN);
            case HEAVY_RAIN -> r < 0.4 ? WeatherType.RAIN  : r < 0.7 ? WeatherType.STORM  : WeatherType.CLEAR;
            case SNOW       -> r < 0.6 ? WeatherType.SNOW  : r < 0.85? WeatherType.CLOUDY : WeatherType.CLEAR;
            case FOG        -> r < 0.5 ? WeatherType.CLEAR : WeatherType.CLOUDY;
            case STORM      -> r < 0.6 ? WeatherType.HEAVY_RAIN : WeatherType.RAIN;
        };
    }

    // ── Particle spawning ─────────────────────────────────────────────────
    private void spawnParticles(double dt) {
        int max = switch (current) {
            case RAIN       -> 5;
            case HEAVY_RAIN -> 12;
            case STORM      -> 18;
            case SNOW       -> 4;
            default         -> 0;
        };
        for (int i = 0; i < max; i++) {
            if (particles.size() < 400) {
                particles.add(new WeatherParticle(current, screenW, rng));
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────
    public void render(GraphicsContext gc, double w, double h, Camera cam) {
        this.screenW = w;
        this.screenH = h;

        // Fog overlay
        if (current == WeatherType.FOG || current == WeatherType.CLOUDY) {
            double alpha = current == WeatherType.FOG ? 0.45 : 0.15;
            gc.setFill(Color.color(0.75, 0.78, 0.82, alpha));
            gc.fillRect(0, 0, w, h);
        }
        // Storm dark tint
        if (current == WeatherType.STORM) {
            gc.setFill(Color.color(0.03, 0.03, 0.08, 0.4));
            gc.fillRect(0, 0, w, h);
            // Random lightning flash
            if (rng.nextDouble() < 0.003) {
                gc.setFill(Color.color(0.9, 0.95, 1.0, 0.25));
                gc.fillRect(0, 0, w, h);
            }
        }
        // Particles
        for (WeatherParticle p : particles) p.render(gc);
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public WeatherType getCurrent()  { return current; }
    public String      getLabel()    {
        return switch (current) {
            case CLEAR      -> "☀ Clear";
            case CLOUDY     -> "☁ Cloudy";
            case RAIN       -> "🌧 Rain";
            case HEAVY_RAIN -> "⛈ Heavy Rain";
            case SNOW       -> "❄ Snow";
            case FOG        -> "🌫 Fog";
            case STORM      -> "⛈ Storm";
        };
    }

    public String getAmbientAudio() {
        return switch (current) {
            case RAIN, HEAVY_RAIN -> "audio/rain.mp3";
            case STORM            -> "audio/storm.mp3";
            case SNOW             -> "audio/wind.mp3";
            default               -> null;
        };
    }

    // ── Inner particle class ──────────────────────────────────────────────
    private static class WeatherParticle {
        float x, y, vx, vy, alpha;
        WeatherType type;

        WeatherParticle(WeatherType type, double screenW, Random rng) {
            this.type  = type;
            this.x     = (float)(rng.nextDouble() * screenW);
            this.y     = 0;
            this.alpha = 0.5f + rng.nextFloat() * 0.4f;
            switch (type) {
                case RAIN, HEAVY_RAIN -> { vy = 300 + rng.nextFloat() * 150; vx = -30; }
                case STORM            -> { vy = 400 + rng.nextFloat() * 200; vx = -80 - rng.nextFloat() * 60; }
                case SNOW             -> { vy = 40  + rng.nextFloat() * 30;  vx = -10 + rng.nextFloat() * 20; }
                default               -> { vy = 0; vx = 0; }
            }
        }

        boolean update(double dt, double screenH) {
            x += vx * dt;
            y += vy * dt;
            return y < screenH + 10;
        }

        void render(GraphicsContext gc) {
            gc.setStroke(switch (type) {
                case SNOW  -> Color.color(0.9, 0.95, 1.0, alpha);
                case STORM -> Color.color(0.6, 0.7, 0.9, alpha * 0.9);
                default    -> Color.color(0.5, 0.65, 0.9, alpha);
            });
            gc.setLineWidth(type == WeatherType.SNOW ? 2 : 1);
            if (type == WeatherType.SNOW) {
                gc.strokeOval(x, y, 3, 3);
            } else {
                double len = type == WeatherType.STORM ? 14 : 9;
                gc.strokeLine(x, y, x + vx * 0.02, y + len);
            }
        }
    }
}
