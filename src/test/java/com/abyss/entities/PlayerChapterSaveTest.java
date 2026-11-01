package com.abyss.entities;

import com.abyss.campaign.ChapterOneProgress;
import com.abyss.utils.AssetLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerChapterSaveTest {

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
