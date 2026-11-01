package com.abyss.campaign;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChapterOneProgressTest {

    @Test
    void questRequiresThreeThornwoodDefeatsAndElderTurnIn() {
        ChapterOneProgress progress = new ChapterOneProgress();

        assertTrue(progress.acceptQuest());
        assertFalse(progress.recordForestDefeat(false, false));
        assertFalse(progress.recordForestDefeat(true, true));
        assertTrue(progress.recordForestDefeat(true, false));
        assertTrue(progress.recordForestDefeat(true, false));
        assertEquals(ChapterOneProgress.Stage.CLEAR_THORNWOOD, progress.getStage());
        assertFalse(progress.turnInRoadQuest());

        assertTrue(progress.recordForestDefeat(true, false));
        assertEquals(ChapterOneProgress.Stage.RETURN_TO_ELDER, progress.getStage());
        assertTrue(progress.turnInRoadQuest());
        assertEquals(ChapterOneProgress.Stage.FIND_CRYPT_RELIC, progress.getStage());
    }

    @Test
    void relicBossAndEpilogueMustBeCompletedInOrder() {
        ChapterOneProgress progress = new ChapterOneProgress();
        progress.acceptQuest();
        for (int i = 0; i < 3; i++) progress.recordForestDefeat(true, false);
        progress.turnInRoadQuest();

        assertTrue(progress.canEnterCrypt());
        assertFalse(progress.canEnterBossArena());
        assertTrue(progress.recoverCryptRelic());
        assertTrue(progress.canEnterCrypt());
        assertTrue(progress.canEnterBossArena());
        assertFalse(progress.completeChapter());
        assertTrue(progress.defeatMalachar());
        assertFalse(progress.canEnterBossArena());
        assertTrue(progress.completeChapter());
        assertEquals(ChapterOneProgress.Stage.COMPLETE, progress.getStage());
    }

    @Test
    void savedProgressRoundTripsAndInvalidValuesAreBounded() {
        ChapterOneProgress progress = new ChapterOneProgress();
        progress.acceptQuest();
        progress.recordForestDefeat(true, false);
        progress.recordForestDefeat(true, false);

        ChapterOneProgress restored = new ChapterOneProgress();
        restored.restoreFromSaveMap(progress.toSaveMap());
        assertEquals(ChapterOneProgress.Stage.CLEAR_THORNWOOD, restored.getStage());
        assertEquals(2, restored.getForestDefeats());

        restored.restoreFromSaveMap(Map.of("stage", "CLEAR_THORNWOOD", "forestDefeats", 99));
        assertEquals(ChapterOneProgress.Stage.RETURN_TO_ELDER, restored.getStage());
        assertEquals(3, restored.getForestDefeats());

        restored.restoreFromSaveMap(Map.of("stage", "unknown", "forestDefeats", -7));
        assertEquals(ChapterOneProgress.Stage.MEET_ELDER, restored.getStage());
        assertEquals(0, restored.getForestDefeats());
    }
}
