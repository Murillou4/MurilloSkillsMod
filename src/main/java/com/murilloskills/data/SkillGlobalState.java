package com.murilloskills.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murilloskills.MurilloSkills;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SkillGlobalState extends PersistentState {
    public static final Logger LOGGER = LoggerFactory.getLogger(MurilloSkills.MOD_ID);

    public SkillGlobalState() {}

    public HashMap<UUID, PlayerSkillData> players = new HashMap<>();

    public static final Codec<SkillGlobalState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, PlayerSkillData.CODEC).fieldOf("players").forGetter(state -> {
                Map<String, PlayerSkillData> map = new HashMap<>();
                state.players.forEach((uuid, data) -> map.put(uuid.toString(), data));
                return map;
            })).apply(instance, SkillGlobalState::newFromMap));

    public SkillGlobalState(Context context) {}

    private static SkillGlobalState newFromMap(Map<String, PlayerSkillData> map) {
        SkillGlobalState state = new SkillGlobalState();
        map.forEach((key, value) -> {
            try {
                state.players.put(UUID.fromString(key), value);
            } catch (IllegalArgumentException e) {
                LOGGER.error("UUID inválido encontrado no save:", key);
            }
        });
        return state;
    }

    // --- SAVE/LOAD NBT ---
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound playersNbt = new NbtCompound();
        players.forEach((uuid, playerData) -> {
            NbtCompound playerCompound = new NbtCompound();

            // Save Paragon Skill
            if (playerData.paragonSkill != null) {
                playerCompound.putString("paragonSkill", playerData.paragonSkill.name());
            }

            playerData.skills.forEach((skill, stats) -> {
                NbtCompound skillCompound = new NbtCompound();
                skillCompound.putInt("level", stats.level);
                skillCompound.putDouble("xp", stats.xp);
                skillCompound.putLong("lastAbilityUse", stats.lastAbilityUse); // Novo campo
                playerCompound.put(skill.name(), skillCompound);
            });
            playersNbt.put(uuid.toString(), playerCompound);
        });
        nbt.put("players", playersNbt);
        return nbt;
    }

    public static SkillGlobalState createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        SkillGlobalState state = new SkillGlobalState();
        NbtCompound playersNbt = tag.getCompoundOrEmpty("players");

        playersNbt.getKeys().forEach(key -> {
            UUID uuid = UUID.fromString(key);
            PlayerSkillData playerData = new PlayerSkillData();
            NbtCompound playerCompound = playersNbt.getCompoundOrEmpty(key);

            // Load Paragon Skill
            if (playerCompound.contains("paragonSkill")) {
                try {
                    playerData.paragonSkill = MurilloSkillsList.valueOf(String.valueOf(playerCompound.getString("paragonSkill")));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Unknown paragon skill in NBT");
                }
            }

            for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                if (playerCompound.contains(skill.name())) {
                    NbtCompound skillCompound = playerCompound.getCompoundOrEmpty(skill.name());
                    Optional<Integer> lvl = skillCompound.getInt("level");
                    Optional<Double> xp = skillCompound.getDouble("xp");
                    Optional<Long> lastUse = skillCompound.getLong("lastAbilityUse");

                    if(lvl.isPresent() && xp.isPresent() && lastUse.isPresent()){
                        playerData.setSkill(skill, lvl.get(), xp.get(), lastUse.get());
                    }
                    else{
                        LOGGER.error("skill values not present");
                    }
                }
            }
            state.players.put(uuid, playerData);
        });
        return state;
    }

    private static final PersistentStateType<SkillGlobalState> TYPE = new PersistentStateType<>(
            MurilloSkills.MOD_ID, SkillGlobalState::new, ctx -> SkillGlobalState.CODEC, null);

    public static SkillGlobalState getServerState(MinecraftServer server) {
        ServerWorld serverWorld = server.getWorld(World.OVERWORLD);
        if (serverWorld == null) throw new IllegalStateException("Overworld not found");
        SkillGlobalState state = serverWorld.getPersistentStateManager().getOrCreate(TYPE);
        state.markDirty();
        return state;
    }

    public PlayerSkillData getPlayerData(ServerPlayerEntity player) {
        return players.computeIfAbsent(player.getUuid(), uuid -> new PlayerSkillData());
    }

    public static class PlayerSkillData {
        public EnumMap<MurilloSkillsList, SkillStats> skills = new EnumMap<>(MurilloSkillsList.class);
        public MurilloSkillsList paragonSkill = null; // New Field: The one skill allowed to reach 100

        public PlayerSkillData() {
            for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                skills.put(skill, new SkillStats(0, 0.0, 0));
            }
        }

        public PlayerSkillData(MurilloSkillsList paragonSkill, EnumMap<MurilloSkillsList, SkillStats> skills) {
            this.paragonSkill = paragonSkill;
            this.skills = skills;
        }

        /**
         * Central logic for adding XP and handling Paragon System constraints.
         * @return true if leveled up
         */
// >>> ESTE MÉTODO FICA AQUI (Dentro de PlayerSkillData), não dentro de SkillStats <<<
        public boolean addXpToSkill(MurilloSkillsList skill, int amount) {
            SkillStats stats = skills.get(skill); // Agora ele consegue achar a variável 'skills'

            int maxLevelAllowed = 99;
            // Se já for Paragon OU se não houver Paragon, permite ir até 100
            if (paragonSkill == null || paragonSkill == skill) {
                maxLevelAllowed = 100;
            }

            return stats.addXp(amount, maxLevelAllowed);
        }

        public void setSkill(MurilloSkillsList skill, int level, double xp) {
            setSkill(skill, level, xp, 0);
        }

        public void setSkill(MurilloSkillsList skill, int level, double xp, long lastAbilityUse) {
            skills.put(skill, new SkillStats(level, xp, lastAbilityUse));
        }

        public SkillStats getSkill(MurilloSkillsList skill) {
            return skills.get(skill);
        }

        // Updated Codec to include paragonSkill
        public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("paragonSkill").forGetter(d ->
                        Optional.ofNullable(d.paragonSkill).map(Enum::name)),
                Codec.unboundedMap(Codec.STRING, SkillStats.CODEC).fieldOf("skills").forGetter(d -> {
                    Map<String, SkillStats> map = new HashMap<>();
                    d.skills.forEach((k, v) -> map.put(k.name(), v));
                    return map;
                })
        ).apply(instance, (paragonOpt, skillsMap) -> {
            MurilloSkillsList p = paragonOpt.map(MurilloSkillsList::valueOf).orElse(null);
            EnumMap<MurilloSkillsList, SkillStats> map = new EnumMap<>(MurilloSkillsList.class);
            skillsMap.forEach((k, v) -> {
                try { map.put(MurilloSkillsList.valueOf(k), v); } catch (Exception ignored) {}
            });
            // Ensure all enums are present
            for (MurilloSkillsList s : MurilloSkillsList.values()) {
                map.putIfAbsent(s, new SkillStats(0, 0, 0));
            }
            return new PlayerSkillData(p, map);
        }));
    }

    public static class SkillStats {
        public int level;
        public double xp;
        public long lastAbilityUse; // Timestamp do último uso da habilidade (World Time)

        public SkillStats(int level, double xp) {
            this(level, xp, 0);
        }

        public SkillStats(int level, double xp, long lastAbilityUse) {
            this.level = level;
            this.xp = xp;
            this.lastAbilityUse = lastAbilityUse;
        }

        public static final Codec<SkillStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("level").forGetter(s -> s.level),
                Codec.DOUBLE.fieldOf("xp").forGetter(s -> s.xp),
                Codec.LONG.optionalFieldOf("lastAbilityUse", 0L).forGetter(s -> s.lastAbilityUse)
        ).apply(instance, SkillStats::new));

        // Logic moved to respect dynamic cap
        public boolean addXp(int amount, int maxLevelAllowed) {
            if (this.level >= maxLevelAllowed) return false;

            this.xp += amount;
            boolean leveledUp = false;

            while (this.xp >= getXpNeededForNextLevel() && this.level < maxLevelAllowed) {
                this.xp -= getXpNeededForNextLevel();
                this.level++;
                leveledUp = true;
            }

            // Cap XP if max level reached
            if (this.level >= maxLevelAllowed) {
                this.xp = 0; // Or keep it as "Maxed"
            }

            return leveledUp;
        }

        private int getXpNeededForNextLevel() {
            return 50 + (this.level * 10) + (4 * this.level * this.level);
        }
    }
}