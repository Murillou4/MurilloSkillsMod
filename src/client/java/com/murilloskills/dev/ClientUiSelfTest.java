package com.murilloskills.dev;

import com.murilloskills.MurilloSkillsClient;
import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.gui.ConfirmationScreen;
import com.murilloskills.gui.ModInfoScreen;
import com.murilloskills.gui.OreFilterScreen;
import com.murilloskills.gui.ParagonAbilityScreen;
import com.murilloskills.gui.SkillsScreen;
import com.murilloskills.gui.StorageWhitelistPickerScreen;
import com.murilloskills.gui.TerminalBulkCraftAmountScreen;
import com.murilloskills.gui.TerminalMachineTransferAmountScreen;
import com.murilloskills.gui.TrashItemPickerScreen;
import com.murilloskills.gui.UltPlaceConfigScreen;
import com.murilloskills.gui.UltmineClassicBlockPickerScreen;
import com.murilloskills.gui.UltmineConfigScreen;
import com.murilloskills.gui.UltmineRadialMenuScreen;
import com.murilloskills.gui.data.SkillUiData;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientUiSelfTest {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Path LOG_PATH = Path.of("logs", "murilloskills-ui-selftest.log");
    private static int tickDelay = 240;
    private static int screenIndex = -1;
    private static int screenTicks = 0;
    private static int passiveWaitTicks = 0;
    private static boolean metadataChecked = false;
    private static boolean passiveChecked = false;
    private static int failures = 0;
    private static final StringBuilder LOG = new StringBuilder();
    private static final int MAX_PASSIVE_WAIT_TICKS = 240;

    private static final ScreenCase[] SCREENS = new ScreenCase[] {
            new ScreenCase("skills", SkillsScreen::new),
            new ScreenCase("guide", () -> new ModInfoScreen(new SkillsScreen())),
            new ScreenCase("ore_filter", () -> new OreFilterScreen(new SkillsScreen())),
            new ScreenCase("paragon_ability", ParagonAbilityScreen::new),
            new ScreenCase("ultmine_config", () -> new UltmineConfigScreen(new SkillsScreen())),
            new ScreenCase("ultmine_radial", UltmineRadialMenuScreen::new),
            new ScreenCase("ultmine_classic_blocks", () -> new UltmineClassicBlockPickerScreen(new SkillsScreen())),
            new ScreenCase("ultplace_config", () -> new UltPlaceConfigScreen(new SkillsScreen())),
            new ScreenCase("storage_whitelist", () -> new StorageWhitelistPickerScreen(new SkillsScreen())),
            new ScreenCase("trash_picker", () -> new TrashItemPickerScreen(new SkillsScreen())),
            new ScreenCase("terminal_machine_amount",
                    () -> new TerminalMachineTransferAmountScreen(new SkillsScreen(), new ItemStack(Items.STONE), 64)),
            new ScreenCase("terminal_bulk_amount",
                    () -> new TerminalBulkCraftAmountScreen(new SkillsScreen(), new ItemStack(Items.STONE))),
            new ScreenCase("confirmation",
                    () -> new ConfirmationScreen(new SkillsScreen(), Text.translatable("murilloskills.gui.title"),
                            Text.translatable("murilloskills.gui.description"), () -> {
                            }))
    };

    private ClientUiSelfTest() {
    }

    public static void register() {
        if (!isEnabled() || !REGISTERED.compareAndSet(false, true)) {
            return;
        }

        line("START screen_count=" + SCREENS.length);
        ClientTickEvents.END_CLIENT_TICK.register(ClientUiSelfTest::tick);
    }

    private static boolean isEnabled() {
        return "1".equals(System.getenv("MURILLOSKILLS_SELFTEST"))
                || "true".equalsIgnoreCase(System.getenv("MURILLOSKILLS_SELFTEST"))
                || Boolean.getBoolean("murilloskills.selftest");
    }

    private static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        if (!metadataChecked) {
            validateSkillMetadata();
            metadataChecked = true;
        }

        if (screenIndex >= 0) {
            screenTicks++;
            if (screenTicks < 8) {
                return;
            }
        }

        screenIndex++;
        screenTicks = 0;
        if (screenIndex >= SCREENS.length) {
            if (!passiveChecked && !validateBuilderReach(client)) {
                return;
            }
            client.setScreen(null);
            line(failures == 0 ? "PASS" : "FAIL failures=" + failures);
            flush();
            tickDelay = Integer.MAX_VALUE;
            return;
        }

        ScreenCase screenCase = SCREENS[screenIndex];
        try {
            Screen screen = screenCase.factory().create();
            client.setScreen(screen);
            line("PASS open." + screenCase.name());
        } catch (Throwable t) {
            failures++;
            line("FAIL open." + screenCase.name() + " " + t.getClass().getName() + ": " + t.getMessage());
            client.setScreen(null);
        }
    }

    private static boolean validateBuilderReach(MinecraftClient client) {
        if (!ClientSkillData.isSkillSelected(MurilloSkillsList.BUILDER)
                || ClientSkillData.get(MurilloSkillsList.BUILDER).level < 100) {
            passiveWaitTicks++;
            if (passiveWaitTicks < MAX_PASSIVE_WAIT_TICKS) {
                screenIndex = SCREENS.length - 1;
                return false;
            }
            failures++;
            passiveChecked = true;
            line("FAIL passive.BUILDER.client_reach missing_synced_builder_data");
            return true;
        }

        PlayerSkillData.SkillStats stats = ClientSkillData.get(MurilloSkillsList.BUILDER);
        double bonus = BuilderSkill.getReachBonus(stats.level, stats.prestige);
        double reach = client.player.getBlockInteractionRange();
        if (reach >= 4.5D + bonus) {
            line("PASS passive.BUILDER.client_reach value=" + reach + " bonus=" + bonus);
        } else {
            failures++;
            line("FAIL passive.BUILDER.client_reach value=" + reach + " bonus=" + bonus);
        }
        passiveChecked = true;
        return true;
    }

    private static void validateSkillMetadata() {
        check("keybinds.no_default_conflicts", !MurilloSkillsClient.hasDefaultKeybindConflictsForSelfTest());
        line("DETAIL keybinds " + MurilloSkillsClient.getDefaultKeybindSummaryForSelfTest());

        for (MurilloSkillsList skill : MurilloSkillsList.values()) {
            List<SkillUiData.PerkInfo> perks = SkillUiData.SKILL_PERKS.get(skill);
            check("ui." + skill.name() + ".perks.present", perks != null && !perks.isEmpty());
            if (perks != null && !perks.isEmpty()) {
                check("ui." + skill.name() + ".perks.master",
                        perks.stream().anyMatch(perk -> perk.level() == SkillConfig.getMaxLevel()));
                check("ui." + skill.name() + ".perks.bounds",
                        perks.stream().allMatch(perk -> perk.level() > 0 && perk.level() <= SkillConfig.getMaxLevel()
                                && perk.nameKey() != null && !perk.nameKey().isBlank()
                                && perk.descKey() != null && !perk.descKey().isBlank()));
                line("DETAIL ui." + skill.name() + ".perks count=" + perks.size());
            }

            List<SkillUiData.XpSourceInfo> xpSources = SkillUiData.getDetailedXpSources(skill);
            check("ui." + skill.name() + ".xp_sources.present", xpSources != null && !xpSources.isEmpty());

            List<SkillUiData.GuideEntry> guide = SkillUiData.getGuideTimeline(skill);
            check("ui." + skill.name() + ".guide.full_timeline",
                    guide != null && guide.size() >= SkillConfig.getMaxLevel()
                            && guide.stream().anyMatch(entry -> entry.level() == SkillConfig.getMaxLevel()));
            check("ui." + skill.name() + ".guide.has_milestones",
                    guide != null && guide.stream().anyMatch(SkillUiData.GuideEntry::milestone));

            List<Text> passives = SkillUiData.getMaxPassiveGuide(skill);
            check("ui." + skill.name() + ".passives.present", passives != null && !passives.isEmpty());

            String masterDetails = SkillUiData.getMasterAbilityDetails(skill);
            check("ui." + skill.name() + ".master_details.present",
                    masterDetails != null && !masterDetails.isBlank() && masterDetails.contains("Cooldown"));

            line("DETAIL ui." + skill.name() + " xpSources=" + (xpSources == null ? 0 : xpSources.size())
                    + " guide=" + (guide == null ? 0 : guide.size())
                    + " passives=" + (passives == null ? 0 : passives.size()));
        }

        Set<MurilloSkillsList> synergySkills = new HashSet<>();
        check("ui.synergy.list.present", !SkillUiData.SYNERGIES.isEmpty());
        for (SkillUiData.SynergyInfo synergy : SkillUiData.SYNERGIES) {
            check("ui.synergy." + synergy.id() + ".valid",
                    synergy.id() != null && !synergy.id().isBlank()
                            && synergy.skill1() != null
                            && synergy.skill2() != null
                            && synergy.skill1() != synergy.skill2()
                            && synergy.typeKey() != null && !synergy.typeKey().isBlank()
                            && synergy.bonus() >= 0);
            synergySkills.add(synergy.skill1());
            synergySkills.add(synergy.skill2());
        }
        check("ui.synergy.all_skills_covered", synergySkills.containsAll(List.of(MurilloSkillsList.values())));
    }

    private static void check(String name, boolean passed) {
        if (passed) {
            line("PASS " + name);
        } else {
            failures++;
            line("FAIL " + name);
        }
    }

    private static void line(String text) {
        LOG.append(text).append(System.lineSeparator());
    }

    private static void flush() {
        try {
            Files.createDirectories(LOG_PATH.getParent());
            Files.writeString(LOG_PATH, LOG.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private record ScreenCase(String name, ScreenFactory factory) {
    }

    @FunctionalInterface
    private interface ScreenFactory {
        Screen create();
    }
}
