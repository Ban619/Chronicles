package com.abyss.world;

import com.abyss.utils.AssetLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapLayoutTest {

    @Test
    void villageHasWalkableRoutesAcrossTheRiverAndTowardTheCrypt() {
        GameMap village = new GameMap("maps/world_map.json", AssetLoader.getInstance());
        int center = village.getCols() / 2;
        int river = village.getRows() / 2;

        assertEquals(Tile.BRIDGE, village.getGroundTileId(river - 1, center));
        assertFalse(village.isBlocked(center * Tile.SIZE, (river - 1) * Tile.SIZE,
                Tile.SIZE, Tile.SIZE));
        assertFalse(village.isBlocked(center * Tile.SIZE, (river + 8) * Tile.SIZE,
                Tile.SIZE, Tile.SIZE));
    }

    @Test
    void forestClearsTheRoutesToBothRegionExits() {
        GameMap forest = new GameMap("maps/thornwood_forest.json", AssetLoader.getInstance());
        int center = forest.getCols() / 2;
        int middle = forest.getRows() / 2;

        assertFalse(forest.isBlocked(center * Tile.SIZE, 2 * Tile.SIZE,
                Tile.SIZE, Tile.SIZE));
        assertFalse(forest.isBlocked(2 * Tile.SIZE, middle * Tile.SIZE,
                Tile.SIZE, Tile.SIZE));
        assertFalse(forest.isBlocked(center * Tile.SIZE, (forest.getRows() - 2) * Tile.SIZE,
                Tile.SIZE, Tile.SIZE));
    }

    @Test
    void bossArenaHasSafeCentralPlatformAndWalkableEntrance() {
        GameMap arena = new GameMap("maps/dragons_maw.json", AssetLoader.getInstance());
        int centerCol = arena.getCols() / 2;
        int centerRow = arena.getRows() / 2;

        assertEquals(Tile.FLOOR_STONE, arena.getGroundTileId(centerRow, centerCol));
        assertFalse(arena.isBlocked(centerCol * Tile.SIZE, centerRow * Tile.SIZE,
                Tile.SIZE, Tile.SIZE));
        for (int row = 1; row < centerRow; row++) {
            assertFalse(arena.isBlocked(centerCol * Tile.SIZE, row * Tile.SIZE,
                    Tile.SIZE, Tile.SIZE), "north path should be clear at row " + row);
        }
    }

    @Test
    void regionsHaveDistinctWalkableLandmarksAndStableLayouts() {
        AssetLoader assets = AssetLoader.getInstance();
        GameMap village = new GameMap("maps/world_map.json", assets);
        GameMap forest = new GameMap("maps/thornwood_forest.json", assets);
        GameMap crypt = new GameMap("maps/crypt_of_echoes.json", assets);
        GameMap arena = new GameMap("maps/dragons_maw.json", assets);

        assertTrue(countObjects(village, Tile.WELL) > 0, "village should place its central well");
        assertTrue(countObjects(village, Tile.BENCH) > 0);
        assertTrue(countObjects(village, Tile.FENCE) > 0);
        assertTrue(countObjects(village, Tile.FLOWERS) > 0);
        assertTrue(countObjects(village, Tile.SHRUB) > 0);
        assertTrue(countObjects(village, Tile.LANTERN) >= 2);
        assertTrue(countObjects(forest, Tile.FERN) > 0);
        assertTrue(countObjects(forest, Tile.ROCK) > 0);
        assertTrue(countObjects(forest, Tile.MUSHROOM) > 0);
        assertTrue(countObjects(forest, Tile.REEDS) > 0);
        assertTrue(countObjects(forest, Tile.CRYSTAL) >= 2);
        assertTrue(countObjects(crypt, Tile.PILLAR) >= 4,
                "crypt pillar count: " + countObjects(crypt, Tile.PILLAR));
        assertTrue(countObjects(crypt, Tile.GRAVE) >= 2);
        assertTrue(countObjects(crypt, Tile.RUBBLE) > 0);
        assertTrue(countObjects(arena, Tile.PILLAR) >= 4);
        assertTrue(countObjects(arena, Tile.CRYSTAL) >= 4);

        GameMap regeneratedVillage = new GameMap("maps/world_map.json", assets);
        for (int row = 0; row < village.getRows(); row++) {
            for (int col = 0; col < village.getCols(); col++) {
                assertEquals(village.getObjectTileId(row, col),
                        regeneratedVillage.getObjectTileId(row, col));
            }
        }
    }

    private int countObjects(GameMap map, int tileId) {
        int count = 0;
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                if (map.getObjectTileId(row, col) == tileId) {
                    count++;
                    assertTrue(new Tile(tileId).isWalkable(),
                            "decoration should remain walkable: " + tileId);
                    assertFalse(map.isBlocked(col * Tile.SIZE, row * Tile.SIZE,
                            Tile.SIZE, Tile.SIZE),
                            "decoration should not block movement: " + tileId);
                }
            }
        }
        return count;
    }
}
