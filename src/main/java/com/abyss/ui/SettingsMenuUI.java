package com.abyss.ui;

import com.abyss.utils.InputHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.prefs.Preferences;

/** In-game settings for selecting and remembering the world view. */
public final class SettingsMenuUI {

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(SettingsMenuUI.class);
    private static final String VIEW_KEY = "closeUpPerspective";

    private boolean open;
    private boolean closeUpPerspective = PREFERENCES.getBoolean(VIEW_KEY, false);

    public void open() { open = true; }
    public void close() { open = false; }
    public boolean isOpen() { return open; }
    public boolean isCloseUpPerspective() { return closeUpPerspective; }

    /** Returns true when the selected view changes. */
    public boolean setCloseUpPerspective(boolean enabled) {
        if (closeUpPerspective == enabled) return false;
        closeUpPerspective = enabled;
        PREFERENCES.putBoolean(VIEW_KEY, enabled);
        return true;
    }

    /** Keyboard controls work the same with the menu open in either view. */
    public void handleInput(InputHandler input) {
        if (!open) return;
        if (input.isJustPressed("left")) setCloseUpPerspective(false);
        if (input.isJustPressed("right")) setCloseUpPerspective(true);
        if (input.isJustPressed("enter")) setCloseUpPerspective(!closeUpPerspective);
    }

    public void render(GraphicsContext gc, double width, double height) {
        if (!open) return;

        gc.setFill(Color.color(0.015, 0.02, 0.035, 0.78));
        gc.fillRect(0, 0, width, height);

        double panelWidth = Math.min(650, width - 36);
        double panelHeight = Math.min(350, height - 36);
        double x = (width - panelWidth) / 2;
        double y = (height - panelHeight) / 2;
        gc.setFill(Color.web("#11131b", 0.98));
        gc.fillRoundRect(x, y, panelWidth, panelHeight, 16, 16);
        gc.setStroke(Color.web("#b89961"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, panelWidth, panelHeight, 16, 16);

        gc.setFill(Color.web("#ead4a4"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 24));
        gc.fillText("SETTINGS", x + 26, y + 42);
        gc.setFill(Color.web("#aaa28e"));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("Choose the view that feels right for your adventure.", x + 27, y + 67);

        double gap = 14;
        double cardY = y + 92;
        double cardWidth = (panelWidth - 54 - gap) / 2;
        double cardHeight = Math.max(120, panelHeight - 157);
        renderViewCard(gc, x + 27, cardY, cardWidth, cardHeight,
                "2D  ·  TOP-DOWN", "Classic overhead view with the whole area in sight.", !closeUpPerspective);
        renderViewCard(gc, x + 27 + cardWidth + gap, cardY, cardWidth, cardHeight,
                "CLOSE-UP PERSPECTIVE", "Follow your hero from behind in a 3D world.", closeUpPerspective);

        gc.setFill(Color.web("#b8ae98"));
        gc.setFont(Font.font("Serif", 12));
        gc.fillText("[← / →] Change view    [ENTER] Toggle    [ESC] Resume", x + 27, y + panelHeight - 22);
    }

    private void renderViewCard(GraphicsContext gc, double x, double y, double width,
                                double height, String title, String description, boolean selected) {
        gc.setFill(selected ? Color.web("#28251d") : Color.web("#191b23"));
        gc.fillRoundRect(x, y, width, height, 11, 11);
        gc.setStroke(selected ? Color.web("#e4bd70") : Color.web("#393a42"));
        gc.setLineWidth(selected ? 2 : 1);
        gc.strokeRoundRect(x, y, width, height, 11, 11);

        gc.setFill(selected ? Color.web("#ffe2a3") : Color.web("#c6c0b2"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, width < 250 ? 12 : 14));
        gc.fillText(title, x + 14, y + 29);
        gc.setFill(Color.web("#aaa493"));
        gc.setFont(Font.font("Serif", 12));
        drawWrapped(gc, description, x + 14, y + 55, width - 28, 17);

        if (selected) {
            gc.setFill(Color.web("#f0c66e"));
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 11));
            gc.fillText("SELECTED", x + 14, y + height - 14);
        }
    }

    private void drawWrapped(GraphicsContext gc, String text, double x, double y,
                             double maxWidth, double lineHeight) {
        StringBuilder line = new StringBuilder();
        double baseline = y;
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && candidate.length() * 6.3 > maxWidth) {
                gc.fillText(line.toString(), x, baseline);
                baseline += lineHeight;
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) gc.fillText(line.toString(), x, baseline);
    }
}
