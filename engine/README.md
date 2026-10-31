# Abyss Model Studio

Run the game with `mvn javafx:run`, switch to 3D with `V`, then press `M` to open the model studio.

The studio builds editable models from box, sphere, cylinder, cone, and ring mesh components. Select a component to rename it or adjust its position, dimensions, rotation, base color, semantic material, and finish. Material presets cover wood, stone, brick, iron, gold, glass, leather, foliage, and flame; finishes range from rough and matte to polished, glass, and emissive ug uban pa. Presets affect rendered reflectance, glass transparency, and flame self-illumination as well as providing a starting color. These surface responses use JavaFX Phong lighting as a practical approximation, not a full physically based renderer. Drag in the preview to orbit; use the mouse wheel to zoom. Choose a map object in **Use for tile** to replace that tile's 3D appearance, then save the model.

The scene component list supports duplication, deletion, and ordering. Use **Save Component** to store a reusable assembly in `engine/components/`. Use **Add Component** to import that assembly into the current model, then move or recolor its parts and bind the finished model to a game tile. This keeps reusable assemblies separate from tile-bound model assets.

The Component Inspector is a docked side panel. Close it with **×** in its title bar and reopen it with **Component Inspector** in the toolbar; its divider position is preserved while it is hidden.

Models are JSON files in `engine/models/`. Saving a model bound to a tile refreshes the current 3D map immediately. Custom model coordinates use tile-center origin, with positive Y pointing upward. Model dimensions and positions are in world pixels (32 pixels per map tile).

Studio saves validate component shapes, dimensions, transforms, and colors before replacing a JSON file. Writes use a temporary file and atomic replacement where the filesystem supports it.

Shortcuts: `Ctrl+S` saves a tile model, `Ctrl+Shift+S` saves a reusable component, `Ctrl+O` opens a model, `Ctrl+D` duplicates the selected part, and `Delete` removes the selected part unless a text field is focused.

In the game, `Q` and `R` cast the first two learned active skills; they use MP, have accuracy checks, and apply supported timed status effects.

A model file contains a name, the target tile ID, and a list of primitive parts. `starter_lantern.json` is a preview-only example; `components/lantern_cluster.json` is a reusable component example. Available bindings include stone/brick walls, doors, chests, signs, torches, and tree parts.
