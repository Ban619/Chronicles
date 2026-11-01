package com.abyss.world;

import com.abyss.utils.AssetLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
