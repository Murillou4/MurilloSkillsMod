package com.murilloskills.data;
import com.murilloskills.config.ModConfig;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** One authoritative JSON store. Runtime caches are cleared on disconnect/stop. */
public final class PlayerStore {
    public final MinecraftServer server;
    public final ModConfig config;
    public final Path directory;
    private final Map<UUID,PlayerProfile> profiles=new HashMap<>();
    private final Map<UUID,RuntimeState> runtime=new HashMap<>();
    private final Set<UUID> dirty=new HashSet<>();
    private final Set<String> placed=new HashSet<>(), paidPlacements=new HashSet<>();
    private boolean worldDirty;
    public PlayerStore(MinecraftServer server,ModConfig config){
        this.server=server;this.config=config;
        directory=server.getWorldPath(LevelResource.ROOT).resolve("murilloskills/v2");
        Path world=directory.resolve("provenance.json");
        if(Files.exists(world))try{
            var data=ProfileCodec.JSON.fromJson(Files.readString(world),Provenance.class);
            if(data==null||data.placed==null||data.paid==null)throw new IOException("Proveniência inválida");
            placed.addAll(data.placed);paidPlacements.addAll(data.paid);
        }catch(Exception e){throw new IllegalStateException("Não foi possível ler "+world+"; arquivo preservado.",e);}
    }
    public long now(){return server.overworld().getGameTime();}
    public Path path(UUID id){return directory.resolve("players").resolve(id+".json");}
    public PlayerProfile get(ServerPlayer player){return get(player.getUUID());}
    public PlayerProfile get(UUID id){return profiles.computeIfAbsent(id,key->{
        Path file=path(key);if(!Files.exists(file))return new PlayerProfile();
        try{return ProfileCodec.read(file);}catch(IOException e){throw new IllegalStateException("Progressão não foi substituída: "+file,e);}
    });}
    public RuntimeState runtime(ServerPlayer player){return runtime.computeIfAbsent(player.getUUID(),id->new RuntimeState());}
    public void dirty(ServerPlayer player){dirty.add(player.getUUID());}
    public void replace(ServerPlayer player,PlayerProfile profile){profiles.put(player.getUUID(),profile);dirty(player);}
    public void save(ServerPlayer player){dirty.add(player.getUUID());save(player.getUUID());}
    private void save(UUID id){
        if(!dirty.contains(id)||!profiles.containsKey(id))return;
        try{ProfileCodec.write(path(id),profiles.get(id));dirty.remove(id);}catch(IOException e){throw new IllegalStateException("Falha ao salvar progressão "+id,e);}
    }
    public void flush(){for(var id:List.copyOf(dirty))save(id);if(worldDirty)try{ProfileCodec.atomicWrite(directory.resolve("provenance.json"),ProfileCodec.JSON.toJson(new Provenance(placed,paidPlacements)));worldDirty=false;}catch(IOException e){throw new IllegalStateException(e);}}
    public void disconnect(ServerPlayer player){save(player);profiles.remove(player.getUUID());runtime.remove(player.getUUID());}
    public String key(ServerPlayer p,BlockPos pos){return p.level().dimension().identifier()+":"+pos.asLong();}
    public boolean natural(ServerPlayer p,BlockPos pos){return !placed.contains(key(p,pos));}
    public boolean placed(ServerPlayer p,BlockPos pos){String key=key(p,pos);placed.add(key);worldDirty=true;return paidPlacements.add(key);}
    public void removed(ServerPlayer p,BlockPos pos){if(placed.remove(key(p,pos)))worldDirty=true;}
    private record Provenance(Set<String> placed,Set<String> paid){}
}

