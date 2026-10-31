package com.abyss.entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Draws the game's friendly, storybook-style NPC and party portraits in-world. */
public final class CharacterPainter {

    public enum Style {
        VILLAGER, MERCHANT, GUARD, QUEST_GIVER, HEALER, BLACKSMITH, COMPANION,
        WARRIOR, MAGE, ROGUE, CLERIC, RANGER
    }

    private static final Color OUTLINE = Color.web("#66536a");
    private static final Color[] SKIN_TONES = {
        Color.web("#f4c9a7"), Color.web("#dfa47c"), Color.web("#bf805d"),
        Color.web("#8f604c"), Color.web("#f1d4b4")
    };
    private static final Color[] HAIR_COLORS = {
        Color.web("#49354b"), Color.web("#70452f"), Color.web("#d38c42"),
        Color.web("#3d405c"), Color.web("#a45e45"), Color.web("#d46c70")
    };
    private static final Color[] VILLAGER_OUTFITS = colors(
            "#e98572", "#61b7a5", "#efc452", "#73a6d2", "#9c81ce");
    private static final Color[] MERCHANT_OUTFITS = colors("#e7aa4e", "#63b89d", "#e98572");
    private static final Color[] GUARD_OUTFITS = colors("#5b9ec0", "#65b4ad", "#667ed0");
    private static final Color[] QUEST_OUTFITS = colors("#987dd0", "#df9b58", "#58ac9b");
    private static final Color[] HEALER_OUTFITS = colors("#59b8a2", "#83b966", "#70a8db");
    private static final Color[] SMITH_OUTFITS = colors("#d77955", "#c98a57", "#5798a4");
    private static final Color[] COMPANION_OUTFITS = colors("#9879d0", "#e57686", "#58b7ad");
    private static final Color[] GOLD_ACCENTS = colors("#f4d16f", "#e7c7f0", "#fff0b2");
    private static final Color[] HEALER_ACCENTS = colors("#f2d27a", "#d7f0c7", "#f5e6b2");
    private static final Color[] SMITH_ACCENTS = colors("#f1bd72", "#f2dfb4", "#f1c883");
    private static final Color[] COMPANION_ACCENTS = colors("#f5d06f", "#d8f0a5", "#f5c6d0");
    private static final Color[] VILLAGER_ACCENTS = colors("#f5d273", "#f4f0dc", "#f0c4a4");

    private CharacterPainter() {}

    public static void draw(GraphicsContext gc, float x, float y, int width, int height,
                            Style style, String name, int direction, boolean moving) {
        int seed = name == null ? style.ordinal() : name.hashCode();
        int variant = Math.floorMod(seed, 3);
        Color outfit = outfitColor(style, variant);
        Color accent = accentColor(style, variant);
        Color skin = SKIN_TONES[Math.floorMod(seed >>> 3, SKIN_TONES.length)];
        Color hair = isElder(name) ? Color.web("#c4b9a6")
                : HAIR_COLORS[Math.floorMod(seed >>> 5, HAIR_COLORS.length)];
        double scaleX = Math.max(0.5, width / 32.0);
        double scaleY = Math.max(0.5, height / 32.0);
        double time = System.currentTimeMillis();
        double bob = moving ? Math.sin(time / 95.0 + variant) * 0.8
                : Math.sin(time / 620.0 + variant) * 0.25;
        double step = moving ? Math.sin(time / 95.0 + variant) * 1.4 : 0;

        gc.save();
        gc.translate(x, y);
        gc.scale(scaleX, scaleY);

        // Soft ground shadow and tiny feet keep the character anchored to the tile.
        gc.setFill(Color.color(0.20, 0.22, 0.30, 0.17));
        gc.fillOval(4, 27, 24, 5);
        gc.setFill(Color.web("#73556a"));
        gc.fillRoundRect(9 + step, 25 + bob, 7, 5, 4, 4);
        gc.fillRoundRect(17 - step, 25 + bob, 7, 5, 4, 4);
        gc.setFill(accent);
        gc.fillRoundRect(10 + step, 25, 5, 2, 2, 2);
        gc.fillRoundRect(18 - step, 25, 5, 2, 2, 2);

        // Hair behind the face, then a bright, rounded coat silhouette.
        gc.setFill(hair);
        gc.fillRoundRect(6, 6 + bob, 5, 14, 4, 4);
        gc.fillRoundRect(21, 6 + bob, 5, 14, 4, 4);
        gc.setStroke(OUTLINE);
        gc.setLineWidth(0.9);
        gc.strokeRoundRect(5.5, 14 + bob, 21, 13, 9, 9);
        gc.setFill(shade(outfit, 0.82));
        gc.fillRoundRect(5.5, 14 + bob, 21, 13, 9, 9);
        gc.setFill(outfit);
        gc.fillRoundRect(7, 15 + bob, 18, 10, 7, 7);

        // Moving sleeves swing gently. The light cuffs make each pose readable.
        gc.setFill(shade(outfit, 0.88));
        gc.fillRoundRect(3 + step, 16 + bob, 7, 10, 5, 5);
        gc.fillRoundRect(22 - step, 16 + bob, 7, 10, 5, 5);
        gc.setFill(accent);
        gc.fillRoundRect(4 + step, 23 + bob, 5, 3, 3, 3);
        gc.fillRoundRect(23 - step, 23 + bob, 5, 3, 3, 3);

        // Aprons distinguish the shopkeepers and maker without using dark armor.
        if (style == Style.MERCHANT || style == Style.BLACKSMITH) {
            Color apron = style == Style.BLACKSMITH ? Color.web("#bd6a4d") : shade(accent, 0.88);
            gc.setFill(OUTLINE);
            gc.fillRoundRect(10, 18 + bob, 12, 9, 4, 4);
            gc.setFill(apron);
            gc.fillRoundRect(10.8, 18.5 + bob, 10.4, 7.5, 3.5, 3.5);
            gc.setFill(style == Style.MERCHANT ? Color.web("#f7d56d") : Color.web("#f2bb73"));
            gc.fillOval(15, 20 + bob, 3.5, 3.5);
        }

        // Neck and ears.
        gc.setFill(shade(skin, 0.92));
        gc.fillRoundRect(13, 12 + bob, 6, 5, 3, 3);
        gc.setFill(skin);
        gc.fillOval(6.5, 8 + bob, 4, 5);
        gc.fillOval(21.5, 8 + bob, 4, 5);

        // Big, friendly face with simple, readable expressions.
        gc.setFill(OUTLINE);
        gc.fillOval(7, 3 + bob, 18, 17);
        gc.setFill(skin);
        gc.fillOval(8, 4 + bob, 16, 15);

        // Hair shape and small role-specific headwear.
        gc.setFill(hair);
        gc.fillOval(7, 2 + bob, 18, 9);
        if (variant == 0) {
            gc.fillOval(8, 5 + bob, 7, 5);
        } else if (variant == 1) {
            gc.fillOval(20, 4 + bob, 5, 5);
        } else {
            gc.fillOval(11, 2 + bob, 4, 5);
            gc.fillOval(17, 2 + bob, 4, 5);
        }
        if (direction != 3) {
            gc.setFill(Color.color(0.95, 0.47, 0.48, 0.36));
            gc.fillOval(9.5, 12 + bob, 3.5, 2);
            gc.fillOval(19, 12 + bob, 3.5, 2);
            gc.setFill(Color.web("#44364c"));
            if (direction == 1) {
                gc.fillOval(11.5, 9 + bob, 2.2, 2.6);
            } else if (direction == 2) {
                gc.fillOval(18.3, 9 + bob, 2.2, 2.6);
            } else {
                gc.fillOval(11.5, 9 + bob, 2.2, 2.6);
                gc.fillOval(18.3, 9 + bob, 2.2, 2.6);
            }
            gc.setFill(Color.web("#a95763"));
            gc.fillOval(direction == 1 ? 13 : direction == 2 ? 18 : 15, 14 + bob, 2.4, 1.5);
        }
        if (isElder(name) && direction != 3) {
            gc.setFill(Color.web("#c4b9a6"));
            gc.fillRoundRect(11, 13 + bob, 10, 6, 5, 5);
        }
        drawHeadwear(gc, style, outfit, accent, bob);
        drawBadge(gc, style, accent, bob);

        gc.restore();
    }

    private static void drawHeadwear(GraphicsContext gc, Style style, Color outfit,
                                     Color accent, double bob) {
        switch (style) {
            case MERCHANT -> {
                gc.setFill(outfit);
                gc.fillRoundRect(9, 1 + bob, 14, 5, 5, 5);
                gc.setFill(accent);
                gc.fillRoundRect(7, 5 + bob, 18, 2.5, 3, 3);
                gc.fillOval(19, 1 + bob, 3, 3);
            }
            case GUARD -> {
                gc.setFill(outfit);
                gc.fillRoundRect(8, 1 + bob, 16, 6, 5, 5);
                gc.setFill(Color.web("#f5d16b"));
                gc.fillRoundRect(7, 6 + bob, 18, 2.5, 3, 3);
                gc.fillOval(14, 2 + bob, 4, 4);
            }
            case QUEST_GIVER -> {
                gc.setFill(shade(outfit, 0.82));
                gc.fillRoundRect(7, 2 + bob, 18, 7, 7, 7);
                gc.setFill(accent);
                gc.fillOval(14, 3 + bob, 4, 4);
            }
            case HEALER, CLERIC -> {
                gc.setFill(Color.web("#f4e8b3"));
                gc.fillRoundRect(8, 6 + bob, 16, 2.5, 3, 3);
                gc.setFill(Color.web("#75c99e"));
                gc.fillOval(14, 4 + bob, 4, 4);
                gc.fillOval(12.5, 5 + bob, 3, 3);
                gc.fillOval(16.5, 5 + bob, 3, 3);
            }
            case BLACKSMITH -> {
                gc.setFill(Color.web("#f2aa68"));
                gc.fillRoundRect(8, 2 + bob, 16, 5, 4, 4);
                gc.setFill(accent);
                gc.fillRoundRect(7, 6 + bob, 18, 2, 2, 2);
            }
            case COMPANION, RANGER -> {
                gc.setFill(accent);
                gc.fillRoundRect(8, 5 + bob, 16, 3, 4, 4);
                gc.fillOval(19, 1 + bob, 4, 7);
            }
            case WARRIOR -> {
                gc.setFill(outfit);
                gc.fillRoundRect(8, 2 + bob, 16, 6, 5, 5);
                gc.setFill(Color.web("#f5d16b"));
                gc.fillOval(14, 3 + bob, 4, 4);
            }
            case MAGE -> {
                gc.setFill(outfit);
                gc.fillPolygon(new double[]{9, 16, 23},
                        new double[]{7 + bob, -2 + bob, 7 + bob}, 3);
                gc.setFill(Color.web("#f8dc75"));
                gc.fillOval(14.3, 0 + bob, 3.5, 3.5);
            }
            case ROGUE -> {
                gc.setFill(outfit);
                gc.fillRoundRect(7, 3 + bob, 18, 6, 6, 6);
                gc.setFill(accent);
                gc.fillOval(20, 1 + bob, 4, 7);
            }
            default -> { }
        }
    }

    private static void drawBadge(GraphicsContext gc, Style style, Color accent, double bob) {
        double cx = 16;
        double cy = 20 + bob;
        switch (style) {
            case GUARD, WARRIOR -> {
                gc.setFill(Color.web("#f7e6b6"));
                gc.fillRoundRect(cx - 3, cy - 3, 6, 6, 2, 2);
                gc.setFill(accent);
                gc.fillRoundRect(cx - 1, cy - 2, 2, 4, 1, 1);
                gc.fillRoundRect(cx - 2, cy - 1, 4, 2, 1, 1);
            }
            case QUEST_GIVER, MAGE -> {
                gc.setFill(Color.web("#ffe17b"));
                gc.fillOval(cx - 2.2, cy - 2.2, 4.4, 4.4);
                gc.setFill(Color.web("#fff5cf"));
                gc.fillOval(cx - 0.7, cy - 0.7, 1.4, 1.4);
            }
            case MERCHANT -> {
                gc.setFill(Color.web("#ffd66f"));
                gc.fillOval(cx - 2.5, cy - 2.5, 5, 5);
                gc.setStroke(Color.web("#b87938"));
                gc.setLineWidth(0.8);
                gc.strokeOval(cx - 1.3, cy - 1.3, 2.6, 2.6);
            }
            case HEALER, CLERIC -> {
                gc.setFill(Color.web("#fff1c8"));
                gc.fillRoundRect(cx - 1, cy - 3, 2, 6, 1, 1);
                gc.fillRoundRect(cx - 3, cy - 1, 6, 2, 1, 1);
            }
            case BLACKSMITH -> {
                gc.setFill(Color.web("#fff0c7"));
                gc.fillRoundRect(cx - 0.8, cy - 1, 1.6, 4, 0.8, 0.8);
                gc.fillRoundRect(cx - 2.8, cy - 3, 5.6, 2.6, 1.1, 1.1);
            }
            case COMPANION, ROGUE, RANGER -> {
                gc.setFill(accent);
                gc.fillOval(cx - 2.3, cy - 2.3, 4.6, 4.6);
                gc.setFill(Color.web("#fff2cf"));
                gc.fillOval(cx - 0.7, cy - 0.7, 1.4, 1.4);
            }
            case VILLAGER -> {
                gc.setFill(accent);
                gc.fillOval(cx - 1.8, cy - 1.8, 3.6, 3.6);
            }
        }
    }

    private static Color outfitColor(Style style, int variant) {
        Color[] palette = switch (style) {
            case MERCHANT -> MERCHANT_OUTFITS;
            case GUARD, WARRIOR -> GUARD_OUTFITS;
            case QUEST_GIVER, MAGE -> QUEST_OUTFITS;
            case HEALER, CLERIC -> HEALER_OUTFITS;
            case BLACKSMITH -> SMITH_OUTFITS;
            case COMPANION, ROGUE, RANGER -> COMPANION_OUTFITS;
            default -> VILLAGER_OUTFITS;
        };
        return palette[Math.floorMod(variant + style.ordinal(), palette.length)];
    }

    private static Color accentColor(Style style, int variant) {
        Color[] accents = switch (style) {
            case GUARD, WARRIOR, QUEST_GIVER, MAGE -> GOLD_ACCENTS;
            case HEALER, CLERIC -> HEALER_ACCENTS;
            case BLACKSMITH -> SMITH_ACCENTS;
            case COMPANION, ROGUE, RANGER -> COMPANION_ACCENTS;
            default -> VILLAGER_ACCENTS;
        };
        return accents[Math.floorMod(variant, accents.length)];
    }

    private static Color[] colors(String... hex) {
        Color[] result = new Color[hex.length];
        for (int i = 0; i < hex.length; i++) result[i] = Color.web(hex[i]);
        return result;
    }

    private static Color shade(Color color, double factor) {
        return Color.color(color.getRed() * factor, color.getGreen() * factor,
                color.getBlue() * factor, color.getOpacity());
    }

    private static boolean isElder(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("elder") || lower.contains("hermit");
    }
}
