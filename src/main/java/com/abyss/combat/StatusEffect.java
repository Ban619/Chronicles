package com.abyss.combat;

import com.abyss.entities.Enemy;

/** A timed condition applied to an enemy during real-time combat. */
public class StatusEffect {

    private final String type;
    private double durationRemaining;
    private double damageTimer;

    public StatusEffect(String type, double durationSeconds) {
        this.type = type == null ? "" : type.toLowerCase();
        this.durationRemaining = Math.max(0, durationSeconds);
    }

    /** Advances the effect and applies damage-over-time once per second. */
    public boolean tick(Enemy target, double dt) {
        if (durationRemaining <= 0) return false;
        durationRemaining = Math.max(0, durationRemaining - dt);
        damageTimer += dt;
        while (damageTimer >= 1.0) {
            switch (type) {
                case "poison" -> target.takeDamage(8);
                case "burn" -> target.takeDamage(6);
                case "bleed" -> target.takeDamage(5);
            }
            damageTimer -= 1.0;
        }
        return durationRemaining > 0;
    }

    public double getDamageTakenModifier() { return type.equals("marked") ? 1.3 : 1.0; }

    public boolean disablesMovement() {
        return type.equals("freeze") || type.equals("sleep") || type.equals("stun");
    }

    public String getType() { return type; }
    public double getDurationRemaining() { return durationRemaining; }
    public int getTurnsLeft() { return (int) Math.ceil(durationRemaining); }
}
