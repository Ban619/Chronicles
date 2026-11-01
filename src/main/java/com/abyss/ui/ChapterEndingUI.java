package com.abyss.ui;

import com.abyss.utils.InputHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** Closing story beat shown when the player completes Chapter One. */
public final class ChapterEndingUI {

    private boolean open;

    public void open() { open = true; }
    public boolean isOpen() { return open; }

    public void handleInput(InputHandler input) {
        if (input.isJustPressed("enter") || input.isJustPressed("escape")
                || input.isJustPressed("space")) open = false;
    }

    public void render(GraphicsContext gc, double width, double height) {
        if (!open) return;
        double panelWidth = Math.min(720, width - 36);
        double panelHeight = 290;
        double x = (width - panelWidth) / 2;
        double y = (height - panelHeight) / 2;

        gc.save();
        gc.setFill(Color.color(0, 0, 0, 0.82));
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.web("#11131b", 0.99));
        gc.fillRoundRect(x, y, panelWidth, panelHeight, 14, 14);
        gc.setStroke(Color.web("#e2bd72"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, panelWidth, panelHeight, 14, 14);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#ffe2a3"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 27));
        gc.fillText("CHAPTER ONE COMPLETE", width / 2, y + 52);
        gc.setFill(Color.web("#d4c5a9"));
        gc.setFont(Font.font("Serif", 16));
        gc.fillText("With the Dawn Seal restored and Malachar defeated,", width / 2, y + 112);
        gc.fillText("the shadows retreat from Thornwood and Ashenveil.", width / 2, y + 144);
        gc.fillText("The village lights its lanterns again. This is only the beginning.", width / 2, y + 176);
        gc.setFill(Color.web("#c8a96e"));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("[ENTER / SPACE / ESC] Return to the world", width / 2, y + panelHeight - 28);
        gc.restore();
    }
}
