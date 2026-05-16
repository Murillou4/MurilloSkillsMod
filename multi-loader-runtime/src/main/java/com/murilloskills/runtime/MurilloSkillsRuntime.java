package com.murilloskills.runtime;

import com.murilloskills.core.config.SkillProgressionConfig;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.core.data.XpAddResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class MurilloSkillsRuntime {
    public static final String MOD_ID = "murilloskills";
    private static final AtomicReference<MurilloSkillsRuntime> INSTANCE = new AtomicReference<MurilloSkillsRuntime>();

    private final String loader;
    private final String minecraftVersion;
    private final SkillProgressionConfig progressionConfig = SkillProgressionConfig.DEFAULT;
    private final RuntimePlayerStore playerStore;
    private int ticksSinceSave;

    private MurilloSkillsRuntime(String loader, String minecraftVersion) {
        this.loader = loader;
        this.minecraftVersion = minecraftVersion;
        this.playerStore = new RuntimePlayerStore(defaultPlayerDirectory());
    }

    public static MurilloSkillsRuntime bootstrap(String loader, String minecraftVersion) {
        MurilloSkillsRuntime runtime = new MurilloSkillsRuntime(loader, minecraftVersion);
        if (INSTANCE.compareAndSet(null, runtime)) {
            runtime.log("bootstrapped " + loader + " " + minecraftVersion);
            return runtime;
        }
        return INSTANCE.get();
    }

    public static MurilloSkillsRuntime get() {
        MurilloSkillsRuntime runtime = INSTANCE.get();
        if (runtime == null) {
            runtime = bootstrap("unknown", "unknown");
        }
        return runtime;
    }

    public PlatformEventBridge events() {
        return new PlatformEventBridge(this);
    }

    public void registerFabricHooks() {
        FabricReflectiveHooks.register(this);
    }

    public void onServerStarted(Object server) {
        Path worldPath = ReflectionSupport.findServerSaveRoot(server);
        if (worldPath != null) {
            playerStore.setPlayerDirectory(worldPath.resolve("murilloskills").resolve("players"));
        }
        log("data path: " + playerStore.getPlayerDirectory());
    }

    public void onServerStopping(Object server) {
        playerStore.saveAll();
    }

    public void onTick(Object server) {
        ticksSinceSave++;
        if (ticksSinceSave >= 6000) {
            ticksSinceSave = 0;
            playerStore.saveAll();
        }
    }

    public void onPlayerJoin(Object playerSource) {
        PlayerIdentity identity = ReflectionSupport.findPlayerIdentity(playerSource);
        if (identity.hasUuid()) {
            playerStore.load(identity.getUuid());
        }
    }

    public void onPlayerLeave(Object playerSource) {
        PlayerIdentity identity = ReflectionSupport.findPlayerIdentity(playerSource);
        if (identity.hasUuid()) {
            playerStore.save(identity.getUuid());
        }
    }

    public int selectSkill(Object source, SkillType skill) {
        PlayerContext context = context(source);
        if (!context.identity.hasUuid()) {
            return fail(source, "MurilloSkills: jogador nao encontrado para selecionar skill.");
        }

        PlayerSkillDataCore data = context.data;
        if (data.isSkillSelected(skill)) {
            return say(source, "MurilloSkills: " + skill.name().toLowerCase() + " ja esta selecionada.");
        }
        boolean selected = data.setSelectedSkills(singleton(skill), progressionConfig);
        if (!selected) {
            return fail(source, "MurilloSkills: limite de 3 skills selecionadas atingido.");
        }
        playerStore.save(context.identity.getUuid());
        return say(source, "MurilloSkills: skill selecionada: " + skill.name().toLowerCase() + ".");
    }

    public int activateParagon(Object source, SkillType skill) {
        PlayerContext context = context(source);
        if (!context.identity.hasUuid()) {
            return fail(source, "MurilloSkills: jogador nao encontrado para paragon.");
        }
        if (!context.data.activateParagonSkill(skill)) {
            return fail(source, "MurilloSkills: nao foi possivel ativar paragon para " + skill.name().toLowerCase() + ".");
        }
        playerStore.save(context.identity.getUuid());
        return say(source, "MurilloSkills: paragon ativo em " + skill.name().toLowerCase() + ".");
    }

    public int addXp(Object source, SkillType skill, int amount) {
        PlayerContext context = context(source);
        if (!context.identity.hasUuid()) {
            return fail(source, "MurilloSkills: jogador nao encontrado para XP.");
        }
        XpAddResult result = context.data.addXpToSkill(skill, amount, progressionConfig);
        playerStore.save(context.identity.getUuid());
        if (result.isLeveledUp()) {
            return say(source, "MurilloSkills: " + skill.name().toLowerCase() + " subiu para o nivel " + result.getNewLevel() + ".");
        }
        return say(source, "MurilloSkills: +" + amount + " XP em " + skill.name().toLowerCase() + ".");
    }

    public int prestige(Object source, SkillType skill) {
        PlayerContext context = context(source);
        if (!context.identity.hasUuid()) {
            return fail(source, "MurilloSkills: jogador nao encontrado para prestige.");
        }
        SkillStatsCore stats = context.data.getSkill(skill);
        if (stats.getPrestige() >= progressionConfig.getMaxPrestigeLevel()) {
            return fail(source, "MurilloSkills: prestige maximo ja atingido.");
        }
        if (stats.getLevel() < progressionConfig.getMaxLevel() - 1 && !context.data.isParagonSkill(skill)) {
            return fail(source, "MurilloSkills: precisa estar no nivel maximo permitido para prestigiar.");
        }
        stats.setPrestige(stats.getPrestige() + 1);
        stats.setLevel(0);
        stats.setXp(0.0);
        playerStore.save(context.identity.getUuid());
        return say(source, "MurilloSkills: prestige " + stats.getPrestige() + " aplicado em " + skill.name().toLowerCase() + ".");
    }

    public int triggerAbility(Object source, SkillType skill) {
        PlayerContext context = context(source);
        if (!context.identity.hasUuid()) {
            return fail(source, "MurilloSkills: jogador nao encontrado para habilidade.");
        }
        SkillStatsCore stats = context.data.getSkill(skill);
        if (stats.getLevel() < progressionConfig.getMaxLevel()) {
            return fail(source, "MurilloSkills: habilidade ativa exige nivel 100.");
        }
        stats.setLastAbilityUse(System.currentTimeMillis());
        playerStore.save(context.identity.getUuid());
        return say(source, "MurilloSkills: habilidade ativa disparada para " + skill.name().toLowerCase() + ".");
    }

    public int reset(Object source) {
        PlayerContext context = context(source);
        if (!context.identity.hasUuid()) {
            return fail(source, "MurilloSkills: jogador nao encontrado para reset.");
        }
        playerStore.put(context.identity.getUuid(), new PlayerSkillDataCore());
        playerStore.save(context.identity.getUuid());
        return say(source, "MurilloSkills: progresso resetado.");
    }

    public int stats(Object source) {
        PlayerContext context = context(source);
        if (!context.identity.hasUuid()) {
            return fail(source, "MurilloSkills: jogador nao encontrado para stats.");
        }
        StringBuilder message = new StringBuilder("MurilloSkills");
        SkillType active = context.data.getActiveParagonSkill();
        if (active != null) {
            message.append(" paragon=").append(active.name().toLowerCase());
        }
        message.append(" selected=");
        List<String> selected = new ArrayList<String>();
        for (SkillType skill : context.data.getSelectedSkills()) {
            selected.add(skill.name().toLowerCase());
        }
        message.append(selected);
        for (SkillType skill : SkillType.values()) {
            SkillStatsCore stats = context.data.getSkill(skill);
            message.append(" | ").append(skill.name().toLowerCase())
                    .append(" L").append(stats.getLevel())
                    .append(" P").append(stats.getPrestige());
        }
        return say(source, message.toString());
    }

    public void awardEventXp(Object event, SkillType skill, int amount) {
        PlayerIdentity identity = ReflectionSupport.findPlayerIdentity(event);
        if (!identity.hasUuid()) {
            return;
        }
        PlayerSkillDataCore data = playerStore.load(identity.getUuid());
        data.addXpToSkill(skill, amount, progressionConfig);
        playerStore.save(identity.getUuid());
    }

    public void registerCommands(Object dispatcher) {
        CommandBridge.register(dispatcher, this);
    }

    public void log(String message) {
        System.out.println("[MurilloSkills][" + loader + " " + minecraftVersion + "] " + message);
    }

    private PlayerContext context(Object source) {
        PlayerIdentity identity = ReflectionSupport.findPlayerIdentity(source);
        PlayerSkillDataCore data = identity.hasUuid()
                ? playerStore.load(identity.getUuid())
                : new PlayerSkillDataCore();
        return new PlayerContext(identity, data);
    }

    private int say(Object source, String message) {
        ReflectionSupport.sendMessage(source, message);
        return 1;
    }

    private int fail(Object source, String message) {
        ReflectionSupport.sendMessage(source, message);
        return 0;
    }

    private List<SkillType> singleton(SkillType skill) {
        List<SkillType> values = new ArrayList<SkillType>();
        values.add(skill);
        return values;
    }

    private static Path defaultPlayerDirectory() {
        String configured = System.getProperty("murilloskills.dataDir");
        if (configured != null && configured.trim().length() > 0) {
            return Paths.get(configured).resolve("players");
        }
        return Paths.get(System.getProperty("user.dir", ".")).resolve("murilloskills").resolve("players");
    }

    private static final class PlayerContext {
        private final PlayerIdentity identity;
        private final PlayerSkillDataCore data;

        private PlayerContext(PlayerIdentity identity, PlayerSkillDataCore data) {
            this.identity = identity;
            this.data = data;
        }
    }
}
