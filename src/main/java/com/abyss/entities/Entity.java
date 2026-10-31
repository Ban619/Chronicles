package com.abyss.entities;

import com.abyss.world.GameMap;
import javafx.scene.canvas.GraphicsContext;

/**
 * Entity — Abstract base for every living/interactive object in the game world.
 *
 * All entities share:
 *  - World position (x, y) in pixels
 *  - Bounding box (width × height) for collision detection
 *  - A display name
 *  - Abstract update(dt) and render(gc) contracts
 */
public abstract class Entity {

    protected String name;
    protected float  x, y;
    protected int    width, height;
    protected boolean visible = true;
    protected boolean active  = true;

    // ─────────────────────────────────────────────────────────────────────
    public Entity(String name, float x, float y, int width, int height) {
        this.name   = name;
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
    }

    // ── Contracts ─────────────────────────────────────────────────────────
    /** Physics / logic update. dt is in seconds. */
    public abstract void update(double dt, GameMap map);

    /** Draw this entity to the canvas using world-space coordinates. */
    public abstract void render(GraphicsContext gc);

    // ── Collision helpers ─────────────────────────────────────────────────
    public boolean overlaps(Entity other) {
        return x < other.x + other.width
            && x + width  > other.x
            && y < other.y + other.height
            && y + height > other.y;
    }

    public float getCenterX() { return x + width  / 2f; }
    public float getCenterY() { return y + height / 2f; }

    /** Distance (pixels) between centres. */
    public float distanceTo(Entity other) {
        float dx = getCenterX() - other.getCenterX();
        float dy = getCenterY() - other.getCenterY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // ── Accessors ─────────────────────────────────────────────────────────
    public String  getName()    { return name; }
    public float   getX()       { return x; }
    public float   getY()       { return y; }
    public int     getWidth()   { return width; }
    public int     getHeight()  { return height; }
    public boolean isVisible()  { return visible; }
    public boolean isActive()   { return active; }

    public void setX(float x)       { this.x = x; }
    public void setY(float y)       { this.y = y; }
    public void setVisible(boolean v){ this.visible = v; }
    public void setActive(boolean a) { this.active  = a; }
}
