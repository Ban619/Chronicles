# Chronicles of the Abyss
### 2D/3D Tile-Based RPG 

---

## Features

| System | Details |
|---|---|
| **World** | Four distinct biomes, varied forest layouts, tile collision, and region transitions |
| **Day/Night and Weather** | Time-of-day lighting, weather states, and particle effects |
| **Player** | HP/MP/XP/level, core stats, stackable inventory, equipment, crafting, and hotbar |
| **Party and NPCs** | Recruitable companions, patrol behavior, schedules, and reputation-aware dialogue |
| **Real-time Combat** | Directional melee, multi-target Cleave, dodge chance from skills, and visibly telegraphed enemy attacks |
| **Bosses** | Boss damage and attack rate increase as health drops through phases |
| **Progression** | Skill-tree overlay, XP, levels, a quest objective, merchant purchases, equipment, and crafting materials |
| **Rendering** | 2D tile renderer and optional JavaFX 3D perspective |
| **Model Studio** | Compose, preview, validate, and save reusable 3D tile models and components |
| **Save/Load** | Three JSON slots under `~/.chronicles_abyss/saves/`; F5/F9 quick actions |
| **Input** | Keyboard movement and interaction, touch movement, and mouse-look in 3D mode |

Chapter 1 is a complete story path: meet Elder Aldric, clear three enemies in Thornwood, return for the Crypt clue, recover the Dawn Seal from the Crypt, defeat Malachar in Dragon's Maw, then return to Aldric for the ending. The active objective is shown in the HUD and saved with the player. A first-run guide explains controls and the route. Merchants sell health and mana potions and a starter weapon. Defeat returns the player to the current region's entrance with no loss of items or gold. Achievements and JSON-driven map contents are not implemented yet.

Procedural maps use seeded layouts for consistent landmarks and routes. Ashenveil includes a river bridge, connected village roads, houses, a fenced garden, flowers, shrubs, lanterns, a well, and a bench. Thornwood has clear paths to its exits, a walled ruin, a winding stream, and biome-specific foliage, mushrooms, reeds, and crystals. The Crypt has torch-lit chambers with pillars, graves, rubble, and crystals, while Dragon's Maw features a safe central platform, walkable paths through its lava ring, and surrounding pillars and crystals. These environmental details are drawn procedurally and do not require a tileset.

---

## Folder Structure

```
chronicles-of-the-abyss/
├── pom.xml
├── engine/                         # Editable 3D model and component JSON
│   ├── models/
│   └── components/
└── src/main/
  ├── java/
  │   ├── module-info.java
  │   └── com/abyss/
  │       ├── main/                # JavaFX entry point and game loop
  │       ├── combat/              # Real-time player attacks and boss phases
  │       ├── entities/            # Player, enemies, NPCs, companions
  │       ├── items/               # Inventory item and equipment models
  │       ├── lighting/            # 2D lighting renderer
  │       ├── skills/              # Skill graph and effects
  │       ├── ui/                  # HUD, inventory, merchant, and skill overlays
  │       ├── utils/               # Input, assets, dialogue, save/load
  │       ├── world/               # Maps, tiles, camera, transitions, weather
  │       └── world3d/             # 3D renderer, model library, and Studio
  └── resources/
    ├── maps/                    # Map metadata JSON
    └── ASSETS_README.txt        # Expected optional sprites/audio/fonts
```

---

## Prerequisites

| Tool | Minimum Version |
|------|----------------|
| JDK  | 21 (OpenJDK or Oracle) |
| Maven | 3.8+ |
| JavaFX | 21 (via Maven dependency, downloaded automatically) |

---

## Building & Running

### 1. Clone / Extract
```bash
unzip chronicles-of-the-abyss.zip
cd chronicles-of-the-abyss
```

### 2. Build
```bash
mvn clean package -DskipTests
```

### 3. Run (via Maven plugin)
```bash
mvn javafx:run
```

### 4. Run the fat JAR directly
```bash
java -jar target/chronicles-of-the-abyss-1.0.0-shaded.jar
```

> **Note:** If you get a module error with the fat JAR, use the Maven plugin method.

---

## Controls

### Desktop (Keyboard)
| Key | Action |
|-----|--------|
| `W A S D` / `↑ ← ↓ →` | Move |
| `E` | Interact (NPC / Door / Chest); equip or unequip while inventory is open |
| `I` | Open inventory, equipment, and crafting |
| `M` | Open the 3D Model Studio |
| `1`–`9` | Use hotbar slot |
| `V` | Switch between 2D and 3D perspectives |
| Mouse | Look around while in 3D perspective |
| `Space` | Directional melee attack; confirms skill-tree choices while that overlay is open |
| `Q` / `R` | Cast first / second learned skill-tree move |
| `Enter` | Confirm skill-tree choices; use items or craft while inventory is open |
| `Arrow keys` | Navigate inventory, equipment, and recipes |
| `U` | Use selected item |
| `C` | Craft selected recipe in the Crafting tab |
| `D` | Drop selected item |
| `Tab` | Switch inventory tabs |
| `Ctrl+S` / `Ctrl+Shift+S` | Save model / save reusable component in Model Studio |
| `Ctrl+O` / `Ctrl+D` / `Delete` | Open model / duplicate / remove selected Studio part |
| `Ctrl+Z` / `Ctrl+Y` | Undo / redo Studio edits when a text field is not focused |
| `F` | Frame all scene objects in the Model Studio viewport |
| `Esc` | Back / Close menu |
| `F5` | Quick-save (slot 1) |
| `F9` | Quick-load (slot 1) |

Press `E` near Elder Aldric to accept or advance the chapter quest. Only defeated Thornwood enemies count toward the road objective. Recover the Dawn Seal from the marked reliquary in the Crypt before entering Dragon's Maw. Return to Elder Aldric after Malachar falls to finish Chapter 1. Chapter progress and the first-run guide completion are saved. Press `E` near a merchant to open the shop; use `1`–`3` to buy an item. Enemy attacks have a short visual windup; wraiths attack from range, golems hit slowly and heavily, and wolves strike quickly. Malachar announces phase changes; his final nova has a large warning radius. Evasion skills can dodge attacks. Defeat restores the player at the current region entrance without removing gold or items.

Combine two movement keys for diagonal movement. In close-up perspective, the hero faces movement in eight compass directions, including northeast and southwest; mouse-look continues to rotate the camera freely. The 2D sprite sheet has four facing rows, so diagonal facing uses the nearest cardinal sprite.

The Model Studio uses a viewport-first editor layout: menus and workspace tabs above the viewport, an Outliner above Properties at the right, and a timeline-style strip below. The viewport toolbar provides View reset, Frame All, Select, Add, Object actions, and axis-alignment controls. **Frame All** (or `F`) fits the complete model in view, including components positioned away from the origin. The left tool shelf offers Select, Box select, Brush, and a toggle for the Create / Assets drawer. The right dock and its Outliner/Properties divider are resizable. The timeline is currently a static-pose reference; animation playback and keyframes are not implemented. Resize the window down to 720×480 as needed. Drag a scene object near another row's edge to reorder it; drop on the row center to nest it as a child. Child transforms are relative to their parent, and removing a parent removes its children. In the viewport, click an object to select it; enable **Box** and drag a rectangle to select every part whose projected bounds touch it, including parts hidden behind other parts. **Select** returns to click selection and orbiting; **Brush** paints the clicked part and nearby components. Use **Duplicate** or `Ctrl+D` to duplicate every selected object and its child hierarchy; **Delete** removes all selected objects and their descendants. Select a scene object to edit its name, primitive shape, semantic material, finish, color/opacity, and position, rotation, or dimensions. Click **Apply part changes** or press Enter in a transform field to commit numeric values; non-finite numbers and non-positive dimensions are rejected and highlighted. Inspector transform fields adapt to narrower panes. `Ctrl+S` saves a tile model, `Ctrl+Shift+S` saves a reusable component, `Ctrl+O` opens a model, and `Ctrl+Z` / `Ctrl+Y` undo and redo edits; the same commands are available in the Edit menu and Layout ribbon. Studio history retains up to 100 model edits per session.

### Mobile (Touch)
| Gesture | Action |
|---------|--------|
| Touch & drag (left half) | Move via virtual joystick |
| Tap (right half) | Interact |
| Swipe hotbar | Scroll slots |

Real-time melee currently uses the keyboard Space key; touch attack controls are not implemented.

---

## Adding Content

### New Region
1. Add map metadata under `src/main/resources/maps/` with `name`, `cols`, `rows`, `startX`, and `startY`.
2. Add the region's procedural layout, entity spawns, and transitions in `GameMap`.
3. Add the map path to a transition in `GameMap.buildTransitionsForBiome()`.

### New NPC
1. Add the NPC spawn in the appropriate `GameMap.spawn*()` method.
2. Add dialogue to `DialogueHandler.loadDefaultDialogues()`.

### New Enemy
1. Add the enemy spawn in the appropriate `GameMap.spawn*()` method.
2. Configure its stats and sprite key in the spawn call.

### Crafting Material
1. Add a `Material` item and include it in the biome loot table in `GameMap.dropCraftingLoot()`.
2. Add a `CraftingRecipe` to `CraftingSystem` using the material IDs and an item factory.

### New Item
1. Create a `Consumable`, `Equipment`, or `Material` instance.
2. Register it in the content flow that should grant it. Save/load supports all three item types.

### New Quest
Quest tracking is not implemented yet. A quest system will need persistent quest state, interaction/progression rules, and a connected UI before content can be authored.

---

## Asset Conventions

### Sprite Sheets
- Frame size: `32 × 32` pixels per frame
- Layout: 4 rows × 4 frames
  - Row 0 → Walk down
  - Row 1 → Walk left
  - Row 2 → Walk right
  - Row 3 → Walk up

### Tileset
- Cell size: `32 × 32` pixels
- 16 tiles per row
- Tile IDs must match constants in `Tile.java`

### Audio
- SFX: `.mp3` or `.wav`, short (< 2 sec)
- Music: `.mp3`, looping, path registered in `AssetLoader`

---

## Architecture Notes

- **Fixed timestep** (60 UPS) via `AnimationTimer` accumulator pattern
- **Entity composition** over inheritance beyond Entity base
- **Null-safe asset loading** — missing sprites fall back to coloured rectangles
- **No external asset required to run** — game starts with procedural fallback map
- **Responsive camera** — canvas binds to window, camera recalculates viewport each frame

---
