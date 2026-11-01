package com.abyss.world3d;

import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomModelHierarchyTest {

    @Test
    void nestedPartsAreRenderedUnderTheirParent() {
        CustomModelDefinition.Part parent = new CustomModelDefinition.Part();
        parent.name = "Lantern frame";
        CustomModelDefinition.Part child = new CustomModelDefinition.Part();
        child.name = "Lantern glass";
        child.parentId = parent.id;

        Group model = CustomModelLibrary.createHierarchy(List.of(parent, child));

        assertEquals(1, model.getChildren().size());
        Group parentNode = (Group) model.getChildren().get(0);
        assertEquals(parent, parentNode.getUserData());
        assertEquals(2, parentNode.getChildren().size());
        assertEquals(child, parentNode.getChildren().get(1).getUserData());
    }

    @Test
    void savingRejectsCyclicPartHierarchy(@TempDir Path directory) {
        CustomModelDefinition definition = new CustomModelDefinition();
        CustomModelDefinition.Part first = new CustomModelDefinition.Part();
        CustomModelDefinition.Part second = new CustomModelDefinition.Part();
        first.parentId = second.id;
        second.parentId = first.id;
        definition.parts = List.of(first, second);

        assertThrows(IllegalArgumentException.class,
                () -> CustomModelLibrary.save(definition, directory.resolve("cyclic.json")));
    }

    @Test
    void nestedRelationshipsSurviveSaveAndReload(@TempDir Path directory) throws Exception {
        CustomModelDefinition definition = new CustomModelDefinition();
        CustomModelDefinition.Part parent = new CustomModelDefinition.Part();
        CustomModelDefinition.Part child = new CustomModelDefinition.Part();
        child.parentId = parent.id;
        definition.parts = List.of(parent, child);
        Path file = directory.resolve("nested-model.json");

        CustomModelLibrary.save(definition, file);
        CustomModelDefinition loaded = CustomModelLibrary.read(file);

        assertEquals(parent.id, loaded.parts.get(1).parentId);
    }

    @Test
    void olderModelsWithoutHierarchyIdsRemainLoadable(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("legacy-model.json");
        java.nio.file.Files.writeString(file,
                "{\"name\":\"Legacy\",\"tileId\":0,\"parts\":[{\"name\":\"Legacy part\",\"shape\":\"BOX\"}]}");

        CustomModelDefinition loaded = CustomModelLibrary.read(file);

        assertNotNull(loaded.parts.get(0).id);
        assertNull(loaded.parts.get(0).parentId);
    }

    @Test
    void partPropertiesAreAppliedToRenderedGeometry() {
        CustomModelDefinition.Part part = new CustomModelDefinition.Part();
        part.x = 7;
        part.y = 11;
        part.z = -4;
        part.width = 18;
        part.height = 26;
        part.depth = 30;
        part.rotateX = 12;
        part.rotateY = 28;
        part.rotateZ = 42;
        part.color = "#33669980";

        Group node = CustomModelLibrary.createPartNode(part);
        Box box = (Box) node.getChildren().get(0);
        PhongMaterial material = (PhongMaterial) box.getMaterial();

        assertEquals(7, node.getTranslateX());
        assertEquals(-11, node.getTranslateY());
        assertEquals(-4, node.getTranslateZ());
        assertEquals(18, box.getWidth());
        assertEquals(26, box.getHeight());
        assertEquals(30, box.getDepth());
        assertEquals(12, ((Rotate) node.getTransforms().get(0)).getAngle());
        assertEquals(28, ((Rotate) node.getTransforms().get(1)).getAngle());
        assertEquals(42, ((Rotate) node.getTransforms().get(2)).getAngle());
        assertEquals(0x80 / 255.0, material.getDiffuseColor().getOpacity(), 0.01);
    }
}
