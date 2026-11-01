package com.abyss.main;

import com.abyss.combat.CombatManager;
import com.abyss.entities.Player;
import com.abyss.lighting.LightingEngine;
import com.abyss.ui.HUD;
import com.abyss.ui.InventoryUI;
import com.abyss.ui.MerchantShopUI;
import com.abyss.ui.TutorialUI;
import com.abyss.ui.ChapterEndingUI;
import com.abyss.ui.SettingsMenuUI;
import com.abyss.ui.SkillTreeUI;
import com.abyss.utils.AssetLoader;
import com.abyss.utils.DialogueHandler;
import com.abyss.utils.InputHandler;
import com.abyss.utils.SaveLoadManager;
import com.abyss.world.*;
import com.abyss.world3d.Perspective3DEngine;
import com.abyss.world3d.ModelStudio;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * GameLoop — Enhanced with:
 *   WorldManager  — multi-region map transitions replace single GameMap
 *   LightingEngine— dynamic point-light shadow mask render pass
 *   SkillTreeUI   — skill tree overlay (K key)
 *
 * Render order (back to front):
 *   1. Clear (deep void black)
 *   2. World-space: ground → objects → entities (Y-sorted) → overlay
 *   3. Lighting mask (MULTIPLY blend — darkness with light holes)
 *   4. Day/Night tint overlay
 *   5. Weather particles
 *   6. WorldManager fade + location banner
 *   7. HUD
 *   8. World-space melee effects and the skill-tree overlay
 *   9. Portal loading screen
 */
public class GameLoop extends AnimationTimer {

    private static final long NS_PER_UPDATE = 1_000_000_000L / 60;

    private final GraphicsContext gc;
    private final InputHandler    input;
    private final WorldManager    world;       // replaces single GameMap
    private final Player          player;
    private final HUD             hud;
    private final DayNightCycle   dayNight;
    private final WeatherSystem   weather;
    private final Camera          camera;
    private final CombatManager   combat;
    private final LightingEngine  lighting;
    private final SkillTreeUI     skillTreeUI;
    private final InventoryUI     inventoryUI = new InventoryUI();
    private final MerchantShopUI merchantShop = new MerchantShopUI();
    private final TutorialUI tutorial;
    private final ChapterEndingUI chapterEnding = new ChapterEndingUI();
    private final SettingsMenuUI  settingsUI = new SettingsMenuUI();
    private final Perspective3DEngine perspective3D;
    private final DialogueHandler dialogueHandler = new DialogueHandler();
    private final SaveLoadManager saveLoad = new SaveLoadManager();
    private boolean threeDimensionalView;

    private long previousTime = 0;
    private long lag           = 0;
    private int  frames = 0, fps = 0;
    private long fpsTimer = 0;

    // ─────────────────────────────────────────────────────────────────────
    public GameLoop(GraphicsContext gc, InputHandler input, WorldManager world,
                    Player player, HUD hud, DayNightCycle dayNight,
                    WeatherSystem weather, SkillTreeUI skillTreeUI,
                    Perspective3DEngine perspective3D) {
        this.gc          = gc;
        this.input       = input;
        this.world       = world;
        this.player      = player;
        this.tutorial    = new TutorialUI(player);
        this.hud         = hud;
        this.dayNight    = dayNight;
        this.weather     = weather;
        this.skillTreeUI = skillTreeUI;
        this.perspective3D = perspective3D;
        this.threeDimensionalView = settingsUI.isCloseUpPerspective();
        this.perspective3D.setEnabled(threeDimensionalView);
        this.input.setFirstPersonLookEnabled(threeDimensionalView);
        this.camera      = new Camera(player,
                               (int) gc.getCanvas().getWidth(),
                               (int) gc.getCanvas().getHeight());
        this.combat      = new CombatManager();
        this.lighting    = new LightingEngine();
    }

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void handle(long now) {
        if (previousTime == 0) { previousTime = now; return; }
        long elapsed = now - previousTime;
        previousTime = now;
        lag += elapsed;

        while (lag >= NS_PER_UPDATE) {
            update(NS_PER_UPDATE / 1_000_000_000.0);
            lag -= NS_PER_UPDATE;
        }

        frames++;
        fpsTimer += elapsed;
        if (fpsTimer >= 1_000_000_000L) { fps = frames; frames = 0; fpsTimer = 0; }

        double w = gc.getCanvas().getWidth();
        double h = gc.getCanvas().getHeight();
        camera.resize((int) w, (int) h);
        render(w, h);
    }

    // ─────────────────────────────────────────────────────────────────────
    private void update(double dt) {
        if (tutorial.isVisible()) {
            tutorial.handleInput(input);
            player.stopMoving();
            input.endFrame();
            return;
        }
        if (chapterEnding.isOpen()) {
            chapterEnding.handleInput(input);
            player.stopMoving();
            input.endFrame();
            return;
        }
        boolean wasTransitioning = world.isTransitioning();
        if (!skillTreeUI.isVisible() && input.isJustPressed("escape")) {
            if (settingsUI.isOpen()) {
                settingsUI.close();
                input.setFirstPersonLookEnabled(threeDimensionalView);
            } else if (inventoryUI.isOpen()) {
                inventoryUI.close();
                AssetLoader.getInstance().playSfx("audio/ui_click.mp3");
            } else if (merchantShop.isOpen()) {
                merchantShop.close();
            } else if (!world.isTransitioning()) {
                settingsUI.open();
                input.setFirstPersonLookEnabled(false);
            }
        }
        if (input.isJustPressed("p") && !settingsUI.isOpen() && !inventoryUI.isOpen()
                && !merchantShop.isOpen()
                && !skillTreeUI.isVisible() && !world.isTransitioning()) {
            settingsUI.open();
            input.setFirstPersonLookEnabled(false);
        }
        if (input.isJustPressed("i") && !skillTreeUI.isVisible()
                && !settingsUI.isOpen() && !merchantShop.isOpen() && !world.isTransitioning()) {
            inventoryUI.toggle();
            AssetLoader.getInstance().playSfx("audio/ui_click.mp3");
        }

        boolean oldView = settingsUI.isCloseUpPerspective();
        settingsUI.handleInput(input);
        if (oldView != settingsUI.isCloseUpPerspective()) {
            applyViewMode(settingsUI.isCloseUpPerspective(), true);
        }

        boolean overlayOpen = inventoryUI.isOpen() || skillTreeUI.isVisible()
                || settingsUI.isOpen() || merchantShop.isOpen() || chapterEnding.isOpen();
        boolean inputBlocked = overlayOpen || world.isTransitioning();
        if (input.isJustPressed("v") && !inputBlocked) {
            settingsUI.setCloseUpPerspective(!threeDimensionalView);
            applyViewMode(!threeDimensionalView, true);
            AssetLoader.getInstance().playSfx("audio/ui_click.mp3");
        }

        if (input.isJustPressed("m") && !inputBlocked) {
            ModelStudio.open(perspective3D);
        }

        if (input.isJustPressed("f5") && !inputBlocked) {
            boolean saved = saveLoad.save(1, player, world.getActiveMap().getMapName(),
                    world.getActivePath(), world);
            hud.showNotification(saved ? "Game saved" : "Save failed", Color.web("#c8a96e"));
        }
        if (input.isJustPressed("f9") && !inputBlocked) {
            boolean loaded = saveLoad.load(1, player, world);
            hud.showNotification(loaded ? "Game loaded" : "No valid save found", Color.web("#c8a96e"));
        }

        if (threeDimensionalView) {
            double mouseDeltaX = input.consumeMouseDeltaX();
            double mouseDeltaY = input.consumeMouseDeltaY();
            if (!inputBlocked) perspective3D.updateLook(mouseDeltaX, mouseDeltaY);
        }

        if (input.isJustPressed("k") && !inventoryUI.isOpen() && !settingsUI.isOpen()
                && !merchantShop.isOpen()
                && !world.isTransitioning()) {
            skillTreeUI.toggle();
            AssetLoader.getInstance().playSfx("audio/ui_click.mp3");
        }
        skillTreeUI.handleInput(input);

        GameMap map = world.getActiveMap();
        if (merchantShop.isOpen()) {
            String message = merchantShop.handleInput(input, player);
            if (!message.isBlank()) hud.showNotification(message, Color.web("#c8a96e"));
        }
        if (inventoryUI.isOpen()) {
            String message = inventoryUI.handleInput(input, player, map);
            if (!message.isBlank()) hud.showNotification(message, Color.web("#c8a96e"));
        }

        boolean modalOpen = skillTreeUI.isVisible() || inventoryUI.isOpen()
                || settingsUI.isOpen() || merchantShop.isOpen() || chapterEnding.isOpen();
        // Block gameplay while a modal screen or portal transition is active.
        // Keep the whole frame blocked when the transition ends mid-update too.
        boolean uiOpen = modalOpen || wasTransitioning || world.isTransitioning();
        if (uiOpen) player.stopMoving();

        camera.update();

        if (!uiOpen && player.isAlive()) {
            if (threeDimensionalView) player.handleFirstPersonInput(input, dt, perspective3D.getYawRadians());
            else player.handleInput(input, dt);
            player.update(dt, world.getActiveMap());
        }

        // Detect portal entry immediately after movement so combat cannot run
        // for another frame once the player reaches the trigger.
        if (!modalOpen || world.isTransitioning()) world.update(dt, player);
        if (world.isTransitioning()) {
            player.stopMoving();
            uiOpen = true;
        }
        map = world.getActiveMap();

        if (input.isJustPressed("e") && !uiOpen) {
            if (map.isNearMerchant(player)) {
                merchantShop.open();
            } else {
                var previousChapterStage = player.getChapterOne().getStage();
                hud.showNotification(map.interact(player, dialogueHandler), Color.web("#c8a96e"));
                if (previousChapterStage
                        != com.abyss.campaign.ChapterOneProgress.Stage.COMPLETE
                        && player.getChapterOne().getStage()
                        == com.abyss.campaign.ChapterOneProgress.Stage.COMPLETE) {
                    chapterEnding.open();
                }
            }
        }
        if (!uiOpen) map.updateNPCs(dt, player, dayNight);
        dayNight.update(dt);
        weather.update(dt, dayNight);

        if (!uiOpen) combat.update(dt, input, player, map);

        if (!uiOpen) map.checkEvents(player);

        player.updateAnimations(dt);
        map.updateParticles(dt);

        // Keep player carry-light in sync (always index 0)
        GameMap activeMap = world.getActiveMap();
        var lights = activeMap.getLightSources();
        if (!lights.isEmpty()) {
            lights.get(0).moveTo(player.getX() + 16, player.getY() + 16);
        }

        // Force dungeon/boss darkness
        boolean dark = activeMap.getBiome() == GameMap.Biome.DUNGEON ||
                       activeMap.getBiome() == GameMap.Biome.BOSS_ARENA;
        lighting.setForceDark(dark);

        input.endFrame();
    }

    // ─────────────────────────────────────────────────────────────────────
    private void render(double w, double h) {
        GameMap map = world.getActiveMap();
        if (threeDimensionalView) {
            gc.clearRect(0, 0, w, h);
            perspective3D.render(map, player, dayNight);
            world.renderOverlays(gc, w, h);
        } else {
            gc.setFill(Color.web("#050508"));
            gc.fillRect(0, 0, w, h);

            gc.save();
            gc.translate(-camera.getOffsetX(), -camera.getOffsetY());
            map.renderGroundLayer(gc, camera);
            map.renderObjectLayer(gc, camera);
            map.renderEntities(gc, camera, player);
            combat.render(gc, player);
            map.renderOverlayLayer(gc, camera);
            gc.restore();

            lighting.render(gc, w, h, map.getLightSources(), camera, dayNight);
            dayNight.renderOverlay(gc, w, h);
            weather.render(gc, w, h, camera);
            world.renderOverlays(gc, w, h);
        }

        hud.setLocation(map.getMapName());
        hud.render(gc, w, h, fps);
        inventoryUI.render(gc, w, h, player);
        skillTreeUI.render(gc, w, h);
        settingsUI.render(gc, w, h);
        merchantShop.render(gc, w, h, player);
        world.renderPortalLoading(gc, w, h);
        tutorial.render(gc, w, h);
        chapterEnding.render(gc, w, h);
    }

    private void applyViewMode(boolean closeUpPerspective, boolean notify) {
        threeDimensionalView = closeUpPerspective;
        perspective3D.setEnabled(closeUpPerspective);
        input.setFirstPersonLookEnabled(closeUpPerspective && !settingsUI.isOpen());
        if (notify) {
            hud.showNotification(closeUpPerspective ? "Close-up perspective" : "2D top-down view",
                    Color.web("#c8a96e"));
        }
    }

}
