package com.abyss.world3d;

import com.abyss.entities.CompanionNPC;
import com.abyss.entities.Enemy;
import com.abyss.entities.Entity;
import com.abyss.entities.NPC;
import com.abyss.entities.PartyMember;
import com.abyss.entities.Player;
import com.abyss.world.DayNightCycle;
import com.abyss.world.GameMap;
import com.abyss.world.Tile;
import javafx.beans.value.ObservableValue;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class Perspective3DEngine {

    private final Group sceneRoot = new Group();
    private final Group groundRoot = new Group();
    private final Group objectRoot = new Group();
    private final Group overlayRoot = new Group();
    private final Group actorRoot = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate yawRotation = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pitchRotation = new Rotate(-22, Rotate.X_AXIS);
    private final AmbientLight ambientLight = new AmbientLight(Color.color(0.72, 0.76, 0.81));
    private final PointLight playerLight = new PointLight(Color.color(1.0, 0.82, 0.58));
    private final SubScene view;
    private final Map<Color, PhongMaterial> materials = new HashMap<>();
    private final Map<Integer, PhongMaterial> terrainMaterials = new HashMap<>();
    private final Map<Entity, Group> actorModels = new IdentityHashMap<>();
    private final CustomModelLibrary customModels = new CustomModelLibrary();

    private GameMap displayedMap;
    private int[][] displayedObjectIds;
    private Group[][] objectModels;
    private double yawDegrees;
    private double pitchDegrees;

    public Perspective3DEngine() {
        sceneRoot.getChildren().addAll(groundRoot, objectRoot, overlayRoot, actorRoot,
                ambientLight, playerLight);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);
        camera.setFieldOfView(58);
        camera.getTransforms().setAll(yawRotation, pitchRotation);

        view = new SubScene(sceneRoot, 960, 640, true, SceneAntialiasing.BALANCED);
        view.setFill(Color.web("#172126"));
        view.setCamera(camera);
        view.setVisible(false);
        view.setMouseTransparent(true);
    }

    public SubScene getView() {
        return view;
    }

    public void bindSize(ObservableValue<? extends Number> width,
                         ObservableValue<? extends Number> height) {
        view.widthProperty().bind(width);
        view.heightProperty().bind(height);
    }

    public void setEnabled(boolean enabled) {
        view.setVisible(enabled);
        view.setManaged(enabled);
    }

    public void render(GameMap map, Player player, DayNightCycle dayNight) {
        if (displayedMap != map) rebuildMap(map);
        refreshInteractiveObjects(map);
        refreshActors(map, player);
        updateTimeLighting(map, dayNight);

        double yaw = Math.toRadians(yawDegrees);
        double followDistance = 112;
        for (double distance = 16; distance < followDistance; distance += 8) {
            double testX = player.getCenterX() - Math.sin(yaw) * distance;
            double testZ = player.getCenterY() - Math.cos(yaw) * distance;
            if (map.isBlocked((float) testX - 3, (float) testZ - 3, 6, 6)) {
                followDistance = Math.max(16, distance - 8);
                break;
            }
        }
        camera.setTranslateX(player.getCenterX() - Math.sin(yaw) * followDistance);
        camera.setTranslateY(-82);
        camera.setTranslateZ(player.getCenterY() - Math.cos(yaw) * followDistance);
        playerLight.setTranslateX(player.getCenterX());
        playerLight.setTranslateY(-18);
        playerLight.setTranslateZ(player.getCenterY());
    }

    private void updateTimeLighting(GameMap map, DayNightCycle dayNight) {
        double daylight = dayNight.getDaylightLevel();
        boolean enclosed = map.getBiome() == GameMap.Biome.DUNGEON
                || map.getBiome() == GameMap.Biome.BOSS_ARENA;
        double brightness = enclosed ? 0.28 : 0.22 + daylight * 0.66;
        Color tint = switch (dayNight.getPhase()) {
            case DAWN -> Color.color(1.0, 0.76, 0.53);
            case DAY -> Color.color(0.87, 0.94, 1.0);
            case DUSK -> Color.color(1.0, 0.61, 0.38);
            case NIGHT -> Color.color(0.43, 0.53, 0.82);
        };
        ambientLight.setColor(Color.color(tint.getRed() * brightness,
                tint.getGreen() * brightness, tint.getBlue() * brightness));

        Color sky = switch (dayNight.getPhase()) {
            case DAWN -> Color.web("#b78163");
            case DAY -> Color.web("#718b91");
            case DUSK -> Color.web("#9c604c");
            case NIGHT -> Color.web("#101827");
        };
        view.setFill(sky);
        playerLight.setColor(daylight < 0.72 || enclosed
                ? Color.web("#ffd49a") : Color.web("#dce8ff"));
        playerLight.setLightOn(daylight < 0.78 || enclosed);
    }

    public void updateLook(double mouseDeltaX, double mouseDeltaY) {
        yawDegrees = (yawDegrees + mouseDeltaX * 0.14) % 360;
        pitchDegrees = Math.max(-12, Math.min(28, pitchDegrees + mouseDeltaY * 0.14));
        yawRotation.setAngle(yawDegrees);
        pitchRotation.setAngle(-22 - pitchDegrees);
    }

    public double getYawRadians() {
        return Math.toRadians(yawDegrees);
    }

    public void reloadCustomModels() {
        customModels.reload();
        if (displayedMap != null) rebuildMap(displayedMap);
    }

    private void rebuildMap(GameMap map) {
        groundRoot.getChildren().clear();
        objectRoot.getChildren().clear();
        overlayRoot.getChildren().clear();
        actorRoot.getChildren().clear();
        actorModels.clear();

        int rows = map.getRows();
        int cols = map.getCols();
        displayedObjectIds = new int[rows][cols];
        objectModels = new Group[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int groundId = map.getGroundTileId(row, col);
                Box ground = new Box(Tile.SIZE, 8, Tile.SIZE);
                ground.setTranslateX(col * Tile.SIZE + Tile.SIZE / 2.0);
                ground.setTranslateY(4);
                ground.setTranslateZ(row * Tile.SIZE + Tile.SIZE / 2.0);
                ground.setMaterial(terrainMaterial(groundId, col, row));
                groundRoot.getChildren().add(ground);

                int objectId = map.getObjectTileId(row, col);
                displayedObjectIds[row][col] = objectId;
                if (objectId != 0) {
                    Group model = createObjectModel(objectId, col, row);
                    objectModels[row][col] = model;
                    objectRoot.getChildren().add(model);
                }

                int overlayId = map.getOverlayTileId(row, col);
                if (overlayId != 0) {
                    Group model = createObjectModel(overlayId, col, row);
                    overlayRoot.getChildren().add(model);
                }
            }
        }
        displayedMap = map;
    }

    private void refreshInteractiveObjects(GameMap map) {
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                int objectId = map.getObjectTileId(row, col);
                if (objectId == displayedObjectIds[row][col]) continue;
                Group oldModel = objectModels[row][col];
                if (oldModel != null) objectRoot.getChildren().remove(oldModel);
                Group newModel = objectId == 0 ? null : createObjectModel(objectId, col, row);
                objectModels[row][col] = newModel;
                displayedObjectIds[row][col] = objectId;
                if (newModel != null) objectRoot.getChildren().add(newModel);
            }
        }
    }

    private Group createObjectModel(int id, int col, int row) {
        Group model = new Group();
        model.setTranslateX(col * Tile.SIZE + Tile.SIZE / 2.0);
        model.setTranslateZ(row * Tile.SIZE + Tile.SIZE / 2.0);
        Group customModel = customModels.createForTile(id);
        if (customModel != null) {
            model.getChildren().add(customModel);
            return model;
        }
        switch (id) {
            case Tile.WALL_STONE -> createMasonryWall(model, col, row, false);
            case Tile.WALL_BRICK -> createMasonryWall(model, col, row, true);
            case Tile.DOOR_CLOSED -> createDoor(model, false);
            case Tile.DOOR_OPEN -> createDoor(model, true);
            case Tile.CHEST -> createChest(model, false);
            case Tile.CHEST_OPEN -> createChest(model, true);
            case Tile.SIGN -> createSign(model);
            case Tile.TORCH -> createTorch(model, col, row);
            case Tile.TREE_BASE -> {
                createTreeTrunk(model, col, row);
            }
            case Tile.TREE_TOP -> {
                createTreeCanopy(model, col, row);
            }
            default -> addBox(model, 30, 24, 30, Color.web("#59636a"), 0, -12, 0);
        }
        return model;
    }

    private void createMasonryWall(Group model, int col, int row, boolean brick) {
        Color base = brick ? Color.web("#805742") : Color.web("#626d70");
        Color mortar = brick ? Color.web("#493a33") : Color.web("#41494c");
        addBox(model, 32, 42, 32, mortar, 0, -21, 0);

        Random shape = new Random(0x4D41534FL + col * 7919L + row * 104729L);
        for (int course = 0; course < 3; course++) {
            double centerY = -7 - course * 14;
            boolean split = (course + col + row) % 2 == 0;
            int stones = split ? 2 : 3;
            double stoneWidth = split ? 14.8 : 9.55;
            double gap = 1.0;
            double totalWidth = stones * stoneWidth + (stones - 1) * gap;
            double startX = -totalWidth / 2 + stoneWidth / 2;

            for (int stone = 0; stone < stones; stone++) {
                double x = startX + stone * (stoneWidth + gap);
                double y = centerY + (shape.nextDouble() - 0.5) * 0.8;
                double width = stoneWidth + (shape.nextDouble() - 0.5) * 0.8;
                double height = 12.8 + (shape.nextDouble() - 0.5) * 0.8;
                Color stoneColor = base.deriveColor(0, 1,
                        0.88 + shape.nextDouble() * 0.22, 1);
                addBeveledStone(model, width, height, 29.4, x, y, 0,
                        stoneColor, shape);
            }
        }
    }

    private void addBeveledStone(Group parent, double width, double height, double depth,
                                 double x, double y, double z, Color color, Random shape) {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        List<Float> points = new ArrayList<>(54);
        List<Integer> faces = new ArrayList<>(192);
        double bevel = Math.min(2.0, Math.min(width, height) * 0.18);
        double[] ringX = {-width / 2 + bevel, 0, width / 2 - bevel, width / 2,
                width / 2, width / 2 - bevel, -width / 2 + bevel, -width / 2};
        double[] ringY = {-height / 2, -height / 2, -height / 2, -height / 2 + bevel,
                height / 2 - bevel, height / 2, height / 2, height / 2 - bevel};

        points.add(0f);
        points.add(0f);
        points.add((float) (-depth / 2 - 0.65));
        for (int i = 0; i < 8; i++) {
            points.add((float) (ringX[i] + (shape.nextDouble() - 0.5) * 0.65));
            points.add((float) (ringY[i] + (shape.nextDouble() - 0.5) * 0.55));
            points.add((float) (-depth / 2 + (shape.nextDouble() - 0.5) * 0.9));
        }

        points.add(0f);
        points.add(0f);
        points.add((float) (depth / 2 + 0.45));
        for (int i = 0; i < 8; i++) {
            points.add((float) (ringX[i] + (shape.nextDouble() - 0.5) * 0.55));
            points.add((float) (ringY[i] + (shape.nextDouble() - 0.5) * 0.5));
            points.add((float) (depth / 2 + (shape.nextDouble() - 0.5) * 0.8));
        }

        for (int i = 0; i < 8; i++) {
            int next = (i + 1) % 8;
            addTriangle(faces, 0, 1 + i, 1 + next);
            addTriangle(faces, 9, 10 + next, 10 + i);
            addTriangle(faces, 1 + i, 10 + i, 10 + next);
            addTriangle(faces, 1 + i, 10 + next, 1 + next);
        }

        mesh.getPoints().setAll(toFloatArray(points));
        mesh.getTexCoords().setAll(0, 0);
        mesh.getFaces().setAll(toIntArray(faces));
        MeshView stone = new MeshView(mesh);
        stone.setCullFace(CullFace.NONE);
        stone.setTranslateX(x);
        stone.setTranslateY(y);
        stone.setTranslateZ(z);
        stone.setMaterial(material(color));
        parent.getChildren().add(stone);
    }

    private void addTriangle(List<Integer> faces, int first, int second, int third) {
        faces.add(first);
        faces.add(0);
        faces.add(second);
        faces.add(0);
        faces.add(third);
        faces.add(0);
    }

    private float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }

    private int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }

    private void createDoor(Group model, boolean open) {
        Color frame = Color.web("#593c2b");
        Color wood = Color.web("#98683e");
        Color plank = Color.web("#b17b49");
        Color iron = Color.web("#41494d");

        addBox(model, 4, 40, 8, frame, -14, -20, 0);
        addBox(model, 4, 40, 8, frame, 14, -20, 0);
        addBox(model, 32, 4, 8, frame, 0, -40, 0);
        addBox(model, 24, 3, 7, wood, 0, -1.5, 0);

        if (open) {
            addBox(model, 23, 34, 1.5, Color.web("#17191a"), 0, -21, -2.5);
            addBox(model, 21, 32, 2, Color.web("#302a24"), 8, -21, -4);
            return;
        }

        addBox(model, 23, 34, 4, wood, 0, -21, -1.5);
        for (int x = -7; x <= 7; x += 7) {
            addBox(model, 1.1, 31, 0.6, plank, x, -21, -3.8);
        }
        addBox(model, 22, 2, 0.9, iron, 0, -10, -4);
        addBox(model, 22, 2, 0.9, iron, 0, -32, -4);
        addBox(model, 3, 7, 1.5, Color.web("#d2ad5b"), 8, -20, -4.8);
        addBox(model, 1, 1, 1, Color.web("#f0d27b"), 8, -20, -5.7);
    }

    private void createChest(Group model, boolean open) {
        Color body = open ? Color.web("#65462f") : Color.web("#80532f");
        Color wood = Color.web("#b17a42");
        Color iron = Color.web("#43494a");
        addBox(model, 24, 14, 21, body, 0, -8, 0);
        addBox(model, 21, 9, 19, wood, 0, -7, -1);
        addBox(model, 25, 2, 22, iron, 0, -14, 0);
        addBox(model, 3, 12, 1.5, iron, 0, -8, -11.5);
        addBox(model, 4, 5, 2, Color.web("#dfbd62"), 0, -9, -12.2);

        if (open) {
            Box lid = new Box(22, 4, 19);
            lid.setTranslateX(0);
            lid.setTranslateY(-20);
            lid.setTranslateZ(3);
            lid.setRotationAxis(Rotate.X_AXIS);
            lid.setRotate(-28);
            lid.setMaterial(material(wood.brighter()));
            model.getChildren().add(lid);
            addBox(model, 14, 1, 9, Color.web("#e1bd69"), 0, -12, -3);
        } else {
            addBox(model, 24, 5, 21, wood, 0, -17, 0);
            addBox(model, 3, 2, 1.5, Color.web("#f1d179"), 0, -18, -11.2);
        }
    }

    private void createSign(Group model) {
        Cylinder post = new Cylinder(2.2, 25, 7);
        post.setTranslateY(-12.5);
        post.setMaterial(material(Color.web("#65452e")));
        model.getChildren().add(post);
        addBox(model, 27, 15, 4, Color.web("#785234"), 0, -25, -1);
        addBox(model, 23, 11, 3, Color.web("#bd8a50"), 0, -25, -3.2);
        addBox(model, 12, 1, 0.7, Color.web("#59432e"), 0, -22, -5);
        addBox(model, 8, 1, 0.7, Color.web("#59432e"), -2, -26, -5);
        addBox(model, 1.3, 8, 0.7, Color.web("#59432e"), -7, -25, -5);
    }

    private void createTorch(Group model, int col, int row) {
        Color wood = Color.web("#62452d");
        Cylinder handle = new Cylinder(2, 19, 7);
        handle.setTranslateY(-10);
        handle.setMaterial(material(wood));
        model.getChildren().add(handle);

        Cylinder bracket = new Cylinder(1.5, 9, 6);
        bracket.setTranslateX(-4);
        bracket.setTranslateY(-6);
        bracket.setTranslateZ(-3);
        bracket.setRotationAxis(Rotate.Z_AXIS);
        bracket.setRotate(90);
        bracket.setMaterial(material(Color.web("#42494b")));
        model.getChildren().add(bracket);

        addBox(model, 7, 2, 5, Color.web("#454442"), 0, -19, 0);
        Sphere flame = new Sphere(5, 8);
        flame.setScaleX(0.72);
        flame.setScaleY(1.45);
        flame.setScaleZ(0.72);
        flame.setTranslateY(-25);
        flame.setMaterial(material(Color.web("#e96b2f")));
        model.getChildren().add(flame);

        Sphere core = new Sphere(3, 7);
        core.setScaleY(1.35);
        core.setTranslateY(-24);
        core.setMaterial(material(Color.web("#ffd36a")));
        model.getChildren().add(core);

        if (Math.floorMod(col * 3 + row, 2) == 0) {
            Sphere spark = new Sphere(1.2, 5);
            spark.setTranslateX(3);
            spark.setTranslateY(-31);
            spark.setMaterial(material(Color.web("#fff0ad")));
            model.getChildren().add(spark);
        }
    }

    private void createTreeTrunk(Group model, int col, int row) {
        Color bark = Color.web("#67452f");
        Color barkLight = Color.web("#98704a");
        Cylinder lowerTrunk = new Cylinder(4.4, 24, 7);
        lowerTrunk.setTranslateY(-12);
        lowerTrunk.setMaterial(material(bark));
        model.getChildren().add(lowerTrunk);

        Cylinder upperTrunk = new Cylinder(2.7, 21, 7);
        upperTrunk.setTranslateY(-31);
        upperTrunk.setMaterial(material(barkLight));
        model.getChildren().add(upperTrunk);

        addBranch(model, 1.7, 17, -4, -31, -42, 48, bark);
        addBranch(model, 1.6, 15, 4, -34, -42, -48, barkLight);
        addBranch(model, 1.3, 12, -2, -39, -48, 32, bark);
        addBranch(model, 1.2, 11, 2, -40, -48, -34, barkLight);

        int variant = Math.floorMod(col * 13 + row * 7, 3);
        if (variant == 0) {
            addBranch(model, 1.1, 10, -1, -25, -31, 35, bark);
        } else if (variant == 1) {
            addBranch(model, 1.1, 10, 1, -28, -34, -38, barkLight);
        }
    }

    private void addBranch(Group model, double radius, double length, double x, double y,
                           double z, double angle, Color bark) {
        Cylinder branch = new Cylinder(radius, length, 6);
        branch.setTranslateX(x);
        branch.setTranslateY(y);
        branch.setTranslateZ(z);
        branch.setRotationAxis(Rotate.Z_AXIS);
        branch.setRotate(angle);
        branch.setMaterial(material(bark));
        model.getChildren().add(branch);
    }

    private void createTreeCanopy(Group model, int col, int row) {
        int variant = Math.floorMod(col * 17 + row * 29, 5);
        double width = 0.9 + variant * 0.055;
        Color deepGreen = Color.web("#28563b");
        Color forestGreen = Color.web("#347548");
        Color leafGreen = Color.web("#4b8950");
        Color sunlitGreen = Color.web("#6a9a55");

        addFoliage(model, 0, -43, 0, 13.5 * width, 17, 13, deepGreen);
        addFoliage(model, -10, -36, -5, 11 * width, 13, 10, forestGreen);
        addFoliage(model, 10, -38, -3, 12 * width, 14, 11, leafGreen);
        addFoliage(model, -6, -52, 2, 11.5 * width, 13, 10, leafGreen);
        addFoliage(model, 7, -53, 4, 12 * width, 15, 11, forestGreen);
        addFoliage(model, 0, -63 - variant, 0, 9.5 * width, 12, 9, sunlitGreen);
        addFoliage(model, -14, -46, 3, 7.5 * width, 9, 8, deepGreen);
        addFoliage(model, 14, -47, -1, 8 * width, 10, 8, forestGreen);
        addFoliage(model, -4, -42, -13, 7 * width, 8, 7, sunlitGreen);
    }

    private void addFoliage(Group model, double x, double y, double z,
                            double radiusX, double radiusY, double radiusZ, Color color) {
        Sphere foliage = new Sphere(1, 10);
        foliage.setScaleX(radiusX);
        foliage.setScaleY(radiusY);
        foliage.setScaleZ(radiusZ);
        foliage.setTranslateX(x);
        foliage.setTranslateY(y);
        foliage.setTranslateZ(z);
        foliage.setMaterial(material(color));
        model.getChildren().add(foliage);
    }

    private void refreshActors(GameMap map, Player player) {
        List<Entity> entities = new ArrayList<>();
        entities.add(player);
        entities.addAll(map.getNpcs());
        entities.addAll(map.getEnemies());
        entities.addAll(player.getParty());
        Set<Entity> visibleEntities = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Entity entity : entities) {
            visibleEntities.add(entity);
            Group model = actorModels.computeIfAbsent(entity, this::createActorModel);
            model.setVisible(entity.isActive() && entity.isVisible());
            model.setTranslateX(entity.getCenterX());
            model.setTranslateZ(entity.getCenterY());
            if (entity == player) {
                model.setRotationAxis(Rotate.Y_AXIS);
                model.setRotate(Math.toDegrees(Math.atan2(player.getFacingX(), player.getFacingY())));
            }
            if (!actorRoot.getChildren().contains(model)) actorRoot.getChildren().add(model);
        }

        actorModels.forEach((entity, model) -> {
            if (!visibleEntities.contains(entity)) model.setVisible(false);
        });
    }

    private Group createActorModel(Entity entity) {
        if (entity instanceof Player) return createPlayerModel();

        Color color;
        if (entity instanceof Enemy) {
            color = entity.getName().toLowerCase().contains("malachar")
                    ? Color.web("#782e3c") : Color.web("#bd4e43");
        } else if (entity instanceof CompanionNPC) {
            color = Color.web("#58a879");
        } else if (entity instanceof NPC npc) {
            color = switch (npc.getType()) {
                case MERCHANT -> Color.web("#d5a64e");
                case GUARD -> Color.web("#547bb1");
                case QUEST_GIVER -> Color.web("#9b70b1");
                case HEALER -> Color.web("#62a879");
                case BLACKSMITH -> Color.web("#bd6250");
                default -> Color.web("#a38c6b");
            };
        } else if (entity instanceof PartyMember) {
            color = Color.web("#64b7bc");
        } else {
            color = Color.web("#a38c6b");
        }

        Group model = new Group();
        addBox(model, 17, 23, 13, color, 0, -15, 0);
        Sphere head = new Sphere(7);
        head.setTranslateY(-32);
        head.setMaterial(material(color.brighter()));
        model.getChildren().add(head);
        return model;
    }

    private Group createPlayerModel() {
        Group model = new Group();
        Color coat = Color.web("#34383d");
        Color trousers = Color.web("#292b30");
        Color boots = Color.web("#57402f");
        Color hair = Color.web("#858b8e");
        Color skin = Color.web("#c99771");

        addBox(model, 17, 21, 13, coat, 0, -19, 0);
        addBox(model, 18, 3, 14, Color.web("#a9433c"), 0, -10, 0);
        addBox(model, 5, 17, 7, coat.brighter(), -11, -19, 0);
        addBox(model, 5, 17, 7, coat.brighter(), 11, -19, 0);
        addBox(model, 6, 13, 7, trousers, -4, -3, 0);
        addBox(model, 6, 13, 7, trousers, 4, -3, 0);
        addBox(model, 7, 4, 10, boots, -4, -1, -1);
        addBox(model, 7, 4, 10, boots, 4, -1, -1);

        // A small trail pack and loose hair make the hero easy to read from behind.
        addBox(model, 10, 13, 6, Color.web("#654834"), 0, -21, -8);
        addBox(model, 3, 14, 2, Color.web("#a37a49"), 0, -21, -11.2);
        Sphere head = new Sphere(7);
        head.setTranslateY(-34);
        head.setMaterial(material(skin));
        model.getChildren().add(head);
        addBox(model, 15, 5, 14, hair, 0, -40, 0);
        addBox(model, 14, 8, 4, hair.darker(), 0, -34, -5.4);
        addBox(model, 4, 8, 5, hair, -7, -37, 1);
        addBox(model, 4, 9, 5, hair, 7, -38, 1);
        addBox(model, 3, 7, 4, hair.brighter(), 0, -43, 1);
        return model;
    }

    private void addBox(Group parent, double width, double height, double depth,
                        Color color, double x, double y, double z) {
        Box box = new Box(width, height, depth);
        box.setMaterial(material(color));
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        parent.getChildren().add(box);
    }

    private PhongMaterial material(Color color) {
        return materials.computeIfAbsent(color, PhongMaterial::new);
    }

    private PhongMaterial terrainMaterial(int id, int col, int row) {
        int variation = Math.floorMod(col * 17 + row * 31, 5);
        int key = id * 8 + variation;
        return terrainMaterials.computeIfAbsent(key, ignored ->
                createTerrainMaterial(id, variation, terrainColor(id, col, row)));
    }

    private PhongMaterial createTerrainMaterial(int id, int variation, Color base) {
        PhongMaterial terrain = new PhongMaterial(base);
        terrain.setDiffuseMap(createTerrainTexture(id, variation, base));
        terrain.setSpecularColor(id == Tile.WATER ? Color.color(0.35, 0.52, 0.58)
                : id == Tile.SNOW ? Color.color(0.22, 0.24, 0.26) : Color.color(0.08, 0.08, 0.07));
        terrain.setSpecularPower(id == Tile.WATER ? 24 : 8);
        return terrain;
    }

    private WritableImage createTerrainTexture(int id, int variation, Color base) {
        int size = 64;
        WritableImage texture = new WritableImage(size, size);
        PixelWriter pixels = texture.getPixelWriter();
        Random random = new Random(0x5EEDL + id * 101L + variation * 997L);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double shade = 0.88 + random.nextDouble() * 0.20;
                if (id == Tile.WATER) {
                    double ripple = Math.sin((x * 0.18 + y * 0.11 + variation) * 2.1);
                    if (ripple > 0.82) shade += 0.18;
                    else if (ripple < -0.88) shade -= 0.10;
                } else if (id == Tile.LAVA) {
                    double vein = Math.sin(x * 0.19 + Math.sin(y * 0.14 + variation) * 2.0);
                    if (Math.abs(vein) < 0.09) shade += 0.65;
                    else if (vein < -0.75) shade *= 0.72;
                } else if (id == Tile.STONE || id == Tile.FLOOR_STONE) {
                    if (x < 2 || y < 2 || x > size - 3 || y > size - 3) shade *= 0.76;
                    if ((x + y * 3 + variation * 7) % 29 == 0) shade += 0.16;
                } else if (id == Tile.GRASS) {
                    if ((x * 7 + y * 13 + variation * 17) % 43 < 3) shade += 0.19;
                    if ((x * 11 + y * 5 + variation) % 47 < 2) shade *= 0.74;
                } else if (id == Tile.DIRT || id == Tile.SAND || id == Tile.SNOW) {
                    if ((x * 5 + y * 17 + variation * 11) % 37 < 3) shade += 0.17;
                } else if (id == Tile.FLOOR_WOOD || id == Tile.BRIDGE) {
                    if (x % 16 < 2) shade *= 0.74;
                    else if (y % 11 == 2) shade += 0.12;
                }
                pixels.setColor(x, y, Color.color(
                        clamp(base.getRed() * shade),
                        clamp(base.getGreen() * shade),
                        clamp(base.getBlue() * shade)));
            }
        }
        return texture;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private Color terrainColor(int id, int col, int row) {
        Color base = switch (id) {
            case Tile.GRASS -> Color.web("#426746");
            case Tile.DIRT -> Color.web("#806044");
            case Tile.STONE, Tile.FLOOR_STONE -> Color.web("#697379");
            case Tile.WATER -> Color.web("#286784");
            case Tile.LAVA -> Color.web("#a33a24");
            case Tile.SAND -> Color.web("#c6ae68");
            case Tile.SNOW -> Color.web("#dce8e7");
            case Tile.FLOOR_WOOD, Tile.BRIDGE -> Color.web("#87613f");
            default -> Color.web("#414c50");
        };
        int variation = Math.floorMod(col * 17 + row * 31, 5) - 2;
        return base.deriveColor(0, 1, 1 + variation * 0.035, 1);
    }
}
