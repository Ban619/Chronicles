package com.abyss.world;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Tile — One cell in the world grid.
 *
 * Properties:
 *  id          – tile type identifier (links to tileset sprite index)
 *  walkable    – can entities walk through this tile?
 *  interactive – can the player interact (E) with this tile? (doors, chests, signs)
 *  animated    – does this tile animate? (water, lava, torches)
 *  frameCount  – number of animation frames in the tileset row
 *  light       – base light emission level (0.0 – 1.0)
 *
 * Sprite lookup:
 *  The tileset image is sliced into 32×32 cells.
 *  srcX = (id % tilesPerRow) * 32;  srcY = (id / tilesPerRow) * 32;
 */
public class Tile {

    // ── Constants ─────────────────────────────────────────────────────────
    public static final int SIZE = 32; // pixels

    private static final Color TILE_EDGE = Color.rgb(12, 17, 22, 0.28);
    private static final Color GRASS_LIGHT = Color.web("#79a94a");
    private static final Color GRASS_DARK = Color.web("#244d25");
    private static final Color SOIL_LIGHT = Color.web("#94704a");
    private static final Color SOIL_DARK = Color.web("#493421");
    private static final Color WATER_LIGHT = Color.web("#65b8c8");
    private static final Color WATER_DARK = Color.web("#153958");
    private static final Color STONE_LIGHT = Color.web("#a2a6a1");
    private static final Color STONE_DARK = Color.web("#353d43");
    private static final Color WOOD_LIGHT = Color.web("#bd8a4e");
    private static final Color WOOD_DARK = Color.web("#543922");

    // ── Well-known tile IDs ───────────────────────────────────────────────
    public static final int GRASS       = 0;
    public static final int DIRT        = 1;
    public static final int STONE       = 2;
    public static final int WATER       = 3;
    public static final int LAVA        = 4;
    public static final int SAND        = 5;
    public static final int SNOW        = 6;
    public static final int WALL_STONE  = 10;
    public static final int WALL_BRICK  = 11;
    public static final int FLOOR_WOOD  = 12;
    public static final int FLOOR_STONE = 13;
    public static final int DOOR_CLOSED = 20;
    public static final int DOOR_OPEN   = 21;
    public static final int CHEST       = 30;
    public static final int CHEST_OPEN  = 31;
    public static final int SIGN        = 32;
    public static final int TORCH       = 40;
    public static final int TREE_BASE   = 50;
    public static final int TREE_TOP    = 51;
    public static final int BRIDGE      = 60;

    // ── Fields ────────────────────────────────────────────────────────────
    private int     id;
    private boolean walkable;
    private boolean interactive;
    private boolean animated;
    private int     frameCount;
    private float   lightEmission;  // 0 = none, 1 = full torch
    private String  interactionLabel; // "Open", "Read", "Enter"

    // ── Animation state ───────────────────────────────────────────────────
    private int    currentFrame = 0;
    private double animTimer    = 0;
    private static final double ANIM_SPEED = 0.25; // seconds per frame

    // ─────────────────────────────────────────────────────────────────────
    public Tile(int id) {
        this.id = id;
        applyDefaults(id);
    }

    public Tile(int id, boolean walkable, boolean interactive,
                boolean animated, int frameCount, float lightEmission,
                String interactionLabel) {
        this.id               = id;
        this.walkable         = walkable;
        this.interactive      = interactive;
        this.animated         = animated;
        this.frameCount       = Math.max(1, frameCount);
        this.lightEmission    = lightEmission;
        this.interactionLabel = interactionLabel;
    }

    /** Apply sensible defaults based on tile ID. */
    private void applyDefaults(int id) {
        walkable         = true;
        interactive      = false;
        animated         = false;
        frameCount       = 1;
        lightEmission    = 0f;
        interactionLabel = null;

        if (id == WATER || id == LAVA) { walkable = false; animated = true; frameCount = 4; }
        if (id >= WALL_STONE && id <= WALL_BRICK) walkable = false;
        if (id == DOOR_CLOSED) { walkable = false; interactive = true; interactionLabel = "Open"; }
        if (id == DOOR_OPEN)   { walkable = true;  interactive = true; interactionLabel = "Close"; }
        if (id == CHEST)       { walkable = false; interactive = true; interactionLabel = "Open"; }
        if (id == SIGN)        { walkable = true;  interactive = true; interactionLabel = "Read"; }
        if (id == TORCH)       { walkable = true;  lightEmission = 0.9f; animated = true; frameCount = 3; }
        if (id == TREE_BASE)   { walkable = false; }
    }

    // ── Animate ───────────────────────────────────────────────────────────
    public void update(double dt) {
        if (!animated || frameCount <= 1) return;
        animTimer += dt;
        if (animTimer >= ANIM_SPEED) {
            animTimer = 0;
            currentFrame = (currentFrame + 1) % frameCount;
        }
    }

    /** Render a single tile cell from the tileset. */
    public void render(GraphicsContext gc, Image tileset, int pixelX, int pixelY, int tilesPerRow) {
        if (tileset == null) {
            renderFallback(gc, pixelX, pixelY);
            return;
        }
        int col  = (id + currentFrame) % tilesPerRow;
        int row  = (id + currentFrame) / tilesPerRow;
        gc.drawImage(tileset, col * SIZE, row * SIZE, SIZE, SIZE,
                     pixelX, pixelY, SIZE, SIZE);
    }

    private void renderFallback(GraphicsContext gc, int x, int y) {
        int variation = Math.floorMod(x * 31 + y * 17 + id * 13, 11);
        Color base = fallbackColor();
        gc.setFill(base);
        gc.fillRect(x, y, SIZE, SIZE);

        switch (id) {
            case GRASS -> renderGrass(gc, x, y, variation);
            case DIRT, SAND, SNOW -> renderLooseGround(gc, x, y, variation);
            case STONE, FLOOR_STONE -> renderStone(gc, x, y, variation);
            case WATER -> renderWater(gc, x, y, variation);
            case LAVA -> renderLava(gc, x, y, variation);
            case WALL_STONE, WALL_BRICK -> renderWall(gc, x, y, variation);
            case FLOOR_WOOD -> renderWoodFloor(gc, x, y, variation);
            case DOOR_CLOSED, DOOR_OPEN -> renderDoor(gc, x, y);
            case CHEST, CHEST_OPEN -> renderChest(gc, x, y);
            case SIGN -> renderSign(gc, x, y);
            case TORCH -> renderTorch(gc, x, y);
            case TREE_BASE -> renderTreeTrunk(gc, x, y);
            case TREE_TOP -> renderTreeCanopy(gc, x, y, variation);
            case BRIDGE -> renderBridge(gc, x, y, variation);
            default -> renderStone(gc, x, y, variation);
        }

        gc.setStroke(TILE_EDGE);
        gc.setLineWidth(1);
        gc.strokeRect(x + 0.5, y + 0.5, SIZE - 1, SIZE - 1);
    }

    private void renderGrass(GraphicsContext gc, int x, int y, int variation) {
        gc.setFill(GRASS_DARK);
        gc.fillOval(x + 5 + variation, y + 20, 2, 2);
        gc.fillOval(x + 22 - variation / 2.0, y + 7 + variation % 4, 2, 2);
        gc.setStroke(GRASS_LIGHT);
        gc.setLineWidth(1);
        gc.strokeLine(x + 12, y + 24, x + 11 + variation % 3, y + 20);
        gc.strokeLine(x + 13, y + 24, x + 15, y + 21);
        gc.strokeLine(x + 25, y + 15, x + 24, y + 12);
    }

    private void renderLooseGround(GraphicsContext gc, int x, int y, int variation) {
        Color fleck = id == SNOW ? Color.WHITE : id == SAND ? Color.web("#e4d08a") : SOIL_LIGHT;
        Color shade = id == SNOW ? Color.web("#a9c3d2") : id == SAND ? Color.web("#9a854d") : SOIL_DARK;
        gc.setFill(fleck);
        gc.fillOval(x + 5 + variation, y + 7, 2, 2);
        gc.fillOval(x + 20 - variation / 2.0, y + 19, 2, 2);
        gc.fillOval(x + 10, y + 25 - variation % 5, 1.5, 1.5);
        gc.setFill(shade);
        gc.fillOval(x + 24, y + 5 + variation % 6, 2, 2);
        gc.fillOval(x + 4 + variation % 4, y + 22, 1.5, 1.5);
    }

    private void renderStone(GraphicsContext gc, int x, int y, int variation) {
        gc.setStroke(STONE_DARK);
        gc.setLineWidth(1);
        gc.strokeLine(x + 4, y + 8 + variation % 5, x + 11, y + 8 + variation % 5);
        gc.strokeLine(x + 19, y + 22, x + 26, y + 22);
        gc.setStroke(STONE_LIGHT);
        gc.strokeLine(x + 5, y + 9 + variation % 5, x + 10, y + 9 + variation % 5);
        gc.strokeLine(x + 20, y + 23, x + 25, y + 23);
        gc.setStroke(TILE_EDGE);
        gc.strokeRect(x + 1.5, y + 1.5, SIZE - 3, SIZE - 3);
    }

    private void renderWater(GraphicsContext gc, int x, int y, int variation) {
        int phase = (int) (System.currentTimeMillis() / 350L);
        double shift = Math.floorMod(phase + variation, 8);
        gc.setStroke(WATER_DARK);
        gc.setLineWidth(2);
        gc.strokeLine(x + 3, y + 9 + shift, x + 12, y + 9 + shift);
        gc.strokeLine(x + 18, y + 21 - shift / 2, x + 28, y + 21 - shift / 2);
        gc.setStroke(WATER_LIGHT);
        gc.setLineWidth(1);
        gc.strokeLine(x + 5, y + 8 + shift, x + 13, y + 8 + shift);
        gc.strokeLine(x + 17, y + 20 - shift / 2, x + 25, y + 20 - shift / 2);
    }

    private void renderLava(GraphicsContext gc, int x, int y, int variation) {
        int pulse = (int) (System.currentTimeMillis() / 450L + variation) % 3;
        gc.setStroke(Color.web("#ffb347"));
        gc.setLineWidth(2);
        gc.strokeLine(x + 5, y + 5 + pulse, x + 12, y + 13);
        gc.strokeLine(x + 12, y + 13, x + 9, y + 21 - pulse);
        gc.strokeLine(x + 22, y + 4, x + 19, y + 10 + pulse);
        gc.setFill(Color.web("#fff0a1"));
        gc.fillOval(x + 21 + variation % 4, y + 22, 2, 2);
    }

    private void renderWall(GraphicsContext gc, int x, int y, int variation) {
        Color mortar = id == WALL_BRICK ? WOOD_DARK : STONE_DARK;
        Color highlight = id == WALL_BRICK ? Color.web("#ad7652") : STONE_LIGHT;
        gc.setStroke(mortar);
        gc.setLineWidth(1);
        int offset = variation % 8;
        gc.strokeLine(x, y + 11, x + SIZE, y + 11);
        gc.strokeLine(x, y + 22, x + SIZE, y + 22);
        gc.strokeLine(x + 16 - offset, y, x + 16 - offset, y + 11);
        gc.strokeLine(x + 8 + offset, y + 11, x + 8 + offset, y + 22);
        gc.strokeLine(x + 22 - offset, y + 22, x + 22 - offset, y + SIZE);
        gc.setStroke(highlight);
        gc.strokeLine(x + 2, y + 2, x + 13, y + 2);
        gc.strokeLine(x + 18, y + 13, x + 28, y + 13);
    }

    private void renderWoodFloor(GraphicsContext gc, int x, int y, int variation) {
        gc.setStroke(WOOD_DARK);
        gc.setLineWidth(1);
        gc.strokeLine(x, y + 8, x + SIZE, y + 8);
        gc.strokeLine(x, y + 17, x + SIZE, y + 17);
        gc.strokeLine(x, y + 26, x + SIZE, y + 26);
        int seam = 8 + variation;
        gc.strokeLine(x + seam, y, x + seam, y + 8);
        gc.strokeLine(x + 25 - variation, y + 8, x + 25 - variation, y + 17);
        gc.strokeLine(x + seam, y + 17, x + seam, y + 26);
        gc.strokeLine(x + 25 - variation, y + 26, x + 25 - variation, y + SIZE);
        gc.setStroke(WOOD_LIGHT);
        gc.strokeLine(x + 2, y + 3, x + 13, y + 3);
        gc.strokeLine(x + 18, y + 12, x + 27, y + 12);
    }

    private void renderDoor(GraphicsContext gc, int x, int y) {
        gc.setFill(WOOD_DARK);
        gc.fillRect(x + 6, y + 2, 20, 29);
        gc.setFill(id == DOOR_OPEN ? Color.web("#171922") : WOOD_LIGHT);
        gc.fillRect(x + 9, y + 5, 14, 26);
        if (id == DOOR_CLOSED) {
            gc.setStroke(WOOD_DARK);
            gc.strokeLine(x + 9, y + 16, x + 23, y + 16);
            gc.setFill(Color.web("#e4c36a"));
            gc.fillOval(x + 19, y + 18, 2, 2);
        }
        gc.setFill(STONE_LIGHT);
        gc.fillRect(x + 5, y + 1, 22, 3);
    }

    private void renderChest(GraphicsContext gc, int x, int y) {
        gc.setFill(WOOD_DARK);
        gc.fillRoundRect(x + 5, y + 13, 22, 15, 2, 2);
        gc.setFill(id == CHEST_OPEN ? Color.web("#322719") : WOOD_LIGHT);
        gc.fillRect(x + 7, y + 11, 18, 9);
        gc.setStroke(WOOD_DARK);
        gc.strokeLine(x + 7, y + 20, x + 25, y + 20);
        gc.setFill(Color.web("#e4c36a"));
        gc.fillRect(x + 15, y + 17, 3, 5);
        if (id == CHEST_OPEN) {
            gc.setStroke(WOOD_DARK);
            gc.strokeLine(x + 7, y + 11, x + 10, y + 6);
            gc.strokeLine(x + 10, y + 6, x + 24, y + 6);
            gc.strokeLine(x + 24, y + 6, x + 25, y + 11);
        }
    }

    private void renderSign(GraphicsContext gc, int x, int y) {
        gc.setFill(WOOD_DARK);
        gc.fillRect(x + 14, y + 15, 4, 15);
        gc.setFill(WOOD_LIGHT);
        gc.fillRect(x + 4, y + 5, 24, 13);
        gc.setStroke(WOOD_DARK);
        gc.strokeRect(x + 4.5, y + 5.5, 23, 12);
        gc.strokeLine(x + 9, y + 10, x + 23, y + 10);
        gc.strokeLine(x + 9, y + 14, x + 19, y + 14);
    }

    private void renderTorch(GraphicsContext gc, int x, int y) {
        gc.setFill(WOOD_DARK);
        gc.fillRect(x + 14, y + 13, 4, 16);
        gc.setFill(Color.web("#ff8b32"));
        gc.fillOval(x + 10, y + 4, 12, 13);
        gc.setFill(Color.web("#ffe27a"));
        gc.fillOval(x + 13, y + 7 + currentFrame % 2, 6, 8);
    }

    private void renderTreeTrunk(GraphicsContext gc, int x, int y) {
        gc.setFill(Color.color(0, 0, 0, 0.2));
        gc.fillOval(x + 5, y + 26, 23, 5);
        gc.setFill(WOOD_DARK);
        gc.fillPolygon(new double[]{x + 10, x + 13, x + 14, x + 21, x + 23, x + 18, x + 12},
            new double[]{y + 31, y + 25, y + 8, y + 8, y + 29, y + 27, y + 31}, 7);
        gc.setFill(Color.web("#8c623d"));
        gc.fillPolygon(new double[]{x + 14, x + 16, x + 17, x + 18},
            new double[]{y + 26, y + 21, y + 9, y + 24}, 4);

        gc.setStroke(WOOD_DARK);
        gc.setLineWidth(2);
        gc.strokeLine(x + 15, y + 17, x + 8, y + 9);
        gc.strokeLine(x + 18, y + 16, x + 25, y + 7);
        gc.setStroke(Color.web("#a77a4d"));
        gc.setLineWidth(1);
        gc.strokeLine(x + 13, y + 28, x + 11, y + 31);
        gc.strokeLine(x + 18, y + 28, x + 21, y + 31);
        gc.strokeLine(x + 16, y + 18, x + 15, y + 24);
    }

    private void renderTreeCanopy(GraphicsContext gc, int x, int y, int variation) {
        Color shade = Color.web("#1e442c");
        Color middle = Color.web("#32683a");
        Color leaf = Color.web("#467f43");
        gc.setFill(shade);
        gc.fillOval(x + 2, y + 12, 15, 15);
        gc.fillOval(x + 14, y + 9, 16, 17);
        gc.fillOval(x + 8, y + 2, 16, 18);
        gc.fillOval(x + 5, y + 18, 20, 12);
        gc.setFill(middle);
        gc.fillOval(x + 4, y + 10, 13, 14);
        gc.fillOval(x + 14, y + 7, 13, 15);
        gc.fillOval(x + 9, y + 3, 13, 15);
        gc.setFill(leaf);
        gc.fillOval(x + 7 + variation % 4, y + 7, 8, 8);
        gc.fillOval(x + 18, y + 11 + variation % 3, 7, 8);
        gc.fillOval(x + 9, y + 19, 8, 6);
        gc.setFill(Color.web("#92ad59"));
        gc.fillOval(x + 8 + variation % 5, y + 8, 3, 2);
        gc.fillOval(x + 19, y + 10 + variation % 4, 2, 3);
        gc.fillOval(x + 13, y + 20, 3, 2);
    }

    private void renderBridge(GraphicsContext gc, int x, int y, int variation) {
        gc.setFill(WOOD_DARK);
        gc.fillRect(x, y + 2, SIZE, SIZE - 4);
        gc.setFill(WOOD_LIGHT);
        for (int plank = 0; plank < 4; plank++) {
            int plankX = x + 2 + plank * 8;
            gc.fillRect(plankX, y + 4, 6, SIZE - 8);
        }
        gc.setStroke(WOOD_DARK);
        gc.setLineWidth(1);
        gc.strokeLine(x + 2, y + 5 + variation % 2, x + SIZE - 2, y + 5 + variation % 2);
        gc.strokeLine(x + 2, y + SIZE - 6, x + SIZE - 2, y + SIZE - 6);
    }

    private Color fallbackColor() {
        return switch (id) {
            case GRASS -> Color.web("#365e32");
            case DIRT -> Color.web("#765536");
            case STONE, FLOOR_STONE -> Color.web("#626c70");
            case WATER -> Color.web("#245d78");
            case LAVA -> Color.web("#a6321d");
            case SAND -> Color.web("#c5ad68");
            case SNOW -> Color.web("#dce8e7");
            case WALL_STONE -> Color.web("#535d61");
            case WALL_BRICK -> Color.web("#815743");
            case FLOOR_WOOD, BRIDGE -> Color.web("#86603c");
            case DOOR_CLOSED, DOOR_OPEN, CHEST, CHEST_OPEN, SIGN, TREE_BASE -> WOOD_LIGHT;
            case TORCH -> Color.web("#3e423a");
            case TREE_TOP -> Color.web("#285333");
            default -> Color.web("#303840");
        };
    }

    // ── Door toggle ───────────────────────────────────────────────────────
    public void toggleDoor() {
        if (id == DOOR_CLOSED) { id = DOOR_OPEN;   walkable = true;  interactionLabel = "Close"; applyDefaults(DOOR_OPEN); }
        else if (id == DOOR_OPEN){ id = DOOR_CLOSED; walkable = false; interactionLabel = "Open"; applyDefaults(DOOR_CLOSED); }
    }

    public void openChest()  { if (id == CHEST) { id = CHEST_OPEN; interactive = false; } }

    // ── Getters ───────────────────────────────────────────────────────────
    public int     getId()               { return id; }
    public boolean isWalkable()          { return walkable; }
    public boolean isInteractive()       { return interactive; }
    public boolean isAnimated()          { return animated; }
    public float   getLightEmission()    { return lightEmission; }
    public String  getInteractionLabel() { return interactionLabel; }
}
