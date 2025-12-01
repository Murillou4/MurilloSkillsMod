package com.murilloskills;

import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.network.SkillsSyncPayload;
import com.murilloskills.render.OreHighlighter;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MurilloSkillsClient implements ClientModInitializer {

    private static KeyBinding skillsKey;
    private static KeyBinding abilityKey;

    @Override
    public void onInitializeClient() {

        // --- NETWORKING ---

        // 1. Sync Skills (Updated for Paragon)
        ClientPlayNetworking.registerGlobalReceiver(SkillsSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                com.murilloskills.data.ClientSkillData.update(payload.skills());

                // Update Paragon Status in Client Data
                MurilloSkillsList paragon = null;
                if (payload.paragonSkillName() != null && !payload.paragonSkillName().isEmpty() && !payload.paragonSkillName().equals("null")) {
                    try {
                        paragon = MurilloSkillsList.valueOf(payload.paragonSkillName());
                    } catch (Exception ignored) {}
                }
                com.murilloskills.data.ClientSkillData.setParagonSkill(paragon);
            });
        });

        // 2. Scan Result (Habilidade Miner)
        ClientPlayNetworking.registerGlobalReceiver(MinerScanResultPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                OreHighlighter.setHighlights(payload.ores());
            });
        });

        // --- KEYBINDINGS ---

        skillsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyBinding.Category.INVENTORY
        ));

        abilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.use_ability",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                KeyBinding.Category.GAMEPLAY
        ));

        // --- EVENTS ---

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (skillsKey.wasPressed()) {
                client.setScreen(new com.murilloskills.gui.SkillsScreen());
            }
            while (abilityKey.wasPressed()) {
                // Envia pacote genérico de habilidade (Server decide o que fazer)
                ClientPlayNetworking.send(new SkillAbilityC2SPayload());
            }
        });

        WorldRenderEvents.END_MAIN.register(OreHighlighter::render);
    }
}