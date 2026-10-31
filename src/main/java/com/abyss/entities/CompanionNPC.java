package com.abyss.entities;

import com.abyss.utils.AssetLoader;
import com.abyss.world.GameMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * CompanionNPC — An NPC who can permanently or temporarily join the player.
 *
 * Extends NPC with:
 *  - A joinable flag and recruitment conditions
 *  - Combat participation (delegated to the embedded PartyMember)
 *  - Exploration dialogue unique to companion relationship
 *
 * When recruited, a PartyMember proxy is added to the player's party.
 * The CompanionNPC map entity becomes inactive (they're "with the party").
 */
public class CompanionNPC extends NPC {

    // ── Recruitment ───────────────────────────────────────────────────────
    private boolean recruited      = false;
    private boolean permanent      = false;   // false = temporarily hired
    private int     hireCost       = 0;       // gold cost (0 = free)
    private int     minReputation  = 0;       // required reputation to recruit
    private String  recruitQuestId = null;    // null = no quest gate

    // ── Companion party proxy ─────────────────────────────────────────────
    private PartyMember partyProxy;

    // ── Personal dialogue ─────────────────────────────────────────────────
    private String[] explorationQuips;        // random map-travel comments
    private int      quipIdx = 0;

    // ─────────────────────────────────────────────────────────────────────
    public CompanionNPC(String name, float x, float y,
                        String dialogueKey, boolean permanent,
                        int hireCost, int minReputation,
                        PartyMember.Archetype archetype,
                        AssetLoader loader) {
        super(name, NPCType.COMPANION, x, y, dialogueKey, loader);
        this.permanent     = permanent;
        this.hireCost      = hireCost;
        this.minReputation = minReputation;
        this.partyProxy    = new PartyMember(name, archetype, x, y, 0, loader);
        this.explorationQuips = defaultQuips(name);
    }

    private String[] defaultQuips(String name) {
        return new String[]{
            name + ": \"Stay close. These roads are dangerous.\"",
            name + ": \"I've heard rumours about this area...\"",
            name + ": \"My sword arm's ready whenever you need it.\"",
            name + ": \"There's something off about this place.\"",
            name + ": \"Lead the way — I've got your back.\""
        };
    }

    // ── Recruitment ───────────────────────────────────────────────────────
    /**
     * Attempt to recruit this companion.
     * @return true if successful (conditions met, enough gold, party not full).
     */
    public boolean tryRecruit(Player player) {
        if (recruited) return false;
        if (player.getReputation() < minReputation) return false;
        if (recruitQuestId != null) {
            // Quest gate check would be done via QuestManager (injected externally)
            // For now, assume the caller has already verified
        }
        if (hireCost > 0 && player.getGold() < hireCost) return false;
        if (!player.addPartyMember(partyProxy)) return false;  // party full

        if (hireCost > 0) player.spendGold(hireCost);
        recruited = true;
        setActive(false);   // remove from map
        System.out.println("[CompanionNPC] " + name + " has joined the party!");
        return true;
    }

    public void dismiss(Player player) {
        if (!recruited) return;
        player.removePartyMember(partyProxy);
        recruited = false;
        // Restore NPC to the map at player position
        x = player.getX() + 48;
        y = player.getY();
        setActive(true);
    }

    // ── Quips ─────────────────────────────────────────────────────────────
    public String nextQuip() {
        String q = explorationQuips[quipIdx % explorationQuips.length];
        quipIdx++;
        return q;
    }

    // ── Update ────────────────────────────────────────────────────────────
    @Override
    public void update(double dt, GameMap map) {
        if (recruited) return; // handled by party formation
        super.update(dt, map);
    }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void render(GraphicsContext gc) {
        if (recruited) return;
        super.render(gc);

        // Small friendly plus badge marks companions the player can recruit.
        if (isVisible() && isActive()) {
            gc.save();
            gc.setFill(Color.web("#d8f0a5"));
            gc.fillOval(x + 22, y + 1, 11, 11);
            gc.setStroke(Color.web("#438c68"));
            gc.setLineWidth(1);
            gc.strokeOval(x + 22, y + 1, 11, 11);
            gc.setLineWidth(2);
            gc.strokeLine(x + 27.5, y + 3.5, x + 27.5, y + 9.5);
            gc.strokeLine(x + 24.5, y + 6.5, x + 30.5, y + 6.5);
            gc.restore();
        }
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public boolean       isRecruited()     { return recruited; }
    public boolean       isPermanent()     { return permanent; }
    public int           getHireCost()     { return hireCost; }
    public int           getMinReputation(){ return minReputation; }
    public String        getRecruitQuestId(){ return recruitQuestId; }
    public PartyMember   getPartyProxy()   { return partyProxy; }

    public void setRecruitQuestId(String q){ this.recruitQuestId = q; }
    public void setExplorationQuips(String[] quips){ this.explorationQuips = quips; }
}
