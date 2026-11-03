package com.murilloskills.data;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.murilloskills.*;
import com.murilloskills.api.SkillContext;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.io.IOException;

/** Opaque legacy attachment is preserved, never used as live v2 progression. */
public final class LegacySource {
    public static final AttachmentType<Dynamic<?>> ATTACHMENT=AttachmentRegistry.create(
        MurilloSkills.id("player_skills"),builder->builder.persistent(Codec.PASSTHROUGH).copyOnDeath());
    public record Snapshot(Path file,String json){}
    private LegacySource(){}
    public static void register(){}
    public static String read(Path file)throws IOException {
        if(file.toString().endsWith(".json"))return Files.readString(file);
        var root=NbtIo.readCompressed(file,NbtAccounter.create(16*1024*1024));
        var legacy=findAttachment(root,0);
        if(legacy==null)throw new IOException("Sem progressão MurilloSkills neste NBT.");
        return NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE,legacy).toString();
    }
    private static CompoundTag findAttachment(CompoundTag tag,int depth){
        if(tag.get("murilloskills:player_skills") instanceof CompoundTag data)return data;
        if(depth>=6)return null;
        for(var entry:tag.entrySet())if(entry.getValue() instanceof CompoundTag child){var found=findAttachment(child,depth+1);if(found!=null)return found;}
        return null;
    }
    public static Snapshot locate(SkillContext c)throws IOException {
        var base=c.store().directory.getParent();String id=c.player().getUUID().toString();
        for(Path candidate:java.util.List.of(base.resolve("players/"+id+".json"),base.resolve("legacy/"+id+".json"),base.resolve("legacy/"+id+".dat")))
            if(Files.isRegularFile(candidate))return new Snapshot(candidate,read(candidate));
        Dynamic<?> attached=c.player().getAttached(ATTACHMENT);
        if(attached!=null){
            String json=attached.convert(JsonOps.INSTANCE).getValue().toString();
            Path archive=base.resolve("legacy/"+id+".json");ProfileCodec.atomicWrite(archive,json);
            return new Snapshot(archive,json);
        }
        for(Path candidate:java.util.List.of(c.store().server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat"),
            c.store().server.getWorldPath(LevelResource.ROOT).resolve("playerdata/"+id+".dat")))
            if(Files.isRegularFile(candidate))try{return new Snapshot(candidate,read(candidate));}catch(IOException ignored){}
        throw new IOException("Nenhuma progressão legada encontrada.");
    }
}
