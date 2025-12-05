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

    public SkillGlobalState() {
    }

    public HashMap<UUID, PlayerSkillData> players = new HashMap<>();

    public static final Codec<SkillGlobalState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, PlayerSkillData.CODEC).fieldOf("players").forGetter(state -> {
                Map<String, PlayerSkillData> map = new HashMap<>();
                state.players.forEach((uuid, data) -> map.put(uuid.toString(), data));
                return map;
            })).apply(instance, SkillGlobalState::newFromMap));

    public SkillGlobalState(Context context) {
    }

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

            // Save Selected Skills
            if (playerData.selectedSkills != null && !playerData.selectedSkills.isEmpty()) {
                NbtCompound selectedNbt = new NbtCompound();
                for (int i = 0; i < playerData.selectedSkills.size(); i++) {
                    selectedNbt.putString("skill" + i, playerData.selectedSkills.get(i).name());
                }
                selectedNbt.putInt("count", playerData.selectedSkills.size());
                playerCompound.put("selectedSkills", selectedNbt);
            }

            playerData.skills.forEach((skill, stats) -> {
                NbtCompound skillCompound = new NbtCompound();
                skillCompound.putInt("level", stats.level);
                skillCompound.putDouble("xp", stats.xp);
                skillCompound.putLong("lastAbilityUse", stats.lastAbilityUse);
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
                    playerData.paragonSkill = MurilloSkillsList
                            .valueOf(String.valueOf(playerCompound.getString("paragonSkill")));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Unknown paragon skill in NBT");
                }
            }

            // Load Selected Skills
            if (playerCompound.contains("selectedSkills")) {
                NbtCompound selectedNbt = playerCompound.getCompoundOrEmpty("selectedSkills");
                Optional<Integer> countOpt = selectedNbt.getInt("count");
                int count = countOpt.orElse(0);
                for (int i = 0; i < count; i++) {
                    Optional<String> skillName = selectedNbt.getString("skill" + i);
                    if (skillName.isPresent()) {
                        try {
                            playerData.selectedSkills.add(MurilloSkillsList.valueOf(skillName.get()));
                        } catch (IllegalArgumentException e) {
                            LOGGER.error("Unknown selected skill in NBT: " + skillName);
                        }
                    }
                }
            }

            for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                if (playerCompound.contains(skill.name())) {
                    NbtCompound skillCompound = playerCompound.getCompoundOrEmpty(skill.name());
                    Optional<Integer> lvl = skillCompound.getInt("level");
                    Optional<Double> xp = skillCompound.getDouble("xp");
                    Optional<Long> lastUse = skillCompound.getLong("lastAbilityUse");

                    if (lvl.isPresent() && xp.isPresent() && lastUse.isPresent()) {
                        playerData.setSkill(skill, lvl.get(), xp.get(), lastUse.get());
                    } else {
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
        if (serverWorld == null)
            throw new IllegalStateException("Overworld not found");
        SkillGlobalState state = serverWorld.getPersistentStateManager().getOrCreate(TYPE);
        state.markDirty();
        return state;
    }

    public PlayerSkillData getPlayerData(ServerPlayerEntity player) {
        return players.computeIfAbsent(player.getUuid(), uuid -> new PlayerSkillData());
    }

    public static class PlayerSkillData {
        public static final int MAX_SELECTED_SKILLS = 2;

        public EnumMap<MurilloSkillsList, SkillStats> skills = new EnumMap<>(MurilloSkillsList.class);
        public MurilloSkillsList paragonSkill = null; // The one skill allowed to reach 100
        public List<MurilloSkillsList> selectedSkills = new ArrayList<>(); // The 2 main skills chosen by player

        public PlayerSkillData() {
            for (MurilloSkillsList skill : MurilloSkillsList.values()) {
                skills.put(skill, new SkillStats(0, 0.0, -1)); // -1 = nunca usou a habilidade
            }
        }

        public PlayerSkillData(MurilloSkillsList paragonSkill, EnumMap<MurilloSkillsList, SkillStats> skills,
                List<MurilloSkillsList> selectedSkills) {
            this.paragonSkill = paragonSkill;
            this.skills = skills;
            this.selectedSkills = selectedSkills != null ? new ArrayList<>(selectedSkills) : new ArrayList<>();
        }

        /**
         * Check if player has selected their main skills
         */
        public boolean hasSelectedSkills() {
            return selectedSkills != null && selectedSkills.size() == MAX_SELECTED_SKILLS;
        }

        /**
         * Check if a skill is one of the selected main skills
         */
        public boolean isSkillSelected(MurilloSkillsList skill) {
            return selectedSkills != null && selectedSkills.contains(skill);
        }

        /**
         * Set the selected skills (max 2). Returns true if successful.
         */
        public boolean setSelectedSkills(List<MurilloSkillsList> skills) {
            if (skills == null || skills.size() != MAX_SELECTED_SKILLS) {
                return false;
            }
            // Prevent changing if already selected
            if (hasSelectedSkills()) {
                return false;
            }
            this.selectedSkills = new ArrayList<>(skills);
            return true;
        }

        /**
         * Get the list of selected skills (unmodifiable)
         */
        public List<MurilloSkillsList> getSelectedSkills() {
            return Collections.unmodifiableList(selectedSkills);
        }

        /**
         * Central logic for adding XP and handling Paragon System constraints.
         * 
         * @return true if leveled up
         */
        public boolean addXpToSkill(MurilloSkillsList skill, int amount) {
            // RESTRICTION: If player hasn't selected skills yet, block all XP
            if (!hasSelectedSkills()) {
                return false;
            }

            // RESTRICTION: If skill is not in selectedSkills, block XP
            if (!isSkillSelected(skill)) {
                return false;
            }

            SkillStats stats = skills.get(skill);

            int maxLevelAllowed = 99;
            // Nível 100 só é permitido se paragonSkill estiver definido E for igual à skill
            // atual
            // Jogador deve travar no 99 até ir no menu e selecionar o Paragon
            if (paragonSkill != null && paragonSkill == skill) {
                maxLevelAllowed = 100;
            }

            return stats.addXp(amount, maxLevelAllowed);
        }

        public void setSkill(MurilloSkillsList skill, int level, double xp) {
            setSkill(skill, level, xp, -1); // -1 = nunca usou a habilidade
        }

        public void setSkill(MurilloSkillsList skill, int level, double xp, long lastAbilityUse) {
            skills.put(skill, new SkillStats(level, xp, lastAbilityUse));
        }

        public SkillStats getSkill(MurilloSkillsList skill) {
            return skills.get(skill);
        }

        // Updated Codec to include paragonSkill and selectedSkills
        public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("paragonSkill")
                        .forGetter(d -> Optional.ofNullable(d.paragonSkill).map(Enum::name)),
                Codec.unboundedMap(Codec.STRING, SkillStats.CODEC).fieldOf("skills").forGetter(d -> {
                    Map<String, SkillStats> map = new HashMap<>();
                    d.skills.forEach((k, v) -> map.put(k.name(), v));
                    return map;
                }),
                Codec.STRING.listOf().optionalFieldOf("selectedSkills", List.of())
                        .forGetter(d -> d.selectedSkills.stream().map(Enum::name).toList()))
                .apply(instance, (paragonOpt, skillsMap, selectedList) -> {
                    MurilloSkillsList p = paragonOpt.map(MurilloSkillsList::valueOf).orElse(null);
                    EnumMap<MurilloSkillsList, SkillStats> map = new EnumMap<>(MurilloSkillsList.class);
                    skillsMap.forEach((k, v) -> {
                        try {
                            map.put(MurilloSkillsList.valueOf(k), v);
                        } catch (Exception ignored) {
                        }
                    });
                    // Ensure all enums are present
                    for (MurilloSkillsList s : MurilloSkillsList.values()) {
                        map.putIfAbsent(s, new SkillStats(0, 0, -1)); // -1 = nunca usou a habilidade
                    }
                    // Parse selectedSkills
                    List<MurilloSkillsList> selected = new ArrayList<>();
                    for (String name : selectedList) {
                        try {
                            selected.add(MurilloSkillsList.valueOf(name));
                        } catch (Exception ignored) {
                        }
                    }
                    return new PlayerSkillData(p, map, selected);
                }));
    }

    public static class SkillStats {
        public int level;
        public double xp;
        public long lastAbilityUse; // Timestamp do último uso da habilidade (World Time)

        public SkillStats(int level, double xp) {
            this(level, xp, -1); // -1 = nunca usou a habilidade
        }

        public SkillStats(int level, double xp, long lastAbilityUse) {
            this.level = level;
            this.xp = xp;
            this.lastAbilityUse = lastAbilityUse;
        }

        public static final Codec<SkillStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("level").forGetter(s -> s.level),
                Codec.DOUBLE.fieldOf("xp").forGetter(s -> s.xp),
                Codec.LONG.optionalFieldOf("lastAbilityUse", 0L).forGetter(s -> s.lastAbilityUse))
                .apply(instance, SkillStats::new));

        // Logic moved to respect dynamic cap
        public boolean addXp(int amount, int maxLevelAllowed) {
            if (this.level >= maxLevelAllowed)
                return false;

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