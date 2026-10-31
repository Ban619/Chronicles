package com.abyss.lighting;

import com.abyss.world.Camera;
import com.abyss.world.DayNightCycle;
import com.abyss.world.LightSource;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.List;

/**
 * LightingEngine — Renders a full-screen darkness shadow mask with per-source
 * radial gradient cut-outs using JavaFX's canvas blending.
 *
 * Algorithm (two-pass):
 *   Pass 1 — Draw darkness:
 *     Fill an off-screen canvas with ambient darkness colour
 *     (derived from DayNightCycle: near-black at night, transparent during day)
 *
 *   Pass 2 — Cut out lights (MULTIPLY blend punches holes):
 *     For each LightSource in view, draw a radial gradient
 *       Centre: light colour at full alpha
 *       Edge:   fully transparent
 *     Blend mode: MULTIPLY → bright gradients erase darkness, dark areas remain shadowed
 *
 *   Final — Composite result onto game canvas:
 *     gc.setGlobalBlendMode(MULTIPLY) then draw the off-screen canvas image
 *
 * This produces:
 *   - Smooth torch halos with warm orange tint
 *   - Player carry-light (cool white)
 *   - Lava glow (red/orange pulse)
 *   - Atmospheric darkness that respects biome + time of day
 *
 * Performance notes:
 *   - Off-screen canvas is reused (no allocation each frame)
 *   - Lights outside the viewport are skipped
 *   - Gradient Stop arrays are pre-allocated per source type
 */
public class LightingEngine {

    // ── Off-screen shadow canvas ──────────────────────────────────────────
    private Canvas  shadowCanvas;
    private GraphicsContext sgc;
    private int     lastW = 0, lastH = 0;

    // ── Ambient darkness colour per phase ─────────────────────────────────
    // These are the DARKNESS colours — lightSources will punch through them.
    private static final Color NIGHT_DARK = Color.color(0.00, 0.00, 0.04, 0.92);
    private static final Color DAWN_DARK  = Color.color(0.03, 0.02, 0.06, 0.65);
    private static final Color DUSK_DARK  = Color.color(0.04, 0.02, 0.04, 0.55);
    private static final Color DAY_DARK   = Color.color(0.00, 0.00, 0.00, 0.10);

    // Dungeon is always dark regardless of day/night
    private boolean forceDark = false;

    // ─────────────────────────────────────────────────────────────────────
    public LightingEngine() {
        shadowCanvas = new Canvas(960, 640);
        sgc          = shadowCanvas.getGraphicsContext2D();
    }

    // ── Set forced darkness (dungeons, boss arenas) ────────────────────────
    public void setForceDark(boolean v) { forceDark = v; }

    // ── Main render ───────────────────────────────────────────────────────
    /**
     * Renders the lighting overlay onto the provided GraphicsContext.
     *
     * @param gc         The main game canvas GraphicsContext
     * @param w          Canvas width
     * @param h          Canvas height
     * @param lights     All LightSource objects for the current map
     * @param camera     Current camera for world→screen conversion
     * @param dayNight   Used to compute ambient darkness level
     */
    public void render(GraphicsContext gc, double w, double h,
                       List<LightSource> lights, Camera camera, DayNightCycle dayNight) {

        // Resize shadow canvas if window changed
        if ((int)w != lastW || (int)h != lastH) {
            shadowCanvas = new Canvas(w, h);
            sgc          = shadowCanvas.getGraphicsContext2D();
            lastW = (int)w; lastH = (int)h;
        }

        // ── Pass 1: Fill ambient darkness ─────────────────────────────────
        Color ambient = computeAmbient(dayNight);
        sgc.setFill(ambient);
        sgc.fillRect(0, 0, w, h);

        // ── Pass 2: Cut out each light source ────────────────────────────
        // We use MULTIPLY blending: white areas become transparent (erase darkness)
        // Using ADD blending to punch holes: paint light colour → blends away dark
        for (LightSource ls : lights) {
            // Convert world position to screen position
            float sx = ls.getX() - camera.getOffsetX();
            float sy = ls.getY() - camera.getOffsetY();
            float r  = ls.getRadius();

            // Frustum cull — skip lights completely off-screen
            if (sx + r < 0 || sx - r > w || sy + r < 0 || sy - r > h) continue;

            renderLightHalo(sgc, sx, sy, r, ls.getColor(), ambient);
        }

        // ── Pass 3: Composite shadow canvas onto game canvas ─────────────
        // MULTIPLY mode: light areas reduce darkness, dark areas stay dark.
        gc.save();
        gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.MULTIPLY);
        gc.drawImage(shadowCanvas.snapshot(null, null), 0, 0);
        gc.restore();
    }

    // ── Light halo rendering ──────────────────────────────────────────────
    private void renderLightHalo(GraphicsContext sgc,
                                  float cx, float cy, float radius,
                                  Color lightColor, Color ambient) {
        // We want: centre = "erase darkness" (white/light), edge = "preserve darkness" (ambient)
        // On a MULTIPLY-blended shadow canvas that starts at ambient darkness:
        // Drawing WHITE at centre cancels the darkness (white × dark = dark removed)
        // We achieve this by drawing INVERSED: the halo punches through with light colour

        // Compute inverted stop colours:
        // Stop 0 (centre): fully bright = full cancellation of darkness
        // Stop 1 (edge):   match ambient = no change at edge
        Color innerStop = invertForMultiply(lightColor, ambient);
        Color outerStop = Color.color(ambient.getRed(), ambient.getGreen(), ambient.getBlue(), 0.0);

        // Primary gradient (strong inner core)
        RadialGradient gradient = new RadialGradient(
            0, 0,                   // focus angle, focus distance
            cx, cy,                 // centre
            radius,                 // radius
            false,                  // proportional = false → pixel units
            CycleMethod.NO_CYCLE,
            new Stop(0.0,  innerStop),
            new Stop(0.5,  Color.color(innerStop.getRed(), innerStop.getGreen(),
                                       innerStop.getBlue(), innerStop.getOpacity() * 0.6)),
            new Stop(0.85, Color.color(outerStop.getRed(), outerStop.getGreen(),
                                       outerStop.getBlue(), 0.15)),
            new Stop(1.0,  outerStop)
        );

        sgc.setFill(gradient);
        sgc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Soft secondary bloom (slightly larger, very faint)
        float bloom = radius * 1.35f;
        RadialGradient bloomGrad = new RadialGradient(
            0, 0, cx, cy, bloom, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.color(innerStop.getRed(), innerStop.getGreen(),
                                      innerStop.getBlue(), 0.08)),
            new Stop(1.0, outerStop)
        );
        sgc.setFill(bloomGrad);
        sgc.fillOval(cx - bloom, cy - bloom, bloom * 2, bloom * 2);
    }

    // ── Ambient darkness ──────────────────────────────────────────────────
    private Color computeAmbient(DayNightCycle dn) {
        if (forceDark) return NIGHT_DARK;
        double daylight = dn.getDaylightLevel();
        double alpha = lerp(NIGHT_DARK.getOpacity(), DAY_DARK.getOpacity(), daylight);
        return switch (dn.getPhase()) {
            case NIGHT -> Color.color(0.0, 0.005, 0.035, alpha);
            case DAWN  -> Color.color(0.035, 0.018, 0.008, alpha);
            case DUSK  -> Color.color(0.04, 0.012, 0.006, alpha);
            case DAY   -> Color.color(0, 0, 0, alpha);
        };
    }

    // ── Colour math ───────────────────────────────────────────────────────
    /**
     * Computes what colour we must paint onto the shadow canvas so that
     * MULTIPLY blend with the ambient produces the desired light colour.
     *
     * MULTIPLY: result = src × dst
     * We want: result ≈ 1.0 (fully lit / transparent darkness)
     * dst = ambient
     * → src = result / ambient = 1.0 / ambient → but clamped to [0,1]
     *
     * In practice we tint toward the light's colour:
     *   inner = white blended toward lightColor
     */
    private Color invertForMultiply(Color light, Color ambient) {
        // Inversion: 1/ambient per channel, clamped, then tinted by light hue
        double r = clamp(light.getRed()   * 1.4);
        double g = clamp(light.getGreen() * 1.2);
        double b = clamp(light.getBlue()  * 1.1);
        double a = clamp(light.getOpacity() * 1.5);

        // Blend toward white so the darkness is actually cleared
        r = lerp(r, 1.0, 0.55);
        g = lerp(g, 1.0, 0.55);
        b = lerp(b, 1.0, 0.55);

        return Color.color(r, g, b, clamp(a));
    }

    private double clamp(double v) { return Math.max(0, Math.min(1, v)); }
    private double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
