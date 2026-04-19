package com.murilloskills.gui.data;

import com.murilloskills.impl.ArcherSkill;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central repository for UI-related skill data.
 * Contains definitions for Perks, Synergies, and common helpers.
 * Refactored to eliminate duplication between ModInfoScreen and SkillsScreen.
 */
public final class SkillUiData {

        public record PerkInfo(int level, String nameKey, String descKey) {
        }

        public record SynergyInfo(String id, MurilloSkillsList skill1, MurilloSkillsList skill2, int bonus,
                        String typeKey) {
        }

        public record GuideEntry(int level, String text, boolean milestone) {
        }

        public record XpSourceInfo(String name, String xp, int color) {
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
                                new PerkInfo(35, "murilloskills.perk.name.farmer.natures_vitality",
                                                "murilloskills.perk.desc.farmer.natures_vitality"),
                                new PerkInfo(50, "murilloskills.perk.name.farmer.nutrient_cycle",
                                                "murilloskills.perk.desc.farmer.nutrient_cycle"),
                                new PerkInfo(60, "murilloskills.perk.name.farmer.seed_master",
                                                "murilloskills.perk.desc.farmer.seed_master"),
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
                                new PerkInfo(35, "murilloskills.perk.name.fisher.ocean_blessing",
                                                "murilloskills.perk.desc.fisher.ocean_blessing"),
                                new PerkInfo(50, "murilloskills.perk.name.fisher.dolphins_grace",
                                                "murilloskills.perk.desc.fisher.dolphins_grace"),
                                new PerkInfo(60, "murilloskills.perk.name.fisher.seas_fortune",
                                                "murilloskills.perk.desc.fisher.seas_fortune"),
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
                                new PerkInfo(35, "murilloskills.perk.name.blacksmith.fire_mastery",
                                                "murilloskills.perk.desc.blacksmith.fire_mastery"),
                                new PerkInfo(50, "murilloskills.perk.name.blacksmith.forged_resilience",
                                                "murilloskills.perk.desc.blacksmith.forged_resilience"),
                                new PerkInfo(60, "murilloskills.perk.name.blacksmith.repair_aura",
                                                "murilloskills.perk.desc.blacksmith.repair_aura"),
                                new PerkInfo(75, "murilloskills.perk.name.blacksmith.thorns_master",
                                                "murilloskills.perk.desc.blacksmith.thorns_master"),
                                new PerkInfo(99, "murilloskills.perk.name.blacksmith.master_enchanter",
                                                "murilloskills.perk.desc.blacksmith.master_enchanter"),
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
                                new PerkInfo(35, "murilloskills.perk.name.builder.builders_vigor",
                                                "murilloskills.perk.desc.builder.builders_vigor"),
                                new PerkInfo(50, "murilloskills.perk.name.builder.scaffold_master",
                                                "murilloskills.perk.desc.builder.scaffold_master"),
                                new PerkInfo(60, "murilloskills.perk.name.builder.feather_build",
                                                "murilloskills.perk.desc.builder.feather_build"),
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
                                new PerkInfo(45, "murilloskills.perk.name.explorer.pathfinder",
                                                "murilloskills.perk.desc.explorer.pathfinder"),
                                new PerkInfo(55, "murilloskills.perk.name.explorer.swift_recovery",
                                                "murilloskills.perk.desc.explorer.swift_recovery"),
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

        public static Text getSkillDescription(MurilloSkillsList skill) {
                return Text.translatable("murilloskills.skill.desc." + skill.name().toLowerCase());
        }

        public static Text getXpGainDescription(MurilloSkillsList skill) {
                return Text.translatable("murilloskills.tooltip.xp_gain." + skill.name().toLowerCase());
        }

        public static Text getSpecialAbilityDescription(MurilloSkillsList skill) {
                return Text.translatable("murilloskills.ability.desc." + skill.name().toLowerCase());
        }

        public static String getWhyChooseDescription(MurilloSkillsList skill) {
                return Text.translatable("murilloskills.info.guide.choose." + skill.name().toLowerCase(Locale.ROOT))
                                .getString();
        }

        public static String getMasterAbilityDetails(MurilloSkillsList skill) {
                String base = getSpecialAbilityDescription(skill).getString();
                return switch (skill) {
                        case MINER -> base + " Cooldown " + formatSeconds(SkillConfig.getMinerAbilityCooldownSeconds())
                                        + ", raio " + SkillConfig.getMinerAbilityRadius() + " blocos, leitura por "
                                        + SkillConfig.getMinerAbilityDurationSeconds() + "s.";
                        case WARRIOR -> base + " Cooldown " + formatSeconds(SkillConfig.getWarriorAbilityCooldownSeconds())
                                        + ", dura " + SkillConfig.getWarriorBerserkDurationSeconds() + "s, Strength "
                                        + (SkillConfig.getWarriorBerserkStrengthAmplifier() + 1)
                                        + ", Resistance "
                                        + (SkillConfig.getWarriorBerserkResistanceAmplifier() + 1)
                                        + " e lifesteal "
                                        + formatPercent(SkillConfig.getWarriorBerserkLifesteal()) + ".";
                        case FARMER -> base + " Cooldown " + formatSeconds(SkillConfig.getFarmerAbilityCooldownSeconds())
                                        + ", dura " + SkillConfig.getFarmerAbilityDurationSeconds() + "s, raio "
                                        + SkillConfig.getFarmerAbilityRadius() + " blocos e colheita tripla.";
                        case ARCHER -> base + " Cooldown " + formatSeconds(SkillConfig.getArcherAbilityCooldownSeconds())
                                        + ", dura " + SkillConfig.getArcherMasterRangerDurationSeconds()
                                        + "s, headshot +"
                                        + formatPercent(SkillConfig.getArcherHeadshotDamageBonus())
                                        + " e homing ativo.";
                        case FISHER -> base + " Cooldown " + formatSeconds(SkillConfig.getFisherAbilityCooldownSeconds())
                                        + ", dura " + SkillConfig.getFisherAbilityDurationSeconds() + "s, pesca +"
                                        + formatPercent(SkillConfig.getFisherRainDanceSpeedBonus())
                                        + ", tesouro +"
                                        + formatPercent(SkillConfig.getFisherRainDanceTreasureBonus())
                                        + " e bundle x" + SkillConfig.getFisherRainDanceBundleMultiplier() + ".";
                        case BLACKSMITH -> base + " Cooldown "
                                        + formatSeconds(SkillConfig.getBlacksmithAbilityCooldownSeconds())
                                        + ", dura " + SkillConfig.getBlacksmithAbilityDurationSeconds()
                                        + "s, resistência extra +"
                                        + formatPercent(SkillConfig.getBlacksmithTitaniumResistance())
                                        + " e regeneração "
                                        + formatDecimal(SkillConfig.getBlacksmithTitaniumRegen()) + " HP/s.";
                        case BUILDER -> base + " Cooldown " + formatSeconds(SkillConfig.getBuilderAbilityCooldownSeconds())
                                        + ", dura " + SkillConfig.getBuilderAbilityDurationSeconds()
                                        + "s, até " + SkillConfig.getBuilderBrushMaxDistance()
                                        + " blocos entre pontos e modos cubo/esfera/cilindro/pirâmide/parede.";
                        case EXPLORER -> base + " Cooldown "
                                        + formatSeconds(SkillConfig.getExplorerAbilityCooldownSeconds())
                                        + ", dura 60s e revela tesouros em raio de "
                                        + SkillConfig.getExplorerTreasureRadius() + " blocos.";
                };
        }

        public static List<XpSourceInfo> getDetailedXpSources(MurilloSkillsList skill) {
                return switch (skill) {
                        case MINER -> List.of(
                                        new XpSourceInfo("Stone / Deepslate / Cobblestone",
                                                        SkillConfig.getMinerXpStone() + " XP", 0xFFAAAAAA),
                                        new XpSourceInfo("Coal Ore",
                                                        SkillConfig.getMinerXpCoal() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Copper Ore",
                                                        SkillConfig.getMinerXpCopper() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Iron Ore",
                                                        SkillConfig.getMinerXpIron() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Lapis / Redstone Ore",
                                                        SkillConfig.getMinerXpLapis() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Gold Ore",
                                                        SkillConfig.getMinerXpGold() + " XP", 0xFFFFCC66),
                                        new XpSourceInfo("Nether Quartz / Gold Ore",
                                                        SkillConfig.getMinerXpNetherQuartz() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Diamond Ore",
                                                        SkillConfig.getMinerXpDiamond() + " XP", 0xFF66FFFF),
                                        new XpSourceInfo("Emerald Ore",
                                                        SkillConfig.getMinerXpEmerald() + " XP", 0xFF55FF55),
                                        new XpSourceInfo("Ancient Debris",
                                                        SkillConfig.getMinerXpAncientDebris() + " XP", 0xFFFF88CC),
                                        new XpSourceInfo("Silk Touch = sem XP de ore", "--", 0xFF888888));
                        case WARRIOR -> List.of(
                                        new XpSourceInfo("Mobs comuns (Zombie, Skeleton...)",
                                                        SkillConfig.getWarriorXpMonsterDefault() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Enderman",
                                                        SkillConfig.getWarriorXpEnderman() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Blaze",
                                                        SkillConfig.getWarriorXpBlaze() + " XP", 0xFFFFCC66),
                                        new XpSourceInfo("Warden / Wither",
                                                        SkillConfig.getWarriorXpWarden() + " XP", 0xFF66FFFF),
                                        new XpSourceInfo("Ender Dragon",
                                                        SkillConfig.getWarriorXpEnderDragon() + " XP", 0xFFFF88CC),
                                        new XpSourceInfo("Animais passivos", "0 XP", 0xFF888888));
                        case FARMER -> List.of(
                                        new XpSourceInfo("Sweet Berry",
                                                        SkillConfig.getFarmerXpSweetBerry() + " XP", 0xFFAAAAAA),
                                        new XpSourceInfo("Wheat / Carrot / Potato",
                                                        SkillConfig.getFarmerXpWheat() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Beetroot",
                                                        SkillConfig.getFarmerXpBeetroot() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Cocoa",
                                                        SkillConfig.getFarmerXpCocoa() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Nether Wart",
                                                        SkillConfig.getFarmerXpNetherWart() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Melon / Pumpkin",
                                                        SkillConfig.getFarmerXpMelon() + " XP", 0xFFFFCC66),
                                        new XpSourceInfo("Plantar sementes", "25%", 0xFF88CCFF),
                                        new XpSourceInfo("Compostagem", "2 XP", 0xFFAAAAAA),
                                        new XpSourceInfo("So colhe maduro = XP", "--", 0xFF888888));
                        case ARCHER -> List.of(
                                        new XpSourceInfo("Acertar mob passivo",
                                                        SkillConfig.getArcherXpHitBase() + " XP", 0xFFAAAAAA),
                                        new XpSourceInfo("Acertar mob hostil",
                                                        SkillConfig.getArcherXpHitHostile() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Matar mob passivo",
                                                        SkillConfig.getArcherXpKillBase() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Matar mob hostil",
                                                        SkillConfig.getArcherXpKillHostile() + " XP", 0xFFFFCC66),
                                        new XpSourceInfo("Distancia " + SkillConfig.getArcherLongRangeTier1() + "+ blocos",
                                                        "x" + formatDecimal(SkillConfig.getArcherLongRangeMultiplier1()),
                                                        0xFF66FFFF),
                                        new XpSourceInfo("Distancia " + SkillConfig.getArcherLongRangeTier2() + "+ blocos",
                                                        "x" + formatDecimal(SkillConfig.getArcherLongRangeMultiplier2()),
                                                        0xFFFF88CC),
                                        new XpSourceInfo("PvP = sem XP", "--", 0xFF888888));
                        case FISHER -> List.of(
                                        new XpSourceInfo("Junk (Bowl, Leather, Stick...)",
                                                        SkillConfig.getFisherXpJunk() + " XP", 0xFFAAAAAA),
                                        new XpSourceInfo("Peixe (Cod, Salmon, Tropical...)",
                                                        SkillConfig.getFisherXpFish() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Tesouro (Name Tag, Saddle...)",
                                                        SkillConfig.getFisherXpTreasure() + " XP", 0xFFFFCC66),
                                        new XpSourceInfo("Epic Bundle", "especial", 0xFF66FFFF));
                        case BLACKSMITH -> List.of(
                                        new XpSourceInfo("Smelting Copper",
                                                        SkillConfig.getBlacksmithXpSmeltCopper() + " XP", 0xFFAAAAAA),
                                        new XpSourceInfo("Smelting Iron",
                                                        SkillConfig.getBlacksmithXpSmeltIron() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Smelting Gold",
                                                        SkillConfig.getBlacksmithXpSmeltGold() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Smelting Ancient Debris",
                                                        SkillConfig.getBlacksmithXpSmeltAncientDebris() + " XP",
                                                        0xFFFF88CC),
                                        new XpSourceInfo("Enchant Lv1 / Lv2 / Lv3",
                                                        SkillConfig.getBlacksmithXpEnchantLevel1() + "/"
                                                                        + SkillConfig.getBlacksmithXpEnchantLevel2() + "/"
                                                                        + SkillConfig.getBlacksmithXpEnchantLevel3(),
                                                        0xFFFFCC66),
                                        new XpSourceInfo("Grindstone",
                                                        SkillConfig.getBlacksmithXpGrindstoneUse() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Anvil Rename",
                                                        SkillConfig.getBlacksmithXpAnvilRename() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Anvil Repair",
                                                        SkillConfig.getBlacksmithXpAnvilRepair() + " XP", 0xFFFFCC66),
                                        new XpSourceInfo("Anvil Combine Enchant",
                                                        SkillConfig.getBlacksmithXpAnvilEnchantCombine() + " XP",
                                                        0xFF66FFFF));
                        case BUILDER -> List.of(
                                        new XpSourceInfo("Basicos (Dirt, Sand, Gravel...)",
                                                        SkillConfig.getBuilderXpBasic() + " XP", 0xFFAAAAAA),
                                        new XpSourceInfo("Decorativos (Glass, Stairs...)",
                                                        SkillConfig.getBuilderXpDecorative() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Estruturais (Stone, Bricks...)",
                                                        SkillConfig.getBuilderXpStructural() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Premium (Quartz, Prismarine...)",
                                                        SkillConfig.getBuilderXpPremium() + " XP", 0xFFFFCC66),
                                        new XpSourceInfo("Craft decorativo",
                                                        SkillConfig.getBuilderXpCraftDecorative() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Craft estrutural",
                                                        SkillConfig.getBuilderXpCraftStructural() + " XP", 0xFF88FF88));
                        case EXPLORER -> List.of(
                                        new XpSourceInfo("Distancia (cada "
                                                        + (int) SkillConfig.getExplorerDistanceThreshold() + " blocos)",
                                                        SkillConfig.getExplorerXpPerDistance() + " XP", 0xFFAAAAAA),
                                        new XpSourceInfo("Estrutura encontrada",
                                                        SkillConfig.getExplorerXpStructure() + " XP", 0xFFCCCCCC),
                                        new XpSourceInfo("Bau de loot (primeira vez)",
                                                        SkillConfig.getExplorerXpLootChest() + " XP", 0xFF88FF88),
                                        new XpSourceInfo("Trade com Wandering Trader",
                                                        SkillConfig.getExplorerXpWanderingTrade() + " XP", 0xFFFFCC66),
                                        new XpSourceInfo("Bioma novo descoberto",
                                                        SkillConfig.getExplorerXpBiome() + " XP", 0xFF66FFFF),
                                        new XpSourceInfo("Mapa completo",
                                                        SkillConfig.getExplorerXpMapComplete() + " XP", 0xFFFF88CC));
                };
        }

        public static List<GuideEntry> getGuideTimeline(MurilloSkillsList skill) {
                List<GuideEntry> entries = new ArrayList<>();
                int maxLevel = SkillConfig.getMaxLevel();
                for (int level = 1; level <= maxLevel; level++) {
                        entries.add(buildGuideEntry(skill, level));
                }
                return entries;
        }

        
        private static GuideEntry buildGuideEntry(MurilloSkillsList skill, int level) {
                return switch (skill) {
                        case MINER -> buildMinerEntry(level);
                        case WARRIOR -> buildWarriorEntry(level);
                        case FARMER -> buildFarmerEntry(level);
                        case ARCHER -> buildArcherEntry(level);
                        case FISHER -> buildFisherEntry(level);
                        case BLACKSMITH -> buildBlacksmithEntry(level);
                        case BUILDER -> buildBuilderEntry(level);
                        case EXPLORER -> buildExplorerEntry(level);
                };
        }

        private static GuideEntry buildMinerEntry(int level) {
                List<String> segments = new ArrayList<>();
                segments.add("+" + formatPercent(level * SkillConfig.getMinerSpeedPerLevel()) + "% vel. mineração");
                segments.add("+" + formatDecimal(level * SkillConfig.getMinerFortunePerLevel()) + " fortuna");
                if (level >= SkillConfig.getMinerNightVisionLevel()) {
                        segments.add("Visão Noturna em cavernas");
                }
                if (level >= SkillConfig.getMinerDurabilityLevel()) {
                        segments.add(formatPercent(SkillConfig.getMinerDurabilityChance())
                                        + "% de chance de não gastar durabilidade");
                }
                if (level >= SkillConfig.getMinerRadarLevel()) {
                        segments.add("Radar de minérios");
                }
                if (level >= SkillConfig.getMinerMasterLevel()) {
                        segments.add("Master Miner liberado");
                }
                return new GuideEntry(level, formatLevelLine(level, segments), isMilestone(MurilloSkillsList.MINER, level));
        }

        private static GuideEntry buildWarriorEntry(int level) {
                List<String> segments = new ArrayList<>();
                segments.add("+" + formatDecimal(level * SkillConfig.getWarriorDamagePerLevel()) + " dano melee");
                int hearts = (level >= 10 ? 1 : 0) + (level >= 50 ? 1 : 0) + (level >= 100 ? 3 : 0);
                if (hearts > 0) {
                        segments.add("+" + hearts + " corações");
                }
                if (level >= SkillConfig.getResistanceUnlockLevel()) {
                        segments.add("-" + formatPercent(1.0f - SkillConfig.getResistanceReduction())
                                        + "% dano recebido");
                }
                if (level >= SkillConfig.getLifestealUnlockLevel()) {
                        segments.add("vampirismo " + formatPercent(SkillConfig.getLifestealPercentage()) + "%");
                }
                if (level >= SkillConfig.getWarriorMasterLevel()) {
                        segments.add("Berserk liberado");
                }
                return new GuideEntry(level, formatLevelLine(level, segments), isMilestone(MurilloSkillsList.WARRIOR, level));
        }

        private static GuideEntry buildFarmerEntry(int level) {
                List<String> segments = new ArrayList<>();
                segments.add("colheita dupla " + formatPercent(FarmerSkill.getDoubleHarvestChance(level, 0)) + "%");
                segments.add("cultivo dourado " + formatPercent(FarmerSkill.getGoldenCropChance(level, 0)) + "%");
                if (level >= SkillConfig.getFarmerGreenThumbLevel()) {
                        segments.add("+5% colheita e 10% semente salva");
                }
                if (level >= SkillConfig.getFarmerFertileGroundLevel()) {
                        segments.add("+25% crescimento e plantio 3x3");
                }
                if (level >= SkillConfig.getFarmerNaturesVitalityLevel()) {
                        segments.add("Vitalidade Natural (Regen em terra)");
                }
                if (level >= SkillConfig.getFarmerNutrientCycleLevel()) {
                        segments.add("2x Bone Meal e +5% sementes");
                }
                if (level >= SkillConfig.getFarmerSeedMasterLevel()) {
                        segments.add("Mestre das Sementes (Haste I)");
                }
                if (level >= SkillConfig.getFarmerAbundantHarvestLevel()) {
                        segments.add("+15% colheita e 10% adjacente");
                }
                if (level >= SkillConfig.getFarmerMasterLevel()) {
                        segments.add("Harvest Moon liberado");
                }
                return new GuideEntry(level, formatLevelLine(level, segments), isMilestone(MurilloSkillsList.FARMER, level));
        }

        private static GuideEntry buildArcherEntry(int level) {
                List<String> segments = new ArrayList<>();
                segments.add("+" + formatPercent(ArcherSkill.getRangedDamageMultiplier(level, 0) - 1.0)
                                + "% dano à distância");
                if (level >= SkillConfig.getArcherFastArrowsLevel()) {
                        segments.add("flechas +"
                                        + formatPercent(SkillConfig.getArcherArrowSpeedMultiplier() - 1.0f)
                                        + "% velocidade");
                }
                if (level >= SkillConfig.getArcherBonusDamageLevel()) {
                        segments.add("bônus extra +" + formatPercent(SkillConfig.getArcherBonusDamageAmount()) + "%");
                }
                if (level >= SkillConfig.getArcherPenetrationLevel()) {
                        double scale = (double) (level - SkillConfig.getArcherPenetrationLevel())
                                        / Math.max(1, SkillConfig.getMaxLevel() - SkillConfig.getArcherPenetrationLevel());
                        double penetration = SkillConfig.getArcherArmorPenetrationPercent() * (0.5 + (0.5 * scale));
                        segments.add("penetração e armor pen +" + formatPercent(penetration) + "%");
                }
                if (level >= SkillConfig.getArcherStableShotLevel()) {
                        segments.add("-" + formatPercent(SkillConfig.getArcherSpreadReduction()) + "% dispersão");
                }
                if (level >= SkillConfig.getArcherMasterLevel()) {
                        segments.add("Master Ranger liberado");
                }
                return new GuideEntry(level, formatLevelLine(level, segments), isMilestone(MurilloSkillsList.ARCHER, level));
        }
        public static List<Text> getMaxPassiveGuide(MurilloSkillsList skill) {
                List<Text> lines = new ArrayList<>();
                float prestigeMultiplier = 1.0f;
                int level = com.murilloskills.utils.SkillConfig.getMaxLevel();

                switch (skill) {
                        case MINER -> {
                                int speed = (int) (level * com.murilloskills.utils.SkillConfig.getMinerSpeedPerLevel()
                                                * 100 * prestigeMultiplier);
                                int fortune = (int) (level * com.murilloskills.utils.SkillConfig.getMinerFortunePerLevel()
                                                * prestigeMultiplier);
                                lines.add(Text.translatable("murilloskills.passive.miner.mining_speed", speed)
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.miner.extra_fortune", fortune)
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.miner.night_vision")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.miner.durability")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.miner.ore_radar")
                                                .formatted(Formatting.AQUA));
                        }
                        case WARRIOR -> {
                                double damage = level * com.murilloskills.utils.SkillConfig.getWarriorDamagePerLevel()
                                                * prestigeMultiplier;
                                lines.add(Text.translatable("murilloskills.passive.warrior.base_damage",
                                                formatDecimal(damage)).formatted(Formatting.RED));
                                lines.add(Text.translatable("murilloskills.passive.warrior.max_health", 5)
                                                .formatted(Formatting.RED));
                                lines.add(Text.translatable("murilloskills.passive.warrior.iron_skin")
                                                .formatted(Formatting.GOLD));
                                lines.add(Text.translatable("murilloskills.passive.warrior.vampirism")
                                                .formatted(Formatting.DARK_PURPLE));
                        }
                        case FARMER -> {
                                int doubleChance = (int) (level
                                                * com.murilloskills.utils.SkillConfig.getFarmerDoubleHarvestPerLevel()
                                                * 100 * prestigeMultiplier);
                                int goldenChance = (int) (level
                                                * com.murilloskills.utils.SkillConfig.getFarmerGoldenCropPerLevel()
                                                * 100 * prestigeMultiplier);
                                lines.add(Text.translatable("murilloskills.passive.farmer.double_harvest", doubleChance)
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.farmer.golden_crop", goldenChance)
                                                .formatted(Formatting.GOLD));
                                lines.add(Text.translatable("murilloskills.passive.farmer.green_thumb")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.farmer.fertile_ground")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.farmer.natures_vitality")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.farmer.nutrient_cycle")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.farmer.seed_master")
                                                .formatted(Formatting.GOLD));
                                lines.add(Text.translatable("murilloskills.passive.farmer.abundant_harvest")
                                                .formatted(Formatting.GOLD));
                        }
                        case ARCHER -> {
                                int arrowDamage = (int) (level * com.murilloskills.utils.SkillConfig.getArcherDamagePerLevel()
                                                * 100 * prestigeMultiplier);
                                int arrowSpeed = (int) ((com.murilloskills.utils.SkillConfig.getArcherArrowSpeedMultiplier()
                                                - 1.0f) * 100);
                                int bonusDamage = (int) (com.murilloskills.utils.SkillConfig.getArcherBonusDamageAmount()
                                                * 100);
                                int precision = (int) (com.murilloskills.utils.SkillConfig.getArcherSpreadReduction()
                                                * 100);
                                lines.add(Text.translatable("murilloskills.passive.archer.arrow_damage", arrowDamage)
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.archer.arrow_speed", arrowSpeed)
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.archer.bonus_damage", bonusDamage)
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.archer.penetration")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.archer.precision", precision)
                                                .formatted(Formatting.GOLD));
                                lines.add(Text.translatable("murilloskills.passive.archer.master")
                                                .formatted(Formatting.GOLD));
                        }
                        case FISHER -> {
                                int fishingSpeed = (int) (level * com.murilloskills.utils.SkillConfig.getFisherSpeedPerLevel()
                                                * 100 * prestigeMultiplier);
                                int bundleChance = (int) (level
                                                * com.murilloskills.utils.SkillConfig.getFisherEpicBundlePerLevel()
                                                * 100 * prestigeMultiplier);
                                lines.add(Text.translatable("murilloskills.passive.fisher.fishing_speed", fishingSpeed)
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.fisher.epic_bundle_chance", bundleChance)
                                                .formatted(Formatting.GOLD));
                                lines.add(Text.translatable("murilloskills.passive.fisher.wait_reduction")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.fisher.treasure_chance")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.fisher.ocean_blessing")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.fisher.dolphins_grace")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.fisher.seas_fortune")
                                                .formatted(Formatting.GOLD));
                                lines.add(Text.translatable("murilloskills.passive.fisher.luck_of_sea")
                                                .formatted(Formatting.AQUA));
                        }
                        case BLACKSMITH -> {
                                int resistance = (int) (level
                                                * com.murilloskills.utils.SkillConfig.getBlacksmithResistancePerLevel()
                                                * 100 * prestigeMultiplier);
                                lines.add(Text.translatable("murilloskills.passive.blacksmith.physical_resistance",
                                                resistance).formatted(Formatting.GOLD));
                                lines.add(Text.translatable("murilloskills.passive.blacksmith.iron_skin")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.blacksmith.efficient_anvil")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.blacksmith.fire_mastery")
                                                .formatted(Formatting.RED));
                                lines.add(Text.translatable("murilloskills.passive.blacksmith.forged_resilience")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.blacksmith.repair_aura")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.blacksmith.thorns_master")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.blacksmith.master_enchanter")
                                                .formatted(Formatting.LIGHT_PURPLE));
                        }
                        case BUILDER -> {
                                double reach = com.murilloskills.impl.BuilderSkill.getReachBonus(level, 0);
                                lines.add(Text.translatable("murilloskills.passive.builder.extra_reach",
                                                formatDecimal(reach)).formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.builder.extended_reach")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.builder.efficient_crafting")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.builder.safe_landing")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.builder.builders_vigor")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.builder.scaffold_master")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.builder.feather_build")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.builder.master_reach")
                                                .formatted(Formatting.GOLD));
                        }
                        case EXPLORER -> {
                                int speed = (int) (level * com.murilloskills.utils.SkillConfig.getExplorerSpeedPerLevel()
                                                * 100 * prestigeMultiplier);
                                int luck = level / com.murilloskills.utils.SkillConfig.getExplorerLuckInterval();
                                lines.add(Text.translatable("murilloskills.passive.explorer.speed", speed)
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.explorer.luck", luck)
                                                .formatted(Formatting.GOLD));
                                lines.add(Text.translatable("murilloskills.passive.explorer.step_assist")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.explorer.aquatic")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.explorer.night_vision")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.explorer.pathfinder")
                                                .formatted(Formatting.GREEN));
                                lines.add(Text.translatable("murilloskills.passive.explorer.swift_recovery")
                                                .formatted(Formatting.RED));
                                lines.add(Text.translatable("murilloskills.passive.explorer.feather_feet")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.explorer.nether_walker")
                                                .formatted(Formatting.AQUA));
                                lines.add(Text.translatable("murilloskills.passive.explorer.sixth_sense")
                                                .formatted(Formatting.GOLD));
                        }
                }

                return lines;
        }

        
        private static GuideEntry buildFisherEntry(int level) {
                List<String> segments = new ArrayList<>();
                segments.add("+" + formatPercent(FisherSkill.getFishingSpeedBonus(level, 0)) + "% vel. pesca");
                segments.add("bundle épico " + formatPercent(FisherSkill.getEpicBundleChance(level, 0)) + "%");
                if (level >= SkillConfig.getFisherWaitReductionLevel()) {
                        segments.add("-" + formatPercent(SkillConfig.getFisherWaitReduction()) + "% espera");
                }
                if (level >= SkillConfig.getFisherTreasureBonusLevel()) {
                        segments.add("+" + formatPercent(SkillConfig.getFisherTreasureBonus())
                                        + "% tesouro e +" + formatPercent(SkillConfig.getFisherXpBonus()) + "% XP");
                }
                if (level >= SkillConfig.getFisherOceanBlessingLevel()) {
                        segments.add("Benção do Oceano (Visão Noturna)");
                }
                if (level >= SkillConfig.getFisherDolphinGraceLevel()) {
                        segments.add("Dolphin's Grace");
                }
                if (level >= SkillConfig.getFisherSeasFortuneLevel()) {
                        segments.add("Fortuna do Mar (Luck I)");
                }
                if (level >= SkillConfig.getFisherLuckSeaLevel()) {
                        segments.add("Luck of the Sea I");
                }
                if (level >= SkillConfig.getFisherMasterLevel()) {
                        segments.add("Rain Dance liberado");
                }
                return new GuideEntry(level, formatLevelLine(level, segments), isMilestone(MurilloSkillsList.FISHER, level));
        }

        private static GuideEntry buildBlacksmithEntry(int level) {
                List<String> segments = new ArrayList<>();
                segments.add("-" + formatPercent(Math.min(level * 0.005f, 0.55f)) + "% dano físico");
                if (level >= SkillConfig.getBlacksmithIronSkinLevel()) {
                        segments.add("+" + formatPercent(SkillConfig.getBlacksmithIronSkinBonus()) + "% físico");
                }
                if (level >= SkillConfig.getBlacksmithEfficientAnvilLevel()) {
                        segments.add("-" + formatPercent(SkillConfig.getBlacksmithAnvilXpDiscount())
                                        + "% XP bigorna e "
                                        + formatPercent(SkillConfig.getBlacksmithAnvilMaterialSave())
                                        + "% material salvo");
                }
                if (level >= SkillConfig.getBlacksmithFireMasteryLevel()) {
                        segments.add("Domínio do Fogo (Fire Resistance)");
                }
                if (level >= SkillConfig.getBlacksmithForgedResilienceLevel()) {
                        segments.add("+" + formatPercent(SkillConfig.getBlacksmithFireExplosionResist())
                                        + "% fogo/explosão");
                }
                if (level >= SkillConfig.getBlacksmithRepairAuraLevel()) {
                        segments.add("Aura de Reparo (auto-reparo)");
                }
                if (level >= SkillConfig.getBlacksmithThornsMasterLevel()) {
                        segments.add(formatPercent(SkillConfig.getBlacksmithThornsChance())
                                        + "% refletir "
                                        + formatPercent(SkillConfig.getBlacksmithThornsReflect())
                                        + "% dano e -"
                                        + formatPercent(SkillConfig.getBlacksmithKnockbackReduction())
                                        + "% knockback");
                }
                if (level >= SkillConfig.getBlacksmithOverEnchantUnlockLevel()) {
                        segments.add("Master Enchanter (bigorna até nível "
                                        + SkillConfig.getBlacksmithOverEnchantMaxLevel() + ")");
                }
                if (level >= SkillConfig.getBlacksmithMasterLevel()) {
                        segments.add("Titanium Aura liberado");
                }
                return new GuideEntry(level, formatLevelLine(level, segments), isMilestone(MurilloSkillsList.BLACKSMITH, level));
        }

        private static GuideEntry buildBuilderEntry(int level) {
                List<String> segments = new ArrayList<>();
                segments.add("+" + formatDecimal(BuilderSkill.getReachBonus(level, 0)) + " blocos alcance");
                if (level >= SkillConfig.getBuilderExtendedReachLevel()) {
                        segments.add("Extended Reach ativo");
                }
                if (level >= SkillConfig.getBuilderEfficientCraftingLevel()) {
                        segments.add(formatPercent(SkillConfig.getBuilderDecorativeEconomy()) + "% economia decorativa");
                }
                if (level >= SkillConfig.getBuilderSafeLandingLevel()) {
                        segments.add("-" + formatPercent(SkillConfig.getBuilderFallDamageReduction()) + "% queda");
                }
                if (level >= SkillConfig.getBuilderBuildersVigorLevel()) {
                        segments.add("Vigor do Construtor (Haste I)");
                }
                if (level >= SkillConfig.getBuilderScaffoldMasterLevel()) {
                        segments.add("scaffold x" + formatDecimal(SkillConfig.getBuilderScaffoldSpeedMultiplier())
                                        + " e " + formatPercent(SkillConfig.getBuilderStructuralEconomy())
                                        + "% economia estrutural");
                }
                if (level >= SkillConfig.getBuilderFeatherBuildLevel()) {
                        segments.add("Construção Leve (Slow Fall)");
                }
                if (level >= SkillConfig.getBuilderMasterReachLevel()) {
                        segments.add("Master Reach ativo");
                }
                if (level >= SkillConfig.getBuilderMasterLevel()) {
                        segments.add("Creative Brush liberado");
                }
                return new GuideEntry(level, formatLevelLine(level, segments), isMilestone(MurilloSkillsList.BUILDER, level));
        }

        private static GuideEntry buildExplorerEntry(int level) {
                List<String> segments = new ArrayList<>();
                segments.add("+" + formatPercent(level * SkillConfig.getExplorerSpeedPerLevel()) + "% movimento");
                int luck = level / SkillConfig.getExplorerLuckInterval();
                if (luck > 0) {
                        segments.add("+" + luck + " Luck");
                }
                double hungerReduction = 1.0 - ExplorerSkill.getHungerReductionMultiplier(level);
                if (hungerReduction > 0.0) {
                        segments.add("-" + formatPercent(hungerReduction) + "% fome andando");
                }
                if (level >= SkillConfig.getExplorerStepAssistLevel()) {
                        segments.add("Step Assist");
                }
                if (level >= SkillConfig.getExplorerAquaticLevel()) {
                        segments.add("+" + formatPercent(SkillConfig.getExplorerBreathMultiplier() - 1.0f)
                                        + "% fôlego e Aquatic");
                }
                if (level >= SkillConfig.getExplorerNightVisionLevel()) {
                        segments.add("Visão Noturna toggleável");
                }
                if (level >= SkillConfig.getExplorerPathfinderLevel()) {
                        segments.add("Desbravador (Speed II ao correr)");
                }
                if (level >= SkillConfig.getExplorerSwiftRecoveryLevel()) {
                        segments.add("Recuperação Rápida (Regen <50% HP)");
                }
                if (level >= SkillConfig.getExplorerFeatherFeetLevel()) {
                        segments.add("-" + formatPercent(SkillConfig.getExplorerFallDamageReduction()) + "% queda");
                }
                if (level >= SkillConfig.getExplorerNetherWalkerLevel()) {
                        segments.add("Nether Walker");
                }
                if (level >= SkillConfig.getExplorerMasterLevel()) {
                        segments.add("Sexto Sentido liberado");
                }
                return new GuideEntry(level, formatLevelLine(level, segments), isMilestone(MurilloSkillsList.EXPLORER, level));
        }

        private static boolean isMilestone(MurilloSkillsList skill, int level) {
                if (level == 1 || level == SkillConfig.getMaxLevel()) {
                        return true;
                }
                List<PerkInfo> perks = SKILL_PERKS.get(skill);
                if (perks == null) {
                        return false;
                }
                for (PerkInfo perk : perks) {
                        if (perk.level() == level) {
                                return true;
                        }
                }
                return false;
        }

        private static String formatLevelLine(int level, List<String> segments) {
                return Text.translatable("murilloskills.gui.level_prefix").getString() + " " + level + ": "
                                + String.join(", ", segments);
        }

        private static String formatPercent(double value) {
                return formatDecimal(value * 100.0);
        }

        private static String formatSeconds(int seconds) {
                if (seconds >= 60 && seconds % 60 == 0) {
                        return (seconds / 60) + "m";
                }
                if (seconds > 60) {
                        return (seconds / 60) + "m " + (seconds % 60) + "s";
                }
                return seconds + "s";
        }
        private static String formatDecimal(double value) {
                return String.format(Locale.ROOT, "%.1f", value);
        }
}





