package com.abyss.ui;

import com.abyss.entities.Player;
import com.abyss.utils.InputHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** Brief first-run guide for movement, combat, and the chapter objective. */
public final class TutorialUI {

    private static final String[][] PAGES = {
        {"WELCOME TO ASHENVEIL", "Move with WASD or the arrow keys.",
                "Press E near people, doors, or chests to interact."},
        {"SURVIVE THE ROAD", "Face an enemy and press Space to attack.",
                "Move when a warning ring appears. Q/R cast learned skills; K opens the skill tree."},
        {"FOLLOW THE STORY", "Talk to Elder Aldric, then clear three enemies in Thornwood.",
                "Return to Aldric for the Crypt clue. The Dawn Seal opens Dragon's Maw."},
        {"PREPARE AND SAVE", "Press I for items. Mira's shop: E to open, 1-3 to buy, E to leave.",
                "Use the hotbar with 1-9. Press F5 to save and F9 to load."}
    };

    private final Player player;
    private int page;
    private boolean visible;

    public TutorialUI(Player player) {
        this.player = player;
        visible = !player.isTutorialComplete();
    }

    public boolean isVisible() { return visible; }

    public void handleInput(InputHandler input) {
        if (!visible) return;
        if (input.isJustPressed("escape")) {
            finish();
        } else if (input.isJustPressed("enter") || input.isJustPressed("space")
                || input.isJustPressed("e")) {
            if (page + 1 == PAGES.length) finish();
            else page++;
        }
    }

    public void render(GraphicsContext gc, double width, double height) {
        if (!visible) return;
        double panelWidth = Math.min(720, width - 36);
        double panelHeight = 250;
        double x = (width - panelWidth) / 2;
        double y = (height - panelHeight) / 2;

        gc.save();
        gc.setFill(Color.color(0, 0, 0, 0.78));
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.web("#11131b", 0.99));
        gc.fillRoundRect(x, y, panelWidth, panelHeight, 14, 14);
        gc.setStroke(Color.web("#c8a96e"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, panelWidth, panelHeight, 14, 14);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#ead4a4"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 24));
        gc.fillText(PAGES[page][0], width / 2, y + 48);
        gc.setFill(Color.web("#d4c5a9"));
        gc.setFont(Font.font("Serif", 16));
        gc.fillText(PAGES[page][1], width / 2, y + 102);
        gc.fillText(PAGES[page][2], width / 2, y + 136);

        gc.setFill(Color.web("#c8a96e"));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText((page + 1) + " / " + PAGES.length
                + "     [ENTER / SPACE] Continue     [ESC] Skip", width / 2, y + panelHeight - 28);
        gc.restore();
    }

    private void finish() {
        visible = false;
        player.completeTutorial();
    }
}
