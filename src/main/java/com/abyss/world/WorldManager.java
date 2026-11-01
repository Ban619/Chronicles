package com.abyss.world;

import com.abyss.entities.Player;
import com.abyss.items.Item;
import com.abyss.utils.AssetLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * WorldManager — Owns the map registry, manages active map, and orchestrates
 * region transitions with a full-screen fade effect.
 *
 * Region graph (hard-coded; could be JSON-driven):
 *
 *   ┌──────────────────┐     EAST      ┌──────────────────┐
 *   │   Ashenveil      │ ───────────▶  │  Thornwood Forest │
 *   │   (village hub)  │ ◀───────────  │  (combat zone)   │
 *   └────────┬─────────┘     WEST      └────────┬─────────┘
 *            │ SOUTH                            │ SOUTH
 *            ▼                                  ▼
 *   ┌──────────────────┐              ┌──────────────────┐
 *   │  Crypt of Echoes │              │  Dragon's Maw    │
 *   │  (dungeon L1)    │              │  (boss arena)    │
 *   └──────────────────┘              └──────────────────┘
 *
 * Transition sequence:
 *  1. Player enters trigger zone
 *  2. Fade out (0→1 over 0.4 s)
 *  3. Show portal loading screen
 *  4. Load the destination and place the player at its spawn
 *  5. Fade in (1→0 over 0.4 s)
 *  6. Show location banner for 3 s
 */
public class WorldManager {

    private static final Logger LOG = Logger.getLogger(WorldManager.class.getName());

    // ── Fade ──────────────────────────────────────────────────────────────
    private enum FadeState { NONE, FADE_OUT, PORTAL_LOADING, FADE_IN }
    private FadeState fadeState  = FadeState.NONE;
    private double    fadeAlpha  = 0.0;
    private static final double FADE_SPEED = 2.5; // alpha units per second
    private static final double PORTAL_LOADING_SECONDS = 0.65;
    private double portalLoadingTimer = 0.0;

    // ── Pending transition (stored during fade-out) ───────────────────────
    private MapTransition pendingTransition = null;

    // ── Map registry ──────────────────────────────────────────────────────
    private final Map<String, GameMap> cache = new HashMap<>();
    private final Map<String, Object> deferredMapStates = new HashMap<>();
    private GameMap activeMap;
    private String  activeMapPath;

    // ── Location banner ───────────────────────────────────────────────────
    private String bannerText  = "";
    private double bannerTimer = 0;

    // ── Dependencies ──────────────────────────────────────────────────────
    private final AssetLoader assetLoader;

    // ─────────────────────────────────────────────────────────────────────
    public WorldManager(AssetLoader loader, String startMapPath) {
        this.assetLoader   = loader;
        this.activeMapPath = startMapPath;
        this.activeMap     = loadOrGet(startMapPath);
        registerAllMaps();
    }

    // ── Map registry ──────────────────────────────────────────────────────
    /**
     * Pre-registers all map paths so WorldManager knows the full world graph.
     * Maps are lazy-loaded on first visit to save memory.
     */
    private void registerAllMaps() {
        // Maps are created on demand — registry just tracks known paths
        LOG.info("[WorldManager] Registered world regions.");
    }

    private GameMap loadOrGet(String path) {
        return cache.computeIfAbsent(path, p -> {
            LOG.info("[WorldManager] Loading map: " + p);
            GameMap map = new GameMap(p, assetLoader);
            Object savedState = deferredMapStates.get(p);
            if (savedState instanceof Map<?, ?> state) map.restorePersistentState(state);
            return map;
        });
    }

    // ── Update ────────────────────────────────────────────────────────────
    public void update(double dt, Player player) {
        // Update transition cooldowns on active map
        activeMap.updateTransitions(dt);
        activeMap.ensureRelicRecoverable(player);

        // Banner countdown
        if (bannerTimer > 0) bannerTimer -= dt;

        // Check transition triggers
        if (fadeState == FadeState.NONE) {
            MapTransition hit = activeMap.checkTransitionTrigger(player);
            if (hit != null) {
                if ("maps/crypt_of_echoes.json".equals(hit.getTargetMapPath())
                        && !player.getChapterOne().canEnterCrypt()) {
                    activeMap.spawnParticle(player.getX(), player.getY() - 20,
                            "Speak with Elder Aldric before entering the Crypt.",
                            Color.web("#c8a96e"));
                    hit.setCooldown(1.0);
                    return;
                }
                if ("maps/dragons_maw.json".equals(hit.getTargetMapPath())
                        && !player.getChapterOne().canEnterBossArena()) {
                    activeMap.spawnParticle(player.getX(), player.getY() - 20,
                            "The Dawn Seal is needed to enter Dragon's Maw.",
                            Color.web("#c0392b"));
                    hit.setCooldown(1.0);
                    return;
                }
                // Gate check
                if (hit.getRequiresKey() != null && !playerHasItem(player, hit.getRequiresKey())) {
                    activeMap.spawnParticle(player.getX(), player.getY() - 20,
                            "Locked! Need: " + hit.getRequiresKey(),
                            Color.web("#c0392b"));
                    hit.setCooldown(1.0);
                    return;
                }
                pendingTransition = hit;
                fadeState = FadeState.FADE_OUT;
                fadeAlpha = 0.0;
                portalLoadingTimer = 0.0;
            }
        }

        // Fade state machine
        switch (fadeState) {
            case FADE_OUT -> {
                fadeAlpha += FADE_SPEED * dt;
                if (fadeAlpha >= 1.0) {
                    fadeAlpha = 1.0;
                    fadeState = FadeState.PORTAL_LOADING;
                }
            }
            case PORTAL_LOADING -> {
                portalLoadingTimer += dt;
                if (portalLoadingTimer >= PORTAL_LOADING_SECONDS) {
                    executeTransition(player);
                    fadeState = FadeState.FADE_IN;
                }
            }
            case FADE_IN -> {
                fadeAlpha -= FADE_SPEED * dt;
                if (fadeAlpha <= 0.0) {
                    fadeAlpha = 0.0;
                    fadeState = FadeState.NONE;
                    pendingTransition = null;
                }
            }
            case NONE -> {}
        }
    }

    // ── Transition execution ──────────────────────────────────────────────
    private void executeTransition(Player player) {
        if (pendingTransition == null) return;

        // Switch active map
        String targetPath = pendingTransition.getTargetMapPath();
        activeMap     = loadOrGet(targetPath);
        activeMapPath = targetPath;

        // Place player
        float spawnX = pendingTransition.getTargetSpawnX() * Tile.SIZE;
        float spawnY = pendingTransition.getTargetSpawnY() * Tile.SIZE;
        player.setX(spawnX);
        player.setY(spawnY);
        switch (pendingTransition.getDirection()) {
            case NORTH -> player.faceVector(0, -1);
            case SOUTH -> player.faceVector(0, 1);
            case EAST  -> player.faceVector(1, 0);
            case WEST  -> player.faceVector(-1, 0);
        }

        // Reset cooldown on all transitions of the new map so we don't
        // immediately re-trigger the exit we just came through
        activeMap.resetAllTransitionCooldowns();

        // Banner
        bannerText  = activeMap.getMapName();
        bannerTimer = 3.5;

        LOG.info("[WorldManager] Transitioned to: " + activeMap.getMapName()
                 + " @ (" + spawnX + "," + spawnY + ")");
    }

    public boolean canLoadSavedLocation(String mapPath) {
        return mapPath != null && assetLoader.mapExists(mapPath);
    }

    public boolean loadSavedLocation(String mapPath, float x, float y, Player player) {
        if (!canLoadSavedLocation(mapPath)) {
            LOG.warning("[WorldManager] Saved map does not exist: " + mapPath);
            return false;
        }
        activeMap = loadOrGet(mapPath);
        activeMapPath = mapPath;
        player.setX(Math.max(0, Math.min(x, activeMap.getWidthPx() - player.getWidth())));
        player.setY(Math.max(0, Math.min(y, activeMap.getHeightPx() - player.getHeight())));
        activeMap.resetAllTransitionCooldowns();
        bannerText = activeMap.getMapName();
        bannerTimer = 3.5;
        return true;
    }

    public Map<String, Object> getPersistentMapStates() {
        Map<String, Object> states = new LinkedHashMap<>();
        cache.forEach((path, map) -> states.put(path, map.toPersistentState()));
        return states;
    }

    public void restorePersistentMapStates(Map<?, ?> states) {
        deferredMapStates.clear();
        if (states == null) return;
        states.forEach((path, state) -> {
            if (path instanceof String mapPath && assetLoader.mapExists(mapPath)
                    && state instanceof Map<?, ?> mapState) {
                deferredMapStates.put(mapPath, new LinkedHashMap<>(mapState));
            }
        });
        cache.forEach((path, map) -> {
            Object state = deferredMapStates.get(path);
            if (state instanceof Map<?, ?> mapState) map.restorePersistentState(mapState);
            else map.resetPersistentState();
        });
    }

    // ── Render ────────────────────────────────────────────────────────────
    /** Renders the fade overlay and location banner after the world. */
    public void renderOverlays(GraphicsContext gc, double w, double h) {
        // Fade
        if (fadeAlpha > 0) {
            gc.setFill(Color.color(0, 0, 0, Math.min(1, fadeAlpha)));
            gc.fillRect(0, 0, w, h);
        }

        // Location banner
        if (bannerTimer > 0 && fadeState == FadeState.NONE) {
            double alpha = Math.min(1.0, bannerTimer) * Math.min(1.0, (3.5 - bannerTimer) * 2);
            gc.setGlobalAlpha(alpha);
            gc.setFill(Color.color(0, 0, 0, 0.65));
            double bw = bannerText.length() * 14 + 60;
            gc.fillRoundRect(w / 2 - bw / 2, h / 2 - 28, bw, 52, 8, 8);
            gc.setFill(Color.web("#c8a96e"));
            gc.setFont(javafx.scene.text.Font.font("Serif",
                    javafx.scene.text.FontWeight.BOLD, 22));
            gc.fillText(bannerText, w / 2 - bw / 2 + 18, h / 2 + 8);
            gc.setGlobalAlpha(1.0);
        }
    }

    /** Renders the portal-specific loading screen above the HUD and menus. */
    public void renderPortalLoading(GraphicsContext gc, double w, double h) {
        if (fadeState != FadeState.FADE_OUT && fadeState != FadeState.PORTAL_LOADING
                && fadeState != FadeState.FADE_IN) return;

        double overlayAlpha;
        double progress;
        if (fadeState == FadeState.PORTAL_LOADING) {
            overlayAlpha = 1.0;
            progress = 0.2 + 0.8 * Math.min(1.0, portalLoadingTimer / PORTAL_LOADING_SECONDS);
        } else if (fadeState == FadeState.FADE_IN) {
            overlayAlpha = Math.min(1.0, fadeAlpha);
            progress = 1.0;
        } else {
            overlayAlpha = Math.max(0.0, Math.min(1.0, (fadeAlpha - 0.72) / 0.28));
            progress = 0.2 * Math.min(1.0, fadeAlpha / 0.72);
        }

        gc.save();
        gc.setGlobalAlpha(overlayAlpha);
        gc.setFill(Color.web("#080a10"));
        gc.fillRect(0, 0, w, h);

        double centerX = w / 2.0;
        double centerY = h / 2.0;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#c8a96e"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 27));
        gc.fillText("CROSSING PORTAL", centerX, centerY - 40);

        gc.setFill(Color.web("#d7d3cb"));
        gc.setFont(Font.font("Serif", 17));
        String destination = pendingTransition == null ? "Unknown destination" : pendingTransition.getLabel();
        gc.fillText(destination, centerX, centerY - 8);

        double barWidth = Math.min(360, w * 0.62);
        double barX = centerX - barWidth / 2.0;
        double barY = centerY + 22;
        gc.setFill(Color.web("#272a31"));
        gc.fillRoundRect(barX, barY, barWidth, 8, 8, 8);
        gc.setFill(Color.web("#c8a96e"));
        gc.fillRoundRect(barX, barY, barWidth * progress, 8, 8, 8);
        gc.setFill(Color.web("#99958d"));
        gc.setFont(Font.font("Serif", 12));
        gc.fillText(fadeState == FadeState.FADE_IN ? "Destination ready" : "Preparing destination...",
                centerX, barY + 28);
        gc.restore();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private boolean playerHasItem(Player player, String itemId) {
        return player.hasItem(itemId);
    }

    public boolean isTransitioning() { return fadeState != FadeState.NONE; }

    // ── Accessors ─────────────────────────────────────────────────────────
    public GameMap getActiveMap()    { return activeMap; }
    public String  getActivePath()   { return activeMapPath; }
}
