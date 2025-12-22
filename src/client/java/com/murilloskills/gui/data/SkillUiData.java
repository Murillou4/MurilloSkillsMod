package com.murilloskills.gui.data;

import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Central repository for UI-related skill data.
 * Contains definitions for Perks, Synergies, and common helpers.
 * Refactored to eliminate duplication between ModInfoScreen and SkillsScreen.
 */
public final class SkillUiData {

        // === RECORDS ===

        public record PerkInfo(int level, String nameKey, String descKey) {
        }

        public record SynergyInfo(String id, MurilloSkillsList skill1, MurilloSkillsList skill2, int bonus,
                        String typeKey) {
        }

        // === DATA CONSTANTS ===

        public static final Map<MurilloSkillsList, List<PerkInfo>> SKILL_PERKS = new HashMap<>();
        public static final List<SynergyInfo> SYNERGIES = List.of(
                        new SynergyInfo("iron_will", MurilloSkillsList.WARRIOR, MurilloSkillsList.BLACKSMITH,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyIronWill() * 100),
                                        "murilloskills.synergy.type.damage_reduction"),
                        new SynergyInfo("forge_master", MurilloSkillsList.MINER, MurilloSkillsList.BLACKSMITH,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyForgeMaster() * 100),
                                        "murilloskills.synergy.type.ore_drops"),
                        new SynergyInfo("ranger", MurilloSkillsList.ARCHER, MurilloSkillsList.EXPLORER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyRanger() * 100),
                                        "murilloskills.synergy.type.movement_speed"),
                        new SynergyInfo("natures_bounty", MurilloSkillsList.FARMER, MurilloSkillsList.FISHER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyNaturesBounty() * 100),
                                        "murilloskills.synergy.type.all_drops"),
                        new SynergyInfo("treasure_hunter", MurilloSkillsList.MINER, MurilloSkillsList.EXPLORER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyTreasureHunter() * 100),
                                        "murilloskills.synergy.type.rare_finds"),
                        new SynergyInfo("combat_master", MurilloSkillsList.WARRIOR, MurilloSkillsList.ARCHER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyCombatMaster() * 100),
                                        "murilloskills.synergy.type.all_damage"),
                        new SynergyInfo("master_crafter", MurilloSkillsList.BUILDER, MurilloSkillsList.BLACKSMITH,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyMasterCrafter() * 100),
                                        "murilloskills.synergy.type.crafting"),
                        // === NEW SYNERGIES ===
                        new SynergyInfo("survivor", MurilloSkillsList.WARRIOR, MurilloSkillsList.EXPLORER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergySurvivor() * 100),
                                        "murilloskills.synergy.type.damage_reduction"),
                        new SynergyInfo("industrial", MurilloSkillsList.MINER, MurilloSkillsList.BUILDER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyIndustrial() * 100),
                                        "murilloskills.synergy.type.crafting"),
                        new SynergyInfo("sea_warrior", MurilloSkillsList.WARRIOR, MurilloSkillsList.FISHER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergySeaWarrior() * 100),
                                        "murilloskills.synergy.type.all_damage"),
                        new SynergyInfo("green_archer", MurilloSkillsList.FARMER, MurilloSkillsList.ARCHER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyGreenArcher() * 100),
                                        "murilloskills.synergy.type.movement_speed"),
                        new SynergyInfo("prospector", MurilloSkillsList.MINER, MurilloSkillsList.WARRIOR,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyProspector() * 100),
                                        "murilloskills.synergy.type.ore_drops"),
                        new SynergyInfo("adventurer", MurilloSkillsList.BUILDER, MurilloSkillsList.EXPLORER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyAdventurer() * 100),
                                        "murilloskills.synergy.type.all_drops"),
                        new SynergyInfo("hermit", MurilloSkillsList.FARMER, MurilloSkillsList.BUILDER,
                                        (int) (com.murilloskills.utils.SkillConfig.getSynergyHermit() * 100),
                                        "murilloskills.synergy.type.crafting"));

        static {
                // MINER
                SKILL_PERKS.put(MurilloSkillsList.MINER, List.of(
                                new PerkInfo(10, "murilloskills.perk.name.miner.night_vision",
                                                "murilloskills.perk.desc.miner.night_vision"),
                                new PerkInfo(30, "murilloskills.perk.name.miner.durability",
                                                "murilloskills.perk.desc.miner.durability"),
                                new PerkInfo(60, "murilloskills.perk.name.miner.ore_radar",
                                                "murilloskills.perk.desc.miner.ore_radar"),
                                new PerkInfo(100, "murilloskills.perk.name.miner.master",
                                                "murilloskills.perk.desc.miner.master")));

                // WARRIOR
                SKILL_PERKS.put(MurilloSkillsList.WARRIOR, List.of(
                                new PerkInfo(10, "murilloskills.perk.name.warrior.heart_1",
                                                "murilloskills.perk.desc.warrior.heart_1"),
                                new PerkInfo(25, "murilloskills.perk.name.warrior.iron_skin",
                                                "murilloskills.perk.desc.warrior.iron_skin"),
                                new PerkInfo(50, "murilloskills.perk.name.warrior.heart_2",
                                                "murilloskills.perk.desc.warrior.heart_2"),
                                new PerkInfo(75, "murilloskills.perk.name.warrior.vampirism",
                                                "murilloskills.perk.desc.warrior.vampirism"),
                                new PerkInfo(100, "murilloskills.perk.name.warrior.master",
                                                "murilloskills.perk.desc.warrior.master")));

                // FARMER
                SKILL_PERKS.put(MurilloSkillsList.FARMER, List.of(
                                new PerkInfo(10, "murilloskills.perk.name.farmer.green_thumb",
                                                "murilloskills.perk.desc.farmer.green_thumb"),
                                new PerkInfo(25, "murilloskills.perk.name.farmer.fertile_ground",
                                                "murilloskills.perk.desc.farmer.fertile_ground"),
                                new PerkInfo(50, "murilloskills.perk.name.farmer.nutrient_cycle",
                                                "murilloskills.perk.desc.farmer.nutrient_cycle"),
                                new PerkInfo(75, "murilloskills.perk.name.farmer.abundant_harvest",
                                                "murilloskills.perk.desc.farmer.abundant_harvest"),
                                new PerkInfo(100, "murilloskills.perk.name.farmer.master",
                                                "murilloskills.perk.desc.farmer.master")));

                // ARCHER
                SKILL_PERKS.put(MurilloSkillsList.ARCHER, List.of(
                                new PerkInfo(10, "murilloskills.perk.name.archer.fast_arrows",
                                                "murilloskills.perk.desc.archer.fast_arrows"),
                                new PerkInfo(25, "murilloskills.perk.name.archer.bonus_damage",
                                                "murilloskills.perk.desc.archer.bonus_damage"),
                                new PerkInfo(50, "murilloskills.perk.name.archer.penetration",
                                                "murilloskills.perk.desc.archer.penetration"),
                                new PerkInfo(75, "murilloskills.perk.name.archer.stable_shot",
                                                "murilloskills.perk.desc.archer.stable_shot"),
                                new PerkInfo(100, "murilloskills.perk.name.archer.master",
                                                "murilloskills.perk.desc.archer.master")));

                // FISHER
                SKILL_PERKS.put(MurilloSkillsList.FISHER, List.of(
                                new PerkInfo(10, "murilloskills.perk.name.fisher.fast_fishing",
                                                "murilloskills.perk.desc.fisher.fast_fishing"),
                                new PerkInfo(25, "murilloskills.perk.name.fisher.treasure_hunter",
                                                "murilloskills.perk.desc.fisher.treasure_hunter"),
                                new PerkInfo(50, "murilloskills.perk.name.fisher.dolphins_grace",
                                                "murilloskills.perk.desc.fisher.dolphins_grace"),
                                new PerkInfo(75, "murilloskills.perk.name.fisher.luck_of_sea",
                                                "murilloskills.perk.desc.fisher.luck_of_sea"),
                                new PerkInfo(100, "murilloskills.perk.name.fisher.master",
                                                "murilloskills.perk.desc.fisher.master")));

                // BLACKSMITH
                SKILL_PERKS.put(MurilloSkillsList.BLACKSMITH, List.of(
                                new PerkInfo(10, "murilloskills.perk.name.blacksmith.iron_skin",
                                                "murilloskills.perk.desc.blacksmith.iron_skin"),
                                new PerkInfo(25, "murilloskills.perk.name.blacksmith.efficient_anvil",
                                                "murilloskills.perk.desc.blacksmith.efficient_anvil"),
                                new PerkInfo(50, "murilloskills.perk.name.blacksmith.forged_resilience",
                                                "murilloskills.perk.desc.blacksmith.forged_resilience"),
                                new PerkInfo(75, "murilloskills.perk.name.blacksmith.thorns_master",
                                                "murilloskills.perk.desc.blacksmith.thorns_master"),
                                new PerkInfo(100, "murilloskills.perk.name.blacksmith.master",
                                                "murilloskills.perk.desc.blacksmith.master")));

                // BUILDER
                SKILL_PERKS.put(MurilloSkillsList.BUILDER, List.of(
                                new PerkInfo(10, "murilloskills.perk.name.builder.extended_reach",
                                                "murilloskills.perk.desc.builder.extended_reach"),
                                new PerkInfo(15, "murilloskills.perk.name.builder.efficient_crafting",
                                                "murilloskills.perk.desc.builder.efficient_crafting"),
                                new PerkInfo(25, "murilloskills.perk.name.builder.safe_landing",
                                                "murilloskills.perk.desc.builder.safe_landing"),
                                new PerkInfo(50, "murilloskills.perk.name.builder.scaffold_master",
                                                "murilloskills.perk.desc.builder.scaffold_master"),
                                new PerkInfo(75, "murilloskills.perk.name.builder.master_reach",
                                                "murilloskills.perk.desc.builder.master_reach"),
                                new PerkInfo(100, "murilloskills.perk.name.builder.master",
                                                "murilloskills.perk.desc.builder.master")));

                // EXPLORER
                SKILL_PERKS.put(MurilloSkillsList.EXPLORER, List.of(
                                new PerkInfo(10, "murilloskills.perk.name.explorer.step_assist",
                                                "murilloskills.perk.desc.explorer.step_assist"),
                                new PerkInfo(20, "murilloskills.perk.name.explorer.aquatic",
                                                "murilloskills.perk.desc.explorer.aquatic"),
                                new PerkInfo(35, "murilloskills.perk.name.explorer.night_vision",
                                                "murilloskills.perk.desc.explorer.night_vision"),
                                new PerkInfo(65, "murilloskills.perk.name.explorer.feather_feet",
                                                "murilloskills.perk.desc.explorer.feather_feet"),
                                new PerkInfo(80, "murilloskills.perk.name.explorer.nether_walker",
                                                "murilloskills.perk.desc.explorer.nether_walker"),
                                new PerkInfo(100, "murilloskills.perk.name.explorer.master",
                                                "murilloskills.perk.desc.explorer.master")));
        }

        private SkillUiData() {
                // Pure utility class
        }

        // === HELPER METHODS ===

        /**
         * Gets the next perk for a skill based on current level.
         */
        public static PerkInfo getNextPerk(MurilloSkillsList skill, int currentLevel) {
                List<PerkInfo> perks = SKILL_PERKS.get(skill);
                if (perks == null)
                        return null;

                for (PerkInfo perk : perks) {
                        if (perk.level() > currentLevel) {
                                return perk;
                        }
                }
                return null; // All perks unlocked
        }

        public static ItemStack getSkillIcon(MurilloSkillsList skill) {
                return switch (skill) {
                        case MINER -> new ItemStack(Items.IRON_PICKAXE);
                        case WARRIOR -> new ItemStack(Items.IRON_SWORD);
                        case FARMER -> new ItemStack(Items.WHEAT);
                        case ARCHER -> new ItemStack(Items.BOW);
                        case FISHER -> new ItemStack(Items.FISHING_ROD);
                        case BUILDER -> new ItemStack(Items.BRICKS);
                        case BLACKSMITH -> new ItemStack(Items.ANVIL);
                        case EXPLORER -> new ItemStack(Items.COMPASS);
                };
        }
}
