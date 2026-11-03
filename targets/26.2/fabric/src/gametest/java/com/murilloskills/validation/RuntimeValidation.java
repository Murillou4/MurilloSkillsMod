package com.murilloskills.validation;
import com.murilloskills.*;
import com.murilloskills.client.*;
import com.murilloskills.data.*;
import com.murilloskills.network.*;
import com.murilloskills.skills.*;
import com.murilloskills.events.GameplayEvents;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.*;
import net.minecraft.client.gui.components.*;
import java.nio.file.*;
import java.util.*;
import static com.murilloskills.validation.Checks.check;

public final class RuntimeValidation implements FabricClientGameTest {
    private final Path output=Path.of(System.getProperty("murilloskills.evidence","artifacts/runtime"));
    private final List<String> ui=new ArrayList<>();
    public void runTest(ClientGameTestContext context){
        if(!Boolean.getBoolean("murilloskills.selftest"))throw new IllegalStateException("Explicit self-test flag required");
        try{
            Files.createDirectories(output);Files.deleteIfExists(output.resolve("failure.txt"));TestWorldSave save;UUIDBox id=new UUIDBox();
            Path release=Path.of(MurilloSkills.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if(Boolean.getBoolean("murilloskills.releaseJar"))check(release.toString().endsWith(".jar"),"packaging.actual_release_jar_loaded");
            context.runOnClient(client->{client.options.pauseOnLostFocus=false;client.options.guiScale().set(2);client.resizeGui();});
            try(var world=context.worldBuilder().create()){
                save=world.getWorldSave();context.waitFor(client->client.player!=null&&ClientState.available,600);
                world.getConnection().waitForChunksRender();
                context.waitForScreen(ProfessionScreen.class);
                check(context.computeOnClient(client->ClientState.ready()&&((ProfessionScreen)client.gui.screen()).page==ProfessionScreen.Page.CATALOG),"ui.initial.real_automatic_open");
                catalogCheck(context,"first_open");shot(context,"01-first-steps");
                tooltipShot(context,"Arqueiro","01-tooltip-archer-gui2");
                context.getInput().setCursorPos(1000,600);context.getInput().scroll(-4);catalogCheck(context,"after_scroll");
                context.clickScreenButton("Minerador");
                context.runOnClient(client->{client.options.guiScale().set(3);client.resizeGui();});catalogCheck(context,"gui3_before_reopening");shot(context,"01a-selection-gui3");
                tooltipShot(context,"Fazendeiro","01-tooltip-farmer-gui3");
                context.clickScreenButton("Guerreiro");context.clickScreenButton("Começar (2/4)");
                context.waitFor(client->ClientState.ready()&&ClientState.profile.selected.size()==2&&client.gui.screen() instanceof ProfessionScreen panel&&panel.page==ProfessionScreen.Page.HOME,200);
                shot(context,"01a-partial-home");
                clickAction(context,"slot:2");catalogCheck(context,"complete_free_slots");
                context.clickScreenButton("Construtor");context.clickScreenButton("Explorador");context.clickScreenButton("Começar (4/4)");
                context.waitFor(client->ClientState.ready()&&ClientState.profile.selected.size()==4&&client.gui.screen() instanceof ProfessionScreen panel&&panel.page==ProfessionScreen.Page.HOME,200);
                shot(context,"01a-four-slot-home-gui3");
                clickAction(context,"home:manage");catalogCheck(context,"manage_full_character");click(context,"Profissões");
                world.getServer().runOnServer(server->{var data=MurilloSkills.store.get(server.getPlayerList().getPlayers().getFirst());check(data.selected.size()==4&&data.seals==0,"ui.initial.atomic_batch_and_free_completion");});
                context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);
                context.runOnClient(client->{client.options.guiScale().set(2);client.resizeGui();});
                var mining=new UltmineInputProbes();
                var oldMineKey=context.computeOnClient(client->net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(MurilloSkillsClient.mine));
                try{
                    world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();mining.prepare(player,false,15);MurilloSkills.store.get(player).tools.miningShape="VEIN";Wire.send(player,"");});
                    context.waitFor(client->ClientState.profile.stats(SkillType.MINER).level==15,200);
                    context.getInput().holdKey(MurilloSkillsClient.mineMenu);context.waitForScreen(UltmineRadialScreen.class);shot(context,"01b-ultmine-radial");
                    double[] choice=context.computeOnClient(client->{
                        var screen=client.gui.screen();var button=screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.getMessage().getString().equals("3 × 3")).map(c->(AbstractWidget)c).findFirst().orElseThrow();
                        return new double[]{(button.getX()+button.getWidth()/2d)*client.getWindow().getWidth()/screen.width,(button.getY()+button.getHeight()/2d)*client.getWindow().getHeight()/screen.height};
                    });
                    context.getInput().setCursorPos(choice[0],choice[1]);context.waitTicks(4);
                    var hoverShot=context.takeScreenshot("01b-ultmine-radial-hover");Files.copy(hoverShot,output.resolve("01b-ultmine-radial-hover.png"),StandardCopyOption.REPLACE_EXISTING);ui.add("PASS screenshot 01b-ultmine-radial-hover");
                    context.getInput().releaseKey(MurilloSkillsClient.mineMenu);
                    context.waitFor(client->client.gui.screen()==null&&ClientState.profile.tools.miningShape.equals("3X3"),200);
                    world.getServer().runOnServer(server->check(MurilloSkills.store.get(server.getPlayerList().getPlayers().getFirst()).tools.miningShape.equals("3X3"),"input.ultmine.radial_release_roundtrip"));
                    context.runOnClient(client->{client.options.guiScale().set(3);client.resizeGui();});
                    context.getInput().holdKey(MurilloSkillsClient.mineMenu);context.waitForScreen(UltmineRadialScreen.class);shot(context,"01c-ultmine-radial-gui3");
                    context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);context.getInput().releaseKey(MurilloSkillsClient.mineMenu);
                    context.runOnClient(client->{client.options.guiScale().set(2);client.resizeGui();MurilloSkillsClient.mine.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_K));net.minecraft.client.KeyMapping.resetMapping();});
                    context.getInput().lookAt(mining.root());context.waitTicks(5);
                    context.getInput().holdKey(MurilloSkillsClient.mine);context.waitFor(client->ClientState.layers.containsKey("mine")&&!ClientState.layers.get("mine").positions().isEmpty(),100);shot(context,"01c-ultmine-white-preview");
                    context.getInput().holdMouse(0);context.waitTicks(35);shot(context,"01d-ultmine-keyboard-active");
                    context.getInput().releaseMouse(0);context.getInput().releaseKey(MurilloSkillsClient.mine);context.waitTicks(4);
                    world.getServer().runOnServer(server->mining.verify(server.getPlayerList().getPlayers().getFirst(),9,"survival_rebound_key"));
                    world.getServer().runOnServer(server->mining.prepare(server.getPlayerList().getPlayers().getFirst(),false,15));
                    context.getInput().lookAt(mining.root());context.waitTicks(5);context.getInput().holdMouseFor(0,35);context.waitTicks(3);
                    world.getServer().runOnServer(server->mining.verify(server.getPlayerList().getPlayers().getFirst(),1,"released_key"));
                    world.getServer().runOnServer(server->mining.prepare(server.getPlayerList().getPlayers().getFirst(),true,15));
                    context.getInput().lookAt(mining.root());context.waitTicks(5);context.getInput().holdKey(MurilloSkillsClient.mine);context.getInput().holdMouseFor(0,2);context.getInput().releaseKey(MurilloSkillsClient.mine);context.waitTicks(4);
                    world.getServer().runOnServer(server->mining.verify(server.getPlayerList().getPlayers().getFirst(),9,"creative_first_instant_break"));
                    world.getServer().runOnServer(server->mining.prepare(server.getPlayerList().getPlayers().getFirst(),false,14));
                    context.getInput().lookAt(mining.root());context.waitTicks(5);context.getInput().holdKey(MurilloSkillsClient.mine);context.getInput().holdMouseFor(0,35);shot(context,"01e-ultmine-locked-feedback");context.getInput().releaseKey(MurilloSkillsClient.mine);context.waitTicks(4);
                    world.getServer().runOnServer(server->mining.verify(server.getPlayerList().getPlayers().getFirst(),1,"locked_level_14"));
                    world.getServer().runOnServer(server->mining.prepare(server.getPlayerList().getPlayers().getFirst(),true,15));
                    context.runOnClient(client->{ClientPreferences.current.miningToggle=true;MiningInput.reset();});
                    context.getInput().lookAt(mining.root());context.waitTicks(4);context.getInput().pressKey(MurilloSkillsClient.mine);
                    context.getInput().holdMouseFor(0,2);context.waitTicks(4);
                    world.getServer().runOnServer(server->mining.verify(server.getPlayerList().getPlayers().getFirst(),9,"toggle_mode_after_key_release"));
                    context.getInput().pressKey(MurilloSkillsClient.mine);context.waitTicks(3);
                    world.getServer().runOnServer(server->check(!GameplayEvents.context(server.getPlayerList().getPlayers().getFirst()).runtime().miningHeld,"input.ultmine.toggle_mode_turns_off"));
                    context.runOnClient(client->{ClientPreferences.current.miningToggle=false;MiningInput.reset();});
                }finally{
                    context.getInput().releaseMouse(0);context.getInput().releaseKey(MurilloSkillsClient.mine);context.getInput().releaseKey(MurilloSkillsClient.mineMenu);
                    context.runOnClient(client->{ClientPreferences.current.miningToggle=false;MiningInput.reset();MurilloSkillsClient.mine.setKey(oldMineKey);net.minecraft.client.KeyMapping.resetMapping();client.gui.setScreen(null);});
                }
                NativeFeatureProbes nativeProbes=new NativeFeatureProbes();
                world.getServer().runOnServer(server->nativeProbes.begin(server.getPlayerList().getPlayers().getFirst()));
                context.waitTicks(220);
                world.getServer().runOnServer(server->nativeProbes.finish(server.getPlayerList().getPlayers().getFirst()));
                world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();id.value=player.getUUID();GameplayProbes.run(player);Files.writeString(output.resolve("catalog22.md"),com.murilloskills.skills.effects.SummaryService.catalog(com.murilloskills.events.GameplayEvents.context(player)));});
                context.waitTicks(5);
                context.waitFor(client->ClientState.summaries.containsKey(SkillType.MINER)&&ClientState.summaries.get(SkillType.MINER).level()==100,200);
                final int[] areaCap={0};
                world.getServer().runOnServer(server->{var c=GameplayEvents.context(server.getPlayerList().getPlayers().getFirst());areaCap[0]=c.store().config.maxAreaBlocks;c.store().config.maxAreaBlocks=17;com.murilloskills.api.SkillRegistry.get(SkillType.MINER).activate(c);GameplayEvents.refresh(c);Wire.send(c.player(),"");});
                context.waitFor(client->ClientState.ready()&&UltmineRadialScreen.capacityLabel().equals("17 blocos"),200);
                context.runOnClient(client->client.gui.setScreen(new UltmineRadialScreen(null,false)));shot(context,"01f-ultmine-server-cap");
                check(context.computeOnClient(client->UltmineRadialScreen.capacityLabel().equals(ClientState.summaries.get(SkillType.MINER).effects().stream().filter(e->e.id().equals("miner.area")).findFirst().orElseThrow().value())),"ui.ultmine.capacity_uses_server_with_active_and_custom_cap");
                world.getServer().runOnServer(server->{var c=GameplayEvents.context(server.getPlayerList().getPlayers().getFirst());com.murilloskills.api.SkillRegistry.get(SkillType.MINER).cancel(c);c.store().config.maxAreaBlocks=areaCap[0];GameplayEvents.refresh(c);Wire.send(c.player(),"");});
                context.waitFor(client->ClientState.ready()&&!ClientState.active(SkillType.MINER)&&UltmineRadialScreen.capacityLabel().equals("153 blocos"),200);
                context.runOnClient(client->client.gui.setScreen(new ProfessionScreen()));shot(context,"02-profession-workspace");
                context.clickScreenButton("Minerador");click(context,"Progressão");shot(context,"03-progression");
                context.getInput().setCursorPos(1000,650);context.getInput().scroll(-10);context.waitTicks(2);
                check(context.computeOnClient(client->((ProfessionScreen)client.gui.screen()).professionNavigationCount()==4),"ui.navigation_survives_content_scroll");
                var stableTab=context.computeOnClient(client->((ProfessionScreen)client.gui.screen()).action("tab:PROGRESSION"));
                context.runOnClient(client->client.gui.screen().setFocused(stableTab));
                world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();MurilloSkills.store.get(player).stats(SkillType.MINER).xp+=1;Wire.send(player,"");});context.waitTicks(5);
                check(context.computeOnClient(client->((ProfessionScreen)client.gui.screen()).action("tab:PROGRESSION")==stableTab&&client.gui.screen().getFocused()==stableTab),"ui.xp_update_keeps_widgets_and_focus");
                click(context,"Especialização");shot(context,"04-branch-comparison");
                clickAction(context,"branch:1");context.waitTicks(5);
                world.getServer().runOnServer(server->check(MurilloSkills.store.get(id.value).stats(SkillType.MINER).branch==1,"ui.branch_packet_roundtrip"));
                click(context,"Controles");shot(context,"05-controls");
                clickAction(context,"toggle:night_vision");context.waitTicks(5);
                world.getServer().runOnServer(server->check(com.murilloskills.skills.effects.Feature.NIGHT_VISION.enabled(MurilloSkills.store.get(id.value)),"ui.control_server_roundtrip"));
                var utilityKey=MurilloSkillsClient.featureKeys.get(com.murilloskills.skills.effects.Feature.NIGHT_VISION);
                var previousUtilityKey=context.computeOnClient(client->net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(utilityKey));
                try{
                    clickAction(context,"bind:night_vision");context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_F8);
                    check(context.computeOnClient(client->net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(utilityKey).getValue()==org.lwjgl.glfw.GLFW.GLFW_KEY_F8),"ui.controls.inline_key_capture");
                    context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);context.getInput().pressKey(utilityKey);context.waitTicks(5);
                    world.getServer().runOnServer(server->check(!com.murilloskills.skills.effects.Feature.NIGHT_VISION.enabled(MurilloSkills.store.get(id.value)),"input.utility_binding_turns_off"));
                    context.getInput().pressKey(utilityKey);context.waitTicks(5);
                    world.getServer().runOnServer(server->check(com.murilloskills.skills.effects.Feature.NIGHT_VISION.enabled(MurilloSkills.store.get(id.value)),"input.utility_binding_turns_on"));
                    check(context.computeOnClient(client->client.gui.screen()==null),"ui.controls.returned_to_world");
                    context.getInput().pressKey(MurilloSkillsClient.guide);context.waitTicks(5);
                    shot(context,"diagnostic-guide-key");
                    String result=context.computeOnClient(client->String.valueOf(client.gui.screen())+"; available="+ClientState.available+"; key="+net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(MurilloSkillsClient.guide));
                    check(context.computeOnClient(client->client.gui.screen() instanceof ProfessionScreen),"input.guide.opens_workspace: "+result);
                }finally{context.runOnClient(client->{utilityKey.setKey(previousUtilityKey);net.minecraft.client.KeyMapping.resetMapping();client.options.save();});}
                context.clickScreenButton("Minerador");click(context,"Resumo");clickAction(context,"paragon");context.waitTicks(5);
                world.getServer().runOnServer(server->check(MurilloSkills.store.get(id.value).paragon==SkillType.MINER,"ui.paragon_packet_roundtrip"));
                clickAction(context,"prestige");shot(context,"06-prestige-confirmation");context.clickScreenButton("Confirmar");context.waitTicks(5);
                world.getServer().runOnServer(server->{
                    var player=server.getPlayerList().getPlayers().getFirst();var s=MurilloSkills.store.get(id.value).stats(SkillType.MINER);check(s.level==0&&s.unlockLevel()==100&&s.prestige==1,"ui.prestige_keeps_tools");
                    var model=com.murilloskills.skills.effects.SummaryService.build(GameplayEvents.context(player),SkillType.MINER);
                    var speed=model.effects().stream().filter(e->e.id().equals("miner.speed")).findFirst().orElseThrow();
                    check(speed.nextValue().startsWith("Nv. 1:"),"ui.prestige_next_numeric_level_one");
                    var comparison=model.branches().getFirst().current().stream().filter(e->e.title().equals(speed.title())).findFirst().orElseThrow();
                    check(comparison.before().equals(speed.value()),"ui.prestige_branch_current_matches_gameplay");
                });
                for(var profession:SkillType.values()){
                    final var target=profession;
                    context.runOnClient(client->client.gui.setScreen(new ProfessionScreen(target,ProfessionScreen.Tab.SUMMARY)));shot(context,"profession-"+target.name().toLowerCase(java.util.Locale.ROOT));
                }
                click(context,"Contratos");shot(context,"07-contracts");
                click(context,"Ajuda");shot(context,"08-guide");
                click(context,"Ferramentas");shot(context,"09-ultmine");
                click(context,"Opções e filtros");clickAction(context,"filter:ore");
                context.runOnClient(client->{for(var child:List.copyOf(client.gui.screen().children()))if(child instanceof EditBox search)search.setValue("minecraft:diamond_ore");});
                shot(context,"10-ore-filter");context.clickScreenButton("Diamond Ore");context.waitTicks(5);
                world.getServer().runOnServer(server->check(MurilloSkills.store.get(id.value).tools.oreFilter.contains("minecraft:diamond_ore"),"ui.filter_packet_roundtrip"));
                context.clickScreenButton("Voltar");clickAction(context,"filter:classic");shot(context,"11-classic-filter");context.clickScreenButton("Voltar");
                clickAction(context,"filter:trash");shot(context,"12-trash-picker");context.clickScreenButton("Voltar");
                clickAction(context,"tool:place");shot(context,"13-ultplace");
                clickAction(context,"shape:COLUMN");
                context.waitFor(client->ClientState.profile.tools.placementShape.equals("COLUMN")&&client.gui.screen() instanceof ProfessionScreen panel&&panel.action("more:spacing")!=null,200);
                int oldSpacing=context.computeOnClient(client->ClientState.profile.tools.spacing);
                clickAction(context,"more:spacing");context.waitTicks(4);
                world.getServer().runOnServer(server->check(MurilloSkills.store.get(id.value).tools.spacing==Math.min(4,oldSpacing+1),"ui.tools.column_spacing_roundtrip"));
                shot(context,"13a-column-spacing");
                context.runOnClient(client->ClientState.send(Protocol.Op.TOOL_CONFIG,"spacing",oldSpacing));
                context.runOnClient(client->client.gui.setScreen(new LegacyImportScreen(new ProfessionScreen())));shot(context,"14-legacy-preview");
                context.runOnClient(client->client.gui.setScreen(new AbilityRadialScreen()));shot(context,"15-ability-radial");
                context.clickScreenButton("Minerador");context.waitTicks(5);
                context.runOnClient(client->{client.options.guiScale().set(3);client.resizeGui();client.gui.setScreen(new ProfessionScreen());});shot(context,"16-workspace-gui3");
                context.clickScreenButton("Minerador");click(context,"Especialização");shot(context,"17-branches-gui3");click(context,"Controles");shot(context,"18-controls-gui3");
                context.runOnClient(client->client.gui.setScreen(ProfessionScreen.tools(true)));shot(context,"19-tools-gui3");
                check(context.computeOnClient(client->((ProfessionScreen)client.gui.screen()).action("more:spacing")!=null),"ui.tools.column_spacing_visible_gui3");
                click(context,"Contratos");shot(context,"20-contracts-gui3");
                context.runOnClient(client->{client.options.guiScale().set(2);client.resizeGui();client.gui.setScreen(null);});
                world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();Wire.markers(player,List.of(player.blockPosition().offset(2,0,0)),"ore",120);});
                shot(context,"21-world-markers");
                var oldCardMode=context.computeOnClient(client->ClientPreferences.current.cardMode());
                boolean oldHud=context.computeOnClient(client->ClientPreferences.current.hud);
                try{
                    context.runOnClient(client->{ClientPreferences.current.hud=true;ClientPreferences.current.equippedCard=ClientPreferences.CardMode.ACTIVE_ONLY;ClientPreferences.save();});
                    world.getServer().runOnServer(server->{var c=GameplayEvents.context(server.getPlayerList().getPlayers().getFirst());for(var type:List.copyOf(c.runtime().activeUntil.keySet()))com.murilloskills.api.SkillRegistry.get(type).cancel(c);c.runtime().activities.clear();c.profile().stats(SkillType.MINER).cooldownUntil=0;c.profile().globalCooldownUntil=0;Wire.send(c.player(),"");});
                    context.waitFor(client->!ClientState.active(SkillType.MINER),200);
                    openHudPreferences(context);clickAction(context,"pref:equipped_card");
                    check(context.computeOnClient(client->ClientPreferences.current.cardMode()==ClientPreferences.CardMode.ALWAYS),"ui.hud.card_always_option");
                    context.runOnClient(client->ClientPreferences.load());
                    check(context.computeOnClient(client->ClientPreferences.current.cardMode()==ClientPreferences.CardMode.ALWAYS),"ui.hud.card_preference_saved_and_reloaded");
                    context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);shot(context,"22-hud-card-always-idle");
                    openHudPreferences(context);clickAction(context,"pref:equipped_card");
                    check(context.computeOnClient(client->ClientPreferences.current.cardMode()==ClientPreferences.CardMode.HIDDEN&&ClientPreferences.current.hud),"ui.hud.card_hidden_keeps_other_hud_preferences");
                    context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);
                    context.getInput().pressKey(MurilloSkillsClient.ability);context.waitFor(client->ClientState.active(SkillType.MINER),200);
                    shot(context,"23-hud-card-hidden-active");
                    openHudPreferences(context);clickAction(context,"pref:equipped_card");
                    check(context.computeOnClient(client->ClientPreferences.current.cardMode()==ClientPreferences.CardMode.ACTIVE_ONLY),"ui.hud.card_contextual_option");
                    context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);shot(context,"24-hud-card-contextual-active");
                    context.getInput().pressKey(MurilloSkillsClient.ability);context.waitFor(client->!ClientState.active(SkillType.MINER),200);
                    world.getServer().runOnServer(server->{var c=GameplayEvents.context(server.getPlayerList().getPlayers().getFirst());c.runtime().activities.clear();Wire.send(c.player(),"");});
                    shot(context,"25-hud-card-contextual-idle");
                }finally{context.runOnClient(client->{ClientPreferences.current.equippedCard=oldCardMode;ClientPreferences.current.hud=oldHud;ClientPreferences.save();client.gui.setScreen(null);});}
                ui.add("PASS automatic eight-profession catalog, atomic selection, four-slot home, fixed navigation, numeric comparisons, controls, tools and GUI scales 2/3");
                world.getServer().runOnServer(server->MurilloSkills.store.flush());
            }
            try(var reopened=save.open()){
                context.waitFor(client->client.player!=null&&ClientState.available,600);
                reopened.getServer().runOnServer(server->{
                    var player=server.getPlayerList().getPlayers().getFirst();var data=MurilloSkills.store.get(player);
                    check(player.getAttached(LegacySource.ATTACHMENT)!=null,"import.legacy_attachment_preserved_on_reopen");
                    check(data.stats(SkillType.MINER).prestige==1&&data.stats(SkillType.MINER).highestLevel==100,"persistence.world_reopen_prestige");
                    check(data.tools.oreFilter.contains("minecraft:diamond_ore"),"persistence.world_reopen_filter");
                    check(data.contracts.getFirst().progress==3,"persistence.world_reopen_contract");
                    check(com.murilloskills.skills.effects.Feature.NIGHT_VISION.enabled(data),"persistence.world_reopen_feature_controls");
                });
                context.runOnClient(client->client.gui.setScreen(null));shot(context,"28-world-reopened");
            }
            Path ownedRoot=Path.of(System.getProperty("murilloskills.testRoot")).toAbsolutePath().normalize();
            Path dedicatedWorld=ownedRoot.resolve("dedicated-world").normalize();
            if(!dedicatedWorld.startsWith(ownedRoot)||dedicatedWorld.equals(ownedRoot))throw new IllegalArgumentException("Unsafe disposable world path");
            Files.createDirectories(ownedRoot);
            Properties properties=new Properties();properties.setProperty("online-mode","false");properties.setProperty("server-ip","127.0.0.1");
            try(var socket=new java.net.ServerSocket(0,1,java.net.InetAddress.getLoopbackAddress())){properties.setProperty("server-port",""+socket.getLocalPort());}
            properties.setProperty("level-name",dedicatedWorld.toString());properties.setProperty("spawn-protection","0");
            try(var dedicated=context.worldBuilder().createServer(properties)){
                try(var connection=dedicated.connect()){
                    context.waitFor(client->client.player!=null&&ClientState.available,600);
                    dedicated.runOnServer(server->{check(server.isDedicatedServer(),"dedicated.real_server");GameplayProbes.run(server.getPlayerList().getPlayers().getFirst());});
                    context.runOnClient(client->client.gui.setScreen(new ProfessionScreen()));shot(context,"29-dedicated-world");
                }
            }
            Files.write(output.resolve("gameplay.txt"),concat(Checks.results,"PASS"));
            Files.write(output.resolve("ui.txt"),concat(ui,"PASS"));
            String sha=Files.isRegularFile(release)?java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(release))):"classes";
            Files.writeString(output.resolve("summary.json"),ProfileCodec.JSON.toJson(Map.of("gameplayChecks",Checks.results.size(),"screenshots",ui,"singleplayer","PASS","reopen","PASS","dedicated","PASS","release",release.toString(),"sha256",sha)));
        }catch(Throwable e){
            try{var text=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(text));Files.writeString(output.resolve("failure.txt"),"FAIL\n"+text);}catch(Exception ignored){}
            throw new AssertionError("MurilloSkills runtime validation failed",e);
        }
    }
    private void catalogCheck(ClientGameTestContext context,String stage){
        check(context.computeOnClient(client->{
            var screen=client.gui.screen();
            return screen instanceof ProfessionScreen panel&&panel.page==ProfessionScreen.Page.CATALOG&&Arrays.stream(SkillType.values()).allMatch(type->screen.children().stream().anyMatch(child->child instanceof AbstractWidget widget&&widget.visible&&widget.getMessage().getString().equals(type.title)&&widget.getX()>=0&&widget.getRight()<=screen.width&&widget.getY()>=0&&widget.getBottom()<=screen.height));
        }),"ui.initial.all_eight_"+stage);
    }
    private void openHudPreferences(ClientGameTestContext context){
        context.getInput().pressKey(MurilloSkillsClient.guide);context.waitForScreen(ProfessionScreen.class);click(context,"Ajuda");
    }
    private void tooltipShot(ClientGameTestContext context,String profession,String name)throws Exception{
        var point=context.computeOnClient(client->{
            var screen=client.gui.screen();var widget=screen.children().stream().filter(child->child instanceof AbstractWidget w&&w.getMessage().getString().equals(profession)).map(child->(AbstractWidget)child).findFirst().orElseThrow();
            return new double[]{(widget.getRight()-3d)*client.getWindow().getWidth()/screen.width,(widget.getY()+3d)*client.getWindow().getHeight()/screen.height};
        });
        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(4);
        check(context.computeOnClient(client->{var panel=(ProfessionScreen)client.gui.screen();var box=panel.tooltipBounds;return box!=null&&box.x()>=8&&box.y()>=8&&box.x()+box.width()<=panel.width-8&&box.y()+box.height()<=panel.height-8;}),"ui.tooltip.within_screen."+name);
        var path=context.takeScreenshot(name);Files.copy(path,output.resolve(name+".png"),StandardCopyOption.REPLACE_EXISTING);ui.add("PASS screenshot "+name);
    }
    private void clickAction(ClientGameTestContext context,String id){
        for(int pass=0;pass<2;pass++)for(int i=0;i<80;i++){
            double[] point=context.computeOnClient(client->{
                if(!(client.gui.screen() instanceof ProfessionScreen panel))return null;
                var widget=panel.action(id);if(widget==null||!widget.visible)return null;
                if(!widget.active)throw new AssertionError("Disabled production action: "+id);
                return new double[]{(widget.getX()+widget.getWidth()/2d)*client.getWindow().getWidth()/panel.width,(widget.getY()+widget.getHeight()/2d)*client.getWindow().getHeight()/panel.height};
            });
            if(point!=null){context.getInput().setCursorPos(point[0],point[1]);context.getInput().pressMouse(0);context.waitTicks(2);return;}
            scroll(context,pass==0?-2:2);
        }
        throw new AssertionError("Production action unreachable: "+id);
    }
    private void scroll(ClientGameTestContext context,int amount){
        double[] point=context.computeOnClient(client->{var screen=client.gui.screen();return new double[]{(screen.width-40d)*client.getWindow().getWidth()/screen.width,(screen.height-55d)*client.getWindow().getHeight()/screen.height};});
        context.getInput().setCursorPos(point[0],point[1]);context.getInput().scroll(amount);context.waitTicks(1);
    }
    private void click(ClientGameTestContext context,String label){
        // Find a visible production widget, scrolling the same body that players use.
        for(int pass=0;pass<2;pass++){
            for(int i=0;i<120;i++){
                if(context.tryClickScreenButton(label))return;
                scroll(context,pass==0?-2:2);
            }
        }
        throw new AssertionError("Production button not reachable: "+label);
    }
    private void shot(ClientGameTestContext context,String name)throws Exception{
        context.getInput().setCursorPos(5,5);
        context.runOnClient(client->{client.gui.toastManager().clear();ClientState.messageUntil=0;ClientState.milestoneUntil=0;});
        context.waitTicks(4);var path=context.takeScreenshot(name);Files.copy(path,output.resolve(name+".png"),StandardCopyOption.REPLACE_EXISTING);ui.add("PASS screenshot "+name);
        context.runOnClient(client->{var s=client.gui.screen();if(s!=null)for(var child:s.children())if(child instanceof AbstractWidget widget&&widget.visible)check(widget.getX()>=0&&widget.getY()>=0&&widget.getRight()<=s.width&&widget.getBottom()<=s.height,"layout."+name+"."+widget.getMessage().getString());});
    }
    private List<String> concat(List<String> list,String last){var copy=new ArrayList<>(list);copy.add(last);return copy;}
    private static final class UUIDBox {java.util.UUID value;}
}

