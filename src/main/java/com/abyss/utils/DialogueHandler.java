package com.abyss.utils;

import java.util.*;

// ════════════════════════════════════════════════════════════════════════════
// DialogueHandler
// ════════════════════════════════════════════════════════════════════════════
/**
 * DialogueHandler — Resolves the correct dialogue branch for an NPC based on
 * quest flags, player reputation, and time of day.
 *
 * In production, dialogue trees are loaded from JSON files in resources/dialogues/.
 * Format:
 * {
 *   "elder_aldric": {
 *     "default":   "Greetings, traveller. This village needs your help.",
 *     "rep_high":  "Ah, our heroic champion! What can this old man do for you?",
 *     "rep_low":   "I've heard... troubling things about you. Speak quickly.",
 *     "quest_q001_done": "Thank you for clearing the southern road!"
 *   }
 * }
 *
 * This stub implements the resolution logic and hard-coded fallback strings.
 */
public class DialogueHandler {

    // ── Dialogue data (would be loaded from JSON) ─────────────────────────
    private final Map<String, Map<String, String>> dialogueTrees = new HashMap<>();

    public DialogueHandler() {
        loadDefaultDialogues();
    }

    private void loadDefaultDialogues() {
        // Elder Aldric
        Map<String, String> elder = new LinkedHashMap<>();
        elder.put("rep_high",  "Ah, {name}! Your deeds bring light to these dark times.");
        elder.put("rep_low",   "I must speak plainly — your reputation precedes you, and not kindly.");
        elder.put("default",   "Greetings, traveller. Ashenveil is in desperate need of able hands.");
        dialogueTrees.put("elder_aldric", elder);

        // Merchant Mira
        Map<String, String> mira = new LinkedHashMap<>();
        mira.put("rep_high",  "For a hero such as yourself, I'll offer my best prices!");
        mira.put("rep_low",   "I've heard stories. You're paying full price and leaving no trouble.");
        mira.put("default",   "Welcome! Quality goods, fair prices. Usually.");
        dialogueTrees.put("merchant_mira", mira);

        // Guard
        Map<String, String> guard = new LinkedHashMap<>();
        guard.put("rep_low",   "Move along. We're watching you.");
        guard.put("default",   "Halt! ...Oh, it's just a traveller. Move along, citizen.");
        dialogueTrees.put("guard_generic", guard);

        // Lyra companion
        Map<String, String> lyra = new LinkedHashMap<>();
        lyra.put("recruited", "I'm already with you, aren't I?");
        lyra.put("rep_high",  "I've heard you're becoming quite the legend. Mind if I tag along?");
        lyra.put("default",   "I'm Lyra. I handle the... delicate work. Looking for a capable partner?");
        dialogueTrees.put("lyra_companion", lyra);

        Map<String, String> daisy = new LinkedHashMap<>();
        daisy.put("rep_high", "Daisy beams. 'For Ashenveil's favorite adventurer, today's remedies are on the house!' ");
        daisy.put("default", "Take a breath, dear. My leaf wraps and springwater will have you adventure-ready again.");
        dialogueTrees.put("healer_daisy", daisy);

        Map<String, String> bram = new LinkedHashMap<>();
        bram.put("rep_high", "Bram grins. 'A hero! Bring me ore and Ashwood and we'll make you a blade worth singing about.'");
        bram.put("default", "Iron Ore and Ashwood make a fine sword; hide and Moon Thread make sturdy armour. Check your crafting pack!");
        dialogueTrees.put("smith_bram", bram);

        Map<String, String> pip = new LinkedHashMap<>();
        pip.put("default", "Fresh honey buns, warm from the oven! Only 8 gold, and they make fine trail snacks.");
        dialogueTrees.put("baker_pip", pip);

        Map<String, String> nell = new LinkedHashMap<>();
        nell.put("default", "These little sunflowers keep watch over the road. I think they make the wolves less grumpy.");
        dialogueTrees.put("gardener_nell", nell);

        Map<String, String> guide = new LinkedHashMap<>();
        guide.put("default", "Follow the soft green lantern-mushrooms if you lose the path. They love company!");
        dialogueTrees.put("forest_guide", guide);

        Map<String, String> moss = new LinkedHashMap<>();
        moss.put("default", "I'm Moss. I collect interesting pebbles. This one looks exactly like a tiny potato.");
        dialogueTrees.put("forest_moss", moss);
    }

    /**
     * Resolve the best dialogue string for this NPC.
     * @param npcKey     NPC dialogue identifier
     * @param reputation Player reputation (-100 to +100)
     * @param level      Player level
     * @param playerName Player's display name
     */
    public String resolve(String npcKey, int reputation, int level, String playerName) {
        Map<String, String> tree = dialogueTrees.get(npcKey);
        if (tree == null) return "...";

        String line = null;

        // Priority: high-rep → low-rep → default
        if (reputation >= 50 && tree.containsKey("rep_high")) {
            line = tree.get("rep_high");
        } else if (reputation <= -20 && tree.containsKey("rep_low")) {
            line = tree.get("rep_low");
        } else {
            line = tree.getOrDefault("default", "...");
        }

        return interpolate(line, playerName, level);
    }

    private String interpolate(String template, String name, int level) {
        return template
            .replace("{name}",  name)
            .replace("{level}", String.valueOf(level));
    }

    /** Inject a dialogue tree from external source (quest systems, mods). */
    public void register(String npcKey, Map<String, String> tree) {
        dialogueTrees.put(npcKey, tree);
    }
}
