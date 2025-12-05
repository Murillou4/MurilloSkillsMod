package com.murilloskills;

import com.murilloskills.network.AreaPlantingToggleC2SPayload;
import com.murilloskills.network.AreaPlantingSyncS2CPayload;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.network.RainDanceS2CPayload;
import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.network.SkillsSyncPayload;
import com.murilloskills.render.AreaPlantingHud;
import com.murilloskills.render.OreHighlighter;
import com.murilloskills.render.RainDanceEffect;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MurilloSkillsClient implements ClientModInitializer {

    private static KeyBinding skillsKey;
    private static KeyBinding abilityKey;
    private static KeyBinding areaPlantingToggleKey;

    @Override
    public void onInitializeClient() {

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

        // --- KEYBINDINGS ---

        skillsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyBinding.Category.INVENTORY));

        abilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.use_ability",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                KeyBinding.Category.GAMEPLAY));

        areaPlantingToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.area_planting_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyBinding.Category.GAMEPLAY));

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
        });

        WorldRenderEvents.END_MAIN.register(OreHighlighter::render);

        // HUD rendering for Area Planting indicator
        HudRenderCallback.EVENT.register(AreaPlantingHud::render);
    }
}