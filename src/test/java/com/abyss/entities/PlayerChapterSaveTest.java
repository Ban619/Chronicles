package com.abyss.entities;

import com.abyss.campaign.ChapterOneProgress;
import com.abyss.items.Item;
import com.abyss.items.Material;
import com.abyss.utils.AssetLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerChapterSaveTest {

    @Test
    void questRelicCannotBeDiscardedOrConsumed() {
        Player player = new Player("Tester", 0, 0, AssetLoader.getInstance());
        Item relic = new Material("crypt_relic", "Dawn Seal", "Quest relic.",
                "crypt_relic", 1, 1, 0, Item.Rarity.EPIC);
        assertTrue(player.addItem(relic));
        int relicSlot = -1;
        for (int i = 0; i < player.getInventory().length; i++) {
            if (player.getInventory()[i] != null
                    && "crypt_relic".equals(player.getInventory()[i].getId())) relicSlot = i;
        }

        assertNotEquals(-1, relicSlot);
        assertFalse(player.removeItem(relicSlot));
        assertFalse(player.removeItems(Map.of("crypt_relic", 1)));
        assertTrue(player.hasItem("crypt_relic"));
    }

    @Test
    void chapterObjectiveAndTutorialStateSurvivePlayerSaveRestore() {
        AssetLoader assets = AssetLoader.getInstance();
        Player saved = new Player("Tester", 0, 0, assets);
        ChapterOneProgress chapter = saved.getChapterOne();
        chapter.acceptQuest();
        chapter.recordForestDefeat(true, false);
        chapter.recordForestDefeat(true, false);
        saved.completeTutorial();

        Map<String, Object> snapshot = saved.toSaveMap();
        Player restored = new Player("New", 0, 0, assets);
        restored.restoreFromSaveMap(snapshot);

        assertEquals(ChapterOneProgress.Stage.CLEAR_THORNWOOD,
                restored.getChapterOne().getStage());
        assertEquals(2, restored.getChapterOne().getForestDefeats());
        assertEquals("Thornwood: clear enemies 2/3",
                restored.getChapterOne().getObjective());
        assertTrue(restored.isTutorialComplete());
    }
}
