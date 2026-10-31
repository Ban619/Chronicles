package com.abyss.world3d;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * Meaningful surface categories shared by the model editor and the 3D renderer.
 * JavaFX uses a Phong lighting model, so these values approximate material response
 * instead of representing a full physically based material.
 */
public enum SemanticMaterial {
    CUSTOM("#8b8e83", "MATTE", 0.24, 0.24, 0.24),
    WOOD("#805334", "MATTE", 0.28, 0.19, 0.11),
    STONE("#858985", "ROUGH", 0.22, 0.23, 0.24),
    BRICK("#925d49", "ROUGH", 0.28, 0.19, 0.15),
    IRON("#525b60", "POLISHED", 0.63, 0.68, 0.72),
    GOLD("#d3a94f", "POLISHED", 0.88, 0.68, 0.32),
    GLASS("#8fcbd0", "GLASS", 0.82, 0.91, 1.0),
    LEATHER("#704331", "MATTE", 0.25, 0.18, 0.14),
    FOLIAGE("#467f43", "MATTE", 0.17, 0.26, 0.14),
    FLAME("#ff9a3c", "EMISSIVE", 0.50, 0.28, 0.10);

    private final String baseColor;
    private final String defaultFinish;
    private final double specularRed;
    private final double specularGreen;
    private final double specularBlue;

    SemanticMaterial(String baseColor, String defaultFinish,
                     double specularRed, double specularGreen, double specularBlue) {
        this.baseColor = baseColor;
        this.defaultFinish = defaultFinish;
        this.specularRed = specularRed;
        this.specularGreen = specularGreen;
        this.specularBlue = specularBlue;
    }

    public String baseColor() {
        return baseColor;
    }

    public String defaultFinish() {
        return defaultFinish;
    }

    public static SemanticMaterial from(String value) {
        if (value == null || value.isBlank()) return CUSTOM;
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CUSTOM;
        }
    }

    /** Apply the semantic reflectance and the selected surface finish to a JavaFX material. */
    public void applyTo(PhongMaterial material, String finish) {
        String selectedFinish = finish == null ? defaultFinish : finish.toUpperCase(java.util.Locale.ROOT);
        if (this == CUSTOM) {
            applyCustomFinish(material, selectedFinish);
            return;
        }

        double specularScale;
        double specularPower;
        switch (selectedFinish) {
            case "ROUGH" -> { specularScale = 0.45; specularPower = 4; }
            case "SATIN" -> { specularScale = 0.90; specularPower = 18; }
            case "POLISHED" -> { specularScale = 1.15; specularPower = 54; }
            case "GLASS" -> { specularScale = 1.15; specularPower = 72; }
            case "EMISSIVE" -> { specularScale = 0.65; specularPower = 28; }
            default -> { specularScale = 0.65; specularPower = 10; }
        }

        material.setSpecularColor(Color.color(
                clamp(specularRed * specularScale),
                clamp(specularGreen * specularScale),
                clamp(specularBlue * specularScale)));
        material.setSpecularPower(specularPower);

        if (this == GLASS || "GLASS".equals(selectedFinish)) {
            Color diffuse = material.getDiffuseColor();
            material.setDiffuseColor(new Color(diffuse.getRed(), diffuse.getGreen(), diffuse.getBlue(), 0.48));
        }
        if (this == FLAME || "EMISSIVE".equals(selectedFinish)) {
            material.setSelfIlluminationMap(solidTexture(material.getDiffuseColor()));
        }
    }

    /** Preserve the existing finish-only behavior for older/custom model files. */
    private static void applyCustomFinish(PhongMaterial material, String finish) {
        switch (finish) {
            case "ROUGH" -> {
                material.setSpecularColor(Color.color(0.12, 0.12, 0.12));
                material.setSpecularPower(4);
            }
            case "SATIN" -> {
                material.setSpecularColor(Color.color(0.32, 0.32, 0.32));
                material.setSpecularPower(18);
            }
            case "POLISHED" -> {
                material.setSpecularColor(Color.color(0.62, 0.62, 0.62));
                material.setSpecularPower(54);
            }
            case "GLASS" -> {
                Color diffuse = material.getDiffuseColor();
                material.setDiffuseColor(new Color(diffuse.getRed(), diffuse.getGreen(), diffuse.getBlue(), 0.48));
                material.setSpecularColor(Color.color(0.85, 0.9, 1.0, 0.75));
                material.setSpecularPower(72);
            }
            case "EMISSIVE" -> {
                material.setSelfIlluminationMap(solidTexture(material.getDiffuseColor()));
                material.setSpecularColor(Color.color(0.45, 0.35, 0.18));
                material.setSpecularPower(28);
            }
            default -> {
                material.setSpecularColor(Color.color(0.24, 0.24, 0.24));
                material.setSpecularPower(10);
            }
        }
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static WritableImage solidTexture(Color color) {
        WritableImage image = new WritableImage(2, 2);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) image.getPixelWriter().setColor(x, y, color);
        }
        return image;
    }
}
