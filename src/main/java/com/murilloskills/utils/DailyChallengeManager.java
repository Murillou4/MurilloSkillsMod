package com.murilloskills.utils;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Sistema de Desafios Diários.
 * Cada dia gera novos desafios para os jogadores completarem.
 * Completar desafios concede bônus de XP.
 * 
 * Tipos de desafios:
 * - Mine X blocks
 * - Kill X mobs
 * - Harvest X crops
 * - Catch X fish
 * - Place X blocks
 * - Travel X blocks
 * - Use ability X times
 */
public class DailyChallengeManager {

    // Configurações
    public static final int CHALLENGES_PER_DAY = 3;
    public static final int BASE_XP_REWARD = 500;
    public static final int BONUS_XP_ALL_COMPLETE = 1000;

    // Dados de desafios por jogador - mapa de UUID para dados do player
    private static final Map<UUID, PlayerChallengeData> playerChallenges = new HashMap<>();

    /**
     * Obtém ou gera desafios diários para um jogador.
     */
    public static List<DailyChallenge> getDailyChallenges(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        String today = getCurrentDateKey();

        PlayerChallengeData data = playerChallenges.get(playerId);

        // Verificar se precisa gerar novos desafios (novo dia)
        if (data == null || !data.dateKey.equals(today)) {
            data = generateNewChallenges(player, today);
            playerChallenges.put(playerId, data);
        }

        return data.challenges;
    }

    /**
     * Registra progresso em um tipo de desafio.
     * Chamado pelos handlers quando ações são realizadas.
     */
    public static void recordProgress(ServerPlayerEntity player, ChallengeType type, int amount) {
        List<DailyChallenge> challenges = getDailyChallenges(player);

        for (DailyChallenge challenge : challenges) {
            if (challenge.type == type && !challenge.completed) {
                challenge.currentProgress += amount;

                // Verificar se completou
                if (challenge.currentProgress >= challenge.targetAmount) {
                    challenge.completed = true;
                    challenge.currentProgress = challenge.targetAmount;
                    onChallengeComplete(player, challenge);
                }
            }
        }
    }

    /**
     * Verifica se todos os desafios diários foram completados.
     */
    public static boolean allChallengesComplete(ServerPlayerEntity player) {
        List<DailyChallenge> challenges = getDailyChallenges(player);
        return challenges.stream().allMatch(c -> c.completed);
    }

    /**
     * Retorna quantos desafios foram completados hoje.
     */
    public static int getCompletedCount(ServerPlayerEntity player) {
        List<DailyChallenge> challenges = getDailyChallenges(player);
        return (int) challenges.stream().filter(c -> c.completed).count();
    }

    /**
     * Forces regeneration of daily challenges for a player.
     * Call this when skills are selected/changed to ensure challenges match
     * selected skills.
     */
    public static void forceRegenerate(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        playerChallenges.remove(playerId);
        // Next call to getDailyChallenges will generate new challenges
        getDailyChallenges(player);
        syncChallenges(player);
    }

    // ============ MÉTODOS PRIVADOS ============

    private static PlayerChallengeData generateNewChallenges(ServerPlayerEntity player, String dateKey) {
        UUID playerId = player.getUuid();
        Random random = new Random(playerId.hashCode() + dateKey.hashCode());
        List<DailyChallenge> challenges = new ArrayList<>();

        // Get player's selected skills to filter challenges
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        SkillGlobalState.PlayerSkillData playerData = state.getPlayerData(player);
        List<MurilloSkillsList> selectedSkills = playerData.getSelectedSkills();

        // Filter available challenge types based on selected skills
        List<ChallengeType> availableTypes = new ArrayList<>();
        for (ChallengeType type : ChallengeType.values()) {
            MurilloSkillsList skill = getSkillForType(type);
            // Include if: skill is null (general challenge) OR skill is in selected skills
            if (skill == null || selectedSkills.contains(skill)) {
                availableTypes.add(type);
            }
        }

        // If no skills selected yet, use a subset of general challenges
        if (availableTypes.isEmpty()) {
            availableTypes.add(ChallengeType.EAT_FOOD);
            availableTypes.add(ChallengeType.SLEEP_NIGHTS);
        }

        for (int i = 0; i < CHALLENGES_PER_DAY && !availableTypes.isEmpty(); i++) {
            int index = random.nextInt(availableTypes.size());
            ChallengeType type = availableTypes.remove(index);

            int target = getTargetForType(type, random);
            MurilloSkillsList skill = getSkillForType(type);

            challenges.add(new DailyChallenge(type, target, skill));
        }

        return new PlayerChallengeData(dateKey, challenges, false);
    }

    private static int getTargetForType(ChallengeType type, Random random) {
        return switch (type) {
            // Miner challenges
            case MINE_BLOCKS -> 50 + random.nextInt(51); // 50-100
            case MINE_ORES -> 15 + random.nextInt(16); // 15-30
            case MINE_DEEPSLATE -> 30 + random.nextInt(31); // 30-60
            case FIND_DIAMONDS -> 3 + random.nextInt(5); // 3-7
            // Warrior challenges
            case KILL_MOBS -> 15 + random.nextInt(16); // 15-30
            case KILL_UNDEAD -> 10 + random.nextInt(11); // 10-20
            case KILL_SPIDERS -> 8 + random.nextInt(8); // 8-15
            case TAKE_DAMAGE -> 50 + random.nextInt(51); // 50-100 hearts
            case DEAL_DAMAGE -> 100 + random.nextInt(101); // 100-200 damage
            // Farmer challenges
            case HARVEST_CROPS -> 30 + random.nextInt(31); // 30-60
            case PLANT_SEEDS -> 40 + random.nextInt(41); // 40-80
            case BREED_ANIMALS -> 5 + random.nextInt(6); // 5-10
            case SHEAR_SHEEP -> 10 + random.nextInt(11); // 10-20
            // Archer challenges
            case ARROW_HITS -> 20 + random.nextInt(21); // 20-40
            case LONG_SHOTS -> 5 + random.nextInt(6); // 5-10 (>30 blocks)
            case HEADSHOTS -> 3 + random.nextInt(4); // 3-6
            // Fisher challenges
            case CATCH_FISH -> 5 + random.nextInt(11); // 5-15
            case CATCH_TREASURE -> 2 + random.nextInt(3); // 2-4
            case CATCH_JUNK -> 5 + random.nextInt(6); // 5-10
            // Builder challenges
            case PLACE_BLOCKS -> 100 + random.nextInt(101); // 100-200
            case BUILD_HEIGHT -> 20 + random.nextInt(21); // Y > 100+
            case PLACE_STAIRS -> 15 + random.nextInt(16); // 15-30
            // Explorer challenges
            case TRAVEL_BLOCKS -> 500 + random.nextInt(501); // 500-1000
            case DISCOVER_BIOMES -> 2 + random.nextInt(2); // 2-3
            case OPEN_CHESTS -> 5 + random.nextInt(6); // 5-10
            case ENTER_STRUCTURES -> 1 + random.nextInt(2); // 1-2
            // Blacksmith challenges
            case CRAFT_ITEMS -> 20 + random.nextInt(21); // 20-40
            case SMELT_ITEMS -> 30 + random.nextInt(31); // 30-60
            case ENCHANT_ITEMS -> 2 + random.nextInt(3); // 2-4
            case REPAIR_ITEMS -> 3 + random.nextInt(3); // 3-5
            // General challenges
            case USE_ABILITY -> 1 + random.nextInt(3); // 1-3
            case EAT_FOOD -> 10 + random.nextInt(11); // 10-20
            case SLEEP_NIGHTS -> 1 + random.nextInt(2); // 1-2
        };
    }

    private static MurilloSkillsList getSkillForType(ChallengeType type) {
        return switch (type) {
            case MINE_BLOCKS, MINE_ORES, MINE_DEEPSLATE, FIND_DIAMONDS -> MurilloSkillsList.MINER;
            case KILL_MOBS, KILL_UNDEAD, KILL_SPIDERS, TAKE_DAMAGE, DEAL_DAMAGE -> MurilloSkillsList.WARRIOR;
            case HARVEST_CROPS, PLANT_SEEDS, BREED_ANIMALS, SHEAR_SHEEP -> MurilloSkillsList.FARMER;
            case ARROW_HITS, LONG_SHOTS, HEADSHOTS -> MurilloSkillsList.ARCHER;
            case CATCH_FISH, CATCH_TREASURE, CATCH_JUNK -> MurilloSkillsList.FISHER;
            case PLACE_BLOCKS, BUILD_HEIGHT, PLACE_STAIRS -> MurilloSkillsList.BUILDER;
            case TRAVEL_BLOCKS, DISCOVER_BIOMES, OPEN_CHESTS, ENTER_STRUCTURES -> MurilloSkillsList.EXPLORER;
            case CRAFT_ITEMS, SMELT_ITEMS, ENCHANT_ITEMS, REPAIR_ITEMS -> MurilloSkillsList.BLACKSMITH;
            case USE_ABILITY, EAT_FOOD, SLEEP_NIGHTS -> null;
        };
    }

    private static void onChallengeComplete(ServerPlayerEntity player, DailyChallenge challenge) {
        // Dar XP bônus na skill relacionada (se houver)
        if (challenge.relatedSkill != null) {
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            SkillGlobalState.PlayerSkillData data = state.getPlayerData(player);

            if (data.isSkillSelected(challenge.relatedSkill)) {
                data.addXpToSkill(challenge.relatedSkill, BASE_XP_REWARD);
                state.markDirty();
                SkillsNetworkUtils.syncSkills(player);
            }
        }

        // Notificar jogador
        Text message = Text.empty()
                .append(Text.literal("🎯 ").formatted(Formatting.GREEN))
                .append(Text.translatable("murilloskills.challenge.complete").formatted(Formatting.GREEN,
                        Formatting.BOLD))
                .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                .append(Text.translatable("murilloskills.challenge." + challenge.type.name().toLowerCase()))
                .append(Text.literal(" +" + BASE_XP_REWARD + " XP").formatted(Formatting.YELLOW));

        player.sendMessage(message, false);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.2f);

        // Verificar se completou todos os desafios
        if (allChallengesComplete(player)) {
            PlayerChallengeData data = playerChallenges.get(player.getUuid());
            if (data != null && !data.bonusAwarded) {
                data.bonusAwarded = true;
                awardAllCompleteBonus(player);
            }
        }
    }

    private static void awardAllCompleteBonus(ServerPlayerEntity player) {
        // Dar bônus extra por completar todos os desafios
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        SkillGlobalState.PlayerSkillData data = state.getPlayerData(player);

        // Distribuir XP entre todas as skills selecionadas
        List<MurilloSkillsList> selected = data.getSelectedSkills();
        if (!selected.isEmpty()) {
            int xpPerSkill = BONUS_XP_ALL_COMPLETE / selected.size();
            for (MurilloSkillsList skill : selected) {
                data.addXpToSkill(skill, xpPerSkill);
            }
            state.markDirty();
            SkillsNetworkUtils.syncSkills(player);
        }

        Text message = Text.empty()
                .append(Text.literal("🏆 ").formatted(Formatting.GOLD))
                .append(Text.translatable("murilloskills.challenge.all_complete").formatted(Formatting.GOLD,
                        Formatting.BOLD))
                .append(Text.literal(" +" + BONUS_XP_ALL_COMPLETE + " XP").formatted(Formatting.YELLOW));

        player.sendMessage(message, false);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Syncs daily challenges to client.
     * Call on login and after challenge updates.
     */
    public static void syncChallenges(ServerPlayerEntity player) {
        List<DailyChallenge> challenges = getDailyChallenges(player);

        List<com.murilloskills.network.DailyChallengesSyncS2CPayload.ChallengeData> clientData = challenges.stream()
                .map(c -> new com.murilloskills.network.DailyChallengesSyncS2CPayload.ChallengeData(
                        c.type.name(),
                        c.relatedSkill != null ? c.relatedSkill.name() : "",
                        c.targetAmount,
                        c.currentProgress,
                        c.completed,
                        BASE_XP_REWARD))
                .toList();

        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                new com.murilloskills.network.DailyChallengesSyncS2CPayload(
                        clientData, getCurrentDateKey(), allChallengesComplete(player)));
    }

    private static String getCurrentDateKey() {
        return LocalDate.now(ZoneId.systemDefault()).toString();
    }

    // ============ CLASSES INTERNAS ============

    public static class DailyChallenge {
        public final ChallengeType type;
        public final int targetAmount;
        public final MurilloSkillsList relatedSkill;
        public int currentProgress;
        public boolean completed;

        public DailyChallenge(ChallengeType type, int targetAmount, MurilloSkillsList relatedSkill) {
            this.type = type;
            this.targetAmount = targetAmount;
            this.relatedSkill = relatedSkill;
            this.currentProgress = 0;
            this.completed = false;
        }

        public float getProgressPercentage() {
            return (float) currentProgress / targetAmount;
        }
    }

    private static class PlayerChallengeData {
        final String dateKey;
        final List<DailyChallenge> challenges;
        boolean bonusAwarded;

        PlayerChallengeData(String dateKey, List<DailyChallenge> challenges, boolean bonusAwarded) {
            this.dateKey = dateKey;
            this.challenges = challenges;
            this.bonusAwarded = bonusAwarded;
        }
    }

    public enum ChallengeType {
        // Miner
        MINE_BLOCKS, MINE_ORES, MINE_DEEPSLATE, FIND_DIAMONDS,
        // Warrior
        KILL_MOBS, KILL_UNDEAD, KILL_SPIDERS, TAKE_DAMAGE, DEAL_DAMAGE,
        // Farmer
        HARVEST_CROPS, PLANT_SEEDS, BREED_ANIMALS, SHEAR_SHEEP,
        // Archer
        ARROW_HITS, LONG_SHOTS, HEADSHOTS,
        // Fisher
        CATCH_FISH, CATCH_TREASURE, CATCH_JUNK,
        // Builder
        PLACE_BLOCKS, BUILD_HEIGHT, PLACE_STAIRS,
        // Explorer
        TRAVEL_BLOCKS, DISCOVER_BIOMES, OPEN_CHESTS, ENTER_STRUCTURES,
        // Blacksmith
        CRAFT_ITEMS, SMELT_ITEMS, ENCHANT_ITEMS, REPAIR_ITEMS,
        // General
        USE_ABILITY, EAT_FOOD, SLEEP_NIGHTS
    }
}
