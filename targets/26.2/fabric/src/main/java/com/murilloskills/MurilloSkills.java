package com.murilloskills;
import com.murilloskills.api.*;
import com.murilloskills.config.ModConfig;
import com.murilloskills.data.PlayerStore;
import com.murilloskills.impl.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.network.Wire;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.*;

public final class MurilloSkills implements ModInitializer {
    public static final String ID="murilloskills";
    public static final String VERSION=FabricLoader.getInstance().getModContainer(ID).map(mod->mod.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    public static final Logger LOG=LoggerFactory.getLogger(ID);
    public static PlayerStore store;
    public static Identifier id(String path){return Identifier.fromNamespaceAndPath(ID,path);}
    public void onInitialize(){
        com.murilloskills.data.LegacySource.register();
        SkillRegistry.register(new MinerSkill());SkillRegistry.register(new WarriorSkill());SkillRegistry.register(new ArcherSkill());SkillRegistry.register(new FarmerSkill());
        SkillRegistry.register(new FisherSkill());SkillRegistry.register(new BlacksmithSkill());SkillRegistry.register(new BuilderSkill());SkillRegistry.register(new ExplorerSkill());SkillRegistry.validate();
        Wire.register();GameplayEvents.register();com.murilloskills.commands.SkillCommands.register();
        ServerLifecycleEvents.SERVER_STARTING.register(server->{
            try{store=new PlayerStore(server,ModConfig.load(FabricLoader.getInstance().getConfigDir().resolve("murilloskills-v2.json")));}
            catch(java.io.IOException e){throw new IllegalStateException("Configuração v2 preservada; inicialização interrompida.",e);}
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{if(store!=null)store.flush();});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->store=null);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var player=handler.getPlayer();var p=store.get(player);var runtime=store.runtime(player);
            runtime.session=new java.security.SecureRandom().nextLong();runtime.lastRequest=0;
            com.murilloskills.skills.ContractService.ensure(p);store.dirty(player);GameplayEvents.refresh(new SkillContext(player,store));Wire.send(player,"Bem-vindo à nova jornada.");
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{if(store!=null)store.disconnect(handler.getPlayer());});
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->{
            store.runtime(newPlayer).activeUntil.clear();GameplayEvents.refresh(new SkillContext(newPlayer,store));Wire.send(newPlayer,"");
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(store==null)return;
            for(var player:server.getPlayerList().getPlayers())GameplayEvents.tick(new SkillContext(player,store));
            if(server.getTickCount()%(store.config.autosaveSeconds*20)==0)store.flush();
        });
        LOG.info("MurilloSkills {}: oito profissões nativas, Fabric 26.2.",VERSION);
    }
}

