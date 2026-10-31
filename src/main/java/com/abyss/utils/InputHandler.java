package com.abyss.utils;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.robot.Robot;
import javafx.geometry.Point2D;

import java.util.*;

public class InputHandler {

    private final Canvas canvas;

    // ── Key state ─────────────────────────────────────────────────────────
    private final Set<String> held      = new HashSet<>();
    private final Set<String> justPressed = new HashSet<>();
    private final Set<String> consumed  = new HashSet<>();

    // ── Virtual joystick ──────────────────────────────────────────────────
    private boolean joyActive = false;
    private double  joyBaseX, joyBaseY;
    private double  joyTipX,  joyTipY;
    private static final double JOY_RADIUS = 55.0;
    private boolean firstPersonLookEnabled;
    private boolean mousePositionInitialized;
    private double lastMouseX;
    private double lastMouseY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    private Robot mouseRobot;

    // ── Scroll ────────────────────────────────────────────────────────────
    private int scrollDelta = 0;

    // ── Canvas size (for touch region detection) ──────────────────────────
    private double canvasW = 960, canvasH = 640;

    // ─────────────────────────────────────────────────────────────────────
    public InputHandler(Scene scene, Canvas canvas) {
        this.canvas = canvas;
        // Keyboard events
        scene.setOnKeyPressed(e -> {
            String k = toKey(e.getCode());
            if (!held.contains(k)) justPressed.add(k);
            held.add(k);
        });
        scene.setOnKeyReleased(e -> held.remove(toKey(e.getCode())));

        // Mouse scroll
        scene.setOnScroll(e -> scrollDelta += e.getDeltaY() > 0 ? -1 : 1);

        // Mouse (desktop click/drag acting as virtual joystick)
        canvas.setOnMousePressed(e -> {
            if (!firstPersonLookEnabled) handlePointerDown(e.getX(), e.getY());
        });
        canvas.setOnMouseDragged(e -> {
            if (firstPersonLookEnabled) handleMouseLook(e.getX(), e.getY());
            else handlePointerMove(e.getX(), e.getY());
        });
        canvas.setOnMouseMoved(e -> {
            if (firstPersonLookEnabled) handleMouseLook(e.getX(), e.getY());
        });
        canvas.setOnMouseReleased(e -> {
            if (!firstPersonLookEnabled) handlePointerUp();
        });

        // Touch events
        canvas.setOnTouchPressed(e ->
            e.getTouchPoints().forEach(tp -> handlePointerDown(tp.getX(), tp.getY())));
        canvas.setOnTouchMoved(e ->
            e.getTouchPoints().forEach(tp -> handlePointerMove(tp.getX(), tp.getY())));
        canvas.setOnTouchReleased(e -> handlePointerUp());

        canvas.widthProperty() .addListener((o, ov, nv) -> canvasW = nv.doubleValue());
        canvas.heightProperty().addListener((o, ov, nv) -> canvasH = nv.doubleValue());
    }

    public void setFirstPersonLookEnabled(boolean enabled) {
        if (firstPersonLookEnabled == enabled) return;
        firstPersonLookEnabled = enabled;
        joyActive = false;
        mouseDeltaX = 0;
        mouseDeltaY = 0;
        mousePositionInitialized = enabled;
        if (enabled) {
            canvas.setCursor(Cursor.NONE);
            lastMouseX = canvasW / 2;
            lastMouseY = canvasH / 2;
            centerMousePointer();
        } else {
            canvas.setCursor(Cursor.DEFAULT);
        }
    }

    private void handleMouseLook(double x, double y) {
        if (!mousePositionInitialized) {
            lastMouseX = x;
            lastMouseY = y;
            mousePositionInitialized = true;
        }
        mouseDeltaX += x - lastMouseX;
        mouseDeltaY += y - lastMouseY;
        lastMouseX = canvasW / 2;
        lastMouseY = canvasH / 2;
        centerMousePointer();
    }

    private void centerMousePointer() {
        try {
            if (mouseRobot == null) mouseRobot = new Robot();
            Point2D screenCenter = canvas.localToScreen(canvasW / 2, canvasH / 2);
            if (screenCenter != null) mouseRobot.mouseMove(screenCenter.getX(), screenCenter.getY());
        } catch (RuntimeException ex) {
            mouseRobot = null;
        }
    }

    public double consumeMouseDeltaX() {
        double value = mouseDeltaX;
        mouseDeltaX = 0;
        return value;
    }

    public double consumeMouseDeltaY() {
        double value = mouseDeltaY;
        mouseDeltaY = 0;
        return value;
    }

    // ── Pointer helpers ───────────────────────────────────────────────────
    private void handlePointerDown(double x, double y) {
        if (x < canvasW * 0.55) {
            // Left side → joystick
            joyActive = true;
            joyBaseX  = x;
            joyBaseY  = y;
            joyTipX   = x;
            joyTipY   = y;
        } else {
            // Right side → interact button
            justPressed.add("e");
        }
    }

    private void handlePointerMove(double x, double y) {
        if (!joyActive) return;
        joyTipX = x;
        joyTipY = y;
    }

    private void handlePointerUp() {
        joyActive = false;
        joyTipX   = joyBaseX;
        joyTipY   = joyBaseY;
    }

    // ── Poll methods ──────────────────────────────────────────────────────
    /** Returns true if the given key is currently held down. */
    public boolean isDown(String key) {
        return held.contains(key.toLowerCase());
    }

    /** Returns true if the key was pressed since the last call to this method for that key. */
    public boolean isJustPressed(String key) {
        String k = key.toLowerCase();
        if (justPressed.contains(k) && !consumed.contains(k)) {
            consumed.add(k);
            return true;
        }
        return false;
    }

    /** Call once per frame at the end of the update cycle to flush just-pressed state. */
    public void endFrame() {
        justPressed.clear();
        consumed.clear();
    }

    /** Returns normalised joystick vector [dx, dy], or [0,0] if inactive. */
    public float[] getJoystickVector() {
        if (!joyActive) return new float[]{0, 0};
        double dx  = joyTipX - joyBaseX;
        double dy  = joyTipY - joyBaseY;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 4) return new float[]{0, 0};
        len = Math.min(len, JOY_RADIUS);
        return new float[]{(float)(dx / JOY_RADIUS), (float)(dy / JOY_RADIUS)}; 
    }

    /** Render the on-screen virtual joystick (called by HUD). */
    public void renderJoystick(javafx.scene.canvas.GraphicsContext gc) {
        if (!joyActive) return;
        // Base ring
        gc.setStroke(javafx.scene.paint.Color.color(1, 1, 1, 0.15));
        gc.setLineWidth(2);
        gc.strokeOval(joyBaseX - JOY_RADIUS, joyBaseY - JOY_RADIUS, JOY_RADIUS * 2, JOY_RADIUS * 2);
        // Thumb
        double dx = Math.max(-JOY_RADIUS, Math.min(JOY_RADIUS, joyTipX - joyBaseX));
        double dy = Math.max(-JOY_RADIUS, Math.min(JOY_RADIUS, joyTipY - joyBaseY));
        gc.setFill(javafx.scene.paint.Color.color(1, 1, 1, 0.25));
        gc.fillOval(joyBaseX + dx - 18, joyBaseY + dy - 18, 36, 36);
    }

    /** Consume and return accumulated scroll delta. */
    public int consumeScrollDelta() {
        int v = scrollDelta;
        scrollDelta = 0;
        return v;
    }

    // ── Key name normalisation ────────────────────────────────────────────
    private String toKey(KeyCode code) {
        return switch (code) {
            case W, UP     -> "up";    // map W and UP to same key
            case S, DOWN   -> "down";
            case A, LEFT   -> "left";
            case D, RIGHT  -> "right";
            case SPACE     -> "space";
            case ENTER     -> "enter";
            case ESCAPE    -> "escape";
            case E         -> "e";
            case V         -> "v";
            case P         -> "p";
            case I         -> "i";
            case TAB       -> "tab";
            case DIGIT1    -> "1";
            case DIGIT2    -> "2";
            case DIGIT3    -> "3";
            case DIGIT4    -> "4";
            case DIGIT5    -> "5";
            case DIGIT6    -> "6";
            case DIGIT7    -> "7";
            case DIGIT8    -> "8";
            case DIGIT9    -> "9";
            case F5        -> "f5";
            case F9        -> "f9";
            default        -> code.getName().toLowerCase();
        };
    }

    public boolean isJoyActive() { return joyActive; }
}
