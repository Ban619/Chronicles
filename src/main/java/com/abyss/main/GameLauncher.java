package com.abyss.main;

import com.abyss.world3d.Perspective3DEngine;
import com.abyss.entities.Player;
import com.abyss.skills.SkillTree;
import com.abyss.ui.HUD;
import com.abyss.ui.SkillTreeUI;
import com.abyss.utils.AssetLoader;
import com.abyss.utils.InputHandler;
import com.abyss.world.DayNightCycle;
import com.abyss.world.WeatherSystem;
import com.abyss.world.WorldManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * GameLauncher — Upgraded entry point.
 *
 * Wires:
 *   WorldManager  — multi-region map graph (village → forest → dungeon → boss)
 *   SkillTree     — 3-branch, 45-node skill graph bound to player
 *   SkillTreeUI   — interactive overlay (K key)
 *   LightingEngine— instantiated inside GameLoop, receives map light sources
 */
public class GameLauncher extends Application {

    public static final String TITLE  = "Chronicles of the Abyss";
    public static final int    BASE_W = 960;
    public static final int    BASE_H = 640;

    @Override
    public void start(Stage stage) {
        // ── Window ────────────────────────────────────────────────────────
        Canvas    canvas = new Canvas(BASE_W, BASE_H);
        Perspective3DEngine perspective3D = new Perspective3DEngine();
        StackPane root   = new StackPane(perspective3D.getView(), canvas);
        root.setStyle("-fx-background-color: #050508;");
        Scene scene = new Scene(root, BASE_W, BASE_H, Color.web("#050508"));

        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(480);
        stage.setMinHeight(320);
        stage.show();

        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());
        perspective3D.bindSize(root.widthProperty(), root.heightProperty());

        // ── Assets ────────────────────────────────────────────────────────
        AssetLoader assets = AssetLoader.getInstance();
        assets.loadAll();

        // ── World (multi-region, starts in Ashenveil village) ─────────────
        WorldManager world = new WorldManager(assets, "maps/world_map.json");

        // ── Player ────────────────────────────────────────────────────────
        Player player = new Player("Hero",
                world.getActiveMap().getStartX(),
                world.getActiveMap().getStartY(), assets);

        // ── Skill tree ────────────────────────────────────────────────────
        SkillTree    skillTree    = new SkillTree();
        skillTree.bindPlayer(player);
        player.setSkillTree(skillTree);
        skillTree.addSkillPoints(3); // give 3 starter points so it's immediately interactive

        SkillTreeUI  skillTreeUI  = new SkillTreeUI(player, skillTree);

        // ── Input ─────────────────────────────────────────────────────────
        InputHandler input = new InputHandler(scene, canvas);

        // ── Subsystems ────────────────────────────────────────────────────
        DayNightCycle dayNight = new DayNightCycle();
        WeatherSystem weather  = new WeatherSystem();

        // ── HUD ───────────────────────────────────────────────────────────
        HUD hud = new HUD(player, dayNight, weather);

        // ── Game loop ─────────────────────────────────────────────────────
        GraphicsContext gc   = canvas.getGraphicsContext2D();
        GameLoop        loop = new GameLoop(gc, input, world, player, hud,
                                            dayNight, weather, skillTreeUI, perspective3D);
        loop.start();

        stage.setOnCloseRequest(e -> { loop.stop(); System.exit(0); });
    }

    public static void main(String[] args) { launch(args); }
}
