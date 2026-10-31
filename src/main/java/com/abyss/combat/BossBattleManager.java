package com.abyss.combat;

import com.abyss.entities.Enemy;

/**
 * BossBattleManager — Raises boss damage and attack cadence as health falls.
 */
public class BossBattleManager {

    private int     phase       = 1;
    private Enemy activeBoss;

    public void init(Enemy boss) {
        phase       = boss.getBossPhase();
        activeBoss = boss;
    }

    /** Called after a real-time hit; phase changes are applied immediately. */
    public void checkPhaseTransition(Enemy boss) {
        if (!boss.isBoss()) return;
        if (activeBoss != boss) init(boss);

        double hpPct = (double) boss.getHp() / boss.getMaxHp();
        int hpPhase = hpPct <= 0.30 ? 3 : hpPct <= 0.60 ? 2 : 1;
        phase = Math.max(phase, Math.max(hpPhase, boss.getBossPhase()));
        if (boss.getBossPhase() < phase) boss.setBossPhase(phase);
    }

    public int     getPhase()            { return phase; }
    public boolean isInitialised()       { return activeBoss != null; }
}
