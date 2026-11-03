package com.murilloskills.client;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.network.Protocol;
import java.util.*;
import static com.murilloskills.client.ProfessionRows.Action;
import static com.murilloskills.client.SkillsTheme.*;

/** Profession content separated from layout, input, selection and tool controls. */
final class ProfessionDetails {
    private ProfessionDetails(){}
    static Action action(String id,String text,Runnable run,boolean enabled){return new Action(id,text,run,enabled);}
    static int build(ProfessionScreen s,ProfessionRows rows){
        var summary=ClientState.summaries.get(s.skill);if(summary==null){rows.add("loading","Sincronizando profissão","Aguardando dados completos do servidor.",MUTED);return 0;}
        return switch(s.tab){case SUMMARY->{summary(s,rows,summary);yield 23;}case PROGRESSION->{progression(s,rows,summary);yield 0;}case BRANCH->{branches(s,rows,summary);yield 46;}case CONTROLS->{controls(s,rows,summary);yield 0;}};
    }
    private static void summary(ProfessionScreen s,ProfessionRows rows,SkillSummary view){
        var p=ClientState.profile;var stats=p.stats(s.skill);var skill=s.skill;
        String timing=view.activeTier()==0?"Libera no nível 20 · "+view.milestones().stream().filter(m->m.level()==20).flatMap(m->m.benefits().stream()).filter(b->b.startsWith(view.ability()+" ·")).findFirst().orElse(view.ability()):"Patamar "+view.activeTier()+"/5 · "+view.durationTicks()/20+" s de duração · "+view.cooldownTicks()/20+" s de recarga";
        rows.live("ability",view.ability()+" · "+ProfessionScreen.abilityStatus(skill),timing,GOLD,()->view.ability()+" · "+ProfessionScreen.abilityStatus(skill),
            action("ability:equip","Equipar ativa",()->ClientState.send(Protocol.Op.EQUIP,skill.name(),0),p.selected(skill)),
            action("ability:use",ClientState.active(skill)?"Encerrar":"Ativar",()->ClientState.use(skill),p.unlocked(skill,20)));
        String[] groups={"Passivas","Utilidades","Durante a ativa"};String[] ids={"passive","utility","active"};int gw=(s.contentW()-10)/3;
        for(int i=0;i<3;i++){final var group=SkillSummary.Group.values()[i];s.fixed("group:"+ids[i],s.contentX()+i*(gw+5),s.contentY(),gw,18,groups[i],()->s.group(group)).selected(s.effectGroup==group);}
        if(s.effectGroup==SkillSummary.Group.ACTIVE){
            if(view.activeTier()==0)rows.add("active:locked","Ativa ainda bloqueada","Os efeitos começam na conquista 20. Consulte os valores futuros na aba Progressão.",MUTED);
            else for(int i=0;i<view.activeBenefits().size();i++)rows.add("active:"+i,view.activeBenefits().get(i),"Aplicado durante a habilidade, sujeito aos limites de poder.",TEXT);
            if(skill==SkillType.BUILDER&&view.activeTier()>0)rows.add("brush:help","Pincel por dois pontos","Clique com um bloco em duas superfícies; confira a prévia e confirme com "+ProfessionScreen.key(MurilloSkillsClient.place)+". Desfazer: "+ProfessionScreen.key(MurilloSkillsClient.undo)+".",TEXT,
                action("brush:clear","Limpar marcações",()->ClientState.send(Protocol.Op.CLEAR_BRUSH,"",0),p.selected(skill)));
        }else{
            for(var effect:view.effects())if(effect.group()==s.effectGroup){
                String state=switch(effect.state()){case APPLIED->"aplicado";case CONDITIONAL->"condicional";case DISABLED->"desligado";case LOCKED->"conquista "+effect.unlock();};
                String detail=state+" · "+effect.nextValue();
                String tip=effect.condition()+"\nPotencial: "+effect.value()+"\nAplicado neste contexto: "+EffectDefinition.format(effect.unit(),effect.applied())+(effect.limit().isEmpty()?"":"\nLimite individual: "+effect.limit());
                rows.effect(effect.id(),effect.title()+" · "+effect.value(),detail,tip,effect.unlocked()?TEXT:MUTED);
            }
            if(s.effectGroup==SkillSummary.Group.UTILITY)rows.add("utility:controls","Controle suas utilidades","Você pode ligar ou desligar cada opção na aba Controles.",MUTED,
                action("utility:go","Abrir controles",()->s.tab(ProfessionScreen.Tab.CONTROLS),true));
        }
        for(int i=0;i<view.limits().size();i++)rows.add("limit:"+i,i==0?"Resultado combinado da build":"Condições e limites",view.limits().get(i),MUTED);
        rows.add("mastery","Domínio e prestígio","P"+stats.prestige+" / "+ProgressionService.MAX_PRESTIGE+" · patamares conquistados até "+stats.unlockLevel()+". Preserva ferramentas e ramo; "+EffectDefinition.format(EffectDefinition.Unit.PERCENT,ProgressionCurve.xpBonus(1)-1)+" de XP e "+EffectDefinition.format(EffectDefinition.Unit.PERCENT,ProgressionCurve.passiveBonus(1)-1)+" de passivas adicionais por prestígio.",GOLD,
            action("paragon",p.paragon==skill?"Paragon escolhida":"Escolher Paragon",()->{if(!p.hasChosenParagon)ClientState.send(Protocol.Op.PARAGON,skill.name(),0);else s.confirm("Paragon · "+skill.title,"Trocar custa "+ProgressionService.PARAGON_SWAP_COST+" selos.",()->ClientState.send(Protocol.Op.PARAGON,skill.name(),0));},p.unlocked(skill,100)&&p.paragon!=skill),
            action("prestige","Prestigiar",()->s.confirm("Prestígio · "+skill.title,"Reinicia nível e XP, mantendo ferramentas, ramo e conquistas. As recompensas iniciais não se repetem.",()->ClientState.send(Protocol.Op.PRESTIGE,skill.name(),0)),p.selected(skill)&&stats.level==100&&stats.prestige<ProgressionService.MAX_PRESTIGE));
    }
    private static void progression(ProfessionScreen s,ProfessionRows rows,SkillSummary view){
        rows.add("trail","Sua trilha","Marcos permanecem conquistados após prestígio. Role para ver todos os benefícios e valores.",MUTED);
        for(var milestone:view.milestones()){
            String text=String.join("\n",milestone.benefits());
            rows.milestone("milestone:"+milestone.level(),"Nv. "+milestone.level()+" · "+milestone.title()+(milestone.unlocked()?" · conquistado":""),text,milestone.unlocked()?s.skill.color:MUTED);
        }
    }
    private static void branches(ProfessionScreen s,ProfessionRows rows,SkillSummary view){
        var a=view.branches().get(0);var b=view.branches().get(1);var stats=ClientState.profile.stats(s.skill);
        var ta=a.tiers().get(s.branchTier-1);var tb=b.tiers().get(s.branchTier-1);
        int x=s.contentX(),y=s.contentY(),w=s.contentW(),tierW=Math.min(55,(w-10)/6),leftW=w-3*(tierW+4)-5;
        for(int tier=1;tier<=3;tier++){final int rank=tier;s.fixed("tier:"+tier,x+w-3*(tierW+4)+(tier-1)*(tierW+4),y,tierW,18,"Nv. "+tier*25,()->s.tier(rank)).selected(tier==s.branchTier);}
        for(int i=0;i<2;i++){
            var branch=view.branches().get(i);var action=branchAction(s,branch,stats.branch);int bw=(w-5)/2;
            var button=s.fixed(action.id(),x+i*(bw+5),y+23,bw,18,branch.title()+(stats.branch==branch.choice()?" · escolhido":""),action.run());button.active=action.enabled();button.selected(stats.branch==branch.choice()).accent(s.skill.color);
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.literal(branch.description()+"\n"+(stats.branch==0?"Primeira escolha gratuita.":"Trocar custa "+ProgressionService.BRANCH_SWAP_COST+" selo.")+(!ClientState.profile.unlocked(s.skill,25)?"\nRequer a conquista 25.":""))));
        }
        for(var left:ta.effects()){
            var right=tb.effects().stream().filter(c->c.title().equals(left.title())).findFirst().orElse(null);
            if(right!=null&&!left.after().equals(right.after()))rows.compare("compare:"+left.title(),left.title()+" · atual "+left.before(),a.title()+": "+left.after(),b.title()+": "+right.after(),left.condition());
        }
        rows.add("branch:a",a.title(),a.description(),MUTED);rows.add("branch:b",b.title(),b.description(),MUTED);
    }
    static void render(ProfessionScreen s,net.minecraft.client.gui.GuiGraphicsExtractor g){
        if(s.tab!=ProfessionScreen.Tab.BRANCH)return;
        var view=ClientState.summaries.get(s.skill);if(view==null)return;
        int level=view.branches().getFirst().tiers().get(s.branchTier-1).evaluatedLevel();
        int w=s.contentW()-3*(Math.min(55,(s.contentW()-10)/6)+4)-5;
        String info="Nv. "+level+" · "+(ClientState.profile.stats(s.skill).branch==0?"grátis":"troca: "+ProgressionService.BRANCH_SWAP_COST+" selo");
        g.text(s.uiFont(),SkillsTheme.fit(s.uiFont(),info,w),s.contentX()+2,s.contentY()+5,MUTED,false);
    }
    private static Action branchAction(ProfessionScreen s,SkillSummary.BranchPreview branch,int chosen){
        var p=ClientState.profile;boolean enabled=p.unlocked(s.skill,25)&&chosen!=branch.choice()&&(chosen==0||p.seals>=ProgressionService.BRANCH_SWAP_COST);
        return action("branch:"+branch.choice(),chosen==branch.choice()?branch.title()+" · escolhido":"Escolher "+branch.title(),()->{
            Runnable apply=()->ClientState.send(Protocol.Op.BRANCH,s.skill.name(),branch.choice());
            if(chosen==0)apply.run();else s.confirm("Trocar para "+branch.title(),"Custa "+ProgressionService.BRANCH_SWAP_COST+" selo. Seus níveis e patamares serão mantidos.",apply);
        },enabled);
    }
    private static String group(Feature feature){return switch(feature){
        case NIGHT_VISION,AUTO_TORCH,ORE_RADAR,PATHFINDER->"Visão e orientação";
        case SPEED,STEP,BREATH,SWIM,CLIMB,SLOW_FALL,NETHER->"Movimento e ambiente";
        case PLANT,BONEMEAL,HARVEST,REPLANT,GROWTH,FARM_REGEN->"Cultivo";
        default->"Equipamentos e proteção";
    };}
    private static void controls(ProfessionScreen s,ProfessionRows rows,SkillSummary view){
        String last="";
        var controls=new ArrayList<>(view.controls());controls.sort(Comparator.comparing(v->group(Feature.find(v.id()).orElseThrow())));
        for(var control:controls){
            var feature=Feature.find(control.id()).orElseThrow();String group=group(feature);
            if(!group.equals(last)){rows.add("control:group:"+group,group,"",GOLD);last=group;}
            var binding=MurilloSkillsClient.featureKeys.get(feature);
            rows.add("control:"+feature.id,control.title()+" · "+(control.available()?(control.enabled()?"ligado":"desligado"):"conquista "+control.level()),control.description(),TEXT,
                action("toggle:"+feature.id,control.enabled()?"Desligar":"Ligar",()->ClientState.send(Protocol.Op.SET_FEATURE,feature.id,control.enabled()?0:1),control.available()),
                s.bind("bind:"+feature.id,binding));
        }
        rows.add("bind:ability","Ativar / encerrar","Encerrar mantém a recarga.",TEXT,s.bind("key:ability",MurilloSkillsClient.ability),s.bind("key:radial",MurilloSkillsClient.radial));
        int slot=ClientState.profile.selected.indexOf(s.skill);
        if(slot>=0)rows.add("bind:direct","Atalho direto · "+s.skill.title,"Ativa ou encerra a profissão na posição "+(slot+1)+". Opcional, começa sem tecla.",TEXT,s.bind("bind:direct:"+slot,MurilloSkillsClient.directAbilities[slot]));
        if(s.skill==SkillType.MINER){
            rows.add("mine:key","Ultmine","Quebre o primeiro bloco normalmente; o restante segue sua forma e ferramenta.",TEXT,
                s.bind("key:mine",MurilloSkillsClient.mine),
                action("mode:mine",ClientPreferences.current.miningToggle?"Modo: alternar":"Modo: segurar",()->{ClientPreferences.current.miningToggle=!ClientPreferences.current.miningToggle;MiningInput.reset();ClientPreferences.save();s.refresh();},true));
            rows.add("mine:radial","Radial de Ultmine","Segure, aponte para uma forma e solte.",TEXT,s.bind("key:mineMenu",MurilloSkillsClient.mineMenu),
                action("radial:open","Abrir radial",()->s.minecraftClient().gui.setScreen(new UltmineRadialScreen(s,false)),true));
        }
        if(s.skill==SkillType.BUILDER)rows.add("place:key","UltPlace","No modo contínuo, aperte para armar/desarmar e clique direito para construir.",TEXT,s.bind("key:place",MurilloSkillsClient.place),
            action("mode:place",ClientPreferences.current.placementContinuous?"Modo: contínuo":"Modo: pontual",()->{ClientPreferences.current.placementContinuous=!ClientPreferences.current.placementContinuous;PlacementInput.reset();ClientPreferences.save();s.refresh();},true));
        if(s.skill==SkillType.BUILDER)rows.add("place:undo","Desfazer e marcações","Desfazer funciona fora de combate, inclusive durante o Pincel.",TEXT,s.bind("key:undo",MurilloSkillsClient.undo),action("brush:clear","Limpar marcações",()->ClientState.send(Protocol.Op.CLEAR_BRUSH,"",0),ClientState.profile.selected(s.skill)));
    }
}
