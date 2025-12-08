package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Utility class to determine XP rewards for Fisher skill actions.
 * 
 * XP Sources (Treasure > Fish > Junk):
 * - Treasure items (Name Tag, Saddle, Enchanted Books, etc): 150 XP
 * - Fish (Cod, Salmon, Tropical Fish, Pufferfish): 50 XP
 * - Junk (Bowl, Leather, Rotten Flesh, etc): 15 XP
 */
public class FisherXpGetter {

    // Base XP values for different catch categories
    private static final int XP_TREASURE = 50;
    private static final int XP_FISH = 15;
    private static final int XP_JUNK = 5;

    /**
     * Gets XP for a caught item based on its category.
     * 
     * @param item The item that was caught
     * @return SkillReceptorResult with XP info
     */
    public static SkillReceptorResult getFishingXp(Item item) {
        // Treasure items
        if (isTreasure(item)) {
            return new SkillReceptorResult(true, XP_TREASURE);
        }
        // Fish items
        else if (isFish(item)) {
            return new SkillReceptorResult(true, XP_FISH);
        }
        // Junk items
        else if (isJunk(item)) {
            return new SkillReceptorResult(true, XP_JUNK);
        }

        // Unknown item - give base fish XP
        return new SkillReceptorResult(true, XP_FISH);
    }

    /**
     * Checks if an item is a treasure (high value fishing loot).
     */
    public static boolean isTreasure(Item item) {
        return item == Items.NAME_TAG ||
                item == Items.SADDLE ||
                item == Items.BOW ||
                item == Items.FISHING_ROD ||
                item == Items.ENCHANTED_BOOK ||
                item == Items.NAUTILUS_SHELL;
    }

    /**
     * Checks if an item is a fish.
     */
    public static boolean isFish(Item item) {
        return item == Items.COD ||
                item == Items.SALMON ||
                item == Items.TROPICAL_FISH ||
                item == Items.PUFFERFISH;
    }

    /**
     * Checks if an item is junk (low value fishing loot).
     */
    public static boolean isJunk(Item item) {
        return item == Items.BOWL ||
                item == Items.LEATHER ||
                item == Items.LEATHER_BOOTS ||
                item == Items.ROTTEN_FLESH ||
                item == Items.STICK ||
                item == Items.STRING ||
                item == Items.POTION || // Water Bottle
                item == Items.BONE ||
                item == Items.INK_SAC ||
                item == Items.TRIPWIRE_HOOK ||
                item == Items.BAMBOO ||
                item == Items.LILY_PAD;
    }

    /**
     * Gets the category name for display purposes.
     */
    public static String getCategoryName(Item item) {
        if (isTreasure(item))
            return "Tesouro";
        if (isFish(item))
            return "Peixe";
        if (isJunk(item))
            return "Lixo";
        return "Desconhecido";
    }
}
