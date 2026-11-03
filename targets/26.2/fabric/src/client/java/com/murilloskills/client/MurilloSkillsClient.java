package com.murilloskills.client;
import com.murilloskills.MurilloSkills;
import com.murilloskills.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
public final class MurilloSkillsClient implements ClientModInitializer {
    public static KeyMapping guide,ability,radial,tools,mine,mineMenu,place,undo;
    public static final java.util.Map<com.murilloskills.skills.effects.Feature,KeyMapping> featureKeys=new java.util.EnumMap<>(com.murilloskills.skills.effects.Feature.class);
    public static final KeyMapping[] directAbilities=new KeyMapping[4];
    public static boolean previewPlace;
    private static int selectionIdleTicks;
    private static KeyMapping key(String id,int value,KeyMapping.Category category){return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.murilloskills."+id,InputConstants.Type.KEYSYM,value,category));}
    public void onInitializeClient(){
        ClientPreferences.load();
        ClientPlayNetworking.registerGlobalReceiver(Protocol.LegacyPreview.TYPE,(p,c)->c.client().execute(()->{ClientState.legacyDescription=p.description();ClientState.legacyReady=p.ready();ClientState.legacyReceivedAt=ClientState.clientTicks;}));
        var category=KeyMapping.Category.register(MurilloSkills.id("skills"));
        guide=key("guide",GLFW.GLFW_KEY_O,category);ability=key("ability",GLFW.GLFW_KEY_R,category);radial=key("radial",GLFW.GLFW_KEY_V,category);
        tools=key("tools",GLFW.GLFW_KEY_G,category);mine=key("mine",GLFW.GLFW_KEY_PERIOD,category);mineMenu=key("ultmine_menu",GLFW.GLFW_KEY_APOSTROPHE,category);place=key("place",GLFW.GLFW_KEY_B,category);undo=key("undo",GLFW.GLFW_KEY_U,category);
        for(var feature:com.murilloskills.skills.effects.Feature.values())featureKeys.put(feature,key("toggle."+feature.id,GLFW.GLFW_KEY_UNKNOWN,category));
        for(int i=0;i<4;i++)directAbilities[i]=key("active_"+(i+1),GLFW.GLFW_KEY_UNKNOWN,category);
        // This runs before vanilla sends START_DESTROY_BLOCK, including an instant creative break.
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player,world,hand,pos,face)->{
            if(world.isClientSide()&&player==Minecraft.getInstance().player)MiningInput.sync(Minecraft.getInstance(),true);
            return net.minecraft.world.InteractionResult.PASS;
        });
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(!world.isClientSide()||player!=Minecraft.getInstance().player||hand!=net.minecraft.world.InteractionHand.MAIN_HAND||!ClientState.available||Minecraft.getInstance().gui.screen()!=null)return net.minecraft.world.InteractionResult.PASS;
            if(ClientState.activeSkill==com.murilloskills.skills.SkillType.BUILDER&&ClientState.activeUntil>ClientState.now()&&player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem){
                ClientState.send(Protocol.Op.BRUSH_POINT,"",0);return net.minecraft.world.InteractionResult.SUCCESS;
            }
            if(PlacementInput.armed()){ClientState.send(Protocol.Op.PLACE,"",0);return net.minecraft.world.InteractionResult.SUCCESS;}
            return net.minecraft.world.InteractionResult.PASS;
        });
        ClientPlayNetworking.registerGlobalReceiver(Protocol.State.TYPE,(p,c)->c.client().execute(()->ClientState.accept(p)));
        ClientPlayNetworking.registerGlobalReceiver(Protocol.Summaries.TYPE,(p,c)->c.client().execute(()->ClientState.accept(p)));
        ClientPlayNetworking.registerGlobalReceiver(Protocol.Result.TYPE,(p,c)->c.client().execute(()->ClientState.accept(p)));
        ClientPlayNetworking.registerGlobalReceiver(Protocol.Activity.TYPE,(p,c)->c.client().execute(()->ClientState.accept(p)));
        ClientPlayNetworking.registerGlobalReceiver(Protocol.Markers.TYPE,(p,c)->c.client().execute(()->{ClientState.markers=p.positions();ClientState.markerKind=p.kind();ClientState.markerUntil=ClientState.clientTicks+p.ticks();ClientState.layers.put(p.kind(),new ClientState.MarkerLayer(p.positions(),ClientState.markerUntil));}));
        ClientPlayNetworking.registerGlobalReceiver(Protocol.Xp.TYPE,(p,c)->c.client().execute(()->{ClientState.recentXp=ClientState.xpSkill==p.skill()&&ClientState.xpUntil>ClientState.clientTicks?ClientState.recentXp+p.amount():p.amount();ClientState.xpSkill=p.skill();ClientState.recentCombo=p.combo();ClientState.xpUntil=ClientState.clientTicks+60;}));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{ClientState.reset();MiningInput.reset();PlacementInput.reset();});
        ClientTickEvents.START_CLIENT_TICK.register(MurilloSkillsClient::inputs);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            ClientState.clientTicks++;
            if(client.player==null||client.level==null){MiningInput.reset();PlacementInput.reset();return;}
            String dimension=client.level.dimension().identifier().toString();
            if(!dimension.equals(ClientState.dimension)){ClientState.dimension=dimension;ClientState.layers.clear();ClientState.markers=java.util.List.of();}
            ClientState.layers.entrySet().removeIf(entry->entry.getValue().until()<=ClientState.clientTicks);
            if(!ClientState.available)return;
            if(!ClientState.profile.selected.isEmpty()){ClientState.needsSelection=false;selectionIdleTicks=0;}
            if(ClientState.needsSelection&&ClientState.ready()&&client.gui.screen()==null){
                if(++selectionIdleTicks>=40){ClientState.needsSelection=false;selectionIdleTicks=0;client.gui.setScreen(new ProfessionScreen());}
            }else selectionIdleTicks=0;
            MiningInput.sync(client,false);
            if(client.gui.screen()==null){
                if(ClientState.clientTicks%10==0){if(MiningInput.held())ClientState.send(Protocol.Op.PREVIEW,"",0);else if(PlacementInput.armed()||place.isDown())ClientState.send(Protocol.Op.PREVIEW,"",1);}
            }else{
                for(var mapping:featureKeys.values())while(mapping.consumeClick()){}
                for(var mapping:directAbilities)while(mapping.consumeClick()){}
                for(var mapping:new KeyMapping[]{guide,ability,radial,tools,place,undo})while(mapping.consumeClick()){}
            }
        });
        HudElementRegistry.addLast(MurilloSkills.id("progress"),SkillsHud::extract);
        LevelExtractionEvents.END_EXTRACTION.register(context->MarkerRenderer.extract());
    }
    /** Native bindings (including 26.2's friends key O) must not win before our end-of-tick handler. */
    public static void drainConflicts(KeyMapping used){
        var client=Minecraft.getInstance();var key=KeyMappingHelper.getBoundKeyOf(used);
        for(var mapping:client.options.keyMappings)if(mapping!=used&&!mapping.getName().startsWith("key.murilloskills.")&&KeyMappingHelper.getBoundKeyOf(mapping).equals(key))while(mapping.consumeClick()){}
    }
    private static boolean pressed(KeyMapping mapping){
        boolean result=false;while(mapping.consumeClick())result=true;
        if(result)drainConflicts(mapping);return result;
    }
    private static void inputs(Minecraft client){
        if(!ClientState.available||client.player==null||client.level==null||client.gui.screen()!=null)return;
        if(pressed(guide)){client.gui.setScreen(new ProfessionScreen());return;}
        if(pressed(tools)){client.gui.setScreen(ProfessionScreen.tools(false));return;}
        if(pressed(radial)){client.gui.setScreen(new AbilityRadialScreen());return;}
        if(pressed(mineMenu)){client.gui.setScreen(new UltmineRadialScreen());return;}
        MiningInput.sync(client,false);
        if(pressed(ability))activateEquipped();
        if(pressed(place)){previewPlace=true;PlacementInput.press();}
        if(pressed(undo))ClientState.send(Protocol.Op.UNDO,"",0);
        for(var entry:featureKeys.entrySet())if(pressed(entry.getValue())){
            var feature=entry.getKey();ClientState.send(Protocol.Op.SET_FEATURE,feature.id,feature.enabled(ClientState.profile)?0:1);
        }
        for(int i=0;i<4;i++)if(pressed(directAbilities[i])&&i<ClientState.profile.selected.size()){
            var skill=ClientState.profile.selected.get(i);ClientState.send(Protocol.Op.EQUIP,skill.name(),0);
            ClientState.send(ClientState.active(skill)?Protocol.Op.CANCEL_ACTIVE:Protocol.Op.ACTIVATE,"",0);
        }
    }
    public static boolean dispatchKey(net.minecraft.client.input.KeyEvent key,boolean act){
        var client=Minecraft.getInstance();
        if(client.gui.screen() instanceof ProfessionScreen panel&&guide.matches(key)){if(act)panel.onClose();return true;}
        if(client.gui.screen()!=null)return false;
        if(guide.matches(key)){if(act)client.gui.setScreen(new ProfessionScreen());return true;}
        if(tools.matches(key)){if(act)client.gui.setScreen(ProfessionScreen.tools(false));return true;}
        if(radial.matches(key)){if(act)client.gui.setScreen(new AbilityRadialScreen());return true;}
        if(mineMenu.matches(key)){if(act)client.gui.setScreen(new UltmineRadialScreen());return true;}
        if(ability.matches(key)){if(act)activateEquipped();return true;}
        if(place.matches(key)){if(act)PlacementInput.press();return true;}
        if(undo.matches(key)){if(act)ClientState.send(Protocol.Op.UNDO,"",0);return true;}
        for(var entry:featureKeys.entrySet())if(entry.getValue().matches(key)){
            if(act){var feature=entry.getKey();ClientState.send(Protocol.Op.SET_FEATURE,feature.id,feature.enabled(ClientState.profile)?0:1);}return true;
        }
        for(int i=0;i<4;i++)if(directAbilities[i].matches(key)){
            if(act&&i<ClientState.profile.selected.size()){var skill=ClientState.profile.selected.get(i);ClientState.send(Protocol.Op.EQUIP,skill.name(),0);ClientState.send(ClientState.active(skill)?Protocol.Op.CANCEL_ACTIVE:Protocol.Op.ACTIVATE,"",0);}return true;
        }
        return false;
    }
    public static void activateEquipped(){ClientState.send(ClientState.active(ClientState.profile.equipped)?Protocol.Op.CANCEL_ACTIVE:Protocol.Op.ACTIVATE,"",0);}
}

