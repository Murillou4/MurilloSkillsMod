package com.murilloskills.runtime;

import com.murilloskills.core.config.SkillType;

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
            runtime.awardEventXp(event, SkillType.MINER, 20);
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
        if (name.contains("itemcrafted") || name.contains("smelted") || name.contains("anvil")) {
            runtime.awardEventXp(event, SkillType.BLACKSMITH, 20);
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
            runtime.awardEventXp(args, SkillType.MINER, 20);
        }
    }

    private Object first(Object[] args) {
        return args == null || args.length == 0 ? null : args[0];
    }
}
