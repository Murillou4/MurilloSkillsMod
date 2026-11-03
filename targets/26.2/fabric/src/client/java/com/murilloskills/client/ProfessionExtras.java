package com.murilloskills.client;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.EffectDefinition;
import net.minecraft.network.chat.Component;
import java.util.*;
import static com.murilloskills.client.SkillsTheme.*;
import static com.murilloskills.client.ProfessionRows.Action;

final class ProfessionExtras {
    private ProfessionExtras(){}
    static void contracts(ProfessionScreen s){
        var r=s.rows;
        r.add("contracts","Contratos persistentes","Complete o conjunto para ganhar "+ContractService.SEALS_PER_SET+" selo. Sem prazo ou perda de progresso ao sair.",GOLD);
        for(int i=0;i<ClientState.profile.contracts.size();i++){
            final int index=i;var c=ClientState.profile.contracts.get(i);
            String activity=switch(c.activity){case "mine"->"Mineração";case "combat"->"Combate";case "harvest"->"Colheita";case "plant"->"Plantio";case "fish"->"Pesca";case "forge"->"Forja";case "build"->"Construção";default->"Exploração";};
            r.progress("contract:"+i,c.skill.title+" · "+activity,"Pontos de atividade. Ações em área têm contribuição reduzida.",c.skill.color,
                ()->{var current=ClientState.profile.contracts.get(index);return current.progress/current.target;},
                ()->{var current=ClientState.profile.contracts.get(index);return current.skill.title+" · "+EffectDefinition.number(current.progress)+" / "+EffectDefinition.number(current.target)+" pontos";});
        }
        if(ClientState.profile.contracts.isEmpty())r.add("contracts:none","Escolha sua primeira profissão","Seu primeiro conjunto chega após a escolha.",MUTED);
        r.add("seals","Selos · "+ClientState.profile.seals,"Trocas: ramo "+ProgressionService.BRANCH_SWAP_COST+" selo; profissão "+ProgressionService.SKILL_SWAP_COST+" selos; Paragon "+ProgressionService.PARAGON_SWAP_COST+" selos. Escolhas iniciais gratuitas.",GOLD);
        for(var synergy:SynergyService.active(ClientState.profile))r.add("synergy:"+synergy.name(),synergy.name(),synergy.a().title+" + "+synergy.b().title+" · +"+EffectDefinition.number(synergy.amount()*100)+"% "+effect(synergy.effect()),TEXT);
    }
    private static String effect(SynergyService.Effect effect){return switch(effect){case DEFENSE->"defesa";case DAMAGE->"dano";case ORE->"fortuna";case DROPS->"rendimento";case ECONOMY->"economia";case SPEED->"movimento";case FINDS->"alcance de buscas";};}
    static void help(ProfessionScreen s){
        var r=s.rows;
        r.add("help:card","Card da habilidade","Escolha quando mostrar o card da profissão equipada no canto da tela.",GOLD,
            new Action("pref:equipped_card",ClientPreferences.current.cardMode().label,()->{ClientPreferences.current.equippedCard=ClientPreferences.current.cardMode().next();ClientPreferences.save();s.refresh();},true));
        r.add("help:start","Quatro caminhos","Até quatro profissões chegam ao 100. Uma recebe Paragon. As outras guardam o progresso e não concedem benefícios.",GOLD);
        r.add("help:controls","Atalhos e utilidades",ProfessionScreen.key(MurilloSkillsClient.guide)+" abre/fecha. "+ProfessionScreen.key(MurilloSkillsClient.ability)+" ativa ou encerra a equipada; "+ProfessionScreen.key(MurilloSkillsClient.radial)+" equipa a ativa; "+ProfessionScreen.key(MurilloSkillsClient.tools)+" abre ferramentas. Atalhos opcionais começam sem tecla.",TEXT);
        for(int i=0;i<4;i++)r.add("direct:"+i,"Ativa direta · posição "+(i+1),"Opcional: ativa ou encerra a profissão nessa posição.",TEXT,s.bind("bind:direct:"+i,MurilloSkillsClient.directAbilities[i]));
        r.add("help:visual","Apresentação","Preferências deste cliente.",GOLD,
            toggle(s,"pref:hud","HUD",()->ClientPreferences.current.hud,()->ClientPreferences.current.hud=!ClientPreferences.current.hud),
            toggle(s,"pref:markers","Marcadores",()->ClientPreferences.current.worldMarkers,()->ClientPreferences.current.worldMarkers=!ClientPreferences.current.worldMarkers));
        r.add("help:sound","Avisos e sons","Feedback de ativações, radar e habilidades.",TEXT,
            toggle(s,"pref:sounds","Sons",()->ClientPreferences.current.abilitySounds,()->ClientPreferences.current.abilitySounds=!ClientPreferences.current.abilitySounds),
            toggle(s,"pref:visuals","Efeitos",()->ClientPreferences.current.abilityVisuals,()->ClientPreferences.current.abilityVisuals=!ClientPreferences.current.abilityVisuals));
        r.add("help:xp","XP e marcos","Preferências preservadas ao atualizar o mod.",TEXT,
            toggle(s,"pref:xp","Avisos XP",()->ClientPreferences.current.xpNotices,()->ClientPreferences.current.xpNotices=!ClientPreferences.current.xpNotices),
            toggle(s,"pref:milestones","Som de marco",()->ClientPreferences.current.milestoneSound,()->ClientPreferences.current.milestoneSound=!ClientPreferences.current.milestoneSound));
        r.add("help:legacy","Importação do legado","Prévia, confirmação e backup. Mantém os arquivos antigos e não concede selos retroativos.",TEXT,
            new Action("legacy:open","Importar legado",()->s.minecraftClient().gui.setScreen(new LegacyImportScreen(s)),ClientState.profile.fresh()&&!ClientState.profile.importedLegacy));
    }
    private static Action toggle(ProfessionScreen s,String id,String label,java.util.function.BooleanSupplier value,Runnable toggle){
        return new Action(id,label+": "+(value.getAsBoolean()?"sim":"não"),()->{toggle.run();ClientPreferences.save();s.refresh();},true);
    }
}
