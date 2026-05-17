package com.murilloskills.runtime;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.compat.CrossModCompatRules;

public final class PlatformEventBridge {
    private final MurilloSkillsRuntime runtime;

    PlatformEventBridge(MurilloSkillsRuntime runtime) {
        this.runtime = runtime;
    }

    public void handleEvent(Object event) {
        if (event == null) {
            return;
        }
        String name = event.getClass().getName().toLowerCase();
        if (name.contains("registercommands")) {
            Object dispatcher = ReflectionSupport.invokeAny(event, "getDispatcher");
            if (dispatcher == null) {
                dispatcher = ReflectionSupport.getFieldAny(event, "dispatcher");
            }
            runtime.registerCommands(dispatcher);
            return;
        }
        if (name.contains("loggedin") || name.contains("login") || name.contains("join")) {
            runtime.onPlayerJoin(event);
            return;
        }
        if (name.contains("loggedout") || name.contains("logout") || name.contains("disconnect")) {
            runtime.onPlayerLeave(event);
            return;
        }
        if (name.contains("serverstopping") || name.contains("serverstoppedevent")) {
            runtime.onServerStopping(event);
            return;
        }
        if (name.contains("servertick") || name.contains("tickevent")) {
            runtime.onTick(event);
            return;
        }
        if (name.contains("breakevent") || name.contains("blockbreak")) {
            awardBlockBreakXp(event, ReflectionSupport.describe(event));
            return;
        }
        if (name.contains("harvest") || name.contains("crop")) {
            runtime.awardEventXp(event, SkillType.FARMER, 20);
            return;
        }
        if (name.contains("livingdeath") || name.contains("mob") || name.contains("attack")) {
            runtime.awardEventXp(event, SkillType.WARRIOR, 25);
            return;
        }
        if (name.contains("itemcrafted")) {
            awardCraftingXp(event, ReflectionSupport.describe(event));
            return;
        }
        if (name.contains("smelted") || name.contains("anvil") || name.contains("enchant")) {
            runtime.awardEventXp(event, SkillType.BLACKSMITH, 20);
            return;
        }
        if (name.contains("rightclickblock") || name.contains("interact") || name.contains("useblock")) {
            awardUseBlockXp(event, ReflectionSupport.describe(event));
            return;
        }
        if (name.contains("itemfished") || name.contains("fish")) {
            awardFishingXp(event, ReflectionSupport.describe(event));
        }
    }

    public void handleFabricCallback(String callback, Object[] args) {
        if ("server-started".equals(callback)) {
            runtime.onServerStarted(first(args));
            return;
        }
        if ("server-stopping".equals(callback)) {
            runtime.onServerStopping(first(args));
            return;
        }
        if ("server-tick-end".equals(callback)) {
            runtime.onTick(first(args));
            return;
        }
        if ("command-registration".equals(callback)) {
            runtime.registerCommands(first(args));
            return;
        }
        if ("player-join".equals(callback)) {
            runtime.onPlayerJoin(args);
            return;
        }
        if ("player-disconnect".equals(callback)) {
            runtime.onPlayerLeave(args);
            return;
        }
        if ("block-break".equals(callback)) {
            awardBlockBreakXp(args, ReflectionSupport.describe(args));
            return;
        }
        if ("use-block".equals(callback)) {
            awardUseBlockXp(args, ReflectionSupport.describe(args));
            return;
        }
        if ("attack-entity".equals(callback) || "entity-kill".equals(callback)) {
            runtime.awardEventXp(args, SkillType.WARRIOR, 25);
        }
    }

    private void awardBlockBreakXp(Object source, String description) {
        if (isOre(description)) {
            runtime.awardEventXp(source, SkillType.MINER, 35);
        } else {
            runtime.awardEventXp(source, SkillType.MINER, 20);
        }
        if (isPlant(description)) {
            runtime.awardEventXp(source, SkillType.FARMER, 20);
        }
    }

    private void awardCraftingXp(Object source, String description) {
        if (isConstruction(description)) {
            runtime.awardEventXp(source, SkillType.BUILDER, 20);
        } else {
            runtime.awardEventXp(source, SkillType.BLACKSMITH, 20);
        }
    }

    private void awardUseBlockXp(Object source, String description) {
        if (isLootContainer(description)) {
            runtime.awardEventXp(source, SkillType.EXPLORER, 25);
        }
        if (isMachine(description)) {
            runtime.awardEventXp(source, SkillType.BLACKSMITH, 15);
        }
        if (isPlant(description)) {
            runtime.awardEventXp(source, SkillType.FARMER, 10);
        }
    }

    private void awardFishingXp(Object source, String description) {
        if (CrossModCompatRules.isFishingTreasureItemId(description)) {
            runtime.awardEventXp(source, SkillType.FISHER, 150);
        } else if (CrossModCompatRules.isFishingJunkItemId(description)) {
            runtime.awardEventXp(source, SkillType.FISHER, 15);
        } else {
            runtime.awardEventXp(source, SkillType.FISHER, 50);
        }
    }

    private boolean isOre(String description) {
        return description.contains("_ore") || description.contains(" ores/") || description.contains(":ores")
                || description.contains("ancient_debris") || description.contains("_deposit")
                || description.contains("_cluster") || CrossModCompatRules.isOreResourceId(description);
    }

    private boolean isPlant(String description) {
        return CrossModCompatRules.isPlantLikeId(description)
                || containsAny(description, " crop", "sapling", "plant", "seed", "berry", "bush", "wart",
                        "cane", "cactus", "kelp", "bamboo", "stem", "fungus", "mushroom");
    }

    private boolean isMachine(String description) {
        return CrossModCompatRules.isLikelyMachineIdOrClass(description);
    }

    private boolean isConstruction(String description) {
        return CrossModCompatRules.builderCategory(description) != CrossModCompatRules.BuilderCategory.NONE;
    }

    private boolean isLootContainer(String description) {
        return CrossModCompatRules.isLootContainerId(description);
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private Object first(Object[] args) {
        return args == null || args.length == 0 ? null : args[0];
    }
}
