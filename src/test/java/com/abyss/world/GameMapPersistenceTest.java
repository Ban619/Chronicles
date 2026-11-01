package com.abyss.world;

import com.abyss.entities.Player;
import com.abyss.utils.AssetLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameMapPersistenceTest {

    @Test
    void openedChestsRemainOpenAfterMapStateRoundTrip() {
        AssetLoader assets = AssetLoader.getInstance();
        GameMap original = new GameMap("maps/world_map.json", assets);
        int row = 4;
        int col = original.getCols() - 6;
        int chestIndex = row * original.getCols() + col;
        assertEquals(Tile.CHEST, original.getObjectTileId(row, col));

        original.restorePersistentState(Map.of("openedChests", List.of(chestIndex)));
        Map<String, Object> savedState = original.toPersistentState();

        GameMap restored = new GameMap("maps/world_map.json", assets);
        restored.restorePersistentState(savedState);

        assertEquals(Tile.CHEST_OPEN, restored.getObjectTileId(row, col));
        assertEquals(savedState, restored.toPersistentState());
    }

    @Test
    void missingRelicCanBeRecoveredAfterTheCryptChestWasOpened() {
        AssetLoader assets = AssetLoader.getInstance();
        GameMap crypt = new GameMap("maps/crypt_of_echoes.json", assets);
        int row = 24 * Math.max(1, crypt.getRows() / 28);
        int col = 20 * Math.max(1, crypt.getCols() / 32);
        crypt.restorePersistentState(Map.of("openedChests",
                List.of(row * crypt.getCols() + col)));

        Player player = new Player("Tester", 0, 0, assets);
        var progress = player.getChapterOne();
        progress.acceptQuest();
        for (int i = 0; i < 3; i++) progress.recordForestDefeat(true, false);
        progress.turnInRoadQuest();
        progress.recoverCryptRelic();
        assertEquals(Tile.CHEST_OPEN, crypt.getObjectTileId(row, col));

        crypt.ensureRelicRecoverable(player);

        assertEquals(Tile.CHEST, crypt.getObjectTileId(row, col));
    }
}
