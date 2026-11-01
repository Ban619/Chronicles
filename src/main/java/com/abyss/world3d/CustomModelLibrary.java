package com.abyss.world3d;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** Loads custom primitive models from engine/models and builds fresh scene nodes. */
public final class CustomModelLibrary {
    public static final Path MODEL_DIRECTORY = Path.of("engine", "models");

    private static final Logger LOG = Logger.getLogger(CustomModelLibrary.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Map<Integer, CustomModelDefinition> modelsByTile = new HashMap<>();

    public CustomModelLibrary() {
        reload();
    }

    public void reload() {
        modelsByTile.clear();
        if (!Files.isDirectory(MODEL_DIRECTORY)) return;
        try (Stream<Path> files = Files.list(MODEL_DIRECTORY)) {
            files.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .forEach(this::loadModel);
        } catch (IOException ex) {
            LOG.warning("Could not list custom models: " + ex.getMessage());
        }
    }

    public Group createForTile(int tileId) {
        CustomModelDefinition definition = modelsByTile.get(tileId);
        if (definition == null || definition.parts == null) return null;

        return createHierarchy(definition.parts);
    }

    public static Group createHierarchy(List<CustomModelDefinition.Part> parts) {
        Group model = new Group();
        Map<String, Group> nodesById = new HashMap<>();
        for (CustomModelDefinition.Part part : parts) {
            if (part == null) continue;
            try {
                nodesById.put(part.id, createPartNode(part));
            } catch (RuntimeException ex) {
                LOG.warning("Skipping invalid model part: " + ex.getMessage());
            }
        }
        for (CustomModelDefinition.Part part : parts) {
            Group node = nodesById.get(part.id);
            Group parent = nodesById.get(part.parentId);
            if (node == null) continue;
            if (parent == null) {
                model.getChildren().add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return model;
    }

    public static void save(CustomModelDefinition definition, Path file) throws IOException {
        validate(definition);
        Path destination = file.toAbsolutePath();
        Path parent = destination.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, destination.getFileName().toString(), ".tmp");
        try {
            MAPPER.writeValue(temporary.toFile(), definition);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void validate(CustomModelDefinition definition) {
        if (definition == null || definition.name == null || definition.name.isBlank()) {
            throw new IllegalArgumentException("Model needs a name.");
        }
        if (definition.parts == null || definition.parts.isEmpty()) {
            throw new IllegalArgumentException("Model needs at least one component.");
        }
        for (int index = 0; index < definition.parts.size(); index++) {
            CustomModelDefinition.Part part = definition.parts.get(index);
            if (part == null) throw new IllegalArgumentException("Component " + (index + 1) + " is empty.");
            if (part.id == null || part.id.isBlank()) {
                throw new IllegalArgumentException("Component " + (index + 1) + " needs an identifier.");
            }
            if (part.id.equals(part.parentId)) {
                throw new IllegalArgumentException("Component " + (index + 1) + " cannot parent itself.");
            }
            if (part.shape == null || !List.of("BOX", "SPHERE", "CYLINDER", "CONE", "RING")
                    .contains(part.shape.toUpperCase())) {
                throw new IllegalArgumentException("Component " + (index + 1) + " has an unsupported shape.");
            }
            if (!Double.isFinite(part.x) || !Double.isFinite(part.y) || !Double.isFinite(part.z)
                    || !Double.isFinite(part.rotateX) || !Double.isFinite(part.rotateY)
                    || !Double.isFinite(part.rotateZ) || !Double.isFinite(part.width)
                    || !Double.isFinite(part.height) || !Double.isFinite(part.depth)
                    || part.width <= 0 || part.height <= 0 || part.depth <= 0) {
                throw new IllegalArgumentException("Component " + (index + 1)
                        + " has invalid dimensions or transforms.");
            }
            if (part.color != null) {
                try {
                    Color.web(part.color);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Component " + (index + 1) + " has an invalid color.");
                }
            }
        }
        Set<String> ids = new HashSet<>();
        Map<String, CustomModelDefinition.Part> partsById = new HashMap<>();
        for (CustomModelDefinition.Part part : definition.parts) {
            if (!ids.add(part.id)) throw new IllegalArgumentException("Components must have unique identifiers.");
            partsById.put(part.id, part);
        }
        for (CustomModelDefinition.Part part : definition.parts) {
            if (part.parentId != null && !ids.contains(part.parentId)) {
                throw new IllegalArgumentException("Component " + part.name + " has a missing parent.");
            }
            Set<String> ancestors = new HashSet<>();
            CustomModelDefinition.Part current = part;
            while (current.parentId != null) {
                if (!ancestors.add(current.id)) {
                    throw new IllegalArgumentException("Component hierarchy cannot contain a cycle.");
                }
                current = partsById.get(current.parentId);
            }
        }
    }

    public static CustomModelDefinition read(Path file) throws IOException {
        CustomModelDefinition definition = MAPPER.readValue(file.toFile(), CustomModelDefinition.class);
        if (definition.parts == null) definition.parts = new java.util.ArrayList<>();
        validate(definition);
        return definition;
    }

    private void loadModel(Path file) {
        try {
            CustomModelDefinition definition = read(file);
            if (definition.tileId > 0 && !definition.parts.isEmpty()) {
                modelsByTile.put(definition.tileId, definition);
            }
        } catch (IOException | RuntimeException ex) {
            LOG.warning("Could not load custom model " + file + ": " + ex.getMessage());
        }
    }

    public static Group createPartNode(CustomModelDefinition.Part part) {
        double width = Math.max(1, part.width);
        double height = Math.max(1, part.height);
        double depth = Math.max(1, part.depth);
        Shape3D shape = switch (part.shape == null ? "BOX" : part.shape.toUpperCase()) {
            case "SPHERE" -> {
                Sphere sphere = new Sphere(0.5, 14);
                sphere.setScaleX(width);
                sphere.setScaleY(height);
                sphere.setScaleZ(depth);
                yield sphere;
            }
            case "CYLINDER" -> {
                Cylinder cylinder = new Cylinder(0.5, 1, 14);
                cylinder.setScaleX(width);
                cylinder.setScaleY(height);
                cylinder.setScaleZ(depth);
                yield cylinder;
            }
            case "CONE" -> {
                MeshView cone = createCone();
                cone.setScaleX(width);
                cone.setScaleY(height);
                cone.setScaleZ(depth);
                yield cone;
            }
            case "RING" -> {
                MeshView ring = createRing();
                ring.setScaleX(width);
                ring.setScaleY(height);
                ring.setScaleZ(depth);
                yield ring;
            }
            default -> new Box(width, height, depth);
        };
        SemanticMaterial semanticMaterial = SemanticMaterial.from(part.material);
        Color color;
        try {
            color = Color.web(part.color == null ? semanticMaterial.baseColor() : part.color);
        } catch (IllegalArgumentException ex) {
            color = Color.web(semanticMaterial.baseColor());
        }
        PhongMaterial material = new PhongMaterial(color);
        semanticMaterial.applyTo(material, part.finish);
        shape.setMaterial(material);
        Group wrapper = new Group(shape);
        wrapper.setTranslateX(part.x);
        wrapper.setTranslateY(-part.y);
        wrapper.setTranslateZ(part.z);
        wrapper.getTransforms().addAll(
                new Rotate(part.rotateX, Rotate.X_AXIS),
                new Rotate(part.rotateY, Rotate.Y_AXIS),
                new Rotate(part.rotateZ, Rotate.Z_AXIS));
        wrapper.setUserData(part);
        return wrapper;
    }

    private static MeshView createCone() {
        int segments = 24;
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        List<Float> points = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();
        points.addAll(List.of(0f, -0.5f, 0f));
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            points.add((float) (Math.cos(angle) * 0.5));
            points.add(0.5f);
            points.add((float) (Math.sin(angle) * 0.5));
        }
        int center = segments + 1;
        points.add(0f);
        points.add(0.5f);
        points.add(0f);
        mesh.getTexCoords().setAll(0, 0);
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            addTriangle(faces, 0, i + 1, next + 1);
            addTriangle(faces, center, next + 1, i + 1);
        }
        mesh.getPoints().setAll(toFloatArray(points));
        mesh.getFaces().setAll(toIntArray(faces));
        MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        return view;
    }

    private static MeshView createRing() {
        int segments = 28;
        int sides = 10;
        double majorRadius = 0.34;
        double tubeRadius = 0.16;
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        List<Float> points = new ArrayList<>(segments * sides * 3);
        List<Integer> faces = new ArrayList<>(segments * sides * 12);
        for (int i = 0; i < segments; i++) {
            double around = Math.PI * 2 * i / segments;
            for (int j = 0; j < sides; j++) {
                double tube = Math.PI * 2 * j / sides;
                double radius = majorRadius + tubeRadius * Math.cos(tube);
                points.add((float) (radius * Math.cos(around)));
                points.add((float) (tubeRadius * Math.sin(tube)));
                points.add((float) (radius * Math.sin(around)));
            }
        }
        mesh.getTexCoords().setAll(0, 0);
        for (int i = 0; i < segments; i++) {
            int nextI = (i + 1) % segments;
            for (int j = 0; j < sides; j++) {
                int nextJ = (j + 1) % sides;
                int a = i * sides + j;
                int b = nextI * sides + j;
                int c = nextI * sides + nextJ;
                int d = i * sides + nextJ;
                addTriangle(faces, a, b, c);
                addTriangle(faces, a, c, d);
            }
        }
        mesh.getPoints().setAll(toFloatArray(points));
        mesh.getFaces().setAll(toIntArray(faces));
        MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        return view;
    }

    private static void addTriangle(List<Integer> faces, int a, int b, int c) {
        faces.add(a); faces.add(0);
        faces.add(b); faces.add(0);
        faces.add(c); faces.add(0);
    }

    private static float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }
}
