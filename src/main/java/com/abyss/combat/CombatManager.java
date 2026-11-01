package com.abyss.combat;

import com.abyss.entities.Enemy;
import com.abyss.entities.Player;
import com.abyss.utils.AssetLoader;
import com.abyss.utils.InputHandler;
import com.abyss.world.GameMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Resolves real-time player attacks against enemies in the current map. */
public final class CombatManager {

    private static final double ATTACK_COOLDOWN = 0.38;
    private static final double ATTACK_RANGE = 58;
    private static final double ABILITY_COOLDOWN = 0.65;
    private double attackCooldown;
    private double abilityCooldown;
    private double swingTimer;
    private double downedTimer;
    private final BossBattleManager bossManager = new BossBattleManager();

    public void update(double dt, InputHandler input, Player player, GameMap map) {
        attackCooldown = Math.max(0, attackCooldown - dt);
        abilityCooldown = Math.max(0, abilityCooldown - dt);
        swingTimer = Math.max(0, swingTimer - dt);
        if (!player.isAlive()) {
            downedTimer += dt;
            if (downedTimer >= 2.5) {
                player.recoverAfterDefeat(map);
                map.spawnParticle(player.getX(), player.getY() - 12,
                        "You recover. Nothing was lost.", Color.web("#c8a96e"));
                downedTimer = 0;
            }
            return;
        }
        downedTimer = 0;

        if (input.isJustPressed("q")) {
            castLearnedMove(0, player, map);
            return;
        }
        if (input.isJustPressed("r")) {
            castLearnedMove(1, player, map);
            return;
        }
        if (!input.isJustPressed("space") || attackCooldown > 0) return;

        attackCooldown = ATTACK_COOLDOWN;
        swingTimer = 0.18;
        AssetLoader.getInstance().playSfx("audio/attack_hit.mp3");

        Enemy target = findTarget(player, map, ATTACK_RANGE);
        if (target == null) return;

        int baseDamage = Math.max(1, (int) ((player.getStrength()
                + player.getEquipment().getTotalAttack())
                * (1 + player.getModifier("real_time_damage"))));
        double criticalChance = 0.05 + player.getLuck() * 0.01
                + player.getModifier("crit_chance");
        boolean critical = Math.random() < Math.max(0, Math.min(0.6, criticalChance));
        int previousBossPhase = target.getBossPhase();
        int dealt = target.takeDamage(critical ? (int) Math.ceil(baseDamage * 1.6) : baseDamage);
        bossManager.checkPhaseTransition(target);
        announceBossPhase(target, previousBossPhase, map);
        map.spawnDamageNumber(target.getX() + target.getWidth() / 2f,
                target.getY(), dealt, critical);
        map.resolveEnemyDefeat(target, player);
    }

    private void castLearnedMove(int moveIndex, Player player, GameMap map) {
        if (abilityCooldown > 0 || moveIndex >= player.getLearnedMoves().size()) return;
        Move move = player.getLearnedMoves().get(moveIndex);
        if (move.getType() == Move.Type.HEAL || move.getType() == Move.Type.BUFF) {
            if (!player.spendMp(move.getMpCost())) return;
            if (move.getType() == Move.Type.HEAL) player.heal(move.getPower());
            else player.applyModifier("real_time_damage", move.getPower() / 100.0);
            abilityCooldown = ABILITY_COOLDOWN;
            map.spawnParticle(player.getX(), player.getY() - 12,
                    move.getName() + "!", Color.web("#62d8e8"));
            return;
        }

        double range = move.getType() == Move.Type.PHYSICAL ? 78 : 180;
        java.util.List<Enemy> targets = findTargets(player, map, range, move.hitsAllTargets());
        if (targets.isEmpty() || !player.spendMp(move.getMpCost())) return;
        abilityCooldown = ABILITY_COOLDOWN;
        for (Enemy target : targets) {
            if (!move.hits()) {
                map.spawnParticle(target.getX(), target.getY() - 12,
                        move.getName() + " missed", Color.web("#d5c7a1"));
                continue;
            }

            if (move.getType() != Move.Type.DEBUFF) {
                int baseDamage = move.getPower();
                if (move.getType() == Move.Type.PHYSICAL) baseDamage += player.getStrength() / 2;
                double damageModifier = player.getModifier("real_time_damage");
                if (move.getType() == Move.Type.MAGICAL)
                    damageModifier += player.getModifier("spell_power");
                baseDamage = Math.max(1, (int) (baseDamage * (1 + damageModifier)));
                double critChance = Math.max(0, Math.min(0.6,
                        0.05 + player.getLuck() * 0.01 + player.getModifier("crit_chance")));
                boolean critical = Math.random() < critChance;
                int previousBossPhase = target.getBossPhase();
                int dealt = target.takeDamage(critical ? (int) Math.ceil(baseDamage * 1.6) : baseDamage,
                        move.getDefensePenetration());
                map.spawnDamageNumber(target.getCenterX(), target.getY(), dealt, critical);
                bossManager.checkPhaseTransition(target);
                announceBossPhase(target, previousBossPhase, map);
            }

            if (move.getStatusEffect() != null) {
                target.applyEffect(new StatusEffect(move.getStatusEffect(), 3.0));
            }
            map.spawnParticle(target.getX(), target.getY() - 12,
                    move.getName() + "!", Color.web("#62d8e8"));
            map.resolveEnemyDefeat(target, player);
        }
    }

    private void announceBossPhase(Enemy target, int previousPhase, GameMap map) {
        if (!target.isBoss() || target.getBossPhase() <= previousPhase) return;
        String phase = switch (target.getBossPhase()) {
            case 2 -> "Phase II — the Abyss awakens!";
            case 3 -> "Phase III — survive the nova!";
            default -> "The boss changes its stance!";
        };
        map.spawnParticle(target.getCenterX(), target.getY() - 24, phase, Color.web("#f0c040"));
    }

    private Enemy findTarget(Player player, GameMap map, double maxRange) {
        java.util.List<Enemy> targets = findTargets(player, map, maxRange, false);
        return targets.isEmpty() ? null : targets.get(0);
    }

    private java.util.List<Enemy> findTargets(Player player, GameMap map, double maxRange,
                                               boolean allTargets) {
        double facingX = player.getFacingX();
        double facingY = player.getFacingY();
        java.util.List<Enemy> targets = new java.util.ArrayList<>();
        Enemy nearest = null;
        double nearestDistance = maxRange;

        for (Enemy enemy : map.getEnemies()) {
            if (!enemy.isActive() || !enemy.isAlive()) continue;
            double dx = enemy.getCenterX() - player.getCenterX();
            double dy = enemy.getCenterY() - player.getCenterY();
            double distance = Math.hypot(dx, dy);
            if (distance > maxRange || distance == 0) continue;
            double forward = (dx * facingX + dy * facingY) / distance;
            if (forward < 0.15) continue;
            if (distance < nearestDistance) {
                nearest = enemy;
                nearestDistance = distance;
            }
            if (allTargets) targets.add(enemy);
        }
        if (!allTargets && nearest != null) targets.add(nearest);
        return targets;
    }

    /** Draws a short directional melee arc in world coordinates. */
    public void render(GraphicsContext gc, Player player) {
        if (swingTimer <= 0) return;
        double progress = 1 - swingTimer / 0.18;
        double centerX = player.getCenterX() + player.getFacingX() * 24;
        double centerY = player.getCenterY() + player.getFacingY() * 24;
        double sideX = -player.getFacingY() * 18;
        double sideY = player.getFacingX() * 18;

        gc.save();
        gc.setGlobalAlpha(Math.min(0.9, swingTimer / 0.18));
        gc.setStroke(Color.web("#f4d58a"));
        gc.setLineWidth(4 + progress * 3);
        gc.strokeLine(centerX - sideX, centerY - sideY,
                centerX + sideX, centerY + sideY);
        gc.setStroke(Color.color(1, 1, 1, 0.7));
        gc.setLineWidth(1.5);
        gc.strokeLine(centerX - sideX * 0.65, centerY - sideY * 0.65,
                centerX + sideX * 0.65, centerY + sideY * 0.65);
        gc.restore();
    }
}
