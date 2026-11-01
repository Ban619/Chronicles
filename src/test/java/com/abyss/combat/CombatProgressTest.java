package com.abyss.combat;

import com.abyss.entities.Enemy;
import com.abyss.entities.Player;
import com.abyss.skills.SkillTree;
import com.abyss.utils.AssetLoader;
import com.abyss.world.GameMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatProgressTest {

    @Test
    void cleaveTargetsMultipleEnemiesAndHeavyStrikePenetratesDefense() {
        SkillTree tree = new SkillTree();
        Move cleave = tree.getNode("w_cleave").getEffect().getGrantedMove();
        Move heavyStrike = tree.getNode("w_heavy_strike").getEffect().getGrantedMove();
        assertNotNull(cleave);
        assertTrue(cleave.hitsAllTargets());
        assertNotNull(heavyStrike);
        assertEquals(0.30, heavyStrike.getDefensePenetration());

        AssetLoader assets = AssetLoader.getInstance();
        Enemy armored = new Enemy("Armored", 0, 0, 100, 1, 10, 1,
                0, 0, false, "enemy_golem", assets);
        assertEquals(10, armored.takeDamage(20));
        assertEquals(13, armored.takeDamage(20, heavyStrike.getDefensePenetration()));
    }

    @Test
    void defeatRecoveryRestoresAtEntranceWithoutLosingGoldOrItems() {
        AssetLoader assets = AssetLoader.getInstance();
        Player player = new Player("Tester", 300, 300, assets);
        GameMap map = new GameMap("maps/world_map.json", assets);
        int gold = player.getGold();
        int potions = player.countItems("potion_hp");
        player.takeDamage(100_000);

        player.recoverAfterDefeat(map);

        assertTrue(player.isAlive());
        assertEquals(player.getMaxHp() / 2, player.getHp());
        assertEquals(player.getMaxMp(), player.getMp());
        assertEquals(map.getStartX(), player.getX());
        assertEquals(map.getStartY(), player.getY());
        assertEquals(gold, player.getGold());
        assertEquals(potions, player.countItems("potion_hp"));
    }
}
