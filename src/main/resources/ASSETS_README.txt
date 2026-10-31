# Assets Directory

Place your game assets here following these conventions:

## sprites/
Sprite sheets (PNG) used by the game:
  player.png            – 4×4 walk animation (32×32 per frame)
  enemy_wolf.png        – Enemy sprite sheets
  enemy_golem.png
  enemy_wraith.png
  boss_malachar.png     – Boss sprite (can be larger, e.g. 64×64)

## tiles/
  tileset.png           – 16 tiles/row, 32×32 per tile
                          Rows match Tile.java constants

## audio/
  theme_village.mp3     – Background music (looping)
  theme_battle.mp3
  theme_boss.mp3
  rain.mp3
  storm.mp3
  wind.mp3
  attack_hit.mp3        – SFX
  ui_click.mp3
  level_up.mp3
  item_pickup.mp3
  door_open.mp3
  step.mp3                – Player footsteps

## fonts/
  cinzel.ttf            – Display font (download from Google Fonts)

## ui-elements/
  hotbar_slot.png       – 48×48 slot background
  panel_bg.png          – 9-slice panel background

---
Free asset sources:
  https://itch.io/game-assets/free
  https://opengameart.org
  https://kenney.nl/assets

NPCs and party members use the procedural, role-specific character art in
CharacterPainter.java, so character sprite sheets are optional.
