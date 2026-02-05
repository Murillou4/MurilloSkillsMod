package com.murilloskills;

import com.murilloskills.network.AreaPlantingToggleC2SPayload;
import com.murilloskills.network.AreaPlantingSyncS2CPayload;
import com.murilloskills.network.HollowFillToggleC2SPayload;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.network.NightVisionToggleC2SPayload;
import com.murilloskills.network.StepAssistToggleC2SPayload;
import com.murilloskills.network.RainDanceS2CPayload;
import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.network.SkillsSyncPayload;
import com.murilloskills.network.TreasureHunterS2CPayload;
import com.murilloskills.network.VeinMinerDropsToggleC2SPayload;
import com.murilloskills.network.VeinMinerToggleC2SPayload;
import com.murilloskills.network.XpDataSyncS2CPayload;
import com.murilloskills.network.XpGainS2CPayload;
import com.murilloskills.render.AreaPlantingHud;
import com.murilloskills.render.OreHighlighter;
import com.murilloskills.render.RainDanceEffect;
import com.murilloskills.render.TreasureHighlighter;
import com.murilloskills.render.VeinMinerPreview;
import com.murilloskills.render.XpToastRenderer;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("deprecation") // HudRenderCallback is deprecated but still functional, migration to
                                 // HudElementRegistry pending
public class MurilloSkillsClient implements ClientModInitializer {

    // Custom keybinding category for mod - groups all keybinds together in Controls
    // menu
    private static final KeyBinding.Category KEYBIND_CATEGORY = KeyBinding.Category.create(
            Identifier.of("murilloskills", "keybinds"));

    private static KeyBinding skillsKey;
    private static KeyBinding abilityKey;
    private static KeyBinding areaPlantingToggleKey;
    private static KeyBinding hollowFillToggleKey;
    private static KeyBinding nightVisionToggleKey;
    private static KeyBinding stepAssistToggleKey;
    private static KeyBinding fillModeCycleKey;
    private static KeyBinding veinMinerToggleKey;
    private static KeyBinding veinMinerDropsToggleKey;

    // Vein Miner hold state tracking
    private static boolean veinMinerKeyHeld = false;

    /**
     * Check if the vein miner key is currently being held.
     */
    public static boolean isVeinMinerKeyHeld() {
        return veinMinerKeyHeld;
    }

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new com.murilloskills.data.ClientXpDataReloadListener());

        // --- NETWORKING ---

        // 1. Sync Skills (Updated for Paragon and Selected Skills)
        ClientPlayNetworking.registerGlobalReceiver(SkillsSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                com.murilloskills.data.ClientSkillData.update(payload.skills());

                // Update Paragon Status in Client Data
                MurilloSkillsList paragon = null;
                if (payload.paragonSkillName() != null && !payload.paragonSkillName().isEmpty()
                        && !payload.paragonSkillName().equals("null")) {
                    try {
                        paragon = MurilloSkillsList.valueOf(payload.paragonSkillName());
                    } catch (Exception ignored) {
                    }
                }
                com.murilloskills.data.ClientSkillData.setParagonSkill(paragon);

                // Update Selected Skills in Client Data
                com.murilloskills.data.ClientSkillData.setSelectedSkills(payload.selectedSkills());

                // Refresh SkillsScreen if open (to update buttons/layout)
                if (context.client().currentScreen instanceof com.murilloskills.gui.SkillsScreen screen) {
                    screen.init(context.client(), screen.width, screen.height);
                }
            });
        });

        // 2. Scan Result (Habilidade Miner)
        ClientPlayNetworking.registerGlobalReceiver(MinerScanResultPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                OreHighlighter.setHighlights(payload.ores());
            });
        });

        // 3. Rain Dance (Habilidade Fisher)
        ClientPlayNetworking.registerGlobalReceiver(RainDanceS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.active()) {
                    RainDanceEffect.start(payload.durationTicks());
                } else {
                    RainDanceEffect.stop();
                }
            });
        });

        // 4. Area Planting Sync (Farmer toggle state)
        ClientPlayNetworking.registerGlobalReceiver(AreaPlantingSyncS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                AreaPlantingHud.setEnabled(payload.enabled());
            });
        });

        // 5. Treasure Hunter (Explorer Level 100 Passive)
        ClientPlayNetworking.registerGlobalReceiver(TreasureHunterS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                TreasureHighlighter.setTreasures(payload.positions());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(XpDataSyncS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                com.murilloskills.data.XpCurveDefinition curve = com.murilloskills.data.XpDataManager.fromJson(
                        payload.curveJson(),
                        com.murilloskills.data.XpCurveDefinition.class);
                com.murilloskills.data.XpValuesDefinition values = com.murilloskills.data.XpDataManager.fromJson(
                        payload.valuesJson(),
                        com.murilloskills.data.XpValuesDefinition.class);
                com.murilloskills.data.XpDataManager.applySync(curve, values);

                if (context.client().currentScreen instanceof com.murilloskills.gui.SkillsScreen screen) {
                    screen.init(context.client(), screen.width, screen.height);
                }
            });
        });

        // 6. XP Gain Toast Notifications
        ClientPlayNetworking.registerGlobalReceiver(XpGainS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Only show toast for selected skills
                MurilloSkillsList skill = payload.getSkill();
                if (skill == null || !com.murilloskills.data.ClientSkillData.isSkillSelected(skill)) {
                    return;
                }
                XpToastRenderer.addToast(skill, payload.xpAmount(), payload.source());
            });
        });

        // 7. Daily Challenges Sync
        ClientPlayNetworking.registerGlobalReceiver(
                com.murilloskills.network.DailyChallengesSyncS2CPayload.ID, (payload, context) -> {
                    context.client().execute(() -> {
                        java.util.List<com.murilloskills.data.ClientSkillData.ChallengeInfo> challenges = payload
                                .challenges().stream()
                                .map(c -> new com.murilloskills.data.ClientSkillData.ChallengeInfo(
                                        c.type(), c.skillName(), c.target(), c.progress(), c.completed(), c.xpReward()))
                                .toList();
                        com.murilloskills.data.ClientSkillData.updateDailyChallenges(
                                challenges, payload.dateKey(), payload.allComplete());
                    });
                });

        // --- KEYBINDINGS ---

        skillsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KEYBIND_CATEGORY));

        abilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.use_ability",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                KEYBIND_CATEGORY));

        areaPlantingToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.area_planting_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KEYBIND_CATEGORY));

        hollowFillToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.hollow_fill_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEYBIND_CATEGORY));

        nightVisionToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.night_vision_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                KEYBIND_CATEGORY));

        stepAssistToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.step_assist_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KEYBIND_CATEGORY));

        fillModeCycleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.fill_mode_cycle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KEYBIND_CATEGORY));

        veinMinerToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.vein_miner_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PERIOD,
                KEYBIND_CATEGORY));

        veinMinerDropsToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.vein_miner_drops_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_COMMA,
                KEYBIND_CATEGORY));

        // --- EVENTS ---

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Update Rain Dance visual effect
            RainDanceEffect.tick(client);

            while (skillsKey.wasPressed()) {
                client.setScreen(new com.murilloskills.gui.SkillsScreen());
            }
            while (abilityKey.wasPressed()) {
                // Envia pacote genérico de habilidade (Server decide o que fazer)
                ClientPlayNetworking.send(new SkillAbilityC2SPayload());
            }
            while (areaPlantingToggleKey.wasPressed()) {
                // Envia pacote para toggle de plantio em área 3x3
                ClientPlayNetworking.send(new AreaPlantingToggleC2SPayload());
            }
            while (hollowFillToggleKey.wasPressed()) {
                // Envia pacote para toggle hollow/filled (Builder)
                ClientPlayNetworking.send(new HollowFillToggleC2SPayload());
            }
            while (nightVisionToggleKey.wasPressed()) {
                // Envia pacote para toggle de visão noturna (Explorer)
                ClientPlayNetworking.send(new NightVisionToggleC2SPayload());
            }
            while (stepAssistToggleKey.wasPressed()) {
                // Envia pacote para toggle de step assist (Explorer)
                ClientPlayNetworking.send(new StepAssistToggleC2SPayload());
            }
            while (fillModeCycleKey.wasPressed()) {
                // Envia pacote para ciclar entre modos de preenchimento (Builder)
                ClientPlayNetworking.send(new com.murilloskills.network.FillModeCycleC2SPayload());
            }
            while (veinMinerDropsToggleKey.wasPressed()) {
                // Toggle drops-to-inventory for Vein Miner
                ClientPlayNetworking.send(new VeinMinerDropsToggleC2SPayload());
            }

            // Vein Miner - detect key press and release (hold to activate)
            boolean keyPressed = veinMinerToggleKey.isPressed();
            if (keyPressed != veinMinerKeyHeld) {
                veinMinerKeyHeld = keyPressed;
                ClientPlayNetworking.send(new VeinMinerToggleC2SPayload(keyPressed));
            }
        });

        WorldRenderEvents.END_MAIN.register(OreHighlighter::render);
        WorldRenderEvents.END_MAIN.register(TreasureHighlighter::render);
        WorldRenderEvents.END_MAIN.register(VeinMinerPreview::render);

        // HUD rendering for Area Planting indicator
        HudRenderCallback.EVENT.register(AreaPlantingHud::render);
        HudRenderCallback.EVENT.register((context, tickDelta) -> XpToastRenderer.render(context));
    }
}
