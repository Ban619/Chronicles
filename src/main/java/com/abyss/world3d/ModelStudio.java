package com.abyss.world3d;

import com.abyss.world.Tile;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** JavaFX primitive-model editor for creating props used by the 3D map renderer. */
public final class ModelStudio {
    private static final Path COMPONENT_DIRECTORY = Path.of("engine", "components");
    private static final int MAX_HISTORY = 100;
    private static Stage activeStage;

    private final Perspective3DEngine engine;
    private final Stage stage = new Stage();
    private final CustomModelDefinition definition = new CustomModelDefinition();
    private final BorderPane root = new BorderPane();
    private final Group previewRoot = new Group();
    private final Group partRoot = new Group();
    private final ListView<CustomModelDefinition.Part> partList = new ListView<>();
    private final ListView<TemplateEntry> templateList = new ListView<>();
    private final TextField modelName = new TextField("Custom Model");
    private final TextField[] values = new TextField[9];
    private final TextField partName = new TextField();
    private final javafx.scene.control.ColorPicker colorPicker = new javafx.scene.control.ColorPicker(Color.web("#8b8e83"));
    private final ComboBox<TileOption> tilePicker = new ComboBox<>();
    private final ComboBox<String> shapePicker = new ComboBox<>();
    private final ComboBox<String> materialPicker = new ComboBox<>();
    private final ComboBox<String> finishPicker = new ComboBox<>();
    private final ToggleButton brushToggle = new ToggleButton("Paint brush: off");
    private final Button inspectorOpenButton = new Button("Component Inspector");
    private final Button viewportModeButton = new Button("Expand viewport");
    private final Button inspectorRevealButton = new Button("Open Component Inspector");
    private final Slider brushRadius = new Slider(1, 80, 24);
    private final Label brushRadiusLabel = new Label("Brush radius: 24");
    private final Label statusLabel = new Label("Ready");
    private final ObservableList<CustomModelDefinition.Part> parts = FXCollections.observableArrayList();
    private final Deque<EditorSnapshot> undoHistory = new ArrayDeque<>();
    private final Deque<EditorSnapshot> redoHistory = new ArrayDeque<>();
    private final MenuItem undoMenuItem = new MenuItem("Undo");
    private final MenuItem redoMenuItem = new MenuItem("Redo");
    private final Rotate yaw = new Rotate(-25, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(-18, Rotate.X_AXIS);
    private double dragX;
    private double dragY;
    private double cameraDistance = 190;
    private Path currentFile;
    private int nextPartNumber = 1;
    private boolean syncingInspector;
    private boolean restoringHistory;
    private boolean viewportOnly;
    private boolean inspectorOpen = true;
    private double toolboxDividerPosition = 0.17;
    private double inspectorDividerPosition = 0.78;
    private double explorerDividerPosition = 0.43;
    private double dragStartX;
    private double dragStartY;
    private SubScene preview;
    private StackPane viewport;
    private javafx.scene.control.SplitPane workspace;
    private VBox inspector;
    private VBox toolbox;
    private HBox ribbonCommands;
    private Button undoRibbonButton;
    private Button redoRibbonButton;
    private String activeRibbon = "Home";

    private ModelStudio(Perspective3DEngine engine) {
        this.engine = engine;
        definition.parts = parts;
        definition.parts.add(newPart("BOX"));
        tilePicker.setItems(FXCollections.observableArrayList(tileOptions()));
        tilePicker.getSelectionModel().selectFirst();
        tilePicker.setPrefWidth(170);
        buildWindow();
        refreshPartList(0);
        refreshPreview();
    }

    public static void open(Perspective3DEngine engine) {
        if (activeStage != null && activeStage.isShowing()) {
            activeStage.toFront();
            return;
        }
        ModelStudio studio = new ModelStudio(engine);
        activeStage = studio.stage;
        studio.stage.setOnHidden(event -> {
            activeStage = null;
            engine.reloadCustomModels();
        });
        studio.stage.show();
    }

    private void buildWindow() {
        stage.setTitle("Abyss Model Studio");
        root.setStyle("-fx-background-color: #151a1c;");
        root.setTop(new VBox(buildMenuBar(), buildRibbon()));
        toolbox = buildToolbox();
        viewport = buildViewport();
        inspector = buildInspector();
        workspace = new javafx.scene.control.SplitPane(toolbox, viewport, inspector);
        workspace.setDividerPositions(0.21, 0.79);
        workspace.setMinSize(0, 0);
        root.setCenter(workspace);
        root.setBottom(statusLabel);
        statusLabel.setPadding(new Insets(5, 10, 5, 10));
        statusLabel.setStyle("-fx-text-fill: #b8c5bd; -fx-background-color: #202729;");
        Scene scene = new Scene(root, 1420, 860, true);
        stage.setScene(scene);
        scene.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.S) {
                if (event.isShiftDown()) saveComponent();
                else saveModel();
                event.consume();
            } else if (event.isControlDown()
                    && (event.getCode() == javafx.scene.input.KeyCode.Z
                    || event.getCode() == javafx.scene.input.KeyCode.Y)
                    && !(scene.getFocusOwner() instanceof TextField)) {
                if (event.getCode() == javafx.scene.input.KeyCode.Y || event.isShiftDown()) redo();
                else undo();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.O) {
                openModel();
                event.consume();
                } else if (event.getCode() == javafx.scene.input.KeyCode.DELETE
                    && !(scene.getFocusOwner() instanceof TextField)) {
                removeSelectedPart();
                event.consume();
                } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.D
                    && !(scene.getFocusOwner() instanceof TextField)) {
                duplicateSelectedPart();
                event.consume();
            } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && viewportOnly) {
                toggleViewportOnly(viewportModeButton);
                event.consume();
            }
        });
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.setMaximized(true);
    }

    private MenuBar buildMenuBar() {
        Menu file = new Menu("File");
        MenuItem open = new MenuItem("Open model...");
        open.setOnAction(event -> openModel());
        MenuItem save = new MenuItem("Save tile model...");
        save.setOnAction(event -> saveModel());
        MenuItem saveComponent = new MenuItem("Save reusable component...");
        saveComponent.setOnAction(event -> saveComponent());
        file.getItems().addAll(open, save, saveComponent);

        Menu edit = new Menu("Edit");
        undoMenuItem.setOnAction(event -> undo());
        redoMenuItem.setOnAction(event -> redo());
        undoMenuItem.setDisable(true);
        redoMenuItem.setDisable(true);
        MenuItem duplicate = new MenuItem("Duplicate component");
        duplicate.setOnAction(event -> duplicateSelectedPart());
        MenuItem remove = new MenuItem("Remove component");
        remove.setOnAction(event -> removeSelectedPart());
        edit.getItems().addAll(undoMenuItem, redoMenuItem, new SeparatorMenuItem(), duplicate, remove);

        Menu view = new Menu("View");
        MenuItem expand = new MenuItem("Toggle expanded viewport");
        expand.setOnAction(event -> toggleViewportOnly(viewportModeButton));
        MenuItem showInspector = new MenuItem("Show Component Inspector");
        showInspector.setOnAction(event -> setInspectorOpen(true));
        view.getItems().addAll(expand, showInspector);

        Menu tools = new Menu("Tools");
        MenuItem addComponent = new MenuItem("Import reusable component...");
        addComponent.setOnAction(event -> importComponent());
        tools.getItems().add(addComponent);

        Menu help = new Menu("Help");
        MenuItem about = new MenuItem("About Abyss Studio");
        about.setOnAction(event -> info("Abyss Studio", "Abyss Model Studio · 3D tile and component editor"));
        help.getItems().add(about);
        MenuBar menuBar = new MenuBar(file, edit, view, tools, help);
        menuBar.setStyle("-fx-background-color: #202528; -fx-padding: 2 8 2 8;");
        return menuBar;
    }

    private VBox buildRibbon() {
        Label title = new Label("ABYSS STUDIO");
        title.setStyle("-fx-text-fill: #e4d9bf; -fx-font-size: 13px; -fx-font-weight: bold;");
        modelName.setPromptText("Model name");
        modelName.setPrefColumnCount(16);
        ToggleGroup tabs = new ToggleGroup();
        HBox tabRow = new HBox(2);
        for (String tab : List.of("Home", "Model", "Materials", "Assets")) {
            javafx.scene.control.ToggleButton button = new javafx.scene.control.ToggleButton(tab);
            button.setToggleGroup(tabs);
            button.setOnAction(event -> showRibbon(tab));
            button.setStyle("-fx-background-radius: 0; -fx-padding: 7 16 7 16;");
            tabRow.getChildren().add(button);
            if (tab.equals("Home")) button.setSelected(true);
        }
        ribbonCommands = new HBox(8);
        ribbonCommands.setAlignment(Pos.CENTER_LEFT);
        showRibbon(activeRibbon);

        inspectorOpenButton.setTooltip(new javafx.scene.control.Tooltip("Open the Component Inspector dock"));
        inspectorOpenButton.setOnAction(event -> setInspectorOpen(true));
        viewportModeButton.setOnAction(event -> toggleViewportOnly(viewportModeButton));
        HBox identity = new HBox(10, title, modelName);
        identity.setAlignment(Pos.CENTER_LEFT);
        identity.setPadding(new Insets(6, 12, 6, 12));
        HBox context = new HBox(8, inspectorOpenButton, viewportModeButton);
        context.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(ribbonCommands, Priority.ALWAYS);
        HBox toolRow = new HBox(10, ribbonCommands, context);
        toolRow.setAlignment(Pos.CENTER_LEFT);
        toolRow.setPadding(new Insets(7, 12, 8, 12));
        VBox ribbon = new VBox(tabRow, new Separator(), identity, toolRow);
        ribbon.setStyle("-fx-background-color: #293033; -fx-border-color: #414b4d; -fx-border-width: 0 0 1 0;");
        return ribbon;
    }

    private void showRibbon(String tab) {
        activeRibbon = tab;
        ribbonCommands.getChildren().clear();
        switch (tab) {
            case "Model" -> {
                for (String shape : List.of("BOX", "SPHERE", "CYLINDER", "CONE", "RING")) {
                    Button add = new Button("＋ " + shape);
                    add.setOnAction(event -> addPart(shape));
                    ribbonCommands.getChildren().add(add);
                }
                ribbonCommands.getChildren().addAll(new Label("Tile binding"), tilePicker);
            }
            case "Materials" -> ribbonCommands.getChildren().addAll(
                    new Label("Preset"), materialPicker, new Label("Finish"), finishPicker,
                    new Label("Color"), colorPicker, brushToggle);
            case "Assets" -> {
                Button importButton = new Button("Import component");
                importButton.setOnAction(event -> importComponent());
                Button refreshButton = new Button("Refresh toolbox");
                refreshButton.setOnAction(event -> refreshTemplates());
                Button saveButton = new Button("Save component");
                saveButton.setOnAction(event -> saveComponent());
                ribbonCommands.getChildren().addAll(importButton, refreshButton, saveButton);
            }
            default -> {
                undoRibbonButton = new Button("Undo");
                undoRibbonButton.setOnAction(event -> undo());
                redoRibbonButton = new Button("Redo");
                redoRibbonButton.setOnAction(event -> redo());
                Button openButton = new Button("Open model");
                openButton.setOnAction(event -> openModel());
                Button saveButton = new Button("Save model");
                saveButton.setOnAction(event -> saveModel());
                Button duplicateButton = new Button("Duplicate");
                duplicateButton.setOnAction(event -> duplicateSelectedPart());
                Button removeButton = new Button("Remove");
                removeButton.setOnAction(event -> removeSelectedPart());
                ribbonCommands.getChildren().addAll(undoRibbonButton, redoRibbonButton,
                        openButton, saveButton, saveComponentButton(),
                        duplicateButton, removeButton);
            }
        }
        ribbonCommands.getChildren().forEach(node -> node.setStyle(
                "-fx-background-color: #394245; -fx-text-fill: #e2e7e3; -fx-border-color: #586365;"));
        updateHistoryActions();
    }

    private Button saveComponentButton() {
        Button button = new Button("Save component");
        button.setOnAction(event -> saveComponent());
        return button;
    }

    private VBox buildToolbox() {
        templateList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TemplateEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name);
                setTooltip(empty || item == null ? null
                        : new javafx.scene.control.Tooltip(item.path.toString()));
            }
        });
        templateList.setOnDragDetected(event -> {
            TemplateEntry selected = templateList.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            javafx.scene.input.Dragboard board = templateList.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(selected.path.toString());
            board.setContent(content);
            event.consume();
        });
        Button createTab = new Button("Create");
        Button editTab = new Button("Edit");
        HBox modeTabs = new HBox(createTab, editTab);
        VBox modeContent = new VBox(8);
        modeContent.setPadding(new Insets(10));
        styleDockButton(createTab, true);
        styleDockButton(editTab, false);

        Button importButton = new Button("＋  Import Component");
        importButton.setMaxWidth(Double.MAX_VALUE);
        importButton.setOnAction(event -> importComponent());
        Button generate = new Button("✧  Generate Lantern");
        generate.setMaxWidth(Double.MAX_VALUE);
        generate.setOnAction(event -> generateLantern());
        Button clear = new Button("⌫  Clear Model");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setOnAction(event -> clearModel());
        FlowPane geometry = new FlowPane(6, 6);
        for (String shape : List.of("BOX", "SPHERE", "CYLINDER", "CONE", "RING")) {
            Button add = new Button(shape);
            add.setOnAction(event -> addPart(shape));
            geometry.getChildren().add(add);
        }
        VBox createContent = new VBox(7, importButton, generate, clear,
                new Label("ADD PRIMITIVE"), geometry);
        Button duplicate = new Button("Duplicate Selected");
        duplicate.setMaxWidth(Double.MAX_VALUE);
        duplicate.setOnAction(event -> duplicateSelectedPart());
        Button remove = new Button("Remove Selected");
        remove.setMaxWidth(Double.MAX_VALUE);
        remove.setOnAction(event -> removeSelectedPart());
        Button up = new Button("Move Up");
        up.setOnAction(event -> moveSelectedPart(-1));
        Button down = new Button("Move Down");
        down.setOnAction(event -> moveSelectedPart(1));
        HBox order = new HBox(6, up, down);
        VBox editContent = new VBox(7, duplicate, remove, new Label("DRAW ORDER"), order);
        modeContent.getChildren().setAll(createContent);
        createTab.setOnAction(event -> {
            modeContent.getChildren().setAll(createContent);
            styleDockButton(createTab, true);
            styleDockButton(editTab, false);
        });
        editTab.setOnAction(event -> {
            modeContent.getChildren().setAll(editContent);
            styleDockButton(createTab, false);
            styleDockButton(editTab, true);
        });

        VBox createDock = new VBox(new Label("TERRAIN EDITOR"), modeTabs, modeContent);
        createDock.setStyle("-fx-background-color: #202729; -fx-border-color: #343d3e; -fx-border-width: 0 0 1 0;");
        VBox.setVgrow(modeContent, Priority.ALWAYS);

        Button refresh = new Button("↻ Refresh");
        refresh.setOnAction(event -> refreshTemplates());
        BorderPane toolboxHeader = new BorderPane();
        toolboxHeader.setLeft(new Label("TOOLBOX"));
        toolboxHeader.setRight(refresh);
        toolboxHeader.setPadding(new Insets(7, 10, 7, 10));
        Label hint = new Label("Drag a model or component into the viewport to add its parts.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #94a098; -fx-font-size: 11px;");
        VBox assetDock = new VBox(toolboxHeader, templateList, hint);
        assetDock.setPadding(new Insets(4));
        VBox.setVgrow(templateList, Priority.ALWAYS);

        VBox pane = new VBox(createDock, assetDock);
        pane.setPrefWidth(280);
        pane.setMinWidth(0);
        pane.setStyle("-fx-background-color: #202729; -fx-border-color: #343d3e; -fx-border-width: 0 1 0 0;");
        VBox.setVgrow(assetDock, Priority.ALWAYS);
        refreshTemplates();
        return pane;
    }

    private void styleDockButton(Button button, boolean selected) {
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(selected
                ? "-fx-background-color: #38474a; -fx-text-fill: #f1dfb6; -fx-border-color: #72a9ad; -fx-border-width: 0 0 2 0;"
                : "-fx-background-color: #252d2f; -fx-text-fill: #aeb8b4; -fx-border-color: #343d3e;");
    }

    private void generateLantern() {
        performEdit(() -> {
            parts.clear();
            CustomModelDefinition.Part base = newPart("CYLINDER");
            base.name = "Lantern Base";
            base.y = 4;
            base.width = 22;
            base.height = 5;
            base.depth = 22;
            base.material = "IRON";
            base.color = SemanticMaterial.IRON.baseColor();
            CustomModelDefinition.Part glass = newPart("CYLINDER");
            glass.name = "Glowing Glass";
            glass.height = 18;
            glass.width = 14;
            glass.depth = 14;
            glass.material = "GLASS";
            glass.finish = "EMISSIVE";
            glass.color = SemanticMaterial.FLAME.baseColor();
            CustomModelDefinition.Part cap = newPart("CONE");
            cap.name = "Lantern Cap";
            cap.y = -13;
            cap.width = 20;
            cap.height = 9;
            cap.depth = 20;
            cap.material = "IRON";
            cap.color = SemanticMaterial.IRON.baseColor();
            parts.addAll(base, glass, cap);
            modelName.setText("Lantern");
            tilePicker.getSelectionModel().selectFirst();
            refreshPartList(1);
            refreshPreview();
            statusLabel.setText("Generated a three-part lantern model");
        });
    }

    private void clearModel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove every component from the current model?");
        confirm.setTitle("Clear Model");
        confirm.setHeaderText("Clear the current model");
        confirm.initOwner(stage);
        if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                != javafx.scene.control.ButtonType.OK) return;
        performEdit(() -> {
            parts.clear();
            refreshPartList(-1);
            refreshPreview();
            statusLabel.setText("Model cleared");
        });
    }

    private void refreshTemplates() {
        List<TemplateEntry> entries = new ArrayList<>();
        addTemplatesFrom(CustomModelLibrary.MODEL_DIRECTORY, "Model");
        addTemplatesFrom(COMPONENT_DIRECTORY, "Component");
        try {
            if (Files.isDirectory(CustomModelLibrary.MODEL_DIRECTORY)) {
                try (var files = Files.list(CustomModelLibrary.MODEL_DIRECTORY)) {
                    files.filter(path -> path.toString().toLowerCase().endsWith(".json"))
                            .forEach(path -> entries.add(new TemplateEntry(path, "Model  ·  " + displayName(path))));
                }
            }
            if (Files.isDirectory(COMPONENT_DIRECTORY)) {
                try (var files = Files.list(COMPONENT_DIRECTORY)) {
                    files.filter(path -> path.toString().toLowerCase().endsWith(".json"))
                            .forEach(path -> entries.add(new TemplateEntry(path, "Component  ·  " + displayName(path))));
                }
            }
        } catch (IOException ex) {
            statusLabel.setText("Template scan failed: " + ex.getMessage());
        }
        entries.sort(java.util.Comparator.comparing(entry -> entry.name.toLowerCase()));
        templateList.setItems(FXCollections.observableArrayList(entries));
    }

    private void addTemplatesFrom(Path directory, String kind) {
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            statusLabel.setText(kind + " folder unavailable: " + ex.getMessage());
        }
    }

    private String displayName(Path path) {
        String name = path.getFileName().toString();
        int extension = name.lastIndexOf('.');
        return extension > 0 ? name.substring(0, extension) : name;
    }

    private void toggleViewportOnly(Button button) {
        if (!viewportOnly) rememberWorkspaceLayout();
        viewportOnly = !viewportOnly;
        updateWorkspaceItems();
        updateInspectorRevealButton();
        stage.setFullScreen(viewportOnly);
        button.setText(viewportOnly ? "Exit full viewport" : "Expand viewport");
        statusLabel.setText(viewportOnly ? "Viewport expanded · Escape exits fullscreen"
                : inspectorOpen ? "Full editor layout" : "Full editor layout · Inspector closed");
    }

    private void setInspectorOpen(boolean open) {
        if (open && viewportOnly) {
            viewportOnly = false;
            stage.setFullScreen(false);
            viewportModeButton.setText("Expand viewport");
        }
        rememberWorkspaceLayout();
        inspectorOpen = open;
        updateWorkspaceItems();
        updateInspectorRevealButton();
        statusLabel.setText(open ? "Component inspector opened" : "Component inspector closed");
    }

    private void updateInspectorRevealButton() {
        boolean show = !inspectorOpen;
        inspectorRevealButton.setVisible(show);
        inspectorRevealButton.setManaged(show);
    }

    private void revealInspector() {
        setInspectorOpen(true);
    }

    private void updateWorkspaceItems() {
        if (viewportOnly) {
            if (!workspace.getItems().equals(List.of(viewport))) {
                workspace.getItems().setAll(viewport);
            }
            return;
        }
        List<Node> editorPanes = List.of(toolbox, viewport, inspector);
        if (!workspace.getItems().equals(editorPanes)) {
            workspace.getItems().setAll(editorPanes);
        }
        if (inspectorOpen) {
            inspector.setMinWidth(0);
            inspector.setPrefWidth(330);
            workspace.setDividerPositions(toolboxDividerPosition, inspectorDividerPosition);
        } else {
            inspector.setMinWidth(0);
            inspector.setPrefWidth(0);
            workspace.setDividerPositions(toolboxDividerPosition, 1.0);
        }
    }

    private void rememberWorkspaceLayout() {
        if (workspace == null || viewportOnly || workspace.getDividers().isEmpty()) return;
        toolboxDividerPosition = workspace.getDividers().get(0).getPosition();
        if (inspectorOpen && workspace.getDividers().size() > 1) {
            inspectorDividerPosition = workspace.getDividers().get(1).getPosition();
        }
    }

    private StackPane buildViewport() {
        StackPane viewportPane = new StackPane();
        Group world = new Group();
        Box floor = new Box(180, 2, 180);
        floor.setTranslateY(1);
        floor.setMaterial(new PhongMaterial(Color.web("#313b37")));
        world.getChildren().add(floor);
        for (int offset = -80; offset <= 80; offset += 20) {
            Box xGrid = new Box(0.35, 0.2, 160);
            xGrid.setTranslateX(offset);
            xGrid.setTranslateY(-0.2);
            xGrid.setMaterial(new PhongMaterial(Color.color(0.56, 0.64, 0.57, 0.3)));
            Box zGrid = new Box(160, 0.2, 0.35);
            zGrid.setTranslateZ(offset);
            zGrid.setTranslateY(-0.2);
            zGrid.setMaterial(new PhongMaterial(Color.color(0.56, 0.64, 0.57, 0.3)));
            world.getChildren().addAll(xGrid, zGrid);
        }
        world.getChildren().addAll(partRoot, new AmbientLight(Color.color(0.7, 0.72, 0.7)));
        PointLight keyLight = new PointLight(Color.color(1.0, 0.89, 0.72));
        keyLight.setTranslateX(-80);
        keyLight.setTranslateY(-110);
        keyLight.setTranslateZ(-70);
        world.getChildren().add(keyLight);
        previewRoot.getChildren().add(world);
        previewRoot.getTransforms().setAll(yaw, pitch);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(2000);
        camera.setFieldOfView(42);
        camera.setTranslateY(-55);
        camera.setTranslateZ(-cameraDistance);

        preview = new SubScene(previewRoot, 800, 700, true,
                javafx.scene.SceneAntialiasing.BALANCED);
        preview.setFill(Color.web("#20292a"));
        preview.setCamera(camera);
        preview.widthProperty().bind(Bindings.max(1, viewportPane.widthProperty()));
        preview.heightProperty().bind(Bindings.max(1, viewportPane.heightProperty()));
        preview.setOnMousePressed(event -> {
            dragX = dragStartX = event.getSceneX();
            dragY = dragStartY = event.getSceneY();
        });
        preview.setOnMouseDragged(event -> {
            yaw.setAngle(yaw.getAngle() + (event.getSceneX() - dragX) * 0.7);
            pitch.setAngle(Math.max(-78, Math.min(12,
                    pitch.getAngle() + (event.getSceneY() - dragY) * 0.7)));
            dragX = event.getSceneX();
            dragY = event.getSceneY();
        });
        preview.setOnMouseClicked(event -> {
            if (!brushToggle.isSelected()) return;
            if (Math.hypot(event.getSceneX() - dragStartX, event.getSceneY() - dragStartY) > 4) return;
            paintPickedComponent(event.getPickResult().getIntersectedNode());
        });
        preview.setOnScroll(event -> {
            cameraDistance = Math.max(90, Math.min(440, cameraDistance - event.getDeltaY() * 0.35));
            camera.setTranslateZ(-cameraDistance);
        });
        viewportPane.setOnDragOver(event -> {
            if (event.getGestureSource() != viewportPane && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        viewportPane.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasString()) {
                try {
                    importTemplate(Path.of(event.getDragboard().getString()));
                    success = true;
                } catch (RuntimeException ex) {
                    statusLabel.setText("Could not add template: " + ex.getMessage());
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        Label hint = new Label("Drag to orbit   ·   Scroll to zoom   ·   Model origin is the tile center");
        hint.setStyle("-fx-text-fill: #a7b0a8; -fx-background-color: #171e20cc; -fx-padding: 8 12 8 12;");
        inspectorRevealButton.setOnAction(event -> revealInspector());
        inspectorRevealButton.setStyle("-fx-text-fill: #e4d9bf; -fx-background-color: #202729; -fx-border-color: #6f7d74; -fx-padding: 8 12 8 12;");
        inspectorRevealButton.setVisible(false);
        inspectorRevealButton.setManaged(false);
        viewportPane.getChildren().addAll(preview, hint, inspectorRevealButton);
        viewportPane.setMinSize(0, 0);
        StackPane.setAlignment(hint, Pos.BOTTOM_LEFT);
        StackPane.setMargin(hint, new Insets(12));
        StackPane.setAlignment(inspectorRevealButton, Pos.TOP_RIGHT);
        StackPane.setMargin(inspectorRevealButton, new Insets(12));
        return viewportPane;
    }

    private VBox buildInspector() {
        partList.setItems(parts);
        partList.setPrefWidth(230);
        partList.setMinHeight(70);
        partList.setStyle("-fx-control-inner-background: #171e20;"
                + "-fx-background-color: #171e20;"
                + "-fx-text-fill: #e4e9e5;"
                + "-fx-selection-bar: #078db8;"
                + "-fx-selection-bar-non-focused: #36545d;"
                + "-fx-focus-color: #168eb5;"
                + "-fx-faint-focus-color: transparent;");
        partList.setCellFactory(list -> new ListCell<>() {
            {
                selectedProperty().addListener((obs, wasSelected, isSelected) -> updateCellStyle());
            }

            @Override
            protected void updateItem(CustomModelDefinition.Part item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name + "  ·  " + item.shape);
                updateCellStyle();
            }

            private void updateCellStyle() {
                setStyle(isSelected()
                        ? "-fx-background-color: #078db8; -fx-text-fill: #ffffff;"
                        : "-fx-background-color: #202729; -fx-text-fill: #e4e9e5;");
            }
        });
        partList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> showPart(selected));

        Button remove = new Button("Remove");
        remove.setOnAction(event -> removeSelectedPart());
        Button duplicate = new Button("Duplicate");
        duplicate.setTooltip(new javafx.scene.control.Tooltip("Duplicate selected scene object"));
        duplicate.setOnAction(event -> duplicateSelectedPart());
        Button moveUp = new Button("Move up");
        moveUp.setTooltip(new javafx.scene.control.Tooltip("Move selected object up in draw order"));
        moveUp.setOnAction(event -> moveSelectedPart(-1));
        Button moveDown = new Button("Move down");
        moveDown.setTooltip(new javafx.scene.control.Tooltip("Move selected object down in draw order"));
        moveDown.setOnAction(event -> moveSelectedPart(1));
        remove.setTooltip(new javafx.scene.control.Tooltip("Remove selected scene object"));
        GridPane hierarchyActions = new GridPane();
        hierarchyActions.setHgap(6);
        hierarchyActions.setVgap(6);
        hierarchyActions.setMinHeight(0);
        hierarchyActions.setMaxWidth(Double.MAX_VALUE);
        Button[][] hierarchyButtons = {{duplicate, moveUp}, {moveDown, remove}};
        for (int row = 0; row < hierarchyButtons.length; row++) {
            for (int column = 0; column < hierarchyButtons[row].length; column++) {
                Button button = hierarchyButtons[row][column];
                button.setMaxWidth(Double.MAX_VALUE);
                button.setMinWidth(0);
                button.setStyle("-fx-background-color: #394245; -fx-text-fill: #e2e7e3;"
                        + "-fx-border-color: #586365; -fx-padding: 5 7 5 7;");
                GridPane.setHgrow(button, Priority.ALWAYS);
                hierarchyActions.add(button, column, row);
            }
        }
        for (int column = 0; column < 2; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(50);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setMinWidth(0);
            hierarchyActions.getColumnConstraints().add(constraints);
        }
        for (int row = 0; row < 2; row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setMinHeight(30);
            constraints.setPrefHeight(30);
            constraints.setMaxHeight(30);
            hierarchyActions.getRowConstraints().add(constraints);
        }

        GridPane transformGrid = new GridPane();
        transformGrid.setHgap(7);
        transformGrid.setVgap(5);
        for (int column = 0; column < 3; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setMinWidth(0);
            constraints.setPercentWidth(100.0 / 3);
            constraints.setHgrow(Priority.ALWAYS);
            transformGrid.getColumnConstraints().add(constraints);
        }
        String[] labels = {"X", "Y up", "Z", "Width", "Height", "Depth", "Rot X", "Rot Y", "Rot Z"};
        for (int i = 0; i < values.length; i++) {
            VBox field = new VBox(3, new Label(labels[i]), values[i] = new TextField());
            values[i].setPrefColumnCount(5);
            values[i].setMinWidth(0);
            values[i].setMaxWidth(Double.MAX_VALUE);
            field.setMinWidth(0);
            field.setMaxWidth(Double.MAX_VALUE);
            transformGrid.add(field, i % 3, i / 3);
        }

        Button apply = new Button("Apply part changes");
        apply.setMaxWidth(Double.MAX_VALUE);
        apply.setOnAction(event -> performEdit(() -> {
            applyFields();
            refreshPreview();
            refreshPartList(partList.getSelectionModel().getSelectedIndex());
        }));

        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.setOnAction(event -> {
            if (syncingInspector) return;
            CustomModelDefinition.Part selected = partList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                performEdit(() -> {
                    selected.color = colorPicker.getValue().toString();
                    refreshPreview();
                });
            }
        });
        partName.textProperty().addListener((obs, oldName, newName) -> {
            if (syncingInspector) return;
            CustomModelDefinition.Part selected = partList.getSelectionModel().getSelectedItem();
            if (selected != null && !newName.isBlank()) {
                performEdit(() -> {
                    selected.name = newName;
                    partList.refresh();
                });
            }
        });
        shapePicker.setOnAction(event -> {
            if (syncingInspector) return;
            CustomModelDefinition.Part selected = partList.getSelectionModel().getSelectedItem();
            if (selected != null && shapePicker.getValue() != null) {
                performEdit(() -> {
                    selected.shape = shapePicker.getValue();
                    refreshPartList(partList.getSelectionModel().getSelectedIndex());
                    refreshPreview();
                });
            }
        });
        finishPicker.setOnAction(event -> {
            if (syncingInspector) return;
            CustomModelDefinition.Part selected = partList.getSelectionModel().getSelectedItem();
            if (selected != null && finishPicker.getValue() != null) {
                performEdit(() -> {
                    selected.finish = finishPicker.getValue();
                    refreshPreview();
                });
            }
        });
        brushToggle.setOnAction(event -> {
            brushToggle.setText(brushToggle.isSelected() ? "Paint brush: on" : "Paint brush: off");
            statusLabel.setText(brushToggle.isSelected()
                    ? "Brush active · click a model surface to paint nearby components"
                    : "Brush disabled · drag viewport to orbit");
        });
        brushRadius.setShowTickMarks(true);
        brushRadius.setMajorTickUnit(20);
        brushRadius.valueProperty().addListener((obs, oldValue, newValue) ->
                brushRadiusLabel.setText("Brush radius: " + newValue.intValue()));
        FlowPane palette = new FlowPane(6, 6);
        for (String swatch : List.of("#805334", "#858985", "#925d49", "#525b60", "#d3a94f",
                "#8fcbd0", "#704331", "#467f43", "#ff9a3c", "#d7d0b9", "#573b2f", "#ded8cc")) {
            Button colorChip = new Button();
            colorChip.setPrefSize(25, 25);
            colorChip.setStyle("-fx-background-color: " + swatch + "; -fx-border-color: #bac2bd; -fx-border-width: 1;");
            colorChip.setTooltip(new javafx.scene.control.Tooltip(swatch));
            colorChip.setOnAction(event -> {
                colorPicker.setValue(Color.web(swatch));
                if (brushToggle.isSelected()) statusLabel.setText("Brush color selected: " + swatch);
            });
            palette.getChildren().add(colorChip);
        }

        shapePicker.setItems(FXCollections.observableArrayList("BOX", "SPHERE", "CYLINDER", "CONE", "RING"));
        shapePicker.setMinWidth(0);
        materialPicker.setItems(FXCollections.observableArrayList(
                java.util.Arrays.stream(SemanticMaterial.values()).map(SemanticMaterial::name).toList()));
        materialPicker.setMinWidth(0);
        materialPicker.setOnAction(event -> applySemanticMaterial());
        finishPicker.setItems(FXCollections.observableArrayList("ROUGH", "MATTE", "SATIN", "POLISHED", "GLASS", "EMISSIVE"));

        HBox finishControls = new HBox(8,
            new VBox(3, new Label("Shape"), shapePicker),
            new VBox(3, new Label("Finish"), finishPicker));
        HBox.setHgrow(shapePicker, Priority.ALWAYS);
        HBox.setHgrow(finishPicker, Priority.ALWAYS);

        Label partsLabel = new Label("SCENE OBJECTS");
        partsLabel.setStyle("-fx-text-fill: #d7dfd9; -fx-font-weight: bold;");
        Label transformLabel = new Label("SELECTED COMPONENT");
        Label surfaceLabel = new Label("SURFACE MATERIAL");
        VBox hierarchy = new VBox(8, partsLabel, partList, hierarchyActions);
        hierarchy.setPadding(new Insets(12));
        hierarchy.setMinSize(0, 190);
        hierarchy.setStyle("-fx-background-color: #202729; -fx-border-color: #343d3e; -fx-border-width: 0 0 1 0;");
        VBox.setVgrow(partList, Priority.ALWAYS);

        VBox inspectorContent = new VBox(9, transformLabel,
            new VBox(3, new Label("Component name"), partName), finishControls,
            new VBox(3, new Label("Semantic preset"), materialPicker),
            new Label("Brush palette"), palette, brushToggle, brushRadiusLabel, brushRadius,
            new Label("Base color / opacity"), colorPicker, transformGrid, apply,
            new Separator(), surfaceLabel,
            new Label("Preset sets a useful base color. Finish controls specular response."));
        inspectorContent.setPadding(new Insets(14));
        ScrollPane properties = new ScrollPane(inspectorContent);
        properties.setFitToWidth(true);
        properties.setStyle("-fx-background: #202729; -fx-background-color: #202729;");
        inspectorContent.setStyle("-fx-background-color: #202729;");
        Label dockTitle = new Label("EXPLORER");
        dockTitle.setStyle("-fx-text-fill: #e4d9bf; -fx-font-size: 11px; -fx-font-weight: bold;");
        Button closeInspector = new Button("×");
        closeInspector.setFocusTraversable(false);
        closeInspector.setTooltip(new javafx.scene.control.Tooltip("Close Explorer and Properties"));
        closeInspector.setStyle("-fx-text-fill: #c7cec8; -fx-font-size: 16px; -fx-background-color: transparent; -fx-padding: 0 5 0 5;");
        closeInspector.setOnAction(event -> setInspectorOpen(false));
        BorderPane dockTitleBar = new BorderPane();
        dockTitleBar.setLeft(dockTitle);
        dockTitleBar.setRight(closeInspector);
        dockTitleBar.setPadding(new Insets(5, 8, 5, 12));
        dockTitleBar.setStyle("-fx-background-color: #252d2f; -fx-border-color: #343d3e; -fx-border-width: 0 0 1 0;");

        Label propertiesTitle = new Label("PROPERTIES");
        propertiesTitle.setStyle("-fx-text-fill: #e4d9bf; -fx-font-size: 11px; -fx-font-weight: bold;"
            + "-fx-background-color: #252d2f; -fx-padding: 8 12 8 12;"
            + "-fx-border-color: #343d3e; -fx-border-width: 1 0 1 0;");
        properties.setMinSize(0, 0);
        VBox propertiesPane = new VBox(propertiesTitle, properties);
        propertiesPane.setMinSize(0, 0);
        VBox.setVgrow(properties, Priority.ALWAYS);

        javafx.scene.control.SplitPane explorerProperties =
                new javafx.scene.control.SplitPane(hierarchy, propertiesPane);
        explorerProperties.setOrientation(javafx.geometry.Orientation.VERTICAL);
        explorerProperties.setDividerPositions(explorerDividerPosition);
        explorerProperties.setMinSize(0, 0);
        explorerProperties.getDividers().get(0).positionProperty().addListener(
                (obs, oldPosition, newPosition) -> explorerDividerPosition = newPosition.doubleValue());

        VBox inspectorPane = new VBox(dockTitleBar, explorerProperties);
        inspectorPane.setPrefWidth(330);
        inspectorPane.setMinWidth(0);
        inspectorPane.setStyle("-fx-background-color: #202729; -fx-border-color: #343d3e; -fx-border-width: 0 0 0 1;");
        VBox.setVgrow(explorerProperties, Priority.ALWAYS);
        return inspectorPane;
    }

    private void addPart(String shape) {
        performEdit(() -> {
            applyFields();
            CustomModelDefinition.Part part = newPart(shape);
            parts.add(part);
            refreshPartList(parts.size() - 1);
            refreshPreview();
        });
    }

    private CustomModelDefinition.Part newPart(String shape) {
        CustomModelDefinition.Part part = new CustomModelDefinition.Part();
        part.shape = shape;
        part.name = shape.substring(0, 1) + shape.substring(1).toLowerCase() + " " + nextPartNumber++;
        if (shape.equals("SPHERE")) {
            part.width = 18;
            part.height = 18;
            part.depth = 18;
        } else if (shape.equals("CYLINDER")) {
            part.width = 10;
            part.height = 28;
            part.depth = 10;
        } else if (shape.equals("CONE")) {
            part.width = 20;
            part.height = 28;
            part.depth = 20;
        } else if (shape.equals("RING")) {
            part.width = 26;
            part.height = 8;
            part.depth = 26;
        }
        return part;
    }

    private void duplicateSelectedPart() {
        int index = partList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= parts.size()) return;
        performEdit(() -> {
            applyFields();
            CustomModelDefinition.Part copy = copyPart(parts.get(index));
            copy.name = parts.get(index).name + " Copy";
            copy.x += 4;
            copy.z += 4;
            parts.add(index + 1, copy);
            refreshPartList(index + 1);
            refreshPreview();
        });
    }

    private void moveSelectedPart(int offset) {
        int index = partList.getSelectionModel().getSelectedIndex();
        int target = index + offset;
        if (index < 0 || target < 0 || target >= parts.size()) return;
        performEdit(() -> {
            applyFields();
            CustomModelDefinition.Part part = parts.remove(index);
            parts.add(target, part);
            refreshPartList(target);
            refreshPreview();
        });
    }

    private void applySemanticMaterial() {
        if (syncingInspector) return;
        CustomModelDefinition.Part part = partList.getSelectionModel().getSelectedItem();
        String preset = materialPicker.getValue();
        if (part == null || preset == null) return;
        performEdit(() -> {
            SemanticMaterial semanticMaterial = SemanticMaterial.from(preset);
            part.material = semanticMaterial.name();
            if (semanticMaterial != SemanticMaterial.CUSTOM) {
                part.color = semanticMaterial.baseColor();
                part.finish = semanticMaterial.defaultFinish();
                finishPicker.setValue(part.finish);
            }
            showPart(part);
            refreshPreview();
        });
    }

    private void removeSelectedPart() {
        int index = partList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= parts.size()) return;
        performEdit(() -> {
            parts.remove(index);
            refreshPartList(Math.min(index, parts.size() - 1));
            refreshPreview();
        });
    }

    private void refreshPartList(int selectedIndex) {
        if (parts.isEmpty()) {
            partList.getSelectionModel().clearSelection();
            partList.refresh();
            return;
        }
        int safeIndex = Math.max(0, Math.min(selectedIndex, parts.size() - 1));
        partList.getSelectionModel().select(safeIndex);
        partList.refresh();
    }

    private void showPart(CustomModelDefinition.Part part) {
        if (part == null) return;
        syncingInspector = true;
        partName.setText(part.name);
        shapePicker.setValue(part.shape == null ? "BOX" : part.shape.toUpperCase());
        materialPicker.setValue(SemanticMaterial.from(part.material).name());
        finishPicker.setValue(part.finish == null ? "MATTE" : part.finish.toUpperCase());
        values[0].setText(number(part.x));
        values[1].setText(number(part.y));
        values[2].setText(number(part.z));
        values[3].setText(number(part.width));
        values[4].setText(number(part.height));
        values[5].setText(number(part.depth));
        values[6].setText(number(part.rotateX));
        values[7].setText(number(part.rotateY));
        values[8].setText(number(part.rotateZ));
        try { colorPicker.setValue(Color.web(part.color)); }
        catch (IllegalArgumentException ex) { colorPicker.setValue(Color.web("#8b8e83")); }
        syncingInspector = false;
    }

    private void applyFields() {
        CustomModelDefinition.Part part = partList.getSelectionModel().getSelectedItem();
        if (part == null) return;
        part.x = parse(values[0], part.x);
        part.y = parse(values[1], part.y);
        part.z = parse(values[2], part.z);
        part.width = Math.max(1, parse(values[3], part.width));
        part.height = Math.max(1, parse(values[4], part.height));
        part.depth = Math.max(1, parse(values[5], part.depth));
        part.rotateX = parse(values[6], part.rotateX);
        part.rotateY = parse(values[7], part.rotateY);
        part.rotateZ = parse(values[8], part.rotateZ);
        if (!partName.getText().isBlank()) part.name = partName.getText().trim();
        if (shapePicker.getValue() != null) part.shape = shapePicker.getValue();
        if (materialPicker.getValue() != null) part.material = materialPicker.getValue();
        if (finishPicker.getValue() != null) part.finish = finishPicker.getValue();
        part.color = colorPicker.getValue().toString();
    }

    private void refreshPreview() {
        applyFields();
        partRoot.getChildren().clear();
        for (CustomModelDefinition.Part part : parts) {
            partRoot.getChildren().add(CustomModelLibrary.createPartNode(part));
        }
    }

    private void paintPickedComponent(Node pickedNode) {
        Node current = pickedNode;
        while (current != null && current != partRoot
                && !(current.getUserData() instanceof CustomModelDefinition.Part)) {
            current = current.getParent();
        }
        if (current == null || !(current.getUserData() instanceof CustomModelDefinition.Part target)) {
            statusLabel.setText("Pick a model surface to paint");
            return;
        }

        performEdit(() -> {
            double radius = brushRadius.getValue();
            String color = colorPicker.getValue().toString();
            SemanticMaterial semanticMaterial = SemanticMaterial.from(materialPicker.getValue());
            String material = semanticMaterial.name();
            String finish = finishPicker.getValue() == null
                    ? semanticMaterial.defaultFinish() : finishPicker.getValue();
            int painted = 0;
            for (CustomModelDefinition.Part part : parts) {
                double dx = part.x - target.x;
                double dy = part.y - target.y;
                double dz = part.z - target.z;
                if (part == target || dx * dx + dy * dy + dz * dz <= radius * radius) {
                    part.color = color;
                    part.material = material;
                    part.finish = finish;
                    painted++;
                }
            }
            showPart(target);
            refreshPreview();
            statusLabel.setText("Painted " + painted + " component(s) with " + color);
        });
    }

    private void importTemplate(Path path) {
        try {
            CustomModelDefinition template = CustomModelLibrary.read(path);
            if (template.parts == null || template.parts.isEmpty()) {
                statusLabel.setText("Template contains no components");
                return;
            }
            performEdit(() -> {
                int firstAdded = parts.size();
                for (CustomModelDefinition.Part source : template.parts) {
                    CustomModelDefinition.Part copy = copyPart(source);
                    copy.name = template.name + " / " + source.name;
                    parts.add(copy);
                }
                nextPartNumber = parts.size() + 1;
                refreshPartList(firstAdded);
                refreshPreview();
                statusLabel.setText("Added template: " + template.name);
            });
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private void saveModel() {
        applyFields();
        definition.name = modelName.getText().isBlank() ? "Custom Model" : modelName.getText().trim();
        TileOption tile = tilePicker.getValue();
        definition.tileId = tile == null ? 0 : tile.id;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save 3D model");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Model JSON", "*.json"));
        try {
            Files.createDirectories(CustomModelLibrary.MODEL_DIRECTORY);
            chooser.setInitialDirectory(CustomModelLibrary.MODEL_DIRECTORY.toAbsolutePath().toFile());
        } catch (IOException | IllegalArgumentException ignored) {
        }
        chooser.setInitialFileName(fileName(definition.name) + ".json");
        File selected = chooser.showSaveDialog(stage);
        if (selected == null) return;
        currentFile = selected.toPath();
        try {
            CustomModelLibrary.save(definition, currentFile);
            String usage = definition.tileId == 0
                    ? "It is available as a reusable preview/template."
                    : "The selected tile uses this model in 3D.";
            info("Model saved", "Saved " + currentFile.getFileName() + ". " + usage);
            engine.reloadCustomModels();
            refreshTemplates();
        } catch (IOException | IllegalArgumentException ex) {
            error("Could not save model", ex.getMessage());
        }
    }

    private void openModel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open 3D model");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Model JSON", "*.json"));
        if (Files.isDirectory(CustomModelLibrary.MODEL_DIRECTORY)) {
            chooser.setInitialDirectory(CustomModelLibrary.MODEL_DIRECTORY.toAbsolutePath().toFile());
        }
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) return;
        try {
            CustomModelDefinition loaded = CustomModelLibrary.read(selected.toPath());
            performEdit(() -> {
                definition.name = loaded.name;
                definition.tileId = loaded.tileId;
                parts.clear();
                parts.addAll(loaded.parts);
                modelName.setText(definition.name);
                for (TileOption option : tilePicker.getItems()) {
                    if (option.id == definition.tileId) {
                        tilePicker.getSelectionModel().select(option);
                        break;
                    }
                }
                currentFile = selected.toPath();
                nextPartNumber = parts.size() + 1;
                refreshPartList(parts.isEmpty() ? -1 : 0);
                refreshPreview();
            });
        } catch (IOException ex) {
            error("Could not open model", ex.getMessage());
        }
    }

    private void saveComponent() {
        applyFields();
        if (parts.isEmpty()) {
            error("Component is empty", "Add at least one mesh component before saving.");
            return;
        }
        definition.name = modelName.getText().isBlank() ? "Reusable Component" : modelName.getText().trim();
        definition.tileId = 0;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save reusable component");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Component JSON", "*.json"));
        try {
            Files.createDirectories(COMPONENT_DIRECTORY);
            chooser.setInitialDirectory(COMPONENT_DIRECTORY.toAbsolutePath().toFile());
        } catch (IOException | IllegalArgumentException ignored) {
        }
        chooser.setInitialFileName(fileName(definition.name) + ".json");
        File selected = chooser.showSaveDialog(stage);
        if (selected == null) return;
        try {
            CustomModelLibrary.save(definition, selected.toPath());
            info("Component saved", "Saved reusable component " + selected.getName() + " in engine/components.");
            refreshTemplates();
        } catch (IOException | IllegalArgumentException ex) {
            error("Could not save component", ex.getMessage());
        }
    }

    private void importComponent() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add reusable component");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Component JSON", "*.json"));
        if (Files.isDirectory(COMPONENT_DIRECTORY)) {
            chooser.setInitialDirectory(COMPONENT_DIRECTORY.toAbsolutePath().toFile());
        }
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) return;
        try {
            CustomModelDefinition component = CustomModelLibrary.read(selected.toPath());
            performEdit(() -> {
                int firstAdded = parts.size();
                for (CustomModelDefinition.Part source : component.parts) {
                    CustomModelDefinition.Part copy = copyPart(source);
                    copy.name = component.name + " / " + source.name;
                    parts.add(copy);
                }
                nextPartNumber = parts.size() + 1;
                refreshPartList(firstAdded);
                refreshPreview();
                modelName.requestFocus();
            });
        } catch (IOException ex) {
            error("Could not add component", ex.getMessage());
        }
    }

    private CustomModelDefinition.Part copyPart(CustomModelDefinition.Part source) {
        CustomModelDefinition.Part copy = new CustomModelDefinition.Part();
        copy.name = source.name;
        copy.shape = source.shape;
        copy.x = source.x;
        copy.y = source.y;
        copy.z = source.z;
        copy.width = source.width;
        copy.height = source.height;
        copy.depth = source.depth;
        copy.rotateX = source.rotateX;
        copy.rotateY = source.rotateY;
        copy.rotateZ = source.rotateZ;
        copy.color = source.color;
        copy.material = source.material;
        copy.finish = source.finish;
        return copy;
    }

    private void performEdit(Runnable edit) {
        if (restoringHistory) {
            edit.run();
            return;
        }
        EditorSnapshot before = snapshot();
        edit.run();
        if (sameModel(before, snapshot())) return;
        undoHistory.push(before);
        if (undoHistory.size() > MAX_HISTORY) undoHistory.removeLast();
        redoHistory.clear();
        updateHistoryActions();
    }

    private EditorSnapshot snapshot() {
        List<CustomModelDefinition.Part> copies = parts.stream().map(this::copyPart).toList();
        TileOption selectedTile = tilePicker.getValue();
        return new EditorSnapshot(modelName.getText(), selectedTile == null ? 0 : selectedTile.id,
                currentFile, copies, partList.getSelectionModel().getSelectedIndex());
    }

    private boolean sameModel(EditorSnapshot left, EditorSnapshot right) {
        if (!java.util.Objects.equals(left.name, right.name) || left.tileId != right.tileId
                || !java.util.Objects.equals(left.currentFile, right.currentFile)
                || left.selectedIndex != right.selectedIndex || left.parts.size() != right.parts.size()) {
            return false;
        }
        for (int i = 0; i < left.parts.size(); i++) {
            CustomModelDefinition.Part a = left.parts.get(i);
            CustomModelDefinition.Part b = right.parts.get(i);
            if (!java.util.Objects.equals(a.name, b.name)
                    || !java.util.Objects.equals(a.shape, b.shape)
                    || Double.compare(a.x, b.x) != 0 || Double.compare(a.y, b.y) != 0
                    || Double.compare(a.z, b.z) != 0 || Double.compare(a.width, b.width) != 0
                    || Double.compare(a.height, b.height) != 0 || Double.compare(a.depth, b.depth) != 0
                    || Double.compare(a.rotateX, b.rotateX) != 0
                    || Double.compare(a.rotateY, b.rotateY) != 0
                    || Double.compare(a.rotateZ, b.rotateZ) != 0
                    || !java.util.Objects.equals(a.color, b.color)
                    || !java.util.Objects.equals(a.material, b.material)
                    || !java.util.Objects.equals(a.finish, b.finish)) {
                return false;
            }
        }
        return true;
    }

    private void undo() {
        if (undoHistory.isEmpty()) return;
        redoHistory.push(snapshot());
        restore(undoHistory.pop());
        statusLabel.setText("Undo");
    }

    private void redo() {
        if (redoHistory.isEmpty()) return;
        undoHistory.push(snapshot());
        restore(redoHistory.pop());
        statusLabel.setText("Redo");
    }

    private void restore(EditorSnapshot snapshot) {
        restoringHistory = true;
        try {
            parts.clear();
            snapshot.parts.stream().map(this::copyPart).forEach(parts::add);
            modelName.setText(snapshot.name);
            definition.name = snapshot.name;
            definition.tileId = snapshot.tileId;
            currentFile = snapshot.currentFile;
            for (TileOption option : tilePicker.getItems()) {
                if (option.id == snapshot.tileId) {
                    tilePicker.getSelectionModel().select(option);
                    break;
                }
            }
            nextPartNumber = parts.size() + 1;
            refreshPartList(snapshot.selectedIndex);
            refreshPreview();
        } finally {
            restoringHistory = false;
        }
        updateHistoryActions();
    }

    private void updateHistoryActions() {
        boolean canUndo = !undoHistory.isEmpty();
        boolean canRedo = !redoHistory.isEmpty();
        if (undoMenuItem != null) undoMenuItem.setDisable(!canUndo);
        if (redoMenuItem != null) redoMenuItem.setDisable(!canRedo);
        if (undoRibbonButton != null) undoRibbonButton.setDisable(!canUndo);
        if (redoRibbonButton != null) redoRibbonButton.setDisable(!canRedo);
    }

    private record EditorSnapshot(String name, int tileId, Path currentFile,
                                  List<CustomModelDefinition.Part> parts, int selectedIndex) { }

    private List<TileOption> tileOptions() {
        return List.of(new TileOption("Preview only", 0),
                new TileOption("Stone wall", Tile.WALL_STONE),
                new TileOption("Brick wall", Tile.WALL_BRICK),
                new TileOption("Closed door", Tile.DOOR_CLOSED),
                new TileOption("Open door", Tile.DOOR_OPEN),
                new TileOption("Chest", Tile.CHEST),
                new TileOption("Opened chest", Tile.CHEST_OPEN),
                new TileOption("Sign", Tile.SIGN),
                new TileOption("Torch", Tile.TORCH),
                new TileOption("Tree trunk", Tile.TREE_BASE),
                new TileOption("Tree canopy", Tile.TREE_TOP));
    }

    private String fileName(String name) {
        String safe = name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return safe.isBlank() ? "custom_model" : safe;
    }

    private double parse(TextField field, double fallback) {
        try { return Double.parseDouble(field.getText()); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private String number(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void error(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Unknown error" : message);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private static final class TileOption {
        private final String label;
        private final int id;

        private TileOption(String label, int id) {
            this.label = label;
            this.id = id;
        }

        @Override
        public String toString() { return label; }
    }

    private static final class TemplateEntry {
        private final Path path;
        private final String name;

        private TemplateEntry(Path path, String name) {
            this.path = path;
            this.name = name;
        }
    }
}
