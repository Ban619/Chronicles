package com.abyss.utils;

import com.abyss.entities.Player;
import com.abyss.world.GameMap;
import com.abyss.world.Tile;
import com.abyss.world.WorldManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveLoadManagerTest {

    @Test
    void openedChestStateSurvivesFullSaveAndLoad(@TempDir Path saveDirectory) {
        AssetLoader assets = AssetLoader.getInstance();
        WorldManager world = new WorldManager(assets, "maps/world_map.json");
        GameMap map = world.getActiveMap();
        int row = 4;
        int col = map.getCols() - 6;
        map.restorePersistentState(Map.of("openedChests",
                List.of(row * map.getCols() + col)));

        SaveLoadManager saves = new SaveLoadManager(saveDirectory.toFile());
        Player player = new Player("Tester", 96, 96, assets);
        assertTrue(saves.save(1, player, map.getMapName(), world.getActivePath(), world));

        WorldManager loadedWorld = new WorldManager(assets, "maps/world_map.json");
        Player loadedPlayer = new Player("New", 0, 0, assets);
        assertTrue(saves.load(1, loadedPlayer, loadedWorld));
        assertEquals(Tile.CHEST_OPEN,
                loadedWorld.getActiveMap().getObjectTileId(row, col));
    }
}
