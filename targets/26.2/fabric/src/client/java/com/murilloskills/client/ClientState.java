package com.murilloskills.client;
import com.murilloskills.data.*;
import com.murilloskills.network.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.SkillSummary;
import java.util.*;

public final class ClientState {
    public static PlayerProfile profile=new PlayerProfile();
    public static final Map<SkillType,SkillSummary> summaries=new EnumMap<>(SkillType.class);
    public static boolean available,needsSelection;
    public static long session,revision,presentationRevision,summaryRevision,serverTick,request;
    private static Protocol.Summaries pendingSummary;
    public static int clientTicks,lastStateAt,messageUntil,xpUntil,markerUntil;
    public static String message="",markerKind="";
    public static SkillType xpSkill,activeSkill;
    public static final Map<SkillType,Long> activeAbilities=new EnumMap<>(SkillType.class);
    public static final Map<String,ActivityView> activities=new LinkedHashMap<>();
    public static final Map<Long,Protocol.Result> replies=new LinkedHashMap<>();
    public static long activeUntil;
    public static double recentXp,recentCombo=1;
    public static int milestoneUntil,milestoneLevel;
    public static SkillType milestoneSkill;
    public static String milestoneTitle="";
    public static List<net.minecraft.core.BlockPos> markers=List.of();
    public record MarkerLayer(List<net.minecraft.core.BlockPos> positions,int until){}
    public static final Map<String,MarkerLayer> layers=new LinkedHashMap<>();
    public static String dimension="",legacyDescription="";
    public static boolean legacyReady;
    public static int legacyReceivedAt;
    private ClientState(){}
    public static boolean ready(){return available&&summaryRevision==presentationRevision&&summaries.size()==SkillType.values().length;}
    public static boolean active(SkillType type){return type!=null&&activeAbilities.getOrDefault(type,0L)>now();}
    public static void accept(Protocol.Summaries p){
        if(!available||p.session()!=session){pendingSummary=p;return;}
        if(p.revision()<summaryRevision)return;
        var incoming=new EnumMap<SkillType,SkillSummary>(SkillType.class);
        var values=ProfileCodec.JSON.fromJson(p.json(),SkillSummary[].class);
        if(values==null)throw new IllegalArgumentException("Catálogo ausente");
        for(var value:values)if(value==null||value.skill()==null||incoming.put(value.skill(),value)!=null)throw new IllegalArgumentException("Catálogo incompleto");
        if(incoming.size()!=SkillType.values().length)throw new IllegalArgumentException("Catálogo incompleto");
        summaries.clear();summaries.putAll(incoming);summaryRevision=p.revision();pendingSummary=null;
    }
    public static void accept(Protocol.State p){
        if(available&&p.session()==session&&p.revision()<revision)return;
        var previous=profile;boolean same=available&&p.session()==session;
        if(!same){request=0;summaries.clear();summaryRevision=-1;markers=List.of();activities.clear();activeAbilities.clear();replies.clear();recentXp=0;WorkspaceMemory.reset();}
        try{profile=ProfileCodec.decode(p.json());}catch(java.io.IOException e){throw new IllegalArgumentException("Snapshot de progressão inválido",e);}
        if(same)for(var type:SkillType.values()){
            int before=previous.stats(type).level,after=profile.stats(type).level;
            if(after/5>before/5){milestoneSkill=type;milestoneLevel=after/5*5;milestoneTitle=MilestoneCatalog.milestones(type).get(milestoneLevel/5-1).title();milestoneUntil=clientTicks+120;
                var mc=net.minecraft.client.Minecraft.getInstance();if(ClientPreferences.current.milestoneSound&&mc.player!=null)mc.player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,.35f,1.15f);}
        }
        if(!same&&profile.selected.isEmpty())needsSelection=true;
        available=true;session=p.session();revision=p.revision();presentationRevision=p.presentationRevision();serverTick=p.serverTick();lastStateAt=clientTicks;
        activeSkill=p.activeSkill();activeUntil=p.activeUntil();activeAbilities.clear();activeAbilities.putAll(p.activeAbilities());
        if(pendingSummary!=null&&pendingSummary.session()==session)accept(pendingSummary);
        if(!p.message().isEmpty())message(p.message());
    }
    public static void accept(Protocol.Result result){
        if(result.session()!=session)return;replies.put(result.request(),result);
        while(replies.size()>32)replies.remove(replies.keySet().iterator().next());
        if(!result.message().isEmpty())message(result.message());
    }
    public static void accept(Protocol.Activity p){
        if(p.session()!=session)return;
        var incoming=ProfileCodec.JSON.fromJson(p.json(),ActivityView[].class);if(incoming==null||incoming.length>32)return;
        for(var value:incoming){
            var previous=activities.get(value.kind());
            boolean fresh=previous==null||previous.until()!=value.until()||!previous.phase().equals(value.phase());
            if(fresh&&ClientPreferences.current.abilitySounds&&(value.phase().equals("start")||value.phase().equals("end")||value.phase().equals("radar")||value.phase().equals("headshot"))){
                var player=net.minecraft.client.Minecraft.getInstance().player;
                if(player!=null)player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,.18f,value.phase().equals("end")?.75f:1.3f);
            }
        }
        activities.clear();for(var value:incoming)activities.put(value.kind(),value);
    }
    public static void message(String text){message=text;messageUntil=clientTicks+80;}
    public static long now(){return serverTick+clientTicks-lastStateAt;}
    public static void reset(){
        available=false;needsSelection=false;session=revision=request=presentationRevision=0;summaryRevision=-1;pendingSummary=null;
        profile=new PlayerProfile();summaries.clear();markers=List.of();layers.clear();activities.clear();activeAbilities.clear();replies.clear();
        dimension="";message="";recentXp=0;recentCombo=1;milestoneUntil=0;activeSkill=null;activeUntil=0;WorkspaceMemory.reset();
    }
    public static long send(Protocol.Op op,String argument,int value){
        if(!available&&op!=Protocol.Op.HELLO)return 0;
        long id=++request;net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new Protocol.Action(session,id,op,argument,value));return id;
    }
    public static void use(SkillType type){send(active(type)?Protocol.Op.CANCEL_ACTIVE:Protocol.Op.ACTIVATE,type.name(),0);}
}

