package com.abyss.combat;

import com.abyss.entities.Enemy;
import com.abyss.entities.Player;

/**
 * Move — A combat action with name, type, power, MP cost, accuracy, and optional status effect.
 */
public class Move {

    public enum Type { PHYSICAL, MAGICAL, HEAL, BUFF, DEBUFF }

    private final String name;
    private final Type   type;
    private final int    power;
    private final int    mpCost;
    private final int    accuracy; // 0-100
    private final String statusEffect; // null = no effect

    public Move(String name, Type type, int power, int mpCost, int accuracy, String statusEffect) {
        this.name         = name;
        this.type         = type;
        this.power        = power;
        this.mpCost       = mpCost;
        this.accuracy     = accuracy;
        this.statusEffect = statusEffect;
    }

    public static Move basicAttack(int strength) {
        return new Move("Attack", Type.PHYSICAL, strength, 0, 100, null);
    }

    public boolean hits() { return (int)(Math.random() * 100) < accuracy; }

    public String getName()        { return name; }
    public Type   getType()        { return type; }
    public int    getPower()       { return power; }
    public int    getMpCost()      { return mpCost; }
    public int    getAccuracy()    { return accuracy; }
    public String getStatusEffect(){ return statusEffect; }
}
