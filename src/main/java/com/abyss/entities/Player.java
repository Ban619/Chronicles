package com.abyss.entities;

import com.abyss.items.Item;
import com.abyss.items.Equipment;
import com.abyss.items.Consumable;
import com.abyss.utils.AssetLoader;
import com.abyss.utils.InputHandler;
import com.abyss.world.GameMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Player — The protagonist entity.
 *
 * Stats model:
 *   HP    – current / max hit points
 *   MP    – current / max mana points
 *   XP    – experience; levels up at threshold
 *   STR   – affects physical attack damage
 *   DEF   – damage reduction
 *   SPD   – movement speed + turn priority in combat
 *   LCK   – critical hit / item drop rate
 *
 * Movement:
 *   Desktop → WASD / Arrow keys;  E → interact
 *   Mobile  → touch-to-move (managed by InputHandler's virtual joystick)
 *
 * Sprite sheet layout (32×32 per frame, 4 directions × 4 frames):
 *   Row 0 – walk down;  Row 1 – walk left;  Row 2 – walk right;  Row 3 – walk up
 */
public class Player extends Entity {

    // ── Stats ─────────────────────────────────────────────────────────────
    private int    level     = 1;
    private int    xp        = 0;
    private int    xpToNext  = 100;
    private int    maxHp     = 100;
    private int    hp        = 100;
    private int    maxMp     = 50;
    private int    mp        = 50;
    private int    strength  = 10;
    private int    defense   = 5;
    private int    speed     = 4;
    private int    luck      = 3;
    private int    gold       = 150;
    private int    reputation = 0;       // -100 (villain) … +100 (hero)

    // ── Customisation ─────────────────────────────────────────────────────
    private String  hairStyle  = "short";
    private Color   hairColor  = Color.web("#3a1a00");
    private Color   skinTone   = Color.web("#f4c89a");
    private String  clothing   = "adventurer";
    private String  accessory  = "none";

    // ── Movement ──────────────────────────────────────────────────────────
    private static final float BASE_SPEED = 120f;  // pixels / second
    private float velX = 0, velY = 0;
    private boolean moving = false;

    // ── Animation ─────────────────────────────────────────────────────────
    private int    direction   = 0;   // Sprite row: 0=down 1=left 2=right 3=up
    private double facingX     = 0;
    private double facingY     = 1;
    private int    animFrame   = 0;
    private double animTimer   = 0;
    private double stepTimer   = 0;
    private static final double ANIM_RATE = 0.12; // seconds per frame

    // ── Inventory ─────────────────────────────────────────────────────────
    public static final int INVENTORY_SIZE = 24;
    private final Item[]      inventory   = new Item[INVENTORY_SIZE];
    private final Equipment   equipment   = new Equipment();
    private int               hotbarSlot  = 0;

    // ── Party ─────────────────────────────────────────────────────────────
    private final List<PartyMember> party = new ArrayList<>();
    public  static final int MAX_PARTY    = 3;

    // ── Assets ────────────────────────────────────────────────────────────
    private final AssetLoader assetLoader;
    private Image spriteSheet;

    // ─────────────────────────────────────────────────────────────────────
    public Player(String name, float startX, float startY, AssetLoader loader) {
        super(name, startX, startY, 32, 32);
        this.assetLoader = loader;
        this.spriteSheet = loader.getImage("sprites/player.png");
        initDefaultInventory();
    }

    // ── Initialisation ────────────────────────────────────────────────────
    private void initDefaultInventory() {
        // Give player a starting health potion and short sword
        inventory[0] = new Consumable("Health Potion", "Restores 50 HP", "potion_hp", 50, 0, 25);
        inventory[1] = new Consumable("Mana Potion",   "Restores 30 MP", "potion_mp", 0, 30, 20);
    }

    // ── Input + Movement ──────────────────────────────────────────────────
    public void handleInput(InputHandler input, double dt) {
        velX = 0;
        velY = 0;

        // ── Keyboard ──────────────────────────────────────────────────────
        if (input.isDown("left")  || input.isDown("a")) velX = -1;
        if (input.isDown("right") || input.isDown("d")) velX =  1;
        if (input.isDown("up")    || input.isDown("w")) velY = -1;
        if (input.isDown("down")  || input.isDown("s")) velY =  1;

        // ── Virtual joystick (mobile) ──────────────────────────────────────
        float[] joy = input.getJoystickVector();
        if (joy[0] != 0 || joy[1] != 0) {
            velX = joy[0];
            velY = joy[1];
        }

        // Diagonal normalisation
        float len = (float) Math.sqrt(velX * velX + velY * velY);
        if (len > 0) {
            velX /= len;
            velY /= len;
        }
        moving = (len > 0);
        if (moving) faceVector(velX, velY);

        handleHotbarInput(input);
    }

    public void handleFirstPersonInput(InputHandler input, double dt, double yawRadians) {
        float forward = (input.isDown("up") ? 1 : 0) - (input.isDown("down") ? 1 : 0);
        float strafe = (input.isDown("right") ? 1 : 0) - (input.isDown("left") ? 1 : 0);
        float[] joy = input.getJoystickVector();
        if (joy[0] != 0 || joy[1] != 0) {
            strafe = joy[0];
            forward = -joy[1];
        }

        velX = (float) (Math.sin(yawRadians) * forward + Math.cos(yawRadians) * strafe);
        velY = (float) (Math.cos(yawRadians) * forward - Math.sin(yawRadians) * strafe);
        float length = (float) Math.sqrt(velX * velX + velY * velY);
        if (length > 1) {
            velX /= length;
            velY /= length;
        }
        moving = length > 0;
        if (length > 0) faceVector(velX, velY);
        handleHotbarInput(input);
    }

    public void faceVector(double x, double y) {
        if (x == 0 && y == 0) return;

        // Eight compass sectors, clockwise from north. Snap the 3D actor and
        // combat facing to intercardinal directions as well as the cardinals.
        double angle = Math.atan2(x, -y);
        int sector = Math.floorMod((int) Math.floor((angle + Math.PI / 8) / (Math.PI / 4)), 8);
        double snappedAngle = sector * Math.PI / 4;
        facingX = Math.sin(snappedAngle);
        facingY = -Math.cos(snappedAngle);

        // The existing 2D sprite sheet has only four rows, so use the nearest
        // cardinal pose there while retaining the full eight-way facing vector.
        int cardinal = Math.floorMod((sector + 1) / 2, 4);
        direction = switch (cardinal) {
            case 0 -> 3; // north
            case 1 -> 2; // east
            case 2 -> 0; // south
            default -> 1; // west
        };
    }

    public void stopMoving() {
        velX = 0;
        velY = 0;
        moving = false;
    }

    private void handleHotbarInput(InputHandler input) {
        int scroll = input.consumeScrollDelta();
        hotbarSlot = Math.floorMod(hotbarSlot + scroll, 9);

        // Hotbar quick-use (number keys 1-9)
        for (int i = 1; i <= 9; i++) {
            if (input.isJustPressed(String.valueOf(i))) {
                hotbarSlot = i - 1;
                useHotbarItem(hotbarSlot);
            }
        }
    }

    @Override
    public void update(double dt, GameMap map) {
        float effectiveSpeed = BASE_SPEED * (getSpeed() / 5f);
        float newX = x + velX * effectiveSpeed * (float) dt;
        float newY = y + velY * effectiveSpeed * (float) dt;

        // Collision check (separate axes for sliding)
        if (!map.isBlocked(newX, y, width, height)) x = newX;
        if (!map.isBlocked(x, newY, width, height)) y = newY;

        // Clamp to map bounds
        x = Math.max(0, Math.min(x, map.getWidthPx()  - width));
        y = Math.max(0, Math.min(y, map.getHeightPx() - height));
    }

    /** Called separately to tick animations without physics coupling. */
    public void updateAnimations(double dt) {
        if (moving) {
            animTimer += dt;
            stepTimer += dt;
            if (animTimer >= ANIM_RATE) {
                animTimer = 0;
                animFrame = (animFrame + 1) % 4;
            }
            if (stepTimer >= 0.42) {
                stepTimer = 0;
                assetLoader.playSfx("audio/step.mp3");
            }
        } else {
            animFrame = 0;
            animTimer = 0;
            stepTimer = 0;
        }
    }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void render(GraphicsContext gc) {
        if (spriteSheet != null) {
            int srcX = animFrame * 32;
            int srcY = direction * 32;
            gc.drawImage(spriteSheet, srcX, srcY, 32, 32, x, y, 32, 32);
        } else {
            // Fallback: coloured rectangle
            gc.setFill(Color.web("#c8a96e"));
            gc.fillRect(x, y, width, height);
            gc.setFill(Color.web("#7c5cbf"));
            gc.fillRect(x + 8, y + 4, 16, 16);
        }

        // Shadow beneath player
        gc.setFill(Color.color(0, 0, 0, 0.25));
        gc.fillOval(x + 4, y + height - 6, width - 8, 8);
    }

    // ── Inventory ─────────────────────────────────────────────────────────
    public boolean canAddItem(Item item) {
        if (item == null || item.getCount() <= 0) return false;
        int capacity = 0;
        for (Item slot : inventory) {
            if (slot == null) capacity += item.getMaxStack();
            else if (slot.canStack(item)) capacity += Math.max(0, slot.getMaxStack() - slot.getCount());
        }
        return capacity >= item.getCount();
    }

    public boolean canAddItemAfterRemoving(Item output, java.util.Map<String, Integer> removedItems) {
        if (output == null || output.getCount() <= 0) return false;
        int[] remainingCounts = new int[inventory.length];
        for (int i = 0; i < inventory.length; i++) {
            remainingCounts[i] = inventory[i] == null ? 0 : inventory[i].getCount();
        }
        for (var requirement : removedItems.entrySet()) {
            if (requirement.getValue() < 0 || countItems(requirement.getKey()) < requirement.getValue()) return false;
            int remaining = requirement.getValue();
            for (int i = 0; i < inventory.length && remaining > 0; i++) {
                Item item = inventory[i];
                if (item == null || !item.getId().equals(requirement.getKey())) continue;
                int removed = Math.min(remaining, remainingCounts[i]);
                remainingCounts[i] -= removed;
                remaining -= removed;
            }
        }
        int capacity = 0;
        for (int i = 0; i < inventory.length; i++) {
            Item slot = inventory[i];
            if (remainingCounts[i] == 0) capacity += output.getMaxStack();
            else if (slot != null && slot.canStack(output))
                capacity += Math.max(0, slot.getMaxStack() - remainingCounts[i]);
        }
        return capacity >= output.getCount();
    }

    /** Adds an item atomically, filling matching stacks before empty slots. */
    public boolean addItem(Item item) {
        if (!canAddItem(item)) return false;
        int remaining = item.getCount();
        for (Item slot : inventory) {
            if (remaining == 0) break;
            if (slot != null && slot.canStack(item)) {
                int added = slot.addToStack(remaining);
                remaining -= added;
            }
        }
        for (int i = 0; i < INVENTORY_SIZE && remaining > 0; i++) {
            if (inventory[i] != null) continue;
            int count = Math.min(item.getMaxStack(), remaining);
            inventory[i] = item.clone(count);
            remaining -= count;
        }
        return remaining == 0;
    }

    public boolean removeItem(int slot) {
        if (slot < 0 || slot >= INVENTORY_SIZE || inventory[slot] == null) return false;
        inventory[slot] = null;
        return true;
    }

    public int countItems(String id) {
        int count = 0;
        for (Item item : inventory) {
            if (item != null && item.getId().equals(id)) count += item.getCount();
        }
        return count;
    }

    public boolean removeItems(java.util.Map<String, Integer> required) {
        for (var entry : required.entrySet()) {
            if (entry.getValue() < 0 || countItems(entry.getKey()) < entry.getValue()) return false;
        }
        for (var entry : required.entrySet()) {
            int remaining = entry.getValue();
            for (int i = 0; i < inventory.length && remaining > 0; i++) {
                Item item = inventory[i];
                if (item == null || !item.getId().equals(entry.getKey())) continue;
                int removed = item.removeFromStack(remaining);
                remaining -= removed;
                if (item.getCount() == 0) inventory[i] = null;
            }
        }
        return true;
    }

    public boolean equipInventoryItem(int slot) {
        if (slot < 0 || slot >= inventory.length || !(inventory[slot] instanceof Equipment item)
                || item.getSlot() == null) return false;
        Equipment previous = equipment.equip(item);
        inventory[slot] = null;
        if (previous != null) addItem(previous);
        clampVitalsToEquipment();
        return true;
    }

    public boolean unequipItem(Equipment.Slot slot) {
        Equipment item = equipment.getSlot(slot);
        if (item == null || !canAddItem(item)) return false;
        equipment.unequip(slot);
        addItem(item);
        clampVitalsToEquipment();
        return true;
    }

    public void clearEquippedItems() {
        for (Equipment.Slot slot : Equipment.Slot.values()) equipment.unequip(slot);
    }

    public void clampVitalsToEquipment() {
        hp = Math.min(hp, getMaxHp());
        mp = Math.min(mp, getMaxMp());
    }

    public void useHotbarItem(int slot) {
        if (slot < 0 || slot >= INVENTORY_SIZE) return;
        Item item = inventory[slot];
        if (item instanceof Consumable c) {
            c.use(this);
            if (c.isConsumed()) inventory[slot] = null;
        }
    }

    public boolean useInventoryItem(int slot) {
        if (slot < 0 || slot >= inventory.length || !(inventory[slot] instanceof Consumable consumable))
            return false;
        boolean used = consumable.use(this);
        if (consumable.isConsumed()) inventory[slot] = null;
        return used;
    }

    // ── Combat helpers ────────────────────────────────────────────────────
    public void takeDamage(int amount) {
        int mitigated = Math.max(1, amount - getDefense());
        hp = Math.max(0, hp - mitigated);
    }

    public void heal(int amount) {
        hp = Math.min(getMaxHp(), hp + amount);
    }

    public void restoreMp(int amount) {
        mp = Math.min(getMaxMp(), mp + amount);
    }

    public boolean spendMp(int amount) {
        if (amount < 0 || mp < amount) return false;
        mp -= amount;
        return true;
    }

    public void gainXp(int amount) {
        xp += amount;
        while (xp >= xpToNext) {
            xp     -= xpToNext;
            levelUp();
        }
    }

    private void levelUp() {
        level++;
        xpToNext   = (int) (xpToNext * 1.4);
        maxHp     += 15;
        hp          = getMaxHp();
        maxMp     += 8;
        mp          = getMaxMp();
        strength  += 3;
        defense   += 2;
        speed     += 1;
        luck      += 1;
        System.out.println("[Player] Level up! Now level " + level);
        if (skillTree != null) skillTree.onLevelUp();
    }

    // ── Party ─────────────────────────────────────────────────────────────
    public boolean addPartyMember(PartyMember member) {
        if (party.size() >= MAX_PARTY) return false;
        party.add(member);
        return true;
    }

    public void removePartyMember(PartyMember member) {
        party.remove(member);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public int    getLevel()       { return level; }
    public int    getXp()          { return xp; }
    public int    getXpToNext()    { return xpToNext; }
    public int    getHp()          { return hp; }
    public int    getMaxHp()       { return maxHp + equipment.getTotalBonusHp(); }
    public int    getMp()          { return mp; }
    public int    getMaxMp()       { return maxMp + equipment.getTotalBonusMp(); }
    public int    getStrength()    { return strength + equipment.getTotalBonusStr(); }
    public int    getDefense()     { return defense + equipment.getTotalBonusDef() + equipment.getTotalArmour(); }
    public int    getSpeed()       { return speed + equipment.getTotalBonusSpd(); }
    public int    getLuck()        { return luck + equipment.getTotalBonusLck(); }
    public int    getGold()        { return gold; }
    public int    getReputation()  { return reputation; }
    public boolean isAlive()       { return hp > 0; }
    public Item[]           getInventory()  { return inventory; }
    public Equipment        getEquipment()  { return equipment; }
    public List<PartyMember> getParty()     { return party; }
    public int    getHotbarSlot()  { return hotbarSlot; }
    public String getHairStyle()   { return hairStyle; }
    public Color  getHairColor()   { return hairColor; }
    public Color  getSkinTone()    { return skinTone; }
    public String getClothing()    { return clothing; }
    public String getAccessory()   { return accessory; }
    public float  getVelX()        { return velX; }
    public float  getVelY()        { return velY; }
    public double getFacingX()     { return facingX; }
    public double getFacingY()     { return facingY; }

    public void setGold(int g)              { gold = Math.max(0, g); }
    public void addGold(int g)              { gold += g; }
    public void spendGold(int g)            { gold = Math.max(0, gold - g); }
    public void adjustReputation(int delta) { reputation = Math.max(-100, Math.min(100, reputation + delta)); }
    public void setHairStyle(String s)      { hairStyle = s; }
    public void setHairColor(Color c)       { hairColor = c; }
    public void setSkinTone(Color c)        { skinTone = c; }
    public void setClothing(String s)       { clothing = s; }
    public void setAccessory(String s)      { accessory = s; }

    // ── Serialisation helper ──────────────────────────────────────────────
    /** Return a simple state snapshot for save-game serialisation. */
    public java.util.Map<String, Object> toSaveMap() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("name",       name);
        m.put("x",          x);
        m.put("y",          y);
        m.put("level",      level);
        m.put("xp",         xp);
        m.put("xpToNext",   xpToNext);
        m.put("hp",         hp);
        m.put("maxHp",      maxHp);
        m.put("mp",         mp);
        m.put("maxMp",      maxMp);
        m.put("strength",   strength);
        m.put("defense",    defense);
        m.put("speed",      speed);
        m.put("luck",       luck);
        m.put("gold",       gold);
        m.put("reputation", reputation);
        m.put("hairStyle",  hairStyle);
        m.put("clothing",   clothing);
        m.put("accessory",  accessory);
        return m;
    }

    public void restoreFromSaveMap(java.util.Map<String, Object> data) {
        if (data == null) throw new IllegalArgumentException("Player save data is missing.");
        clearEquippedItems();
        Object savedName = data.get("name");
        if (savedName instanceof String value && !value.isBlank()) name = value;
        x = savedFloat(data, "x", x);
        y = savedFloat(data, "y", y);
        level = Math.max(1, savedInt(data, "level", level));
        xpToNext = Math.max(1, savedInt(data, "xpToNext", xpToNext));
        xp = Math.max(0, savedInt(data, "xp", xp));
        maxHp = Math.max(1, savedInt(data, "maxHp", maxHp));
        hp = Math.max(0, Math.min(maxHp, savedInt(data, "hp", hp)));
        maxMp = Math.max(0, savedInt(data, "maxMp", maxMp));
        mp = Math.max(0, Math.min(maxMp, savedInt(data, "mp", mp)));
        strength = Math.max(0, savedInt(data, "strength", strength));
        defense = Math.max(0, savedInt(data, "defense", defense));
        speed = Math.max(0, savedInt(data, "speed", speed));
        luck = Math.max(0, savedInt(data, "luck", luck));
        gold = Math.max(0, savedInt(data, "gold", gold));
        reputation = Math.max(-100, Math.min(100, savedInt(data, "reputation", reputation)));
        hairStyle = savedString(data, "hairStyle", hairStyle);
        clothing = savedString(data, "clothing", clothing);
        accessory = savedString(data, "accessory", accessory);
    }

    private int savedInt(java.util.Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private float savedFloat(java.util.Map<String, Object> data, String key, float fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private String savedString(java.util.Map<String, Object> data, String key, String fallback) {
        Object value = data.get(key);
        return value instanceof String string ? string : fallback;
    }

// NOTE: The following methods are appended to Player.java to support
// the skill tree, skill-granted moves, and stat modifiers.

    // ── Skill tree integration ────────────────────────────────────────────
    private final java.util.List<com.abyss.combat.Move> learnedMoves = new java.util.ArrayList<>();
    private final java.util.Map<String, Double> modifiers = new java.util.HashMap<>();
    private com.abyss.skills.SkillTree skillTree;

    public void setSkillTree(com.abyss.skills.SkillTree tree) {
        this.skillTree = tree;
        tree.bindPlayer(this);
    }
    public com.abyss.skills.SkillTree getSkillTree() { return skillTree; }

    /** Called by SkillEffect.PASSIVE — applies one rank of stat deltas. */
    public void applyStatDelta(int hp, int mp, int str, int def, int spd, int lck) {
        maxHp     += hp;  this.hp = Math.min(this.hp + hp, maxHp);
        maxMp     += mp;  this.mp = Math.min(this.mp + mp, maxMp);
        strength  += str;
        defense   += def;
        speed     += spd;
        luck      += lck;
    }

    /** Called by SkillEffect.ACTIVE — grants a combat move. */
    public void learnMove(com.abyss.combat.Move move) {
        boolean already = learnedMoves.stream().anyMatch(m -> m.getName().equals(move.getName()));
        if (!already) learnedMoves.add(move);
    }

    /** Called by SkillEffect.MODIFIER — stores a named multiplier. */
    public void applyModifier(String key, double delta) {
        modifiers.merge(key, delta, Double::sum);
    }

    public double getModifier(String key) { return modifiers.getOrDefault(key, 0.0); }
    public java.util.List<com.abyss.combat.Move> getLearnedMoves() { return learnedMoves; }

    // ── Position setters (needed by WorldManager transition) ──────────────
    public void setX(float nx) { this.x = nx; }
    public void setY(float ny) { this.y = ny; }
}
