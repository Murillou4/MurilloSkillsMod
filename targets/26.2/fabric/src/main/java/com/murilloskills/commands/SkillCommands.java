package com.murilloskills.commands;
import com.murilloskills.*;
import com.murilloskills.api.*;
import com.murilloskills.data.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.network.Wire;
import com.murilloskills.skills.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import com.mojang.brigadier.arguments.*;
import java.nio.file.*;

public final class SkillCommands {
    private SkillCommands(){}
    public static void register(){CommandRegistrationCallback.EVENT.register((dispatcher,access,env)->{
        dispatcher.register(Commands.literal("murilloskills")
            .executes(cmd->{var p=cmd.getSource().getPlayerOrException();Wire.send(p,"Abra o Guia de profissões pelo atalho definido em Opções → Controles → MurilloSkills.");return 1;})
            .then(Commands.literal("importlegacy").executes(cmd->{var c=GameplayEvents.context(cmd.getSource().getPlayerOrException());c.message(preview(c));return 1;})
                .then(Commands.literal("confirm").executes(cmd->{var c=GameplayEvents.context(cmd.getSource().getPlayerOrException());c.message(importLegacy(c));return 1;})))
            .then(Commands.literal("save").requires(s->s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).executes(cmd->{MurilloSkills.store.flush();return 1;}))
            .then(Commands.literal("admin").requires(s->s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("maxselected").executes(cmd->maxSelected(cmd.getSource(),cmd.getSource().getPlayerOrException())))
                .then(Commands.argument("player",EntityArgument.player())
                    .then(Commands.literal("maxselected").executes(cmd->maxSelected(cmd.getSource(),EntityArgument.getPlayer(cmd,"player"))))
                    .then(Commands.literal("level").then(Commands.argument("skill",StringArgumentType.word()).then(Commands.argument("value",IntegerArgumentType.integer(0,100)).executes(cmd->{
                        var player=EntityArgument.getPlayer(cmd,"player");SkillType type;
                        try{type=SkillType.valueOf(StringArgumentType.getString(cmd,"skill").toUpperCase(java.util.Locale.ROOT));}catch(IllegalArgumentException e){cmd.getSource().sendFailure(Component.literal("Skill inválida."));return 0;}
                        var c=GameplayEvents.context(player);var s=c.stats(type);s.level=IntegerArgumentType.getInteger(cmd,"value");s.highestLevel=Math.max(s.highestLevel,s.level);s.claimedMilestone=Math.max(s.claimedMilestone,s.level/5*5);s.xp=0;GameplayEvents.refresh(c);c.store().save(player);Wire.send(player,"Nível ajustado pelo administrador.");return 1;
                    }))))
                    .then(Commands.literal("seals").then(Commands.argument("value",IntegerArgumentType.integer(0,100000)).executes(cmd->{
                        var player=EntityArgument.getPlayer(cmd,"player");MurilloSkills.store.get(player).seals=IntegerArgumentType.getInteger(cmd,"value");MurilloSkills.store.save(player);Wire.send(player,"Selos ajustados pelo administrador.");return 1;
                    })))
                )
            )
        );
    });}
    private static int maxSelected(CommandSourceStack source,net.minecraft.server.level.ServerPlayer player){
        var c=GameplayEvents.context(player);var profile=c.profile();
        if(profile.selected.isEmpty()){source.sendFailure(Component.literal("Esse jogador ainda não escolheu nenhuma profissão."));return 0;}
        for(var type:profile.selected){var stats=profile.stats(type);stats.level=100;stats.highestLevel=100;stats.claimedMilestone=100;stats.xp=0;}
        GameplayEvents.refresh(c);c.store().save(player);
        int count=profile.selected.size();
        Wire.send(player,"Suas "+count+" profissões escolhidas foram ajustadas ao nível 100 pelo administrador.");
        source.sendSuccess(()->Component.literal(count+" profissões de "+player.getName().getString()+" ajustadas ao nível 100."),true);
        return count;
    }
    public static String preview(SkillContext c){
        try{
            c.runtime().importPreviewUntil=0;c.runtime().legacyPreview=null;
            if(!c.profile().fresh()||c.profile().importedLegacy)return "A importação só está disponível antes de evoluir nesta jornada.";
            var snapshot=LegacySource.locate(c);var p=LegacyImporter.convert(snapshot.json());
            c.runtime().legacyPreview=snapshot;
            c.runtime().importPreviewUntil=c.now()+600;
            return "Prévia: "+p.selected.stream().map(t->t.title+" "+p.stats(t).level+" (P"+p.stats(t).prestige+")").toList()+". Confirmar em até 30 s; os arquivos antigos serão preservados.";
        }catch(Exception e){return "Não foi encontrado um JSON legado válido para este personagem.";}
    }
    public static String importLegacy(SkillContext c){
        if(c.runtime().importPreviewUntil<=c.now()||c.runtime().locked(c.now()))return "Abra a prévia e confirme fora de combate.";
        c.runtime().importPreviewUntil=0;var snapshot=c.runtime().legacyPreview;c.runtime().legacyPreview=null;
        if(snapshot==null)return "Abra uma nova prévia antes de importar.";
        try{
            var p=LegacyImporter.importFile(snapshot.file(),c.store().path(c.player().getUUID()),c.profile(),snapshot.json());c.store().replace(c.player(),p);GameplayEvents.refresh(c);Wire.send(c.player(),"Legado importado. Escolha seus novos ramos.");return "Legado importado.";
        }catch(Exception e){MurilloSkills.LOG.warn("Importação recusada para {}",c.player().getUUID(),e);return "Importação recusada. Os arquivos foram preservados.";}
    }
}

