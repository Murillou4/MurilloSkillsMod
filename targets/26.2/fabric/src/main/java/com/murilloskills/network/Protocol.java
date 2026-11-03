package com.murilloskills.network;
import com.murilloskills.MurilloSkills;
import com.murilloskills.data.*;
import com.murilloskills.skills.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.*;

public final class Protocol {
    private Protocol(){}
    public enum Op { HELLO,SELECT,BRANCH,PARAGON,PRESTIGE,EQUIP,ACTIVATE,TOGGLE,TOOL_CONFIG,FILTER,MINE,MINING_HOLD,PLACE,UNDO,PREVIEW,IMPORT_PREVIEW,IMPORT_CONFIRM,SET_FEATURE,CANCEL_ACTIVE,BRUSH_POINT,SELECT_BATCH,CLEAR_BRUSH }
    public record Summaries(long session,long revision,String json) implements CustomPacketPayload {
        public static final Type<Summaries> TYPE=new Type<>(MurilloSkills.id("v2/summaries"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Summaries> CODEC=new StreamCodec<>(){
            public Summaries decode(RegistryFriendlyByteBuf b){return new Summaries(b.readLong(),b.readLong(),b.readUtf(262144));}
            public void encode(RegistryFriendlyByteBuf b,Summaries p){b.writeLong(p.session);b.writeLong(p.revision);b.writeUtf(p.json,262144);}
        };
        public Type<Summaries> type(){return TYPE;}
    }
    public enum ResultCode { OK,REJECTED,PARTIAL,NOT_SELECTED,LEVEL,COOLDOWN,GLOBAL_COOLDOWN,COMBAT,BUSY,WRONG_TOOL,NO_TARGET,MATERIALS }
    public record Result(long session,long request,Op op,ResultCode code,String message,int affected) implements CustomPacketPayload {
        public static final Type<Result> TYPE=new Type<>(MurilloSkills.id("v2/result"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=new StreamCodec<>(){
            public Result decode(RegistryFriendlyByteBuf b){return new Result(b.readLong(),b.readLong(),b.readEnum(Op.class),b.readEnum(ResultCode.class),b.readUtf(2048),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Result p){b.writeLong(p.session);b.writeLong(p.request);b.writeEnum(p.op);b.writeEnum(p.code);b.writeUtf(p.message,2048);b.writeVarInt(p.affected);}
        };
        public Type<Result> type(){return TYPE;}
    }
    public record Activity(long session,String json) implements CustomPacketPayload {
        public static final Type<Activity> TYPE=new Type<>(MurilloSkills.id("v2/activity"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Activity> CODEC=new StreamCodec<>(){
            public Activity decode(RegistryFriendlyByteBuf b){return new Activity(b.readLong(),b.readUtf(32768));}
            public void encode(RegistryFriendlyByteBuf b,Activity p){b.writeLong(p.session);b.writeUtf(p.json,32768);}
        };
        public Type<Activity> type(){return TYPE;}
    }
    public record Action(long session,long request,Op op,String argument,int value) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(MurilloSkills.id("v2/action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=new StreamCodec<>(){
            public Action decode(RegistryFriendlyByteBuf b){return new Action(b.readLong(),b.readLong(),b.readEnum(Op.class),b.readUtf(4096),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Action p){b.writeLong(p.session);b.writeLong(p.request);b.writeEnum(p.op);b.writeUtf(p.argument,4096);b.writeVarInt(p.value);}
        };
        public Type<Action> type(){return TYPE;}
    }
    public record State(long session,long revision,long presentationRevision,long serverTick,SkillType activeSkill,long activeUntil,Map<SkillType,Long> activeAbilities,String json,String message) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(MurilloSkills.id("v2/state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=new StreamCodec<>(){
            public State decode(RegistryFriendlyByteBuf b){
                long session=b.readLong(),revision=b.readLong(),presentation=b.readLong(),tick=b.readLong();SkillType skill=b.readBoolean()?b.readEnum(SkillType.class):null;long until=b.readLong();
                int n=b.readVarInt();if(n<0||n>8)throw new IllegalArgumentException();Map<SkillType,Long> active=new EnumMap<>(SkillType.class);
                for(int i=0;i<n;i++)active.put(b.readEnum(SkillType.class),b.readLong());
                return new State(session,revision,presentation,tick,skill,until,Map.copyOf(active),b.readUtf(131072),b.readUtf(2048));
            }
            public void encode(RegistryFriendlyByteBuf b,State p){b.writeLong(p.session);b.writeLong(p.revision);b.writeLong(p.presentationRevision);b.writeLong(p.serverTick);b.writeBoolean(p.activeSkill!=null);if(p.activeSkill!=null)b.writeEnum(p.activeSkill);b.writeLong(p.activeUntil);b.writeVarInt(p.activeAbilities.size());p.activeAbilities.forEach((skill,end)->{b.writeEnum(skill);b.writeLong(end);});b.writeUtf(p.json,131072);b.writeUtf(p.message,2048);}
        };
        public Type<State> type(){return TYPE;}
    }
    public record Markers(List<BlockPos> positions,String kind,int ticks) implements CustomPacketPayload {
        public static final Type<Markers> TYPE=new Type<>(MurilloSkills.id("v2/markers"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Markers> CODEC=new StreamCodec<>(){
            public Markers decode(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>512)throw new IllegalArgumentException("Marker limit");List<BlockPos> out=new ArrayList<>();for(int i=0;i<n;i++)out.add(b.readBlockPos());return new Markers(List.copyOf(out),b.readUtf(16),Math.clamp(b.readVarInt(),1,600));}
            public void encode(RegistryFriendlyByteBuf b,Markers p){b.writeVarInt(p.positions.size());for(var pos:p.positions)b.writeBlockPos(pos);b.writeUtf(p.kind,16);b.writeVarInt(p.ticks);}
        };
        public Type<Markers> type(){return TYPE;}
    }
    public record LegacyPreview(String description,boolean ready) implements CustomPacketPayload {
        public static final Type<LegacyPreview> TYPE=new Type<>(MurilloSkills.id("v2/legacy_preview"));
        public static final StreamCodec<RegistryFriendlyByteBuf,LegacyPreview> CODEC=new StreamCodec<>(){
            public LegacyPreview decode(RegistryFriendlyByteBuf b){return new LegacyPreview(b.readUtf(4096),b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,LegacyPreview p){b.writeUtf(p.description,4096);b.writeBoolean(p.ready);}
        };
        public Type<LegacyPreview> type(){return TYPE;}
    }
    public record Xp(SkillType skill,double amount,double combo) implements CustomPacketPayload {
        public static final Type<Xp> TYPE=new Type<>(MurilloSkills.id("v2/xp"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Xp> CODEC=new StreamCodec<>(){
            public Xp decode(RegistryFriendlyByteBuf b){return new Xp(b.readEnum(SkillType.class),b.readDouble(),Math.clamp(b.readDouble(),1,1.1));}
            public void encode(RegistryFriendlyByteBuf b,Xp p){b.writeEnum(p.skill);b.writeDouble(p.amount);b.writeDouble(p.combo);}
        };
        public Type<Xp> type(){return TYPE;}
    }
    public static String snapshot(PlayerProfile p){
        // Exploration/provenance/achievement histories are server-only and never grow network packets.
        var copy=new PlayerProfile();copy.skills=p.skills;copy.selected=p.selected;copy.paragon=p.paragon;copy.equipped=p.equipped;
        copy.seals=p.seals;copy.globalCooldownUntil=p.globalCooldownUntil;copy.contracts=p.contracts;copy.completedSets=p.completedSets;
        copy.hasChosenParagon=p.hasChosenParagon;copy.tools=p.tools;copy.toggles=p.toggles;copy.featureSettings=p.featureSettings;copy.importedLegacy=p.importedLegacy;
        return ProfileCodec.JSON.toJson(copy);
    }
}

