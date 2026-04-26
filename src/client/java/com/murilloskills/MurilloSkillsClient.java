package com.murilloskills;

import com.murilloskills.network.AreaPlantingToggleC2SPayload;
import com.murilloskills.network.AreaPlantingSyncS2CPayload;
import com.murilloskills.network.HollowFillToggleC2SPayload;
import com.murilloskills.network.MinerScanResultPayload;
import com.murilloskills.network.NightVisionToggleC2SPayload;
import com.murilloskills.network.UltPlacePreviewRequestC2SPayload;
import com.murilloskills.network.UltPlacePreviewS2CPayload;
import com.murilloskills.network.UltPlaceUndoC2SPayload;
import com.murilloskills.network.UltminePreviewS2CPayload;
import com.murilloskills.network.UltmineRequestC2SPayload;
import com.murilloskills.network.UltmineResultS2CPayload;
import com.murilloskills.network.UltmineClassicBlockListSyncC2SPayload;
import com.murilloskills.network.StepAssistToggleC2SPayload;
import com.murilloskills.network.RainDanceS2CPayload;
import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.network.SkillsSyncPayload;
import com.murilloskills.network.TreasureHunterS2CPayload;
import com.murilloskills.network.SpeedBoostToggleC2SPayload;
import com.murilloskills.network.VeinMinerDropsToggleC2SPayload;
import com.murilloskills.network.VeinMinerToggleC2SPayload;
import com.murilloskills.network.XpDirectToggleC2SPayload;
import com.murilloskills.network.XpGainS2CPayload;
import com.murilloskills.client.config.UltmineClientConfig;
import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.UltPlaceClientState;
import com.murilloskills.data.UltmineClientState;
import com.murilloskills.gui.ParagonAbilityScreen;
import com.murilloskills.gui.UltPlaceConfigScreen;
import com.murilloskills.render.AreaPlantingHud;
import com.murilloskills.render.AutoTorchHud;
import com.murilloskills.render.OreHighlighter;
import com.murilloskills.render.PathfinderHud;
import com.murilloskills.render.RainDanceEffect;
import com.murilloskills.render.TreasureHighlighter;
import com.murilloskills.render.UltPlaceHud;
import com.murilloskills.render.UltPlacePreview;
import com.murilloskills.render.UltminePreview;
import com.murilloskills.render.VeinMinerPreview;
import com.murilloskills.render.XpToastRenderer;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.skills.UltPlacePlanner;
import com.murilloskills.tooltip.SkillTooltipAppender;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
    private static KeyBinding speedBoostToggleKey;
    private static KeyBinding fillModeCycleKey;
    private static KeyBinding veinMinerToggleKey;
    private static KeyBinding veinMinerDropsToggleKey;
    private static KeyBinding autoTorchToggleKey;
    private static KeyBinding ultmineRadialMenuKey;
    private static KeyBinding ultPlaceToggleKey;
    private static KeyBinding ultPlaceConfigKey;
    private static KeyBinding ultPlaceUndoKey;

    // Vein Miner hold state tracking
    private static boolean veinMinerKeyHeld = false;
    private static boolean ultmineMenuKeyHeld = false;
    private static long lastUltPlacePreviewSignature = Long.MIN_VALUE;
    private static long lastUltPlaceSentSignature = Long.MIN_VALUE;
    private static long nextUltPlaceRequestKey = 1L;
    private static long lastUltPlaceRequestTick = Long.MIN_VALUE;
    private static long lastUltPlaceFallbackSignature = Long.MIN_VALUE;
    private static final double ULTPLACE_PREVIEW_RANGE_PADDING = 1.0D;

    /**
     * Check if the vein miner key is currently being held.
     */
    public static boolean isVeinMinerKeyHeld() {
        return veinMinerKeyHeld;
    }

    @Override
    public void onInitializeClient() {
        SkillTooltipAppender.register();

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
                com.murilloskills.data.ClientSkillData.setParagonSkills(payload.paragonSkills());

                // Update Selected Skills and max in Client Data
                com.murilloskills.data.ClientSkillData.setSelectedSkills(payload.selectedSkills());
                com.murilloskills.data.ClientSkillData.setMaxSelectedSkills(payload.maxSelectedSkills());

                // Refresh SkillsScreen if open (to update buttons/layout)
                if (context.client().currentScreen instanceof com.murilloskills.gui.SkillsScreen screen) {
                    screen.init(context.client(), screen.width, screen.height);
                }
            });
        });

        // 2. Scan Result (Habilidade Miner)
        ClientPlayNetworking.registerGlobalReceiver(MinerScanResultPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                OreHighlighter.setHighlights(payload.ores(), payload.remainingDurationTicks());
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

        // 4. Area Planting Sync (Farmer area mode state)
        ClientPlayNetworking.registerGlobalReceiver(AreaPlantingSyncS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                AreaPlantingHud.setState(payload.enabled(), payload.diameter());
            });
        });

        // 5. Treasure Hunter (Explorer Level 100 Passive)
        ClientPlayNetworking.registerGlobalReceiver(TreasureHunterS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                TreasureHighlighter.setTreasures(payload.positions());
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

        // 8. Ultmine preview sync
        ClientPlayNetworking.registerGlobalReceiver(UltminePreviewS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> UltmineClientState.updatePreview(payload.positions()));
        });

        ClientPlayNetworking.registerGlobalReceiver(UltPlacePreviewS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> UltPlaceClientState.applyValidatedPreview(payload.requestKey(),
                    payload.positions()));
        });

        // 9. Pathfinder speed boost state sync
        ClientPlayNetworking.registerGlobalReceiver(
                com.murilloskills.network.PathfinderSyncS2CPayload.ID, (payload, context) -> {
                    context.client().execute(() -> {
                        PathfinderHud.setActive(payload.active());
                    });
                });

        // 10. Auto-Torch state sync
        ClientPlayNetworking.registerGlobalReceiver(
                com.murilloskills.network.AutoTorchSyncS2CPayload.ID, (payload, context) -> {
                    context.client().execute(() -> {
                        AutoTorchHud.setEnabled(payload.enabled());
                    });
                });

        // 11. Ultmine result feedback
        ClientPlayNetworking.registerGlobalReceiver(UltmineResultS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player == null || payload.messageKey() == null || payload.messageKey().isEmpty()) {
                    return;
                }
                var color = payload.success() ? Formatting.GREEN : Formatting.RED;
                context.client().player.sendMessage(
                        Text.translatable(payload.messageKey(), payload.minedBlocks(), payload.requestedBlocks())
                                .formatted(color),
                        true);
            });
        });

        // --- SYNC CLIENT CONFIG ON JOIN ---
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            UltmineClientConfig.load();
            UltPlaceClientState.clearPreview();
            resetUltPlacePreviewTracking();
            // Always sync XP direct-to-player preference to server (respects client choice over server default)
            ClientPlayNetworking.send(new XpDirectToggleC2SPayload(UltmineClientConfig.isXpDirectToPlayer()));
            // Sync magnet config to server
            ClientPlayNetworking.send(new com.murilloskills.network.MagnetConfigC2SPayload(
                    UltmineClientConfig.isMagnetEnabled(),
                    UltmineClientConfig.getMagnetRange()));
            // Sync trash list to server
            ClientPlayNetworking.send(new com.murilloskills.network.TrashListSyncC2SPayload(
                    UltmineClientConfig.getTrashItems()));
            // Sync classic-mode blocked blocks list to server
            ClientPlayNetworking.send(new UltmineClassicBlockListSyncC2SPayload(
                    UltmineClientConfig.getLegacyBlockedBlocks()));
            ClientPlayNetworking.send(UltPlaceClientState.toPayload());
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

        ultPlaceUndoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.ultplace_undo",
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

        ultPlaceToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.ultplace_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KEYBIND_CATEGORY));

        speedBoostToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.speed_boost_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KEYBIND_CATEGORY));

        ultPlaceConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.ultplace_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
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

        autoTorchToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.auto_torch_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                KEYBIND_CATEGORY));

        ultmineRadialMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.murilloskills.ultmine_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_APOSTROPHE,
                KEYBIND_CATEGORY));

        // --- EVENTS ---

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Update Rain Dance visual effect
            RainDanceEffect.tick(client);

            while (skillsKey.wasPressed()) {
                client.setScreen(new com.murilloskills.gui.SkillsScreen());
            }
            while (abilityKey.wasPressed()) {
                if (!isUltPlaceUndoChord(client)) {
                    handleAbilityKey(client);
                }
            }
            while (areaPlantingToggleKey.wasPressed()) {
                // Envia pacote para ciclar o modo em área do Farmer
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
                if (!shouldRouteUltPlaceToggle(client)) {
                    ClientPlayNetworking.send(new StepAssistToggleC2SPayload());
                }
            }
            // V key: ultPlaceToggleKey shadows stepAssistToggleKey when both default to V
            // (Minecraft's KEY_TO_BINDINGS only routes events to one KeyBinding per physical key).
            // Fall back to step assist when not routing to UltPlace so the V toggle keeps working.
            while (ultPlaceToggleKey.wasPressed()) {
                if (shouldRouteUltPlaceToggle(client)) {
                    UltPlaceClientState.toggleEnabled();
                    UltPlaceClientState.clearPreview();
                    resetUltPlacePreviewTracking();
                    ClientPlayNetworking.send(UltPlaceClientState.toPayload());
                    showUltPlaceToggleFeedback(client);
                } else {
                    ClientPlayNetworking.send(new StepAssistToggleC2SPayload());
                }
            }
            while (speedBoostToggleKey.wasPressed()) {
                if (!shouldRouteUltPlaceConfig(client)) {
                    ClientPlayNetworking.send(new SpeedBoostToggleC2SPayload());
                }
            }
            // B key: same shadowing as V — fall back to speed boost when not routing to UltPlace config.
            while (ultPlaceConfigKey.wasPressed()) {
                if (shouldRouteUltPlaceConfig(client)) {
                    client.setScreen(new UltPlaceConfigScreen(client.currentScreen));
                } else {
                    ClientPlayNetworking.send(new SpeedBoostToggleC2SPayload());
                }
            }
            while (autoTorchToggleKey.wasPressed()) {
                // Envia pacote para toggle de auto-torch (Miner)
                ClientPlayNetworking.send(new com.murilloskills.network.AutoTorchToggleC2SPayload());
            }
            while (fillModeCycleKey.wasPressed()) {
                // Envia pacote para ciclar entre modos de preenchimento (Builder)
                ClientPlayNetworking.send(new com.murilloskills.network.FillModeCycleC2SPayload());
            }
            while (veinMinerDropsToggleKey.wasPressed()) {
                // Toggle drops-to-inventory for Vein Miner
                ClientPlayNetworking.send(new VeinMinerDropsToggleC2SPayload());
            }
            boolean ultmineKeyPressed = isKeyBindingPhysicallyPressed(client, ultmineRadialMenuKey);
            if (ultmineKeyPressed && !ultmineMenuKeyHeld) {
                ultmineMenuKeyHeld = true;
                if (client.currentScreen == null) {
                    client.setScreen(new com.murilloskills.gui.UltmineRadialMenuScreen());
                }
            } else if (!ultmineKeyPressed && ultmineMenuKeyHeld) {
                ultmineMenuKeyHeld = false;
                if (client.currentScreen instanceof com.murilloskills.gui.UltmineRadialMenuScreen radialScreen) {
                    radialScreen.releaseAndClose();
                }
            }

            // Vein Miner - detect key press and release (hold to activate)
            boolean keyPressed = veinMinerToggleKey.isPressed();
            if (keyPressed != veinMinerKeyHeld) {
                veinMinerKeyHeld = keyPressed;
                ClientPlayNetworking.send(new VeinMinerToggleC2SPayload(keyPressed));
            }

            if (veinMinerKeyHeld && SkillConfig.isUltmineEnabled()) {
                HitResult hitResult = client.crosshairTarget;
                if (hitResult instanceof BlockHitResult blockHit) {
                    if (client.world == null || client.world.getBlockState(blockHit.getBlockPos()).isAir()) {
                        UltmineClientState.clearPreview();
                        return;
                    }
                    int interval = SkillConfig.getUltminePreviewRequestIntervalTicks();
                    if (client.world != null && client.world.getTime() % interval == 0) {
                        ClientPlayNetworking.send(
                                new UltmineRequestC2SPayload(blockHit.getBlockPos(), blockHit.getSide()));
                    }
                } else {
                    UltmineClientState.clearPreview();
                }
            } else {
                UltmineClientState.clearPreview();
            }

            while (ultPlaceUndoKey.wasPressed()) {
                if (isUltPlaceUndoChord(client)) {
                    ClientPlayNetworking.send(new UltPlaceUndoC2SPayload());
                }
            }

            if (shouldRequestUltPlacePreview(client)) {
                HitResult hitResult = getUltPlacePreviewTarget(client);
                if (hitResult instanceof BlockHitResult blockHit
                        && hitResult.getType() == HitResult.Type.BLOCK
                        && client.world != null) {
                    HeldBlockSelection held = getUltPlaceHeldBlockSelection(client);
                    if (held == null) {
                        UltPlaceClientState.clearPreview();
                        resetUltPlacePreviewTracking();
                        return;
                    }

                    long signature = computeUltPlacePreviewSignature(client, blockHit, held.stack());
                    if (signature != lastUltPlacePreviewSignature) {
                        lastUltPlacePreviewSignature = signature;
                        int availablePlacements = Math.max(1, countMatchingUltPlaceBlocks(client, held.stack()));
                        UltPlacePlanner.UltPlacePlan plan = UltPlacePlanner.planPreview(
                                client.world,
                                client.player,
                                held.hand(),
                                held.stack(),
                                blockHit.getBlockPos(),
                                blockHit.getSide(),
                                blockHit.getPos(),
                                UltPlaceClientState.toSelection(),
                                Math.min(SkillConfig.getUltPlaceMaxBlocksPerUse(), availablePlacements));
                        UltPlaceClientState.updateSpeculativePreview(plan.previewBlocks(), plan.fallbackReason());
                        showUltPlaceFallbackFeedback(client, signature, plan.fallbackReason());
                    }

                    int interval = Math.max(1, SkillConfig.getBuilderUltPlacePreviewRequestIntervalTicks());
                    long currentTick = client.world.getTime();
                    if (signature != lastUltPlaceSentSignature
                            && (lastUltPlaceRequestTick == Long.MIN_VALUE
                                    || currentTick - lastUltPlaceRequestTick >= interval)) {
                        long requestKey = nextUltPlaceRequestKey++;
                        lastUltPlaceSentSignature = signature;
                        lastUltPlaceRequestTick = currentTick;
                        UltPlaceClientState.beginValidation(requestKey);
                        ClientPlayNetworking.send(new UltPlacePreviewRequestC2SPayload(
                                blockHit.getBlockPos(), blockHit.getSide(), blockHit.getPos(), requestKey));
                    }
                } else {
                    UltPlaceClientState.clearPreview();
                    resetUltPlacePreviewTracking();
                }
            } else {
                UltPlaceClientState.clearPreview();
                resetUltPlacePreviewTracking();
            }
        });

        WorldRenderEvents.END_MAIN.register(OreHighlighter::render);
        WorldRenderEvents.END_MAIN.register(TreasureHighlighter::render);
        WorldRenderEvents.END_MAIN.register(VeinMinerPreview::render);
        WorldRenderEvents.END_MAIN.register(UltminePreview::render);
        WorldRenderEvents.END_MAIN.register(UltPlacePreview::render);

        // HUD rendering for indicators
        HudRenderCallback.EVENT.register(AreaPlantingHud::render);
        HudRenderCallback.EVENT.register(PathfinderHud::render);
        HudRenderCallback.EVENT.register(AutoTorchHud::render);
        HudRenderCallback.EVENT.register(UltPlaceHud::render);
        HudRenderCallback.EVENT.register(com.murilloskills.render.FarmerCropHud::render);
        HudRenderCallback.EVENT.register((context, tickDelta) -> XpToastRenderer.render(context));
    }

    private static boolean isKeyBindingPhysicallyPressed(MinecraftClient client, KeyBinding keyBinding) {
        InputUtil.Key boundKey = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
        long windowHandle = client.getWindow().getHandle();
        if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, boundKey.getCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(client.getWindow(), boundKey.getCode());
    }

    private static void showUltPlaceToggleFeedback(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        boolean enabled = UltPlaceClientState.isEnabled();
        String messageKey = enabled
                ? "murilloskills.ultplace.enabled.on"
                : "murilloskills.ultplace.enabled.off";
        client.player.sendMessage(
                Text.translatable(messageKey).formatted(enabled ? Formatting.AQUA : Formatting.GRAY),
                true);
    }

    private static boolean shouldRouteUltPlaceToggle(MinecraftClient client) {
        return isBuilderHoldingBlockItem(client);
    }

    private static boolean shouldRouteUltPlaceConfig(MinecraftClient client) {
        return isBuilderHoldingBlockItem(client);
    }

    private static boolean shouldRequestUltPlacePreview(MinecraftClient client) {
        return client.player != null
                && client.world != null
                && UltPlaceClientState.isEnabled()
                && isBuilderHoldingBlockItem(client);
    }

    private static HitResult getUltPlacePreviewTarget(MinecraftClient client) {
        if (client.player == null) {
            return client.crosshairTarget;
        }
        double range = Math.max(5.0D, client.player.getBlockInteractionRange() + ULTPLACE_PREVIEW_RANGE_PADDING);
        HitResult hitResult = client.player.raycast(range, 1.0F, false);
        return hitResult == null ? client.crosshairTarget : hitResult;
    }

    private static boolean isBuilderHoldingBlockItem(MinecraftClient client) {
        return client.player != null
                && ClientSkillData.isSkillSelected(MurilloSkillsList.BUILDER)
                && (client.player.getMainHandStack().getItem() instanceof BlockItem
                        || client.player.getOffHandStack().getItem() instanceof BlockItem);
    }

    private static void handleAbilityKey(MinecraftClient client) {
        int paragonCount = ClientSkillData.getParagonSkills().size();
        if (paragonCount > 1 && client.currentScreen == null) {
            client.setScreen(new ParagonAbilityScreen());
            return;
        }
        if (paragonCount == 1) {
            ClientPlayNetworking.send(new SkillAbilityC2SPayload(ClientSkillData.getParagonSkill()));
            return;
        }
        ClientPlayNetworking.send(new SkillAbilityC2SPayload());
    }

    private static boolean isUltPlaceUndoChord(MinecraftClient client) {
        return client.player != null
                && ClientSkillData.isSkillSelected(MurilloSkillsList.BUILDER)
                && (InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL));
    }

    private static HeldBlockSelection getUltPlaceHeldBlockSelection(MinecraftClient client) {
        if (client.player == null) {
            return null;
        }

        ItemStack main = client.player.getMainHandStack();
        if (main.getItem() instanceof BlockItem) {
            return new HeldBlockSelection(Hand.MAIN_HAND, copySingle(main));
        }

        ItemStack off = client.player.getOffHandStack();
        if (off.getItem() instanceof BlockItem) {
            return new HeldBlockSelection(Hand.OFF_HAND, copySingle(off));
        }

        return null;
    }

    private static int countMatchingUltPlaceBlocks(MinecraftClient client, ItemStack reference) {
        if (client.player == null || reference == null || reference.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (int slot = 0; slot < client.player.getInventory().size(); slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, reference)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static long computeUltPlacePreviewSignature(MinecraftClient client, BlockHitResult blockHit, ItemStack stack) {
        long signature = 17L;
        signature = mixSignature(signature, blockHit.getBlockPos().asLong());
        signature = mixSignature(signature, blockHit.getSide().ordinal());
        signature = mixSignature(signature, quantizeHitOffset(blockHit.getBlockPos(), blockHit.getPos()));

        var selection = UltPlaceClientState.toSelection();
        signature = mixSignature(signature, selection.shape().ordinal());
        signature = mixSignature(signature, selection.size());
        signature = mixSignature(signature, selection.length());
        signature = mixSignature(signature, selection.height());
        signature = mixSignature(signature, selection.variant());
        signature = mixSignature(signature, selection.anchorMode().ordinal());
        signature = mixSignature(signature, selection.rotationMode().ordinal());
        signature = mixSignature(signature, selection.spacing());

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        signature = mixSignature(signature, itemId.hashCode());
        signature = mixSignature(signature, stack.getComponents().hashCode());
        if (client.world != null) {
            signature = mixSignature(signature, client.world.getRegistryKey().getValue().hashCode());
        }
        return signature;
    }

    private static long quantizeHitOffset(BlockPos pos, Vec3d hitPos) {
        int qx = quantizeHitComponent(hitPos.x - pos.getX());
        int qy = quantizeHitComponent(hitPos.y - pos.getY());
        int qz = quantizeHitComponent(hitPos.z - pos.getZ());
        return (qx & 3L) | ((qy & 3L) << 2) | ((qz & 3L) << 4);
    }

    private static int quantizeHitComponent(double value) {
        double clamped = Math.max(0.0, Math.min(value, 0.999999));
        return Math.max(0, Math.min(3, (int) Math.floor(clamped * 4.0)));
    }

    private static long mixSignature(long current, long value) {
        return current * 31L + value;
    }

    private static void showUltPlaceFallbackFeedback(MinecraftClient client, long signature, String fallbackReason) {
        if (client.player == null || fallbackReason == null || signature == lastUltPlaceFallbackSignature) {
            return;
        }
        lastUltPlaceFallbackSignature = signature;
        client.player.sendMessage(Text.translatable(fallbackReason).formatted(Formatting.YELLOW), true);
    }

    private static void resetUltPlacePreviewTracking() {
        lastUltPlacePreviewSignature = Long.MIN_VALUE;
        lastUltPlaceSentSignature = Long.MIN_VALUE;
        nextUltPlaceRequestKey = 1L;
        lastUltPlaceRequestTick = Long.MIN_VALUE;
        lastUltPlaceFallbackSignature = Long.MIN_VALUE;
    }

    private static ItemStack copySingle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private record HeldBlockSelection(Hand hand, ItemStack stack) {
    }
}
