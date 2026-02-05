package com.murilloskills.data;

import com.google.gson.annotations.SerializedName;
import com.murilloskills.config.ModConfig;

public class XpValuesDefinition {
    public Miner miner = new Miner();
    public Warrior warrior = new Warrior();
    public Archer archer = new Archer();
    public Farmer farmer = new Farmer();
    public Fisher fisher = new Fisher();
    public Blacksmith blacksmith = new Blacksmith();
    public Builder builder = new Builder();
    public Explorer explorer = new Explorer();

    public static XpValuesDefinition defaultsFromConfig() {
        ModConfig.ConfigData config = ModConfig.get();
        XpValuesDefinition values = new XpValuesDefinition();

        values.miner.blocks.stone = config.minerSource.xpStone;
        values.miner.blocks.coal = config.minerSource.xpCoal;
        values.miner.blocks.copper = config.minerSource.xpCopper;
        values.miner.blocks.iron = config.minerSource.xpIron;
        values.miner.blocks.gold = config.minerSource.xpGold;
        values.miner.blocks.lapis = config.minerSource.xpLapis;
        values.miner.blocks.redstone = config.minerSource.xpRedstone;
        values.miner.blocks.diamond = config.minerSource.xpDiamond;
        values.miner.blocks.emerald = config.minerSource.xpEmerald;
        values.miner.blocks.ancientDebris = config.minerSource.xpAncientDebris;
        values.miner.blocks.netherQuartz = config.minerSource.xpNetherQuartz;
        values.miner.blocks.netherGold = config.minerSource.xpNetherGold;

        values.warrior.mobs.enderDragon = config.warriorSource.xpEnderDragon;
        values.warrior.mobs.wither = config.warriorSource.xpWither;
        values.warrior.mobs.warden = config.warriorSource.xpWarden;
        values.warrior.mobs.enderman = config.warriorSource.xpEnderman;
        values.warrior.mobs.blaze = config.warriorSource.xpBlaze;
        values.warrior.mobs.defaultMob = config.warriorSource.xpMonsterDefault;

        values.archer.hits.base = config.archerSource.xpHitBase;
        values.archer.hits.hostile = config.archerSource.xpHitHostile;
        values.archer.kills.base = config.archerSource.xpKillBase;
        values.archer.kills.hostile = config.archerSource.xpKillHostile;
        values.archer.longRange.tier1 = config.archerSource.longRangeTier1;
        values.archer.longRange.tier2 = config.archerSource.longRangeTier2;
        values.archer.longRange.tier3 = config.archerSource.longRangeTier3;
        values.archer.longRange.multiplier1 = config.archerSource.longRangeMultiplier1;
        values.archer.longRange.multiplier2 = config.archerSource.longRangeMultiplier2;
        values.archer.longRange.multiplier3 = config.archerSource.longRangeMultiplier3;

        values.farmer.crops.wheat = config.farmerSource.xpWheat;
        values.farmer.crops.carrot = config.farmerSource.xpCarrot;
        values.farmer.crops.potato = config.farmerSource.xpPotato;
        values.farmer.crops.beetroot = config.farmerSource.xpBeetroot;
        values.farmer.crops.melon = config.farmerSource.xpMelon;
        values.farmer.crops.pumpkin = config.farmerSource.xpPumpkin;
        values.farmer.crops.netherWart = config.farmerSource.xpNetherWart;
        values.farmer.crops.sweetBerry = config.farmerSource.xpSweetBerry;
        values.farmer.crops.cocoa = config.farmerSource.xpCocoa;
        values.farmer.plantingMultiplier = 0.25;
        values.farmer.compostingXp = 2;
        values.farmer.boneMealXp = 1;

        values.fisher.categories.treasure = config.fisherSource.xpTreasure;
        values.fisher.categories.fish = config.fisherSource.xpFish;
        values.fisher.categories.junk = config.fisherSource.xpJunk;

        values.blacksmith.anvil.repair = config.blacksmithSource.xpAnvilRepair;
        values.blacksmith.anvil.rename = config.blacksmithSource.xpAnvilRename;
        values.blacksmith.anvil.enchantCombine = config.blacksmithSource.xpAnvilEnchantCombine;
        values.blacksmith.enchanting.level1 = config.blacksmithSource.xpEnchantLevel1;
        values.blacksmith.enchanting.level2 = config.blacksmithSource.xpEnchantLevel2;
        values.blacksmith.enchanting.level3 = config.blacksmithSource.xpEnchantLevel3;
        values.blacksmith.smelting.iron = config.blacksmithSource.xpSmeltIron;
        values.blacksmith.smelting.gold = config.blacksmithSource.xpSmeltGold;
        values.blacksmith.smelting.copper = config.blacksmithSource.xpSmeltCopper;
        values.blacksmith.smelting.ancientDebris = config.blacksmithSource.xpSmeltAncientDebris;
        values.blacksmith.grindstoneXp = config.blacksmithSource.xpGrindstoneUse;

        values.builder.placement.structural = config.builderSource.xpStructural;
        values.builder.placement.decorative = config.builderSource.xpDecorative;
        values.builder.placement.basic = config.builderSource.xpBasic;
        values.builder.placement.premium = config.builderSource.xpPremium;
        values.builder.crafting.structural = config.builderSource.xpCraftStructural;
        values.builder.crafting.decorative = config.builderSource.xpCraftDecorative;

        values.explorer.discoveries.biome = config.explorer.xpBiome;
        values.explorer.discoveries.structure = config.explorer.xpStructure;
        values.explorer.loot.chest = config.explorer.xpLootChest;
        values.explorer.loot.mapComplete = config.explorer.xpMapComplete;
        values.explorer.loot.wanderingTrade = config.explorer.xpWanderingTrade;
        values.explorer.travel.distanceThreshold = config.explorer.distanceThreshold;
        values.explorer.travel.xpPerDistance = config.explorer.xpPerDistance;

        return values;
    }

    public void applyDefaults(XpValuesDefinition defaults) {
        if (miner == null) {
            miner = defaults.miner;
        } else {
            miner.applyDefaults(defaults.miner);
        }
        if (warrior == null) {
            warrior = defaults.warrior;
        } else {
            warrior.applyDefaults(defaults.warrior);
        }
        if (archer == null) {
            archer = defaults.archer;
        } else {
            archer.applyDefaults(defaults.archer);
        }
        if (farmer == null) {
            farmer = defaults.farmer;
        } else {
            farmer.applyDefaults(defaults.farmer);
        }
        if (fisher == null) {
            fisher = defaults.fisher;
        } else {
            fisher.applyDefaults(defaults.fisher);
        }
        if (blacksmith == null) {
            blacksmith = defaults.blacksmith;
        } else {
            blacksmith.applyDefaults(defaults.blacksmith);
        }
        if (builder == null) {
            builder = defaults.builder;
        } else {
            builder.applyDefaults(defaults.builder);
        }
        if (explorer == null) {
            explorer = defaults.explorer;
        } else {
            explorer.applyDefaults(defaults.explorer);
        }
    }

    public static class Miner {
        public Blocks blocks = new Blocks();

        public void applyDefaults(Miner defaults) {
            if (blocks == null) {
                blocks = defaults.blocks;
            }
        }

        public static class Blocks {
            public int stone = 2;
            public int coal = 5;
            public int copper = 5;
            public int iron = 10;
            public int gold = 15;
            public int lapis = 10;
            public int redstone = 10;
            public int diamond = 60;
            public int emerald = 100;
            @SerializedName("ancient_debris")
            public int ancientDebris = 150;
            @SerializedName("nether_quartz")
            public int netherQuartz = 10;
            @SerializedName("nether_gold")
            public int netherGold = 10;
        }
    }

    public static class Warrior {
        public Mobs mobs = new Mobs();

        public void applyDefaults(Warrior defaults) {
            if (mobs == null) {
                mobs = defaults.mobs;
            }
        }

        public static class Mobs {
            @SerializedName("ender_dragon")
            public int enderDragon = 1000;
            public int wither = 500;
            public int warden = 500;
            public int enderman = 20;
            public int blaze = 25;
            @SerializedName("default")
            public int defaultMob = 15;
        }
    }

    public static class Archer {
        public Hit hits = new Hit();
        public Kill kills = new Kill();
        @SerializedName("long_range")
        public LongRange longRange = new LongRange();

        public void applyDefaults(Archer defaults) {
            if (hits == null) {
                hits = defaults.hits;
            }
            if (kills == null) {
                kills = defaults.kills;
            }
            if (longRange == null) {
                longRange = defaults.longRange;
            }
        }

        public static class Hit {
            public int base = 5;
            public int hostile = 10;
        }

        public static class Kill {
            public int base = 15;
            public int hostile = 25;
        }

        public static class LongRange {
            public int tier1 = 20;
            public int tier2 = 40;
            public int tier3 = 60;
            public double multiplier1 = 1.5;
            public double multiplier2 = 2.0;
            public double multiplier3 = 2.0;
        }
    }

    public static class Farmer {
        public Crops crops = new Crops();
        @SerializedName("planting_multiplier")
        public double plantingMultiplier = 0.25;
        @SerializedName("composting_xp")
        public int compostingXp = 2;
        @SerializedName("bone_meal_xp")
        public int boneMealXp = 1;

        public void applyDefaults(Farmer defaults) {
            if (crops == null) {
                crops = defaults.crops;
            }
            if (plantingMultiplier == 0) {
                plantingMultiplier = defaults.plantingMultiplier;
            }
            if (compostingXp == 0) {
                compostingXp = defaults.compostingXp;
            }
            if (boneMealXp == 0) {
                boneMealXp = defaults.boneMealXp;
            }
        }

        public static class Crops {
            public int wheat = 3;
            public int carrot = 3;
            public int potato = 3;
            public int beetroot = 3;
            public int melon = 8;
            public int pumpkin = 8;
            @SerializedName("nether_wart")
            public int netherWart = 5;
            @SerializedName("sweet_berry")
            public int sweetBerry = 2;
            public int cocoa = 4;
        }
    }

    public static class Fisher {
        public Categories categories = new Categories();

        public void applyDefaults(Fisher defaults) {
            if (categories == null) {
                categories = defaults.categories;
            }
        }

        public static class Categories {
            public int treasure = 50;
            public int fish = 15;
            public int junk = 5;
        }
    }

    public static class Blacksmith {
        public Anvil anvil = new Anvil();
        public Enchanting enchanting = new Enchanting();
        public Smelting smelting = new Smelting();
        @SerializedName("grindstone_xp")
        public int grindstoneXp = 30;

        public void applyDefaults(Blacksmith defaults) {
            if (anvil == null) {
                anvil = defaults.anvil;
            }
            if (enchanting == null) {
                enchanting = defaults.enchanting;
            }
            if (smelting == null) {
                smelting = defaults.smelting;
            }
            if (grindstoneXp == 0) {
                grindstoneXp = defaults.grindstoneXp;
            }
        }

        public static class Anvil {
            public int repair = 80;
            public int rename = 50;
            @SerializedName("enchant_combine")
            public int enchantCombine = 100;
        }

        public static class Enchanting {
            @SerializedName("level1")
            public int level1 = 40;
            @SerializedName("level2")
            public int level2 = 70;
            @SerializedName("level3")
            public int level3 = 100;
        }

        public static class Smelting {
            public int iron = 4;
            public int gold = 6;
            public int copper = 3;
            @SerializedName("ancient_debris")
            public int ancientDebris = 80;
        }
    }

    public static class Builder {
        public Placement placement = new Placement();
        public Crafting crafting = new Crafting();

        public void applyDefaults(Builder defaults) {
            if (placement == null) {
                placement = defaults.placement;
            }
            if (crafting == null) {
                crafting = defaults.crafting;
            }
        }

        public static class Placement {
            public int structural = 15;
            public int decorative = 10;
            public int basic = 3;
            public int premium = 25;
        }

        public static class Crafting {
            public int structural = 20;
            public int decorative = 12;
        }
    }

    public static class Explorer {
        public Discoveries discoveries = new Discoveries();
        public Loot loot = new Loot();
        public Travel travel = new Travel();

        public void applyDefaults(Explorer defaults) {
            if (discoveries == null) {
                discoveries = defaults.discoveries;
            }
            if (loot == null) {
                loot = defaults.loot;
            }
            if (travel == null) {
                travel = defaults.travel;
            }
        }

        public static class Discoveries {
            public int biome = 500;
            public int structure = 200;
        }

        public static class Loot {
            public int chest = 300;
            @SerializedName("map_complete")
            public int mapComplete = 2000;
            @SerializedName("wandering_trade")
            public int wanderingTrade = 400;
        }

        public static class Travel {
            @SerializedName("distance_threshold")
            public double distanceThreshold = 50.0;
            @SerializedName("xp_per_distance")
            public int xpPerDistance = 35;
        }
    }
}
