package com.abyss.campaign;

import java.util.Map;

/** Persistent, ordered objectives for the first Chronicles chapter. */
public final class ChapterOneProgress {

    public enum Stage {
        MEET_ELDER,
        CLEAR_THORNWOOD,
        RETURN_TO_ELDER,
        FIND_CRYPT_RELIC,
        DEFEAT_MALACHAR,
        RETURN_AFTER_BOSS,
        COMPLETE
    }

    private Stage stage = Stage.MEET_ELDER;
    private int forestDefeats;

    public Stage getStage() { return stage; }
    public int getForestDefeats() { return forestDefeats; }

    public boolean acceptQuest() {
        if (stage != Stage.MEET_ELDER) return false;
        stage = Stage.CLEAR_THORNWOOD;
        forestDefeats = 0;
        return true;
    }

    public boolean recordForestDefeat(boolean inThornwood, boolean boss) {
        if (stage != Stage.CLEAR_THORNWOOD || !inThornwood || boss) return false;
        forestDefeats = Math.min(3, forestDefeats + 1);
        if (forestDefeats == 3) stage = Stage.RETURN_TO_ELDER;
        return true;
    }

    public boolean turnInRoadQuest() {
        if (stage != Stage.RETURN_TO_ELDER) return false;
        stage = Stage.FIND_CRYPT_RELIC;
        return true;
    }

    public boolean recoverCryptRelic() {
        if (stage != Stage.FIND_CRYPT_RELIC) return false;
        stage = Stage.DEFEAT_MALACHAR;
        return true;
    }

    public boolean defeatMalachar() {
        if (stage != Stage.DEFEAT_MALACHAR) return false;
        stage = Stage.RETURN_AFTER_BOSS;
        return true;
    }

    public boolean completeChapter() {
        if (stage != Stage.RETURN_AFTER_BOSS) return false;
        stage = Stage.COMPLETE;
        return true;
    }

    public boolean canEnterBossArena() {
        return stage == Stage.DEFEAT_MALACHAR;
    }

    public String getObjective() {
        return switch (stage) {
            case MEET_ELDER -> "Find Elder Aldric";
            case CLEAR_THORNWOOD -> "Thornwood: clear enemies " + forestDefeats + "/3";
            case RETURN_TO_ELDER -> "Return to Elder Aldric";
            case FIND_CRYPT_RELIC -> "Crypt: find the Dawn Seal";
            case DEFEAT_MALACHAR -> "Dragon's Maw: defeat Malachar";
            case RETURN_AFTER_BOSS -> "Return to Elder Aldric";
            case COMPLETE -> "Chapter 1 complete";
        };
    }

    public Map<String, Object> toSaveMap() {
        return Map.of("stage", stage.name(), "forestDefeats", forestDefeats);
    }

    public void restoreFromSaveMap(Map<?, ?> data) {
        if (data == null) {
            stage = Stage.MEET_ELDER;
            forestDefeats = 0;
            return;
        }

        Object savedStage = data.get("stage");
        try {
            stage = savedStage instanceof String value ? Stage.valueOf(value) : Stage.MEET_ELDER;
        } catch (IllegalArgumentException ex) {
            stage = Stage.MEET_ELDER;
        }
        Object savedDefeats = data.get("forestDefeats");
        forestDefeats = savedDefeats instanceof Number number
                ? Math.max(0, Math.min(3, number.intValue())) : 0;
        if (stage.ordinal() >= Stage.RETURN_TO_ELDER.ordinal()) forestDefeats = 3;
        if (stage.ordinal() < Stage.CLEAR_THORNWOOD.ordinal()) forestDefeats = 0;
        if (stage == Stage.CLEAR_THORNWOOD && forestDefeats == 3)
            stage = Stage.RETURN_TO_ELDER;
    }
}
