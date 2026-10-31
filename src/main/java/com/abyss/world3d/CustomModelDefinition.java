package com.abyss.world3d;

import java.util.ArrayList;
import java.util.List;

/** JSON-backed editable model composed of simple 3D primitives. */
public final class CustomModelDefinition {
    public String name = "Custom Model";
    public int tileId;
    public List<Part> parts = new ArrayList<>();

    public static final class Part {
        public String name = "Part";
        public String shape = "BOX";
        public double x;
        public double y;
        public double z;
        public double width = 24;
        public double height = 24;
        public double depth = 24;
        public double rotateX;
        public double rotateY;
        public double rotateZ;
        public String color = "#8b8e83";
        /** Name of a SemanticMaterial preset; unknown or older values render as CUSTOM. */
        public String material = "CUSTOM";
        public String finish = "MATTE";
    }
}
