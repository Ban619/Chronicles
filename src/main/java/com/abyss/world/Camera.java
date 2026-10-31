package com.abyss.world;

import com.abyss.entities.Player;

/**
 * Camera — Smooth-follow camera that keeps the player centred on screen.
 *
 * offsetX / offsetY represent the world-space pixel that maps to (0,0) on screen.
 * These are subtracted from every entity world position before drawing.
 */
public class Camera {

    private int offsetX = 0;
    private int offsetY = 0;
    private int screenW;
    private int screenH;
    private int viewCols;
    private int viewRows;

    private final Player player;
    private static final float LERP = 0.12f; // smoothing factor

    public Camera(Player player, int screenW, int screenH) {
        this.player  = player;
        resize(screenW, screenH);
    }

    public void resize(int w, int h) {
        this.screenW  = w;
        this.screenH  = h;
        this.viewCols = w / 32 + 2;
        this.viewRows = h / 32 + 2;
    }

    public void update() {
        int targetX = (int)(player.getCenterX() - screenW / 2.0);
        int targetY = (int)(player.getCenterY() - screenH / 2.0);
        offsetX += (int)((targetX - offsetX) * LERP);
        offsetY += (int)((targetY - offsetY) * LERP);
    }

    public boolean isInView(float x, float y, int w, int h) {
        return x + w > offsetX && x < offsetX + screenW &&
               y + h > offsetY && y < offsetY + screenH;
    }

    public int getOffsetX()  { return offsetX; }
    public int getOffsetY()  { return offsetY; }
    public int getViewCols() { return viewCols; }
    public int getViewRows() { return viewRows; }
}
