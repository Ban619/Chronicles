package com.abyss.world3d;

import com.abyss.world.Tile;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
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
import javafx.scene.shape.Rectangle;
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
import java.util.Set;
import java.util.UUID;

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
    private final Group previewWorld = new Group();
    private final Group partRoot = new Group();
    private final TreeView<CustomModelDefinition.Part> partList = new TreeView<>();
    private final ListView<TemplateEntry> templateList = new ListView<>();
    private final TextField modelName = new TextField("Custom Model");
    private final TextField hierarchySearch = new TextField();
    private final TextField[] values = new TextField[9];
    private final TextField partName = new TextField();
    private final javafx.scene.control.ColorPicker colorPicker = new javafx.scene.control.ColorPicker(Color.web("#8b8e83"));
    private final ComboBox<TileOption> tilePicker = new ComboBox<>();
    private final ComboBox<String> shapePicker = new ComboBox<>();
    private final ComboBox<String> materialPicker = new ComboBox<>();
    private final ComboBox<String> finishPicker = new ComboBox<>();
    private VBox selectedProperties;
    private Label inspectorSelectionLabel;
    private final ToggleButton brushToggle = new ToggleButton("Brush");
    private final ToggleButton boxSelectToggle = new ToggleButton("Box");
    private final Button inspectorOpenButton = new Button("Component Inspector");
    private final Button toolboxOpenButton = new Button("Create / Assets");
    private final Button viewportModeButton = new Button("Expand viewport");
    private final Button inspectorRevealButton = new Button("Open Component Inspector");
    private final Slider brushRadius = new Slider(1, 80, 24);
    private final Label brushRadiusLabel = new Label("Brush radius: 24");
    private final Label statusLabel = new Label("Ready");
    private final Rectangle selectionMarquee = new Rectangle();
    private final Canvas transformGizmo = new Canvas();
    private final ObservableList<CustomModelDefinition.Part> parts = FXCollections.observableArrayList();
    private final Deque<EditorSnapshot> undoHistory = new ArrayDeque<>();
    private final Deque<EditorSnapshot> redoHistory = new ArrayDeque<>();
    private final Set<String> hiddenPartIds = new java.util.HashSet<>();
    private final MenuItem undoMenuItem = new MenuItem("Undo");
    private final MenuItem redoMenuItem = new MenuItem("Redo");
    private final Rotate yaw = new Rotate(-25, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(-18, Rotate.X_AXIS);
    private double dragX;
    private double dragY;
    private double cameraDistance = 190;
    private double selectionStartX;
    private double selectionStartY;
    private Path currentFile;
    private int nextPartNumber = 1;
    private boolean syncingInspector;
    private boolean restoringHistory;
    private boolean rebuildingPartTree;
    private boolean viewportOnly;
    private boolean boxSelecting;
    private boolean boxSelectionAdditive;
    private boolean viewportNavigationDragging;
    private boolean panningViewport;
    private boolean gizmoDragging;
    private boolean inspectorOpen = true;
    private boolean toolboxOpen;
    private String selectedPartId;
    private TransformOperation transformOperation;
    private String transformAxis = "X";
    private final StringBuilder transformInput = new StringBuilder();
    private Point2D gizmoDragOrigin;
    private Point2D gizmoDragDirection;
    private EditorSnapshot gizmoDragSnapshot;
    private List<TransformStart> gizmoDragStarts = List.of();
    private double gizmoAxisPixels;
    private double inspectorDividerPosition = 0.82;
    private double explorerDividerPosition = 0.24;
    private double dragStartX;
    private double dragStartY;
    private SubScene preview;
    private PerspectiveCamera previewCamera;
    private StackPane viewport;
    private javafx.scene.control.SplitPane workspace;
    private VBox inspector;
    private VBox toolbox;
    private BorderPane toolboxOverlay;
    private HBox ribbonCommands;
    private Button undoRibbonButton;
    private Button redoRibbonButton;
    private Button moveToolButton;
    private Button rotateToolButton;
    private Button scaleToolButton;
    private String activeRibbon = "Layout";
    private Label frameLabel;

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
        frameAll();
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
        workspace = new javafx.scene.control.SplitPane(viewport, inspector);
        workspace.setDividerPositions(inspectorDividerPosition);
        workspace.setMinSize(0, 0);
        root.setCenter(workspace);
        root.setBottom(new VBox(buildTimeline(), statusLabel));
        statusLabel.setPadding(new Insets(5, 10, 5, 10));
        statusLabel.setStyle("-fx-text-fill: #b8c5bd; -fx-background-color: #202729;");
        Scene scene = new Scene(root, 1510, 900, true);
        stage.setScene(scene);
        scene.setOnKeyPressed(event -> {
            if (transformOperation != null && handleTransformKey(event)) return;
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.A
                    && !(scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl)) {
                selectVisibleParts();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.I
                    && !(scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl)) {
                invertVisibleSelection();
                event.consume();
            } else if (event.isAltDown() && !event.isControlDown()
                    && event.getCode() == javafx.scene.input.KeyCode.A
                    && !(scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl)) {
                clearStudioSelection();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.S) {
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
            } else if (event.getCode() == javafx.scene.input.KeyCode.F
                    && !(scene.getFocusOwner() instanceof TextField)) {
                if (event.isShiftDown()) frameAll();
                else frameSelected();
                event.consume();
            } else if (!(scene.getFocusOwner() instanceof TextField)
                    && event.isAltDown() && event.getCode() == javafx.scene.input.KeyCode.H) {
                showAllComponents();
                event.consume();
            } else if (!(scene.getFocusOwner() instanceof TextField)
                    && !event.isAltDown() && event.getCode() == javafx.scene.input.KeyCode.H) {
                toggleSelectedVisibility();
                event.consume();
            } else if (!(scene.getFocusOwner() instanceof TextField)
                    && (event.getCode() == javafx.scene.input.KeyCode.G
                    || event.getCode() == javafx.scene.input.KeyCode.R
                    || event.getCode() == javafx.scene.input.KeyCode.S)) {
                beginTransform(switch (event.getCode()) {
                    case G -> TransformOperation.MOVE;
                    case R -> TransformOperation.ROTATE;
                    default -> TransformOperation.SCALE;
                });
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

    private VBox buildTimeline() {
        Label title = new Label("TIMELINE");
        title.setStyle("-fx-text-fill: #dce1dc; -fx-font-size: 10px; -fx-font-weight: bold;");
        Button firstFrame = new Button("|◀");
        Button previousFrame = new Button("◀");
        Button play = new Button("▶");
        Button nextFrame = new Button("▶|");
        for (Button button : List.of(firstFrame, previousFrame, play, nextFrame)) {
            button.setDisable(true);
            button.setTooltip(new javafx.scene.control.Tooltip("Animation playback is not available yet."));
        }
        Label staticPose = new Label("Static pose · animation not available");
        staticPose.setStyle("-fx-text-fill: #929b98; -fx-font-size: 10px;");
        HBox controls = new HBox(6, title, firstFrame, previousFrame, play, nextFrame, staticPose);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(4, 10, 2, 10));

        Slider frame = new Slider(1, 250, 1);
        frame.setShowTickMarks(true);
        frame.setMajorTickUnit(24);
        frame.setMinorTickCount(3);
        frame.setBlockIncrement(1);
        frame.setDisable(true);
        frameLabel = new Label("1");
        frameLabel.setStyle("-fx-text-fill: #dce1dc; -fx-background-color: #394245; -fx-padding: 3 8 3 8;");
        HBox ruler = new HBox(10, new Label("Current frame"), frameLabel, frame);
        ruler.setAlignment(Pos.CENTER_LEFT);
        ruler.setPadding(new Insets(0, 10, 5, 10));
        HBox.setHgrow(frame, Priority.ALWAYS);

        VBox timeline = new VBox(1, controls, ruler);
        timeline.setPrefHeight(58);
        timeline.setMinHeight(58);
        timeline.setStyle("-fx-background-color: #252b2d; -fx-border-color: #41494b;"
                + "-fx-border-width: 1 0 0 0;");
        return timeline;
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
        MenuItem showToolbox = new MenuItem("Toggle Create / Assets shelf");
        showToolbox.setOnAction(event -> toggleToolbox());
        MenuItem frameAll = new MenuItem("Frame all components (Shift+F)");
        frameAll.setOnAction(event -> frameAll());
        MenuItem frameSelection = new MenuItem("Frame selected components (Shift+F)");
        frameSelection.setOnAction(event -> frameSelected());
        MenuItem hideSelection = new MenuItem("Hide selected components (H)");
        hideSelection.setOnAction(event -> toggleSelectedVisibility());
        MenuItem showHidden = new MenuItem("Show all hidden components (Alt+H)");
        showHidden.setOnAction(event -> showAllComponents());
        view.getItems().addAll(expand, showInspector, showToolbox,
                new SeparatorMenuItem(), frameSelection, frameAll,
                new SeparatorMenuItem(), hideSelection, showHidden);

        Menu render = new Menu("Render");
        MenuItem refreshPreview = new MenuItem("Refresh model preview");
        refreshPreview.setOnAction(event -> refreshPreview());
        render.getItems().add(refreshPreview);

        Menu window = new Menu("Window");
        MenuItem openAssets = new MenuItem("Create / Assets shelf");
        openAssets.setOnAction(event -> setToolboxOpen(true));
        MenuItem closeAssets = new MenuItem("Close Create / Assets shelf");
        closeAssets.setOnAction(event -> setToolboxOpen(false));
        window.getItems().addAll(openAssets, closeAssets);

        Menu tools = new Menu("Tools");
        MenuItem addComponent = new MenuItem("Import reusable component...");
        addComponent.setOnAction(event -> importComponent());
        tools.getItems().add(addComponent);

        Menu help = new Menu("Help");
        MenuItem about = new MenuItem("About Abyss Studio");
        about.setOnAction(event -> info("Abyss Studio", "Abyss Model Studio · 3D tile and component editor"));
        help.getItems().add(about);
        MenuBar menuBar = new MenuBar(file, edit, render, view, window, tools, help);
        menuBar.setStyle("-fx-background-color: #202528; -fx-padding: 2 8 2 8;");
        return menuBar;
    }

    private VBox buildRibbon() {
        Label title = new Label("ABYSS STUDIO");
        title.setStyle("-fx-text-fill: #e4d9bf; -fx-font-size: 13px; -fx-font-weight: bold;");
        modelName.setPromptText("Model name");
        modelName.setPrefColumnCount(13);
        ToggleGroup tabs = new ToggleGroup();
        HBox tabRow = new HBox(2);
        Set<String> supportedWorkspaces = Set.of("Layout", "Modeling", "Shading", "Assets");
        for (String tab : List.of("Layout", "Modeling", "Sculpting", "UV Editing",
                "Texture Paint", "Shading", "Animation", "Rendering", "Compositing",
                "Geometry Nodes", "Assets")) {
            javafx.scene.control.ToggleButton button = new javafx.scene.control.ToggleButton(tab);
            if (supportedWorkspaces.contains(tab)) {
                button.setToggleGroup(tabs);
                button.setOnAction(event -> showRibbon(tab.equals("Shading") ? "Materials" : tab));
            } else {
                button.setDisable(true);
                button.setTooltip(new javafx.scene.control.Tooltip(
                        tab + " workspace is not available in this editor yet."));
            }
            button.setStyle(workspaceTabStyle(false));
            button.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                    button.setStyle(workspaceTabStyle(isSelected)));
            button.setStyle(button.isDisabled()
                    ? "-fx-background-color: transparent; -fx-text-fill: #697270; -fx-opacity: 0.65;"
                    : workspaceTabStyle(false));
            tabRow.getChildren().add(button);
            if (tab.equals("Layout")) button.setSelected(true);
        }
        ScrollPane workspaceScroller = new ScrollPane(tabRow);
        workspaceScroller.setFitToHeight(true);
        workspaceScroller.setFitToWidth(false);
        workspaceScroller.setPannable(true);
        workspaceScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        workspaceScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        workspaceScroller.setPrefViewportHeight(34);
        workspaceScroller.setMinWidth(0);
        workspaceScroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + "-fx-padding: 0; -fx-border-color: transparent;");
        ribbonCommands = new HBox(8);
        ribbonCommands.setAlignment(Pos.CENTER_LEFT);
        showRibbon(activeRibbon);

        inspectorOpenButton.setTooltip(new javafx.scene.control.Tooltip("Open the Component Inspector dock"));
        inspectorOpenButton.setOnAction(event -> setInspectorOpen(true));
        viewportModeButton.setOnAction(event -> toggleViewportOnly(viewportModeButton));
        HBox identity = new HBox(10, title, modelName, workspaceScroller);
        identity.setAlignment(Pos.CENTER_LEFT);
        identity.setPadding(new Insets(4, 10, 4, 10));
        HBox.setHgrow(workspaceScroller, Priority.ALWAYS);
        HBox context = new HBox(8, inspectorOpenButton, viewportModeButton);
        context.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(ribbonCommands, Priority.ALWAYS);
        HBox toolRow = new HBox(10, ribbonCommands, context);
        toolRow.setAlignment(Pos.CENTER_LEFT);
        toolRow.setPadding(new Insets(5, 10, 6, 10));
        VBox ribbon = new VBox(identity, new Separator(), toolRow);
        ribbon.setStyle("-fx-background-color: #293033; -fx-border-color: #414b4d; -fx-border-width: 0 0 1 0;");
        return ribbon;
    }

    private void showRibbon(String tab) {
        activeRibbon = tab;
        ribbonCommands.getChildren().clear();
        switch (tab) {
            case "Modeling" -> {
                for (String shape : List.of("BOX", "SPHERE", "CYLINDER", "CONE", "RING")) {
                    Button add = new Button("＋ " + shape);
                    add.setOnAction(event -> addPart(shape));
                    ribbonCommands.getChildren().add(add);
                }
                ribbonCommands.getChildren().addAll(new Label("Tile binding"), tilePicker);
            }
            case "Materials" -> ribbonCommands.getChildren().addAll(
                    new Label("Preset"), materialPicker, new Label("Finish"), finishPicker,
                    new Label("Color"), colorPicker);
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

    private String workspaceTabStyle(boolean selected) {
        return selected
                ? "-fx-background-color: #3a4548; -fx-text-fill: #f0dfbb;"
                + "-fx-border-color: #7aa4a6; -fx-border-width: 0 0 2 0; -fx-background-radius: 0;"
                + "-fx-padding: 7 15 7 15;"
                : "-fx-background-color: transparent; -fx-text-fill: #b7c0bd;"
                + "-fx-border-color: transparent; -fx-background-radius: 0; -fx-padding: 7 15 7 15;";
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
        pane.setPrefWidth(195);
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

    private void updateTransformToolStyles() {
        setTransformToolStyle(moveToolButton, transformOperation == TransformOperation.MOVE);
        setTransformToolStyle(rotateToolButton, transformOperation == TransformOperation.ROTATE);
        setTransformToolStyle(scaleToolButton, transformOperation == TransformOperation.SCALE);
    }

    private void setTransformToolStyle(Button button, boolean active) {
        if (button == null) return;
        button.setStyle(active
                ? "-fx-background-color: #3b555a; -fx-text-fill: #ffffff; -fx-border-color: #82b8b2;"
                    + "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 5 8 5 8;"
                : "-fx-background-color: #2a3437; -fx-text-fill: #edf2ef; -fx-border-color: #566264;"
                    + "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 5 8 5 8;");
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

    private void toggleToolbox() {
        setToolboxOpen(!toolboxOpen);
    }

    private void setToolboxOpen(boolean open) {
        toolboxOpen = open;
        if (toolbox != null) {
            toolbox.setVisible(open);
            toolbox.setManaged(open);
        }
        if (toolboxOverlay != null) {
            toolboxOverlay.setVisible(open);
            toolboxOverlay.setManaged(open);
        }
        toolboxOpenButton.setStyle(open
                ? "-fx-background-color: #3b555a; -fx-text-fill: #ffffff;"
                : "-fx-background-color: #30383a; -fx-text-fill: #e4e9e5;");
        statusLabel.setText(open ? "Create / Assets shelf opened" : "Create / Assets shelf closed");
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
        List<Node> editorPanes = List.of(viewport, inspector);
        if (!workspace.getItems().equals(editorPanes)) {
            workspace.getItems().setAll(editorPanes);
        }
        if (inspectorOpen) {
            inspector.setMinWidth(0);
            inspector.setPrefWidth(285);
            workspace.setDividerPositions(inspectorDividerPosition);
        } else {
            inspector.setMinWidth(0);
            inspector.setPrefWidth(0);
            workspace.setDividerPositions(1.0);
        }
    }

    private void rememberWorkspaceLayout() {
        if (workspace == null || viewportOnly || workspace.getDividers().isEmpty()) return;
        if (inspectorOpen) inspectorDividerPosition = workspace.getDividers().get(0).getPosition();
    }

    private StackPane buildViewport() {
        StackPane viewportPane = new StackPane();
        Group world = previewWorld;
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

        previewCamera = new PerspectiveCamera(true);
        previewCamera.setNearClip(0.1);
        previewCamera.setFarClip(2000);
        previewCamera.setFieldOfView(42);
        previewCamera.setTranslateY(-55);
        previewCamera.setTranslateZ(-cameraDistance);

        preview = new SubScene(previewRoot, 800, 700, true,
                javafx.scene.SceneAntialiasing.BALANCED);
        preview.setFill(Color.web("#20292a"));
        preview.setCamera(previewCamera);
        preview.widthProperty().bind(Bindings.max(1, viewportPane.widthProperty()));
        preview.heightProperty().bind(Bindings.max(1, viewportPane.heightProperty()));
        transformGizmo.widthProperty().bind(preview.widthProperty());
        transformGizmo.heightProperty().bind(preview.heightProperty());
        preview.widthProperty().addListener((obs, oldValue, newValue) -> drawTransformGizmo());
        preview.heightProperty().addListener((obs, oldValue, newValue) -> drawTransformGizmo());
        yaw.angleProperty().addListener((obs, oldValue, newValue) -> drawTransformGizmo());
        pitch.angleProperty().addListener((obs, oldValue, newValue) -> drawTransformGizmo());
        previewCamera.translateXProperty().addListener((obs, oldValue, newValue) -> drawTransformGizmo());
        previewCamera.translateYProperty().addListener((obs, oldValue, newValue) -> drawTransformGizmo());
        previewCamera.translateZProperty().addListener((obs, oldValue, newValue) -> drawTransformGizmo());
        preview.setOnMousePressed(event -> {
            dragX = dragStartX = event.getSceneX();
            dragY = dragStartY = event.getSceneY();
            if (event.getButton() == MouseButton.MIDDLE) {
                viewportNavigationDragging = true;
                panningViewport = event.isShiftDown();
                event.consume();
            } else if (event.getButton() == MouseButton.PRIMARY && transformOperation != null
                    && !brushToggle.isSelected() && !boxSelectToggle.isSelected()
                    && beginGizmoDrag(event.getX(), event.getY())) {
                event.consume();
            } else if (event.getButton() == MouseButton.PRIMARY && boxSelectToggle.isSelected()
                    && !brushToggle.isSelected()) {
                boxSelecting = true;
                boxSelectionAdditive = event.isShiftDown();
                selectionStartX = event.getX();
                selectionStartY = event.getY();
                updateSelectionMarquee(selectionStartX, selectionStartY);
            }
        });
        preview.setOnMouseDragged(event -> {
            if (gizmoDragging) {
                updateGizmoDrag(event.getX(), event.getY());
                event.consume();
                return;
            }
            if (boxSelecting) {
                updateSelectionMarquee(event.getX(), event.getY());
                event.consume();
                return;
            }
            double deltaX = event.getSceneX() - dragX;
            double deltaY = event.getSceneY() - dragY;
            if (viewportNavigationDragging && panningViewport) {
                double worldUnitsPerPixel = 2 * cameraDistance
                        * Math.tan(Math.toRadians(previewCamera.getFieldOfView()) / 2)
                        / Math.max(1, preview.getHeight());
                previewCamera.setTranslateX(previewCamera.getTranslateX() - deltaX * worldUnitsPerPixel);
                previewCamera.setTranslateY(previewCamera.getTranslateY() - deltaY * worldUnitsPerPixel);
            } else if (viewportNavigationDragging) {
                yaw.setAngle(yaw.getAngle() + deltaX * 0.7);
                pitch.setAngle(Math.max(-78, Math.min(12,
                        pitch.getAngle() + deltaY * 0.7)));
            }
            dragX = event.getSceneX();
            dragY = event.getSceneY();
            if (viewportNavigationDragging) event.consume();
        });
        preview.setOnMouseReleased(event -> {
            if (gizmoDragging) {
                finishGizmoDrag();
                event.consume();
                return;
            }
            if (viewportNavigationDragging) {
                viewportNavigationDragging = false;
                panningViewport = false;
                event.consume();
                return;
            }
            if (!boxSelecting) return;
            boxSelecting = false;
            double endX = event.getX();
            double endY = event.getY();
            selectionMarquee.setVisible(false);
            if (Math.hypot(endX - selectionStartX, endY - selectionStartY) > 4) {
                selectPartsInRectangle(selectionStartX, selectionStartY, endX, endY,
                        boxSelectionAdditive);
            }
            event.consume();
        });
        preview.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            if (Math.hypot(event.getSceneX() - dragStartX, event.getSceneY() - dragStartY) > 4) return;
            Node pickedNode = event.getPickResult().getIntersectedNode();
            if (brushToggle.isSelected()) paintPickedComponent(pickedNode);
            else selectPartFromNode(pickedNode, event.isShiftDown());
        });
        preview.setOnScroll(event -> {
            cameraDistance = Math.max(10, Math.min(2500, cameraDistance - event.getDeltaY() * 0.35));
            previewCamera.setTranslateZ(-cameraDistance);
        });
        selectionMarquee.setFill(Color.color(0.15, 0.55, 0.95, 0.24));
        selectionMarquee.setStroke(Color.web("#55b9ff"));
        selectionMarquee.setStrokeWidth(1.5);
        selectionMarquee.setMouseTransparent(true);
        selectionMarquee.setManaged(false);
        selectionMarquee.setVisible(false);
        Button selectTool = new Button("Select");
        selectTool.setTooltip(new javafx.scene.control.Tooltip(
                "Click to select one object or Shift-click to add/toggle it. In Box mode, Shift-drag adds objects."));
        selectTool.setMaxWidth(Double.MAX_VALUE);
        selectTool.setOnAction(event -> {
            boxSelectToggle.setSelected(false);
            brushToggle.setSelected(false);
            statusLabel.setText("Select tool active · click an object or drag to orbit");
        });
        boxSelectToggle.setTooltip(new javafx.scene.control.Tooltip(
                "Drag a rectangle to select parts it touches, including occluded parts; Shift-drag adds to the selection."));
        boxSelectToggle.setOnAction(event -> {
            if (boxSelectToggle.isSelected() && brushToggle.isSelected()) {
                brushToggle.setSelected(false);
            }
            statusLabel.setText(boxSelectToggle.isSelected()
                    ? "Box select active · drag a rectangle across the viewport"
                    : "Box select disabled · drag viewport to orbit");
        });
        brushToggle.setTooltip(new javafx.scene.control.Tooltip("Paint the clicked part and nearby components."));
        brushToggle.setOnAction(event -> {
            if (brushToggle.isSelected() && boxSelectToggle.isSelected()) {
                boxSelectToggle.setSelected(false);
            }
            statusLabel.setText(brushToggle.isSelected()
                    ? "Brush active · click a model surface to paint nearby components"
                    : "Brush disabled · click to select or drag to orbit");
        });
        for (ToggleButton tool : List.of(boxSelectToggle, brushToggle)) {
            tool.setMaxWidth(Double.MAX_VALUE);
            tool.setStyle("-fx-background-color: #2a3437; -fx-text-fill: #edf2ef; -fx-border-color: #566264;"
                    + "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 5 8 5 8;");
        }
        moveToolButton = transformToolButton("Move", "G", TransformOperation.MOVE);
        rotateToolButton = transformToolButton("Rotate", "R", TransformOperation.ROTATE);
        scaleToolButton = transformToolButton("Scale", "S", TransformOperation.SCALE);
        toolboxOpenButton.setText("＋");
        toolboxOpenButton.setTooltip(new javafx.scene.control.Tooltip("Open Create / Assets shelf"));
        toolboxOpenButton.setMaxWidth(Double.MAX_VALUE);
        toolboxOpenButton.setOnAction(event -> toggleToolbox());
        toolboxOpenButton.setStyle("-fx-background-color: #2e383b; -fx-text-fill: #f3f5f3; -fx-border-color: #5a696d;"
                + "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6 0 6 0;");

        Label shelfLabel = new Label("TOOLS");
        shelfLabel.setStyle("-fx-text-fill: #9aa6a3; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 1.2px;");
        VBox toolShelf = new VBox(5, shelfLabel, toolboxOpenButton, selectTool,
                moveToolButton, rotateToolButton, scaleToolButton, boxSelectToggle, brushToggle);
        toolShelf.setPrefWidth(90);
        toolShelf.setMaxWidth(90);
        toolShelf.setPadding(new Insets(9, 8, 9, 8));
        toolShelf.setStyle("-fx-background-color: #202729cc; -fx-border-color: #465153;"
                + "-fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 10, 0.25, 0, 2);");

        Label mode = new Label("OBJECT MODE");
        mode.setStyle("-fx-text-fill: #e2e7e3; -fx-background-color: #353d3f;"
                + "-fx-padding: 6 10 6 10; -fx-border-color: #596365; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        Button viewButton = new Button("View");
        viewButton.setTooltip(new javafx.scene.control.Tooltip("Reset the viewport to the default perspective."));
        viewButton.setOnAction(event -> {
            yaw.setAngle(-25);
            pitch.setAngle(-18);
            frameAll();
        });
        Button frameButton = new Button("Frame Selected");
        frameButton.setTooltip(new javafx.scene.control.Tooltip("Fit selected scene objects in the viewport (F)."));
        frameButton.setOnAction(event -> frameSelected());
        Button frameAllButton = new Button("Frame All");
        frameAllButton.setTooltip(new javafx.scene.control.Tooltip("Fit every scene object in the viewport (Shift+F)."));
        frameAllButton.setOnAction(event -> frameAll());
        Button selectButton = new Button("Select");
        selectButton.setOnAction(event -> {
            boxSelectToggle.setSelected(false);
            brushToggle.setSelected(false);
            statusLabel.setText("Select tool active");
        });
        Button addButton = new Button("Add");
        addButton.setTooltip(new javafx.scene.control.Tooltip("Add the selected primitive to the model."));
        addButton.setOnAction(event -> addPart(shapePicker.getValue() == null ? "BOX" : shapePicker.getValue()));
        MenuItem duplicateObject = new MenuItem("Duplicate selected");
        duplicateObject.setOnAction(event -> duplicateSelectedPart());
        MenuItem removeObject = new MenuItem("Delete selected");
        removeObject.setOnAction(event -> removeSelectedPart());
        MenuButton objectMenu = new MenuButton("Object");
        objectMenu.getItems().addAll(duplicateObject, removeObject);
        HBox viewportHeader = new HBox(6, mode, viewButton, frameButton, frameAllButton,
                selectButton, addButton, objectMenu);
        viewportHeader.setAlignment(Pos.CENTER_LEFT);
        viewportHeader.setPadding(new Insets(5, 10, 5, 10));
        viewportHeader.setStyle("-fx-background-color: #292f31ee; -fx-border-color: #454d4f;"
                + "-fx-border-width: 0 0 1 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.15, 0, 2);");

        Button axisX = axisButton("X", "#d05b56", 90, 0);
        Button axisY = axisButton("Y", "#65a854", -25, -78);
        Button axisZ = axisButton("Z", "#548fc8", -25, 0);
        HBox axisGizmo = new HBox(4, axisX, axisY, axisZ);
        axisGizmo.setPadding(new Insets(6));
        axisGizmo.setStyle("-fx-background-color: #202729cc; -fx-background-radius: 4; -fx-border-color: #4f5c5f; -fx-border-width: 1; -fx-padding: 6; ");

        toolboxOverlay = new BorderPane();
        Label toolboxTitle = new Label("CREATE / ASSETS");
        toolboxTitle.setStyle("-fx-text-fill: #e4d9bf; -fx-font-weight: bold;");
        Button closeToolbox = new Button("×");
        closeToolbox.setOnAction(event -> setToolboxOpen(false));
        BorderPane toolboxBar = new BorderPane();
        toolboxBar.setLeft(toolboxTitle);
        toolboxBar.setRight(closeToolbox);
        toolboxBar.setPadding(new Insets(4, 6, 4, 8));
        toolboxBar.setStyle("-fx-background-color: #252d2f; -fx-border-color: #454d4f;");
        toolboxOverlay.setTop(toolboxBar);
        toolboxOverlay.setCenter(toolbox);
        toolboxOverlay.setPrefWidth(240);
        toolboxOverlay.setMaxWidth(240);
        toolboxOverlay.setMaxHeight(Double.MAX_VALUE);
        toolboxOverlay.setStyle("-fx-background-color: #202729; -fx-border-color: #536063;"
                + "-fx-border-width: 1; -fx-effect: dropshadow(gaussian, #00000099, 14, 0.18, 2, 2);");
        toolbox.setPrefWidth(240);
        toolbox.setMinWidth(0);
        toolbox.setVisible(false);
        toolbox.setManaged(false);
        toolboxOverlay.setVisible(false);
        toolboxOverlay.setManaged(false);
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

        Label hint = new Label("Ctrl+A select · Ctrl+I invert · Alt+A clear · Shift-click/box-drag add · MMB orbit · Shift+MMB pan");
        hint.setStyle("-fx-text-fill: #a7b0a8; -fx-background-color: #171e20cc; -fx-padding: 8 12 8 12;");
        inspectorRevealButton.setOnAction(event -> revealInspector());
        inspectorRevealButton.setStyle("-fx-text-fill: #e4d9bf; -fx-background-color: #202729; -fx-border-color: #6f7d74; -fx-padding: 8 12 8 12;");
        inspectorRevealButton.setVisible(false);
        inspectorRevealButton.setManaged(false);
        transformGizmo.setMouseTransparent(true);
        viewportPane.getChildren().addAll(preview, transformGizmo, selectionMarquee, viewportHeader, axisGizmo,
                toolShelf, toolboxOverlay, hint, inspectorRevealButton);
        viewportPane.setMinSize(0, 0);
        StackPane.setAlignment(hint, Pos.BOTTOM_LEFT);
        StackPane.setMargin(hint, new Insets(12));
        StackPane.setAlignment(selectionMarquee, Pos.TOP_LEFT);
        StackPane.setAlignment(toolShelf, Pos.CENTER_LEFT);
        StackPane.setMargin(toolShelf, new Insets(12));
        StackPane.setAlignment(viewportHeader, Pos.TOP_CENTER);
        StackPane.setMargin(viewportHeader, new Insets(0, 0, 0, 0));
        StackPane.setAlignment(axisGizmo, Pos.TOP_RIGHT);
        StackPane.setMargin(axisGizmo, new Insets(48, 12, 0, 0));
        StackPane.setAlignment(toolboxOverlay, Pos.TOP_LEFT);
        StackPane.setMargin(toolboxOverlay, new Insets(40, 0, 0, 88));
        StackPane.setAlignment(inspectorRevealButton, Pos.TOP_RIGHT);
        StackPane.setMargin(inspectorRevealButton, new Insets(12));
        return viewportPane;
    }

    private Button transformToolButton(String label, String shortcut, TransformOperation operation) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setTooltip(new javafx.scene.control.Tooltip(
                label + " selected scene objects. Shortcut: " + shortcut
                        + ", then choose X/Y/Z, enter a value, and press Enter."));
        button.setOnAction(event -> beginTransform(operation));
        button.setStyle("-fx-background-color: #2a3437; -fx-text-fill: #edf2ef;"
                + "-fx-border-color: #566264; -fx-background-radius: 6;"
                + "-fx-border-radius: 6; -fx-padding: 5 8 5 8;");
        return button;
    }

    private void drawTransformGizmo() {
        GraphicsContext graphics = transformGizmo.getGraphicsContext2D();
        double width = transformGizmo.getWidth();
        double height = transformGizmo.getHeight();
        graphics.clearRect(0, 0, width, height);
        if (transformOperation == null || width <= 0 || height <= 0) return;
        CustomModelDefinition.Part active = selectedPart();
        if (active == null || !isEffectivelyVisible(active)) return;
        Node partNode = active == null ? null : findPartNode(partRoot, active.id);
        if (partNode == null) return;

        Point3D sceneOrigin = partNode.localToScene(Point3D.ZERO);
        Point3D localOrigin = previewRoot.sceneToLocal(sceneOrigin);
        Point2D origin = projectPreviewPoint(localOrigin);
        if (origin == null) return;
        double worldLength = Math.max(2, cameraDistance * 0.14);
        drawGizmoAxis(graphics, origin, projectPreviewPoint(localOrigin.add(worldLength, 0, 0)), "X", "#ec625f");
        drawGizmoAxis(graphics, origin, projectPreviewPoint(localOrigin.add(0, -worldLength, 0)), "Y", "#6db85d");
        drawGizmoAxis(graphics, origin, projectPreviewPoint(localOrigin.add(0, 0, worldLength)), "Z", "#629fe0");
        graphics.setFill(Color.web("#f5f0d7"));
        graphics.fillOval(origin.getX() - 4, origin.getY() - 4, 8, 8);
    }

    private void drawGizmoAxis(GraphicsContext graphics, Point2D origin, Point2D endpoint,
                               String axis, String color) {
        if (endpoint == null) return;
        graphics.setStroke(Color.web(color));
        graphics.setLineWidth(axis.equals(transformAxis) ? 4 : 3);
        graphics.strokeLine(origin.getX(), origin.getY(), endpoint.getX(), endpoint.getY());
        double angle = Math.atan2(endpoint.getY() - origin.getY(), endpoint.getX() - origin.getX());
        double arrowLength = 10;
        graphics.strokeLine(endpoint.getX(), endpoint.getY(),
                endpoint.getX() - arrowLength * Math.cos(angle - Math.PI / 6),
                endpoint.getY() - arrowLength * Math.sin(angle - Math.PI / 6));
        graphics.strokeLine(endpoint.getX(), endpoint.getY(),
                endpoint.getX() - arrowLength * Math.cos(angle + Math.PI / 6),
                endpoint.getY() - arrowLength * Math.sin(angle + Math.PI / 6));
        graphics.setFill(Color.web(color));
        graphics.fillOval(endpoint.getX() - 8, endpoint.getY() - 8, 16, 16);
        graphics.setFill(Color.WHITE);
        graphics.fillText(axis, endpoint.getX() - 3, endpoint.getY() + 4);
    }

    private Point2D projectPreviewPoint(Point3D point) {
        Point3D cameraPoint = previewCamera.sceneToLocal(previewRoot.localToScene(point));
        if (cameraPoint.getZ() <= previewCamera.getNearClip()) return null;
        double verticalFov = previewCamera.isVerticalFieldOfView()
                ? previewCamera.getFieldOfView()
                : Math.toDegrees(2 * Math.atan(Math.tan(
                        Math.toRadians(previewCamera.getFieldOfView()) / 2)
                        * preview.getHeight() / Math.max(1, preview.getWidth())));
        double focalLength = preview.getHeight() / (2 * Math.tan(Math.toRadians(verticalFov) / 2));
        return new Point2D(preview.getWidth() / 2 + cameraPoint.getX() * focalLength / cameraPoint.getZ(),
                preview.getHeight() / 2 + cameraPoint.getY() * focalLength / cameraPoint.getZ());
    }

    private boolean beginGizmoDrag(double x, double y) {
        CustomModelDefinition.Part active = selectedPart();
        Node partNode = active == null || !isEffectivelyVisible(active)
                ? null : findPartNode(partRoot, active.id);
        if (partNode == null) return false;
        Point3D sceneOrigin = partNode.localToScene(Point3D.ZERO);
        Point3D localOrigin = previewRoot.sceneToLocal(sceneOrigin);
        Point2D origin = projectPreviewPoint(localOrigin);
        if (origin == null) return false;
        double worldLength = Math.max(2, cameraDistance * 0.14);
        Point2D[] endpoints = {
                projectPreviewPoint(localOrigin.add(worldLength, 0, 0)),
                projectPreviewPoint(localOrigin.add(0, -worldLength, 0)),
                projectPreviewPoint(localOrigin.add(0, 0, worldLength))
        };
        String[] axes = {"X", "Y", "Z"};
        double closestDistance = 13;
        int selectedAxis = -1;
        for (int i = 0; i < endpoints.length; i++) {
            Point2D endpoint = endpoints[i];
            if (endpoint == null) continue;
            double distance = endpoint.distance(x, y);
            if (distance < closestDistance) {
                closestDistance = distance;
                selectedAxis = i;
            }
        }
        if (selectedAxis < 0) return false;
        List<CustomModelDefinition.Part> roots = selectedRoots();
        if (roots.isEmpty()) return false;
        if (!validateFields()) return false;
        applyFields();
        transformAxis = axes[selectedAxis];
        transformInput.setLength(0);
        gizmoDragOrigin = new Point2D(x, y);
        Point2D delta = endpoints[selectedAxis].subtract(origin);
        gizmoAxisPixels = delta.magnitude();
        if (gizmoAxisPixels < 1) return false;
        gizmoDragDirection = delta.normalize();
        gizmoDragSnapshot = snapshot();
        gizmoDragStarts = roots.stream().map(TransformStart::from).toList();
        gizmoDragging = true;
        updateTransformToolStyles();
        drawTransformGizmo();
        return true;
    }

    private List<CustomModelDefinition.Part> selectedRoots() {
        List<CustomModelDefinition.Part> selected = selectedParts();
        return selected.stream()
                .filter(part -> selected.stream().noneMatch(candidate ->
                        candidate != part && isDescendant(candidate, part)))
                .toList();
    }

    private void updateGizmoDrag(double x, double y) {
        if (gizmoDragOrigin == null || gizmoDragDirection == null) return;
        Point2D mouseDelta = new Point2D(x, y).subtract(gizmoDragOrigin);
        double projectedDelta = mouseDelta.dotProduct(gizmoDragDirection);
        double amount;
        if (transformOperation == TransformOperation.MOVE) {
            double worldLength = Math.max(2, cameraDistance * 0.14);
            amount = projectedDelta * worldLength / gizmoAxisPixels;
        } else if (transformOperation == TransformOperation.ROTATE) {
            amount = projectedDelta * 0.8;
        } else {
            amount = Math.exp(projectedDelta / 100);
        }
        for (TransformStart start : gizmoDragStarts) {
            CustomModelDefinition.Part part = findPart(start.id);
            if (part == null) continue;
            start.restore(part);
            if (!isValidTransformResult(part, transformOperation, transformAxis, amount)) continue;
            switch (transformOperation) {
                case MOVE -> setPartPosition(part, transformAxis, start, amount);
                case ROTATE -> setPartRotation(part, transformAxis, start, amount);
                case SCALE -> setPartScale(part, transformAxis, start, amount);
            }
        }
        rebuildPreview();
        CustomModelDefinition.Part active = selectedPart();
        if (active != null) showPart(active);
    }

    private void setPartPosition(CustomModelDefinition.Part part, String axis,
                                 TransformStart start, double amount) {
        if (axis.equals("X")) part.x = start.x + amount;
        else if (axis.equals("Y")) part.y = start.y + amount;
        else part.z = start.z + amount;
    }

    private void setPartRotation(CustomModelDefinition.Part part, String axis,
                                 TransformStart start, double amount) {
        if (axis.equals("X")) part.rotateX = start.rotateX + amount;
        else if (axis.equals("Y")) part.rotateY = start.rotateY + amount;
        else part.rotateZ = start.rotateZ + amount;
    }

    private void setPartScale(CustomModelDefinition.Part part, String axis,
                              TransformStart start, double amount) {
        if (axis.equals("X")) part.width = start.width * amount;
        else if (axis.equals("Y")) part.height = start.height * amount;
        else part.depth = start.depth * amount;
    }

    private void finishGizmoDrag() {
        gizmoDragging = false;
        if (gizmoDragSnapshot != null && !sameModel(gizmoDragSnapshot, snapshot())) {
            undoHistory.push(gizmoDragSnapshot);
            if (undoHistory.size() > MAX_HISTORY) undoHistory.removeLast();
            redoHistory.clear();
            updateHistoryActions();
        }
        gizmoDragSnapshot = null;
        gizmoDragStarts = List.of();
        gizmoDragOrigin = null;
        gizmoDragDirection = null;
        transformOperation = null;
        updateTransformToolStyles();
        drawTransformGizmo();
        statusLabel.setText("Viewport transform applied");
    }

    private void cancelGizmoDrag() {
        gizmoDragging = false;
        if (gizmoDragSnapshot != null) {
            parts.clear();
            gizmoDragSnapshot.parts.stream().map(this::copyPart).forEach(parts::add);
            refreshPartList(gizmoDragSnapshot.selectedIndex);
            refreshPreview();
        }
        gizmoDragSnapshot = null;
        gizmoDragStarts = List.of();
        gizmoDragOrigin = null;
        gizmoDragDirection = null;
    }

    private void beginTransform(TransformOperation operation) {
        if (selectedParts().isEmpty()) {
            statusLabel.setText("Select one or more scene objects before transforming");
            return;
        }
        if (!validateFields()) return;
        boxSelectToggle.setSelected(false);
        brushToggle.setSelected(false);
        transformOperation = operation;
        transformAxis = "X";
        transformInput.setLength(0);
        updateTransformToolStyles();
        drawTransformGizmo();
        updateTransformStatus();
    }

    private boolean handleTransformKey(javafx.scene.input.KeyEvent event) {
        javafx.scene.input.KeyCode code = event.getCode();
        if (code == javafx.scene.input.KeyCode.ESCAPE) {
            if (gizmoDragging) cancelGizmoDrag();
            transformOperation = null;
            updateTransformToolStyles();
            statusLabel.setText("Transform cancelled");
            event.consume();
            return true;
        }
        if (code == javafx.scene.input.KeyCode.ENTER) {
            applyViewportTransform();
            event.consume();
            return true;
        }
        if (code == javafx.scene.input.KeyCode.BACK_SPACE) {
            if (!transformInput.isEmpty()) transformInput.deleteCharAt(transformInput.length() - 1);
            updateTransformStatus();
            event.consume();
            return true;
        }
        if (code == javafx.scene.input.KeyCode.X || code == javafx.scene.input.KeyCode.Y
                || code == javafx.scene.input.KeyCode.Z) {
            transformAxis = code.getName();
            updateTransformStatus();
            event.consume();
            return true;
        }
        String text = event.getText();
        if (text != null && text.length() == 1) {
            char character = text.charAt(0);
            if (Character.isDigit(character) || (character == '.' && !transformInput.toString().contains("."))
                    || (character == '-' && transformInput.isEmpty())) {
                transformInput.append(character);
                updateTransformStatus();
                event.consume();
                return true;
            }
        }
        event.consume();
        return true;
    }

    private void updateTransformStatus() {
        String action = switch (transformOperation) {
            case MOVE -> "Move";
            case ROTATE -> "Rotate";
            case SCALE -> "Resize";
        };
        String units = transformOperation == TransformOperation.ROTATE ? "°" : "";
        statusLabel.setText(action + " · axis " + transformAxis + " · value "
                + (transformInput.isEmpty() ? "—" : transformInput) + units
                + " · drag an axis handle or type a value, Enter to apply, Esc to cancel");
    }

    private void applyViewportTransform() {
        double amount;
        try {
            amount = transformInput.isEmpty()
                    ? switch (transformOperation) {
                        case MOVE -> 1;
                        case ROTATE -> 15;
                        case SCALE -> 1.1;
                    }
                    : Double.parseDouble(transformInput.toString());
        } catch (NumberFormatException exception) {
            statusLabel.setText("Invalid transform value");
            return;
        }
        if (!Double.isFinite(amount) || (transformOperation == TransformOperation.SCALE && amount <= 0)) {
            statusLabel.setText(transformOperation == TransformOperation.SCALE
                    ? "Scale factor must be finite and greater than zero"
                    : "Transform value must be finite");
            return;
        }

        List<CustomModelDefinition.Part> selected = selectedParts();
        List<CustomModelDefinition.Part> roots = selected.stream()
                .filter(part -> selected.stream().noneMatch(candidate ->
                        candidate != part && isDescendant(candidate, part)))
                .toList();
        if (roots.isEmpty()) {
            transformOperation = null;
            statusLabel.setText("No scene objects selected");
            return;
        }
        TransformOperation appliedOperation = transformOperation;
        String appliedAxis = transformAxis;
        if (roots.stream().anyMatch(part ->
                !isValidTransformResult(part, appliedOperation, appliedAxis, amount))) {
            statusLabel.setText("Transform would create an invalid component value");
            return;
        }
        performEdit(() -> {
            applyFields();
            for (CustomModelDefinition.Part part : roots) {
                switch (appliedOperation) {
                    case MOVE -> {
                        if (appliedAxis.equals("X")) part.x += amount;
                        else if (appliedAxis.equals("Y")) part.y += amount;
                        else part.z += amount;
                    }
                    case ROTATE -> {
                        if (appliedAxis.equals("X")) part.rotateX += amount;
                        else if (appliedAxis.equals("Y")) part.rotateY += amount;
                        else part.rotateZ += amount;
                    }
                    case SCALE -> {
                        if (appliedAxis.equals("X")) part.width *= amount;
                        else if (appliedAxis.equals("Y")) part.height *= amount;
                        else part.depth *= amount;
                    }
                }
            }
            rebuildPreview();
            CustomModelDefinition.Part active = selectedPart();
            if (active != null) showPart(active);
        });
        transformOperation = null;
        updateTransformToolStyles();
        statusLabel.setText(switch (appliedOperation) {
            case MOVE -> "Moved " + roots.size() + " selected component(s) on " + appliedAxis;
            case ROTATE -> "Rotated " + roots.size() + " selected component(s) on " + appliedAxis;
            case SCALE -> "Resized " + roots.size() + " selected component(s) on " + appliedAxis;
        });
    }

    private boolean isValidTransformResult(CustomModelDefinition.Part part,
                                           TransformOperation operation, String axis, double amount) {
        double current = switch (operation) {
            case MOVE -> axis.equals("X") ? part.x : axis.equals("Y") ? part.y : part.z;
            case ROTATE -> axis.equals("X") ? part.rotateX : axis.equals("Y") ? part.rotateY : part.rotateZ;
            case SCALE -> axis.equals("X") ? part.width : axis.equals("Y") ? part.height : part.depth;
        };
        double result = operation == TransformOperation.SCALE ? current * amount : current + amount;
        return Double.isFinite(result) && (operation != TransformOperation.SCALE || result > 0);
    }

    private void frameAll() {
        if (parts.isEmpty()) {
            previewWorld.setTranslateX(0);
            previewWorld.setTranslateY(0);
            previewWorld.setTranslateZ(0);
            cameraDistance = 190;
            previewCamera.setTranslateX(0);
            previewCamera.setTranslateY(-55);
            previewCamera.setTranslateZ(-cameraDistance);
            statusLabel.setText("Viewport reset · no scene objects to frame");
            return;
        }

        Bounds bounds = visibleBounds(parts);
        if (bounds == null) {
            statusLabel.setText("Reveal at least one scene object before framing the model");
            return;
        }
        frameBounds(bounds);
        statusLabel.setText("Framed visible scene objects");
    }

    private void frameSelected() {
        List<CustomModelDefinition.Part> selected = selectedParts().stream()
                .filter(this::isEffectivelyVisible).toList();
        if (selected.isEmpty()) {
            statusLabel.setText("Select visible scene objects to frame");
            return;
        }

        Bounds bounds = visibleBounds(selected);
        if (bounds == null) {
            statusLabel.setText("Selected objects are not available in the viewport");
            return;
        }
        frameBounds(bounds);
        statusLabel.setText("Framed " + selected.size() + " selected component(s)");
    }

    private Bounds visibleBounds(List<CustomModelDefinition.Part> candidates) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (CustomModelDefinition.Part part : candidates) {
            if (!isEffectivelyVisible(part)) continue;
            Node node = findPartNode(partRoot, part.id);
            if (node == null) continue;
            Bounds bounds = node.getBoundsInLocal();
            for (int corner = 0; corner < 8; corner++) {
                Point3D local = new Point3D(
                        (corner & 1) == 0 ? bounds.getMinX() : bounds.getMaxX(),
                        (corner & 2) == 0 ? bounds.getMinY() : bounds.getMaxY(),
                        (corner & 4) == 0 ? bounds.getMinZ() : bounds.getMaxZ());
                Point3D world = previewWorld.sceneToLocal(node.localToScene(local));
                minX = Math.min(minX, world.getX());
                minY = Math.min(minY, world.getY());
                minZ = Math.min(minZ, world.getZ());
                maxX = Math.max(maxX, world.getX());
                maxY = Math.max(maxY, world.getY());
                maxZ = Math.max(maxZ, world.getZ());
            }
        }
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ)) {
            return null;
        }
        return new javafx.geometry.BoundingBox(minX, minY, minZ,
                maxX - minX, maxY - minY, maxZ - minZ);
    }

    private boolean isEffectivelyVisible(CustomModelDefinition.Part part) {
        CustomModelDefinition.Part current = part;
        Set<String> visited = new java.util.HashSet<>();
        while (current != null && visited.add(current.id)) {
            if (hiddenPartIds.contains(current.id)) return false;
            current = current.parentId == null ? null : findPart(current.parentId);
        }
        return true;
    }

    private void frameBounds(Bounds bounds) {
        previewWorld.setTranslateX(-bounds.getCenterX());
        previewWorld.setTranslateY(-bounds.getCenterY());
        previewWorld.setTranslateZ(-bounds.getCenterZ());

        double halfWidth = bounds.getWidth() / 2;
        double halfHeight = bounds.getHeight() / 2;
        double halfDepth = bounds.getDepth() / 2;
        double radius = Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight + halfDepth * halfDepth);
        double verticalFov = Math.toRadians(previewCamera.getFieldOfView());
        double aspect = preview.getHeight() > 0
                ? Math.max(0.1, preview.getWidth() / preview.getHeight()) : 1.5;
        double horizontalFov = 2 * Math.atan(Math.tan(verticalFov / 2) * aspect);
        double limitingFov = Math.min(verticalFov, horizontalFov);
        cameraDistance = Math.max(30, Math.min(2500, radius / Math.tan(limitingFov / 2) * 1.2));
        previewCamera.setTranslateX(0);
        previewCamera.setTranslateY(0);
        previewCamera.setTranslateZ(-cameraDistance);
    }

    private Button axisButton(String axis, String color, double yawAngle, double pitchAngle) {
        Button button = new Button(axis);
        button.setTooltip(new javafx.scene.control.Tooltip("Align view to the " + axis + " axis."));
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;"
                + "-fx-font-weight: bold; -fx-background-radius: 14; -fx-min-width: 27;"
                + "-fx-min-height: 27; -fx-max-width: 27; -fx-max-height: 27;"
                + "-fx-padding: 0;");
        button.setOnAction(event -> {
            yaw.setAngle(yawAngle);
            pitch.setAngle(pitchAngle);
        });
        return button;
    }

    private VBox buildInspector() {
        partList.setMinHeight(70);
        partList.setShowRoot(false);
        partList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        partList.setTooltip(new javafx.scene.control.Tooltip(
                "Drag near a row edge to reorder; drop in the middle to nest. Nested transforms are relative to the parent."));
        partList.setStyle("-fx-control-inner-background: #171e20;"
                + "-fx-background-color: #171e20;"
                + "-fx-text-fill: #e4e9e5;"
                + "-fx-selection-bar: #078db8;"
                + "-fx-selection-bar-non-focused: #36545d;"
                + "-fx-focus-color: #168eb5;"
                + "-fx-faint-focus-color: transparent;");
        partList.setCellFactory(tree -> new TreeCell<>() {
            private final Button visibilityButton = new Button();
            private final Label rowLabel = new Label();
            private final HBox rowContent = new HBox(6, visibilityButton, rowLabel);

            {
                visibilityButton.setFocusTraversable(false);
                visibilityButton.setMinWidth(24);
                visibilityButton.setPrefWidth(24);
                visibilityButton.setMaxWidth(24);
                visibilityButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd4ce;"
                        + "-fx-padding: 0 2 0 2;");
                rowContent.setAlignment(Pos.CENTER_LEFT);
                rowLabel.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(rowLabel, Priority.ALWAYS);
                visibilityButton.setOnAction(event -> {
                    CustomModelDefinition.Part part = getItem();
                    if (part != null) togglePartVisibility(part);
                    event.consume();
                });
                setOnDragDetected(event -> {
                    CustomModelDefinition.Part part = getItem();
                    if (part == null) return;
                    Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(part.id);
                    dragboard.setContent(content);
                    event.consume();
                });
                selectedProperty().addListener((obs, wasSelected, isSelected) -> updateCellStyle());
                setOnDragOver(event -> {
                    if (!event.getDragboard().hasString()) return;
                    CustomModelDefinition.Part source = findPart(event.getDragboard().getString());
                    CustomModelDefinition.Part target = getItem();
                    if (source != null && target != source && !isDescendant(source, target)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                    event.consume();
                });
                setOnDragDropped(event -> {
                    boolean completed = false;
                    if (event.getDragboard().hasString()) {
                        CustomModelDefinition.Part source = findPart(event.getDragboard().getString());
                        CustomModelDefinition.Part target = getItem();
                        if (source != null) {
                            double relativeY = getHeight() <= 0 ? 0.5 : event.getY() / getHeight();
                            TreeDropPosition position = target == null ? TreeDropPosition.AFTER
                                    : relativeY >= 0.25 && relativeY <= 0.75 ? TreeDropPosition.CHILD
                                    : relativeY < 0.25 ? TreeDropPosition.BEFORE : TreeDropPosition.AFTER;
                            completed = movePart(source, target, position);
                        }
                    }
                    event.setDropCompleted(completed);
                    event.consume();
                });
            }

            @Override
            protected void updateItem(CustomModelDefinition.Part item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    boolean visible = isEffectivelyVisible(item);
                    visibilityButton.setText(visible ? "◉" : "○");
                    visibilityButton.setTooltip(new javafx.scene.control.Tooltip(
                            visible ? "Hide this component and its children"
                                    : "Reveal this component"));
                    visibilityButton.setAccessibleText(visible
                            ? "Hide " + item.name : "Reveal " + item.name);
                    rowLabel.setText(item.name + "  ·  " + item.shape);
                    setText(null);
                    setGraphic(rowContent);
                }
                updateCellStyle();
            }

            private void updateCellStyle() {
                setStyle(isSelected()
                        ? "-fx-background-color: #078db8; -fx-text-fill: #ffffff;"
                        : "-fx-background-color: #202729; -fx-text-fill: #e4e9e5;");
                rowLabel.setStyle(isSelected()
                        ? "-fx-text-fill: #ffffff;"
                        : "-fx-text-fill: #e4e9e5;");
            }
        });
        partList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (!rebuildingPartTree && selected != null) {
                selectedPartId = selected.getValue().id;
                showPart(selected.getValue());
                drawTransformGizmo();
            } else if (!rebuildingPartTree && partList.getSelectionModel().getSelectedItems().isEmpty()) {
                selectedPartId = null;
                showPart(null);
                drawTransformGizmo();
            }
        });
        hierarchySearch.setPromptText("Search scene objects");
        hierarchySearch.setStyle("-fx-background-color: #171e20; -fx-text-fill: #e4e9e5;"
                + "-fx-prompt-text-fill: #87918d; -fx-border-color: #3b4648;");
        hierarchySearch.textProperty().addListener((obs, oldValue, newValue) ->
                refreshPartList(selectedPartIndex()));

        Button remove = new Button("Remove");
        remove.setOnAction(event -> removeSelectedPart());
        Button duplicate = new Button("Duplicate");
        duplicate.setTooltip(new javafx.scene.control.Tooltip("Duplicate selected scene objects and their child hierarchy"));
        duplicate.setOnAction(event -> duplicateSelectedPart());
        Button moveUp = new Button("Move up");
        moveUp.setTooltip(new javafx.scene.control.Tooltip("Move selected object up in draw order"));
        moveUp.setOnAction(event -> moveSelectedPart(-1));
        Button moveDown = new Button("Move down");
        moveDown.setTooltip(new javafx.scene.control.Tooltip("Move selected object down in draw order"));
        moveDown.setOnAction(event -> moveSelectedPart(1));
        remove.setTooltip(new javafx.scene.control.Tooltip("Remove selected scene object"));
        Button hide = new Button("Hide/Show");
        hide.setTooltip(new javafx.scene.control.Tooltip("Toggle visibility of selected objects in the viewport"));
        hide.setOnAction(event -> toggleSelectedVisibility());
        Button showAll = new Button("Show All");
        showAll.setTooltip(new javafx.scene.control.Tooltip("Reveal every hidden scene object"));
        showAll.setOnAction(event -> showAllComponents());
        Button selectParent = new Button("Parent");
        selectParent.setTooltip(new javafx.scene.control.Tooltip("Select the active component's parent"));
        selectParent.setOnAction(event -> selectRelatedHierarchy(false));
        Button selectDescendants = new Button("Kids");
        selectDescendants.setTooltip(new javafx.scene.control.Tooltip("Select all visible descendants of the active component"));
        selectDescendants.setOnAction(event -> selectRelatedHierarchy(true));
        duplicate.setText("Dup");
        moveUp.setText("Up");
        moveDown.setText("Down");
        remove.setText("Del");
        GridPane hierarchyActions = new GridPane();
        hierarchyActions.setHgap(6);
        hierarchyActions.setVgap(6);
        hierarchyActions.setMinHeight(0);
        hierarchyActions.setMaxWidth(Double.MAX_VALUE);
        Button[] hierarchyButtons = {
                duplicate, moveUp, moveDown, hide, showAll, remove, selectParent, selectDescendants
        };
        for (int index = 0; index < hierarchyButtons.length; index++) {
            int column = index % 3;
            int row = index / 3;
            Button button = hierarchyButtons[index];
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMinWidth(0);
            button.setStyle("-fx-background-color: #394245; -fx-text-fill: #e2e7e3;"
                    + "-fx-border-color: #586365; -fx-padding: 4 5 4 5;");
            GridPane.setHgrow(button, Priority.ALWAYS);
            hierarchyActions.add(button, column, row);
        }
        for (int column = 0; column < 3; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / 3);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setMinWidth(0);
            hierarchyActions.getColumnConstraints().add(constraints);
        }
        for (int row = 0; row < 3; row++) {
            RowConstraints actionRow = new RowConstraints();
            actionRow.setMinHeight(26);
            actionRow.setPrefHeight(26);
            actionRow.setMaxHeight(26);
            hierarchyActions.getRowConstraints().add(actionRow);
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
            values[i].setOnAction(event -> applyInspectorFields());
            field.setMinWidth(0);
            field.setMaxWidth(Double.MAX_VALUE);
            transformGrid.add(field, i % 3, i / 3);
        }

        Button apply = new Button("Apply part changes");
        apply.setMaxWidth(Double.MAX_VALUE);
        apply.setOnAction(event -> applyInspectorFields());

        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.setOnAction(event -> {
            if (syncingInspector) return;
            CustomModelDefinition.Part selected = selectedPart();
            if (selected != null && validateFields()) {
                performEdit(() -> {
                    applyFields();
                    selected.color = colorPicker.getValue().toString();
                    refreshPreview();
                });
            }
        });
        partName.textProperty().addListener((obs, oldName, newName) -> {
            if (syncingInspector) return;
            CustomModelDefinition.Part selected = selectedPart();
            if (selected != null && !newName.isBlank()) {
                performEdit(() -> {
                    selected.name = newName;
                    refreshPartList(selectedPartIndex());
                });
            }
        });
        shapePicker.setOnAction(event -> {
            if (syncingInspector) return;
            CustomModelDefinition.Part selected = selectedPart();
            if (selected != null && shapePicker.getValue() != null) {
                if (!validateFields()) return;
                performEdit(() -> {
                    applyFields();
                    selected.shape = shapePicker.getValue();
                    refreshPartList(selectedPartIndex());
                    refreshPreview();
                });
            }
        });
        finishPicker.setOnAction(event -> {
            if (syncingInspector) return;
            CustomModelDefinition.Part selected = selectedPart();
            if (selected != null && finishPicker.getValue() != null) {
                if (!validateFields()) return;
                performEdit(() -> {
                    applyFields();
                    selected.finish = finishPicker.getValue();
                    refreshPreview();
                });
            }
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
        inspectorSelectionLabel = new Label("SELECTED COMPONENT");
        Label surfaceLabel = new Label("SURFACE MATERIAL");
        VBox hierarchy = new VBox(8, partsLabel, hierarchySearch, hierarchyActions, partList);
        hierarchy.setPadding(new Insets(12));
        hierarchy.setMinSize(0, 190);
        hierarchy.setStyle("-fx-background-color: #202729; -fx-border-color: #343d3e; -fx-border-width: 0 0 1 0;");
        VBox.setVgrow(partList, Priority.ALWAYS);

        selectedProperties = new VBox(9,
            new VBox(3, new Label("Component name"), partName), finishControls,
            new VBox(3, new Label("Semantic preset"), materialPicker),
            new Label("Brush palette"), palette, brushRadiusLabel, brushRadius,
            new Label("Base color / opacity"), colorPicker, transformGrid, apply,
            new Separator(), surfaceLabel,
            new Label("Preset sets a useful base color. Finish controls specular response."));
        VBox inspectorContent = new VBox(9, inspectorSelectionLabel, selectedProperties);
        inspectorContent.setPadding(new Insets(9));
        showPart(selectedPart());
        ScrollPane properties = new ScrollPane(inspectorContent);
        properties.setFitToWidth(true);
        properties.setStyle("-fx-background: #202729; -fx-background-color: #202729;");
        inspectorContent.setStyle("-fx-background-color: #202729;");
        Label dockTitle = new Label("SCENE COLLECTION");
        dockTitle.setStyle("-fx-text-fill: #e4d9bf; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.7px;");
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
        explorerProperties.setStyle("-fx-background-color: #202729; -fx-padding: 0;");
        explorerProperties.getDividers().get(0).positionProperty().addListener(
                (obs, oldPosition, newPosition) -> explorerDividerPosition = newPosition.doubleValue());

        VBox inspectorPane = new VBox(dockTitleBar, explorerProperties);
        inspectorPane.setPrefWidth(285);
        inspectorPane.setMinWidth(0);
        inspectorPane.setStyle("-fx-background-color: #202729; -fx-border-color: #343d3e; -fx-border-width: 0 0 0 1;");
        VBox.setVgrow(explorerProperties, Priority.ALWAYS);
        return inspectorPane;
    }

    private void addPart(String shape) {
        if (!validateFields()) return;
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
        List<CustomModelDefinition.Part> selected = selectedParts();
        if (selected.isEmpty()) return;
        if (!validateFields()) return;
        performEdit(() -> {
            applyFields();
            List<CustomModelDefinition.Part> roots = selected.stream()
                    .filter(part -> selected.stream().noneMatch(candidate ->
                            candidate != part && isDescendant(candidate, part)))
                    .toList();
            Set<String> rootIds = roots.stream().map(part -> part.id)
                    .collect(java.util.stream.Collectors.toSet());
            List<CustomModelDefinition.Part> sourceParts = parts.stream()
                    .filter(part -> roots.stream().anyMatch(root ->
                            part == root || isDescendant(root, part)))
                    .toList();
            java.util.Map<String, CustomModelDefinition.Part> copiesById = new java.util.HashMap<>();
            List<CustomModelDefinition.Part> copies = new ArrayList<>();
            for (CustomModelDefinition.Part source : sourceParts) {
                CustomModelDefinition.Part copy = copyPart(source);
                copy.id = UUID.randomUUID().toString();
                copy.name = source.name + " Copy";
                if (rootIds.contains(source.id)) {
                    copy.x += 4;
                    copy.z += 4;
                }
                copiesById.put(source.id, copy);
                copies.add(copy);
            }
            for (int i = 0; i < sourceParts.size(); i++) {
                CustomModelDefinition.Part source = sourceParts.get(i);
                CustomModelDefinition.Part copy = copies.get(i);
                CustomModelDefinition.Part copiedParent = copiesById.get(source.parentId);
                if (copiedParent != null) copy.parentId = copiedParent.id;
            }
            parts.addAll(copies);
            refreshPartList(parts.size() - 1);
            refreshPreview();
            statusLabel.setText(copies.size() + " component(s) duplicated");
        });
    }

    private void moveSelectedPart(int offset) {
        int index = selectedPartIndex();
        int target = index + offset;
        if (index < 0 || target < 0 || target >= parts.size()) return;
        if (!validateFields()) return;
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
        CustomModelDefinition.Part part = selectedPart();
        String preset = materialPicker.getValue();
        if (part == null || preset == null) return;
        if (!validateFields()) return;
        performEdit(() -> {
            applyFields();
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
        List<CustomModelDefinition.Part> selected = selectedParts();
        if (selected.isEmpty()) return;
        int index = selected.stream().mapToInt(parts::indexOf).min().orElse(-1);
        if (index < 0) return;
        if (!validateFields()) return;
        performEdit(() -> {
            applyFields();
            Set<String> removedIds = new java.util.HashSet<>();
            selected.forEach(part -> removedIds.add(part.id));
            boolean changed;
            do {
                changed = false;
                for (CustomModelDefinition.Part part : parts) {
                    if (removedIds.contains(part.parentId) && removedIds.add(part.id)) changed = true;
                }
            } while (changed);
            parts.removeIf(part -> removedIds.contains(part.id));
            refreshPartList(Math.min(index, parts.size() - 1));
            refreshPreview();
            statusLabel.setText(removedIds.size() + " component(s) removed");
        });
    }

    private void toggleSelectedVisibility() {
        List<CustomModelDefinition.Part> selected = selectedParts();
        if (selected.isEmpty()) {
            statusLabel.setText("Select one or more scene objects first");
            return;
        }
        List<CustomModelDefinition.Part> roots = selectedRoots();
        boolean hide = roots.stream().anyMatch(this::isEffectivelyVisible);
        for (CustomModelDefinition.Part part : roots) {
            if (hide) {
                hiddenPartIds.add(part.id);
            } else {
                CustomModelDefinition.Part current = part;
                Set<String> visited = new java.util.HashSet<>();
                while (current != null && visited.add(current.id)) {
                    hiddenPartIds.remove(current.id);
                    current = current.parentId == null ? null : findPart(current.parentId);
                }
            }
        }
        refreshPartVisibility();
        refreshPartList(selectedPartIndex());
        statusLabel.setText(hide
                ? "Hid " + roots.size() + " selected component(s)"
                : "Revealed " + roots.size() + " selected component(s)");
    }

    private void togglePartVisibility(CustomModelDefinition.Part part) {
        boolean hide = isEffectivelyVisible(part);
        if (hide) {
            hiddenPartIds.add(part.id);
        } else {
            CustomModelDefinition.Part current = part;
            Set<String> visited = new java.util.HashSet<>();
            while (current != null && visited.add(current.id)) {
                hiddenPartIds.remove(current.id);
                current = current.parentId == null ? null : findPart(current.parentId);
            }
        }
        refreshPartVisibility();
        partList.refresh();
        statusLabel.setText(hide ? "Hidden " + part.name : "Revealed " + part.name);
    }

    private void showAllComponents() {
        if (hiddenPartIds.isEmpty()) {
            statusLabel.setText("All scene objects are already visible");
            return;
        }
        hiddenPartIds.clear();
        refreshPartVisibility();
        refreshPartList(selectedPartIndex());
        statusLabel.setText("All scene objects are visible");
    }

    private void selectRelatedHierarchy(boolean descendants) {
        CustomModelDefinition.Part active = selectedPart();
        if (active == null) {
            statusLabel.setText("Select a component first");
            return;
        }

        List<CustomModelDefinition.Part> related;
        if (descendants) {
            related = parts.stream()
                    .filter(part -> isDescendant(active, part))
                    .filter(this::isEffectivelyVisible)
                    .filter(part -> findTreeItem(partList.getRoot(), part) != null)
                    .toList();
        } else {
            CustomModelDefinition.Part parent = active.parentId == null
                    ? null : findPart(active.parentId);
            related = parent == null || !isEffectivelyVisible(parent)
                    || findTreeItem(partList.getRoot(), parent) == null
                    ? List.of() : List.of(parent);
        }

        if (related.isEmpty()) {
            statusLabel.setText(descendants
                    ? "No visible descendants to select"
                    : "No visible parent to select");
            return;
        }

        partList.getSelectionModel().clearSelection();
        TreeItem<CustomModelDefinition.Part> lastItem = null;
        for (CustomModelDefinition.Part part : related) {
            TreeItem<CustomModelDefinition.Part> item = findTreeItem(partList.getRoot(), part);
            if (item != null) {
                partList.getSelectionModel().select(item);
                lastItem = item;
            }
        }
        if (lastItem != null) {
            CustomModelDefinition.Part selected = lastItem.getValue();
            selectedPartId = selected.id;
            showPart(selected);
            statusLabel.setText("Selected " + related.size()
                    + (descendants ? " visible descendant(s)" : " parent component"));
        }
        drawTransformGizmo();
    }

    private void refreshPartVisibility() {
        for (CustomModelDefinition.Part part : parts) {
            Node node = findPartNode(partRoot, part.id);
            if (node != null) node.setVisible(!hiddenPartIds.contains(part.id));
        }
        drawTransformGizmo();
    }

    private List<CustomModelDefinition.Part> selectedParts() {
        Set<CustomModelDefinition.Part> selected = partList.getSelectionModel().getSelectedItems().stream()
                .map(TreeItem::getValue)
                .collect(java.util.stream.Collectors.toSet());
        if (selected.isEmpty()) {
            CustomModelDefinition.Part single = selectedPart();
            if (single != null) selected.add(single);
        }
        return parts.stream().filter(selected::contains).toList();
    }

    private CustomModelDefinition.Part selectedPart() {
        TreeItem<CustomModelDefinition.Part> selected = partList.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getValue();
    }

    private void updateSelectionMarquee(double x, double y) {
        double minX = Math.min(selectionStartX, x);
        double minY = Math.min(selectionStartY, y);
        selectionMarquee.setX(0);
        selectionMarquee.setY(0);
        selectionMarquee.setTranslateX(minX);
        selectionMarquee.setTranslateY(minY);
        selectionMarquee.setWidth(Math.abs(x - selectionStartX));
        selectionMarquee.setHeight(Math.abs(y - selectionStartY));
        selectionMarquee.setVisible(true);
    }

    private void selectPartsInRectangle(double startX, double startY, double endX, double endY,
                                        boolean additive) {
        double minX = Math.min(startX, endX);
        double minY = Math.min(startY, endY);
        double maxX = Math.max(startX, endX);
        double maxY = Math.max(startY, endY);
        if (!additive) partList.getSelectionModel().clearSelection();

        CustomModelDefinition.Part lastSelected = null;
        for (CustomModelDefinition.Part part : parts) {
            if (!isEffectivelyVisible(part)) continue;
            Node partNode = findPartNode(partRoot, part.id);
            ProjectedBounds bounds = partNode == null ? null : projectBounds(partNode);
            if (bounds != null && bounds.intersects(minX, minY, maxX, maxY)) {
                TreeItem<CustomModelDefinition.Part> item = findTreeItem(partList.getRoot(), part);
                if (item != null) {
                    partList.getSelectionModel().select(item);
                    lastSelected = part;
                }
            }
        }

        List<TreeItem<CustomModelDefinition.Part>> selectedItems =
                partList.getSelectionModel().getSelectedItems();
        if (lastSelected != null) {
            showPart(lastSelected);
            statusLabel.setText("Selected " + selectedItems.size()
                    + " part(s), including parts behind others");
        } else if (!selectedItems.isEmpty()) {
            statusLabel.setText("Box select found no new parts; selection unchanged");
        } else {
            selectedPartId = null;
            statusLabel.setText("Box select found no parts");
        }
        drawTransformGizmo();
    }

    private void selectVisibleParts() {
        partList.getSelectionModel().clearSelection();
        CustomModelDefinition.Part lastSelected = null;
        for (CustomModelDefinition.Part part : parts) {
            if (!isEffectivelyVisible(part)) continue;
            TreeItem<CustomModelDefinition.Part> item = findTreeItem(partList.getRoot(), part);
            if (item != null) {
                partList.getSelectionModel().select(item);
                lastSelected = part;
            }
        }
        if (lastSelected == null) {
            selectedPartId = null;
            statusLabel.setText("No visible Outliner objects to select");
        } else {
            selectedPartId = lastSelected.id;
            showPart(lastSelected);
            statusLabel.setText("Selected " + partList.getSelectionModel().getSelectedItems().size()
                    + " visible Outliner component(s)");
        }
        drawTransformGizmo();
    }

    private void invertVisibleSelection() {
        Set<String> selectedIds = partList.getSelectionModel().getSelectedItems().stream()
                .map(TreeItem::getValue)
                .map(part -> part.id)
                .collect(java.util.stream.Collectors.toSet());
        List<CustomModelDefinition.Part> candidates = parts.stream()
                .filter(this::isEffectivelyVisible)
                .filter(part -> findTreeItem(partList.getRoot(), part) != null)
                .toList();
        CustomModelDefinition.Part lastSelected = null;
        for (CustomModelDefinition.Part part : candidates) {
            TreeItem<CustomModelDefinition.Part> item = findTreeItem(partList.getRoot(), part);
            if (item == null) continue;
            if (selectedIds.contains(part.id)) {
                partList.getSelectionModel().clearSelection(partList.getRow(item));
            } else {
                partList.getSelectionModel().select(item);
                lastSelected = part;
            }
        }

        List<TreeItem<CustomModelDefinition.Part>> selectedItems =
                partList.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            clearStudioSelection();
            statusLabel.setText(candidates.isEmpty()
                    ? "No visible Outliner objects to invert"
                    : "Selection inverted; no components selected");
        } else {
            TreeItem<CustomModelDefinition.Part> activeItem =
                    partList.getSelectionModel().getSelectedItem();
            if (activeItem == null && lastSelected != null) {
                activeItem = findTreeItem(partList.getRoot(), lastSelected);
            }
            if (activeItem == null) activeItem = selectedItems.get(selectedItems.size() - 1);
            CustomModelDefinition.Part active = activeItem.getValue();
            selectedPartId = active.id;
            showPart(active);
            statusLabel.setText("Inverted selection for " + candidates.size()
                    + " visible Outliner component(s); " + selectedItems.size() + " selected");
        }
        drawTransformGizmo();
    }

    private void clearStudioSelection() {
        partList.getSelectionModel().clearSelection();
        selectedPartId = null;
        showPart(null);
        drawTransformGizmo();
        statusLabel.setText("Selection cleared");
    }

    private void selectPartFromNode(Node pickedNode, boolean additive) {
        Node current = pickedNode;
        while (current != null && current != partRoot
                && !(current.getUserData() instanceof CustomModelDefinition.Part)) {
            current = current.getParent();
        }
        if (current != null && current.getUserData() instanceof CustomModelDefinition.Part part) {
            TreeItem<CustomModelDefinition.Part> item = findTreeItem(partList.getRoot(), part);
            if (item != null) {
                if (!additive) {
                    partList.getSelectionModel().clearSelection();
                    partList.getSelectionModel().select(item);
                } else if (partList.getSelectionModel().getSelectedItems().contains(item)) {
                    partList.getSelectionModel().clearSelection(partList.getRow(item));
                } else {
                    partList.getSelectionModel().select(item);
                }
                List<TreeItem<CustomModelDefinition.Part>> selectedItems =
                        partList.getSelectionModel().getSelectedItems();
                if (!selectedItems.isEmpty()) {
                    TreeItem<CustomModelDefinition.Part> activeItem =
                            partList.getSelectionModel().getSelectedItem();
                    if (activeItem == null) activeItem = selectedItems.get(selectedItems.size() - 1);
                    CustomModelDefinition.Part active = activeItem.getValue();
                    selectedPartId = active.id;
                    showPart(active);
                    statusLabel.setText("Selected " + selectedItems.size() + " component(s)");
                } else {
                    clearStudioSelection();
                    statusLabel.setText("Selection cleared");
                }
                drawTransformGizmo();
                return;
            }
        }
        if (additive) return;
        clearStudioSelection();
        statusLabel.setText("No scene object at that position");
    }

    private ProjectedBounds projectBounds(Node node) {
        Bounds bounds = node.getBoundsInLocal();
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double verticalFov = previewCamera.isVerticalFieldOfView()
                ? previewCamera.getFieldOfView()
                : Math.toDegrees(2 * Math.atan(Math.tan(
                        Math.toRadians(previewCamera.getFieldOfView()) / 2)
                        * preview.getHeight() / preview.getWidth()));
        double focalLength = preview.getHeight() / (2 * Math.tan(Math.toRadians(verticalFov) / 2));
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    Point3D localPoint = new Point3D(
                            x == 0 ? bounds.getMinX() : bounds.getMaxX(),
                            y == 0 ? bounds.getMinY() : bounds.getMaxY(),
                            z == 0 ? bounds.getMinZ() : bounds.getMaxZ());
                    Point3D cameraPoint = previewCamera.sceneToLocal(node.localToScene(localPoint));
                    if (cameraPoint.getZ() <= previewCamera.getNearClip()) continue;
                    double screenX = preview.getWidth() / 2
                            + cameraPoint.getX() * focalLength / cameraPoint.getZ();
                    double screenY = preview.getHeight() / 2
                            + cameraPoint.getY() * focalLength / cameraPoint.getZ();
                    minX = Math.min(minX, screenX);
                    minY = Math.min(minY, screenY);
                    maxX = Math.max(maxX, screenX);
                    maxY = Math.max(maxY, screenY);
                }
            }
        }
        return minX == Double.POSITIVE_INFINITY ? null : new ProjectedBounds(minX, minY, maxX, maxY);
    }

    private Node findPartNode(Node node, String id) {
        if (node.getUserData() instanceof CustomModelDefinition.Part part && part.id.equals(id)) {
            return node;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node found = findPartNode(child, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private TreeItem<CustomModelDefinition.Part> findTreeItem(
            TreeItem<CustomModelDefinition.Part> item, CustomModelDefinition.Part part) {
        if (item == null) return null;
        if (item.getValue() == part) return item;
        for (TreeItem<CustomModelDefinition.Part> child : item.getChildren()) {
            TreeItem<CustomModelDefinition.Part> found = findTreeItem(child, part);
            if (found != null) return found;
        }
        return null;
    }

    private record ProjectedBounds(double minX, double minY, double maxX, double maxY) {
        private boolean intersects(double selectionMinX, double selectionMinY,
                                   double selectionMaxX, double selectionMaxY) {
            return maxX >= selectionMinX && minX <= selectionMaxX
                    && maxY >= selectionMinY && minY <= selectionMaxY;
        }
    }

    private int selectedPartIndex() {
        CustomModelDefinition.Part selected = selectedPart();
        return selected == null ? -1 : parts.indexOf(selected);
    }

    private CustomModelDefinition.Part findPart(String id) {
        return parts.stream().filter(part -> part.id.equals(id)).findFirst().orElse(null);
    }

    private boolean isDescendant(CustomModelDefinition.Part ancestor, CustomModelDefinition.Part candidate) {
        if (candidate == null) return false;
        String parentId = candidate.parentId;
        while (parentId != null) {
            if (parentId.equals(ancestor.id)) return true;
            CustomModelDefinition.Part parent = findPart(parentId);
            parentId = parent == null ? null : parent.parentId;
        }
        return false;
    }

    private boolean movePart(CustomModelDefinition.Part source, CustomModelDefinition.Part target,
                             TreeDropPosition position) {
        if (source == target || (target != null && isDescendant(source, target))) return false;
        if (!validateFields()) return false;
        performEdit(() -> {
            applyFields();
            double worldX = worldPosition(source, part -> part.x);
            double worldY = worldPosition(source, part -> part.y);
            double worldZ = worldPosition(source, part -> part.z);
            double worldRotateX = worldPosition(source, part -> part.rotateX);
            double worldRotateY = worldPosition(source, part -> part.rotateY);
            double worldRotateZ = worldPosition(source, part -> part.rotateZ);
            CustomModelDefinition.Part newParent = target == null ? null
                    : position == TreeDropPosition.CHILD ? target : findPart(target.parentId);
            source.parentId = newParent == null ? null : newParent.id;
            source.x = worldX - (newParent == null ? 0 : worldPosition(newParent, part -> part.x));
            source.y = worldY - (newParent == null ? 0 : worldPosition(newParent, part -> part.y));
            source.z = worldZ - (newParent == null ? 0 : worldPosition(newParent, part -> part.z));
            source.rotateX = worldRotateX - (newParent == null ? 0 : worldPosition(newParent, part -> part.rotateX));
            source.rotateY = worldRotateY - (newParent == null ? 0 : worldPosition(newParent, part -> part.rotateY));
            source.rotateZ = worldRotateZ - (newParent == null ? 0 : worldPosition(newParent, part -> part.rotateZ));

            parts.remove(source);
            int targetIndex = target == null ? parts.size() : parts.indexOf(target);
            int insertIndex = switch (position) {
                case BEFORE -> targetIndex;
                case AFTER -> targetIndex + 1;
                case CHILD -> target == null ? parts.size() : subtreeEndIndex(target);
            };
            parts.add(Math.max(0, Math.min(insertIndex, parts.size())), source);
            int selectedIndex = parts.indexOf(source);
            refreshPartList(selectedIndex);
            refreshPreview();
        });
        statusLabel.setText(target != null && position == TreeDropPosition.CHILD
                ? "Object nested in hierarchy" : "Object order updated");
        return true;
    }

    private double worldPosition(CustomModelDefinition.Part part,
                                 java.util.function.ToDoubleFunction<CustomModelDefinition.Part> value) {
        double result = 0;
        CustomModelDefinition.Part current = part;
        Set<String> visited = new java.util.HashSet<>();
        while (current != null && visited.add(current.id)) {
            result += value.applyAsDouble(current);
            current = current.parentId == null ? null : findPart(current.parentId);
        }
        return result;
    }

    private int subtreeEndIndex(CustomModelDefinition.Part parent) {
        int index = parts.indexOf(parent) + 1;
        while (index < parts.size() && isDescendant(parent, parts.get(index))) index++;
        return index;
    }

    private void expandTree(TreeItem<CustomModelDefinition.Part> item) {
        item.setExpanded(true);
        for (TreeItem<CustomModelDefinition.Part> child : item.getChildren()) expandTree(child);
    }

    private enum TransformOperation { MOVE, ROTATE, SCALE }

    private enum TreeDropPosition { BEFORE, CHILD, AFTER }

    private record TransformStart(String id, double x, double y, double z,
                                 double width, double height, double depth,
                                 double rotateX, double rotateY, double rotateZ) {
        private static TransformStart from(CustomModelDefinition.Part part) {
            return new TransformStart(part.id, part.x, part.y, part.z,
                    part.width, part.height, part.depth,
                    part.rotateX, part.rotateY, part.rotateZ);
        }

        private void restore(CustomModelDefinition.Part part) {
            part.x = x;
            part.y = y;
            part.z = z;
            part.width = width;
            part.height = height;
            part.depth = depth;
            part.rotateX = rotateX;
            part.rotateY = rotateY;
            part.rotateZ = rotateZ;
        }
    }

    private void refreshPartList(int selectedIndex) {
        if (parts.isEmpty()) {
            rebuildingPartTree = true;
            partList.setRoot(new TreeItem<>());
            partList.getSelectionModel().clearSelection();
            rebuildingPartTree = false;
            selectedPartId = null;
            return;
        }
        String selectedId = selectedIndex >= 0 && selectedIndex < parts.size()
                ? parts.get(selectedIndex).id : selectedPartId;
        if (selectedId != null && findPart(selectedId) == null) selectedId = null;
        rebuildingPartTree = true;
        TreeItem<CustomModelDefinition.Part> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);
        java.util.Map<String, TreeItem<CustomModelDefinition.Part>> items = new java.util.LinkedHashMap<>();
        String query = hierarchySearch.getText() == null
                ? "" : hierarchySearch.getText().trim().toLowerCase(java.util.Locale.ROOT);
        for (CustomModelDefinition.Part part : parts) {
            if (query.isEmpty() || matchesPartOrDescendant(part, query)) {
                items.put(part.id, new TreeItem<>(part));
            }
        }
        for (CustomModelDefinition.Part part : parts) {
            TreeItem<CustomModelDefinition.Part> item = items.get(part.id);
            if (item == null) continue;
            TreeItem<CustomModelDefinition.Part> parent = items.get(part.parentId);
            (parent == null ? rootItem : parent).getChildren().add(item);
        }
        expandTree(rootItem);
        partList.getSelectionModel().clearSelection();
        partList.setRoot(rootItem);
        TreeItem<CustomModelDefinition.Part> selected = items.get(selectedId);
        if (selected != null) {
            partList.getSelectionModel().select(selected);
            showPart(selected.getValue());
            selectedPartId = selected.getValue().id;
        }
        rebuildingPartTree = false;
    }

    private boolean matchesPartOrDescendant(CustomModelDefinition.Part part, String query) {
        if (part.name != null && part.name.toLowerCase(java.util.Locale.ROOT).contains(query)) return true;
        if (part.shape != null && part.shape.toLowerCase(java.util.Locale.ROOT).contains(query)) return true;
        for (CustomModelDefinition.Part candidate : parts) {
            if (part.id.equals(candidate.parentId) && matchesPartOrDescendant(candidate, query)) return true;
        }
        return false;
    }

    private void showPart(CustomModelDefinition.Part part) {
        syncingInspector = true;
        if (part == null) {
            inspectorSelectionLabel.setText("NO COMPONENT SELECTED");
            selectedProperties.setDisable(true);
            partName.clear();
            shapePicker.setValue(null);
            materialPicker.setValue(null);
            finishPicker.setValue(null);
            for (TextField value : values) {
                value.clear();
                value.setStyle("");
            }
            colorPicker.setValue(Color.web("#8b8e83"));
            syncingInspector = false;
            return;
        }
        inspectorSelectionLabel.setText("SELECTED COMPONENT");
        selectedProperties.setDisable(false);
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
        for (TextField value : values) value.setStyle("");
        try { colorPicker.setValue(Color.web(part.color)); }
        catch (IllegalArgumentException ex) { colorPicker.setValue(Color.web("#8b8e83")); }
        syncingInspector = false;
    }

    private boolean validateFields() {
        CustomModelDefinition.Part part = selectedPart();
        if (part == null) return true;
        boolean valid = true;
        for (int i = 0; i < values.length; i++) {
            try {
                double value = Double.parseDouble(values[i].getText().trim());
                if (!Double.isFinite(value) || (i >= 3 && i <= 5 && value <= 0)) {
                    throw new NumberFormatException();
                }
                values[i].setStyle("");
            } catch (NumberFormatException ex) {
                values[i].setStyle("-fx-border-color: #e26060; -fx-border-width: 1.5;");
                valid = false;
            }
        }
        if (!valid) {
            statusLabel.setText("Enter finite numbers; width, height, and depth must be greater than zero.");
        }
        return valid;
    }

    private boolean applyFields() {
        if (!validateFields()) return false;
        CustomModelDefinition.Part part = selectedPart();
        if (part == null) return true;
        part.x = parse(values[0]);
        part.y = parse(values[1]);
        part.z = parse(values[2]);
        part.width = parse(values[3]);
        part.height = parse(values[4]);
        part.depth = parse(values[5]);
        part.rotateX = parse(values[6]);
        part.rotateY = parse(values[7]);
        part.rotateZ = parse(values[8]);
        if (!partName.getText().isBlank()) part.name = partName.getText().trim();
        if (shapePicker.getValue() != null) part.shape = shapePicker.getValue();
        if (materialPicker.getValue() != null) part.material = materialPicker.getValue();
        if (finishPicker.getValue() != null) part.finish = finishPicker.getValue();
        part.color = colorPicker.getValue().toString();
        return true;
    }

    private void applyInspectorFields() {
        if (!validateFields()) return;
        performEdit(() -> {
            applyFields();
            refreshPreview();
            refreshPartList(selectedPartIndex());
        });
    }

    private void refreshPreview() {
        if (!applyFields()) return;
        rebuildPreview();
    }

    private void rebuildPreview() {
        partRoot.getChildren().clear();
        Group hierarchy = CustomModelLibrary.createHierarchy(parts);
        List<Node> rootNodes = List.copyOf(hierarchy.getChildren());
        hierarchy.getChildren().clear();
        partRoot.getChildren().setAll(rootNodes);
        Set<String> currentIds = parts.stream().map(part -> part.id)
                .collect(java.util.stream.Collectors.toSet());
        hiddenPartIds.retainAll(currentIds);
        refreshPartVisibility();
        drawTransformGizmo();
    }

    private void paintPickedComponent(Node pickedNode) {
        if (!validateFields()) return;
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
            applyFields();
            double radius = brushRadius.getValue();
            String color = colorPicker.getValue().toString();
            SemanticMaterial semanticMaterial = SemanticMaterial.from(materialPicker.getValue());
            String material = semanticMaterial.name();
            String finish = finishPicker.getValue() == null
                    ? semanticMaterial.defaultFinish() : finishPicker.getValue();
            int painted = 0;
            for (CustomModelDefinition.Part part : parts) {
                double dx = worldPosition(part, candidate -> candidate.x)
                        - worldPosition(target, candidate -> candidate.x);
                double dy = worldPosition(part, candidate -> candidate.y)
                        - worldPosition(target, candidate -> candidate.y);
                double dz = worldPosition(part, candidate -> candidate.z)
                        - worldPosition(target, candidate -> candidate.z);
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
                appendImportedParts(template);
                nextPartNumber = parts.size() + 1;
                refreshPartList(firstAdded);
                refreshPreview();
                statusLabel.setText("Added template: " + template.name);
            });
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private void saveModel() {
        if (!applyFields()) return;
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
        } catch (IOException | IllegalArgumentException ex) {
            error("Could not open model", ex.getMessage());
        }
    }

    private void saveComponent() {
        if (!applyFields()) return;
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
                appendImportedParts(component);
                nextPartNumber = parts.size() + 1;
                refreshPartList(firstAdded);
                refreshPreview();
                modelName.requestFocus();
            });
        } catch (IOException | IllegalArgumentException ex) {
            error("Could not add component", ex.getMessage());
        }
    }

    private CustomModelDefinition.Part copyPart(CustomModelDefinition.Part source) {
        CustomModelDefinition.Part copy = new CustomModelDefinition.Part();
        copy.id = source.id;
        copy.parentId = source.parentId;
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

    private void appendImportedParts(CustomModelDefinition sourceModel) {
        java.util.Map<String, String> importedIds = new java.util.HashMap<>();
        for (CustomModelDefinition.Part source : sourceModel.parts) {
            importedIds.put(source.id, UUID.randomUUID().toString());
        }
        for (CustomModelDefinition.Part source : sourceModel.parts) {
            CustomModelDefinition.Part copy = copyPart(source);
            copy.id = importedIds.get(source.id);
            copy.parentId = importedIds.get(source.parentId);
            copy.name = sourceModel.name + " / " + source.name;
            parts.add(copy);
        }
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
                currentFile, copies, selectedPartIndex());
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
                    || !java.util.Objects.equals(a.id, b.id)
                    || !java.util.Objects.equals(a.parentId, b.parentId)
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

    private double parse(TextField field) {
        return Double.parseDouble(field.getText().trim());
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
