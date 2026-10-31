module com.abyss {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.media;
    requires java.desktop;
    requires java.prefs;
    requires com.fasterxml.jackson.databind;
    requires java.logging;

    exports com.abyss.main;
    exports com.abyss.entities;
    exports com.abyss.items;
    exports com.abyss.world;
    exports com.abyss.ui;
    exports com.abyss.combat;
    exports com.abyss.utils;
    exports com.abyss.skills;
    exports com.abyss.lighting;
    exports com.abyss.world3d;

    opens com.abyss.world3d to com.fasterxml.jackson.databind;
}
