package com.murilloskills.network;
import com.murilloskills.*;
import com.murilloskills.api.*;
import com.murilloskills.data.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.skills.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class Wire {
    private static final Set<String> TOGGLES=Set.of("night_vision","auto_torch","step_assist","replant","area_planting","pathfinder","melting");
    private Wire(){}
    public static void register(){
        PayloadTypeRegistry.serverboundPlay().register(Protocol.Action.TYPE,Protocol.Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Protocol.State.TYPE,Protocol.State.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Protocol.Summaries.TYPE,Protocol.Summaries.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Protocol.Result.TYPE,Protocol.Result.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Protocol.Activity.TYPE,Protocol.Activity.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Protocol.Markers.TYPE,Protocol.Markers.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Protocol.Xp.TYPE,Protocol.Xp.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Protocol.LegacyPreview.TYPE,Protocol.LegacyPreview.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Protocol.Action.TYPE,(p,c)->c.server().execute(()->handle(c.player(),p)));
    }
    public static void handle(ServerPlayer player,Protocol.Action action){
        if(MurilloSkills.store==null||!player.isAlive())return;
        var c=new SkillContext(player,MurilloSkills.store);var p=c.profile();var run=c.runtime();
        int window=(int)(c.now()/20);if(run.packetWindow!=window){run.packetWindow=window;run.packetCount=0;}
        if(++run.packetCount>30)return;
        if(action.op()==Protocol.Op.HELLO){run.summarySignature="";send(player,"");return;}
        if(action.session()!=run.session||action.request()<=run.lastRequest)return;
        run.lastRequest=action.request();String message="";Protocol.ResultCode result=Protocol.ResultCode.OK;int affected=0;
        try{
            switch(action.op()){
                case SELECT_BATCH -> {
                    var requested=Arrays.stream(action.argument().split(",",-1)).map(SkillType::valueOf).toList();
                    message=InitialSelection.apply(p,requested,run.locked(c.now()));
                }
                case SELECT -> {
                    String[] parts=action.argument().split(",",-1);var incoming=SkillType.valueOf(parts[0]);var outgoing=parts.length==2&&!parts[1].isEmpty()?SkillType.valueOf(parts[1]):null;
                    message=ProgressionService.select(p,incoming,outgoing,run.locked(c.now()));
                    if(message.isEmpty()&&outgoing!=null)ContractService.reassign(p,outgoing,incoming);
                    if(message.isEmpty()&&outgoing==null&&p.completedSets==0&&p.contracts.stream().allMatch(contract->contract.progress==0))p.contracts.clear();
                }
                case BRANCH -> message=ProgressionService.branch(p,SkillType.valueOf(action.argument()),action.value(),run.locked(c.now()));
                case PARAGON -> message=ProgressionService.paragon(p,SkillType.valueOf(action.argument()),run.locked(c.now()));
                case PRESTIGE -> message=ProgressionService.prestige(p,SkillType.valueOf(action.argument()),run.locked(c.now()));
                case EQUIP -> {var skill=SkillType.valueOf(action.argument());if(p.selected(skill))p.equipped=skill;else {result=Protocol.ResultCode.NOT_SELECTED;message="Escolha essa profissão primeiro.";}}
                case ACTIVATE -> {
                    var skill=action.argument().isEmpty()?p.equipped:SkillType.valueOf(action.argument());if(skill==null)message="Equipe uma habilidade.";
                    else if(player.isSpectator()){result=Protocol.ResultCode.REJECTED;message="Habilidades não podem ser usadas no modo espectador.";}
                    else if(!p.selected(skill)){result=Protocol.ResultCode.NOT_SELECTED;message="Escolha "+skill.title+" primeiro.";}
                    else if(!p.unlocked(skill,20)){result=Protocol.ResultCode.LEVEL;message=skill.ability+" libera no nível 20.";}
                    else if(p.stats(skill).cooldownUntil>c.now()){result=Protocol.ResultCode.COOLDOWN;message="Recarga: "+((p.stats(skill).cooldownUntil-c.now()+19)/20)+" s.";}
                    else if(p.globalCooldownUntil>c.now()){result=Protocol.ResultCode.GLOBAL_COOLDOWN;message="Aguarde "+((p.globalCooldownUntil-c.now()+19)/20)+" s entre ativações.";}
                    else if(ProgressionService.activate(p,skill,c.now(),SkillRegistry.get(skill).cooldown(c))){
                        SkillRegistry.get(skill).activate(c);message=skill.ability+" ativada.";
                        notice(c,"ability:"+skill.name(),"start",skill.ability,c.stats(skill).activeTier()==5?"Domínio"+(c.paragon(skill)?" · Paragon":""):"Patamar "+c.stats(skill).activeTier(),0,60);
                    }
                }
                case TOGGLE,SET_FEATURE -> {
                    var feature=com.murilloskills.skills.effects.Feature.find(action.argument()).orElseThrow();
                    if(!feature.available(p)){result=Protocol.ResultCode.LEVEL;message="Esta utilidade ainda não foi desbloqueada.";break;}
                    boolean enabled=action.op()==Protocol.Op.TOGGLE?!feature.enabled(p):action.value()==1;
                    p.featureSettings.put(feature.id,enabled);
                    if(!enabled)com.murilloskills.utils.OwnedEffects.clearFeature(c,feature);
                    message=feature.title+": "+(enabled?"ligado":"desligado");
                }
                case CANCEL_ACTIVE -> {
                    var skill=action.argument().isEmpty()?p.equipped:SkillType.valueOf(action.argument());
                    if(skill!=null&&c.active(skill)){SkillRegistry.get(skill).cancel(c);message=skill.ability+" encerrada. A recarga continua.";
                        notice(c,"ability:"+skill.name(),"end",skill.ability,"Encerrada · a recarga continua",0,60);}
                    else {result=Protocol.ResultCode.NO_TARGET;message="Essa habilidade não está em uso.";}
                }
                case BRUSH_POINT -> message=com.murilloskills.utils.BrushTools.mark(c);
                case CLEAR_BRUSH -> {if(p.selected(SkillType.BUILDER)){com.murilloskills.utils.BrushTools.clear(c);message="Marcações do Pincel limpas.";}else {result=Protocol.ResultCode.NOT_SELECTED;message="Escolha Construtor primeiro.";}}
                case TOOL_CONFIG -> configure(p,action.argument(),action.value());
                case MINING_HOLD -> {
                    boolean requested=action.value()==1;run.miningHeld=requested&&p.unlocked(SkillType.MINER,15);
                    if(requested&&!run.miningHeld&&c.now()>=run.lastMessageTick+20){
                        message=p.selected(SkillType.MINER)?"Ultmine libera no nível 15 de Minerador.":"Escolha a profissão Minerador para usar Ultmine.";run.lastMessageTick=c.now();
                        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(message));
                    }
                }
                case FILTER -> filter(p,action.argument(),action.value());
                case PREVIEW -> {if(c.now()>=run.nextPreviewTick){run.nextPreviewTick=c.now()+10;AreaTools.preview(c,action.value()==1);}}
                case MINE -> {
                    var list=AreaTools.preview(c,false);String reason=AreaTools.blocked(c,false,list);
                    if(!reason.isEmpty()){result=AreaTools.failureCode(c,false,list);message=reason;}else {affected=AreaTools.mine(c,list);message=affected>0?"Ultmine: "+affected+" blocos minerados.":"A mineração foi recusada pelas regras do mundo.";if(affected<list.size())result=affected==0?Protocol.ResultCode.REJECTED:Protocol.ResultCode.PARTIAL;}
                }
                case PLACE -> {
                    if(com.murilloskills.utils.BrushTools.ready(c)){var brush=com.murilloskills.utils.BrushTools.executeResult(c);result=brush.code();message=brush.message();affected=brush.affected();}
                    else {var list=AreaTools.preview(c,true);String reason=AreaTools.blocked(c,true,list);
                        if(!reason.isEmpty()){result=AreaTools.failureCode(c,true,list);message=reason;}
                        else {affected=AreaTools.place(c,list);message=affected>0?"UltPlace: "+affected+" blocos colocados"+(affected<list.size()?" · colocação parcial.":"."):"Nenhum bloco pôde ser colocado; seu desfazer anterior foi mantido.";if(affected<list.size())result=affected==0?Protocol.ResultCode.REJECTED:Protocol.ResultCode.PARTIAL;}
                    }
                }
                case UNDO -> {if(run.combatUntil>c.now()){result=Protocol.ResultCode.COMBAT;message="Saia do combate para desfazer.";}else {affected=AreaTools.undo(c);message=affected>0?"Desfeitos: "+affected+" blocos.":"Não há colocação válida para desfazer; os blocos podem ter mudado ou o prazo acabou.";if(affected==0)result=Protocol.ResultCode.NO_TARGET;}}
                case IMPORT_PREVIEW -> {
                    String preview=com.murilloskills.commands.SkillCommands.preview(c);
                    ServerPlayNetworking.send(player,new Protocol.LegacyPreview(preview,c.runtime().importPreviewUntil>c.now()));
                }
                case IMPORT_CONFIRM -> message=com.murilloskills.commands.SkillCommands.importLegacy(c);
                default -> {}
            }
        }catch(IllegalArgumentException|IndexOutOfBoundsException e){message="Ação inválida.";result=Protocol.ResultCode.REJECTED;}
        if(!message.isEmpty()&&result==Protocol.ResultCode.OK&&switch(action.op()){case SELECT,SELECT_BATCH,BRANCH,PARAGON,PRESTIGE->true;default->false;})result=Protocol.ResultCode.REJECTED;
        ContractService.ensure(p);GameplayEvents.refresh(c);c.store().dirty(player);send(player,message);
        if(ServerPlayNetworking.canSend(player,Protocol.Result.TYPE))ServerPlayNetworking.send(player,new Protocol.Result(run.session,action.request(),action.op(),result,message,affected));
    }
    private static void configure(PlayerProfile p,String key,int value){
        switch(key){
            case "mining" -> p.tools.miningShape=ShapePlanner.MINING.get(Math.floorMod(value,ShapePlanner.MINING.size()));
            case "placement" -> p.tools.placementShape=ShapePlanner.PLACEMENT.get(Math.floorMod(value,ShapePlanner.PLACEMENT.size()));
            case "size" -> p.tools.size=Math.clamp(value,1,20);
            case "depth" -> p.tools.depth=Math.clamp(value,1,20);
            case "spacing" -> p.tools.spacing=Math.clamp(value,1,4);
            case "inventory" -> p.tools.inventoryDrops=value==1;
            case "direct_xp" -> p.tools.directXp=value==1;
            case "ores" -> p.tools.oresOnly=value==1;
            case "hollow" -> p.tools.hollow=value==1;
            default -> throw new IllegalArgumentException();
        }
    }
    private static void filter(PlayerProfile p,String argument,int value){
        String[] fields=argument.split("\\|",2);if(fields.length!=2)throw new IllegalArgumentException();
        Set<String> set=switch(fields[0]){case "ore"->p.tools.oreFilter;case "classic"->p.tools.classicFilter;case "trash"->p.tools.trash;default->throw new IllegalArgumentException();};
        var id=Identifier.tryParse(fields[1]);if(id==null||!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id))throw new IllegalArgumentException();
        if(value==0)set.remove(id.toString());else if(set.size()<512)set.add(id.toString());
    }
    public static void send(ServerPlayer player,String message){
        if(!ServerPlayNetworking.canSend(player,Protocol.State.TYPE))return;
        var c=new SkillContext(player,MurilloSkills.store);var run=c.runtime();
        String summaries=new com.google.gson.Gson().toJson(com.murilloskills.skills.effects.SummaryService.build(c));boolean changed=!summaries.equals(run.summarySignature);
        if(changed){run.summarySignature=summaries;run.summaryRevision++;}
        var active=run.activeUntil.entrySet().stream().filter(e->e.getKey()==c.profile().equipped&&e.getValue()>c.now()&&c.profile().selected(e.getKey())).findFirst().orElse(null);
        var activeAbilities=new EnumMap<SkillType,Long>(SkillType.class);run.activeUntil.forEach((skill,end)->{if(end>c.now()&&c.profile().selected(skill))activeAbilities.put(skill,end);});
        ServerPlayNetworking.send(player,new Protocol.State(run.session,++run.lastStateRevision,run.summaryRevision,c.now(),active==null?null:active.getKey(),active==null?0:active.getValue(),activeAbilities,Protocol.snapshot(c.profile()),message));
        if(changed&&ServerPlayNetworking.canSend(player,Protocol.Summaries.TYPE)){
            ServerPlayNetworking.send(player,new Protocol.Summaries(run.session,run.summaryRevision,summaries));
        }
        run.activities.entrySet().removeIf(e->e.getValue().until()<c.now());
        if(ServerPlayNetworking.canSend(player,Protocol.Activity.TYPE))ServerPlayNetworking.send(player,new Protocol.Activity(run.session,ProfileCodec.JSON.toJson(run.activities.values())));
    }
    public static void notice(SkillContext c,String kind,String phase,String title,String detail,int affected,int ticks){
        c.runtime().activities.put(kind,new ActivityView(kind,phase,title,detail,0,0,affected,c.now()+ticks));
        if(ServerPlayNetworking.canSend(c.player(),Protocol.Activity.TYPE))ServerPlayNetworking.send(c.player(),new Protocol.Activity(c.runtime().session,ProfileCodec.JSON.toJson(c.runtime().activities.values())));
    }
    public static void heal(SkillContext c,String title,float amount){
        float before=c.player().getHealth();c.player().heal(amount);float recovered=c.player().getHealth()-before;
        if(recovered>0)notice(c,"heal","heal",title,"+"+com.murilloskills.skills.effects.EffectDefinition.format(com.murilloskills.skills.effects.EffectDefinition.Unit.HEARTS,recovered)+" recuperado"+(recovered<=2?"":"s"),0,40);
    }
    public static void markers(ServerPlayer p,List<BlockPos> list,String kind,int ticks){if(ServerPlayNetworking.canSend(p,Protocol.Markers.TYPE))ServerPlayNetworking.send(p,new Protocol.Markers(list.stream().limit(512).map(BlockPos::immutable).toList(),kind,ticks));}
    public static void xp(ServerPlayer p,SkillType skill,double amount,double combo){if(ServerPlayNetworking.canSend(p,Protocol.Xp.TYPE))ServerPlayNetworking.send(p,new Protocol.Xp(skill,amount,combo));}
}

