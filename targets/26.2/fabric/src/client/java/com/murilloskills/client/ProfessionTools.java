package com.murilloskills.client;
import com.murilloskills.skills.*;
import com.murilloskills.network.Protocol;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;
import static com.murilloskills.client.SkillsTheme.*;
import static com.murilloskills.client.ProfessionRows.Action;

final class ProfessionTools {
    private ProfessionTools(){}
    static void build(ProfessionScreen s){
        int x=s.contentX(),y=s.contentY(),w=s.contentW(),h=s.contentH();
        int bw=(w-10)/3;
        s.fixed("tool:mine",x,y,bw,19,"Ultmine",()->s.tool(false)).selected(!s.placement&&!s.toolOptions);
        s.fixed("tool:place",x+bw+5,y,bw,19,"UltPlace",()->s.tool(true)).selected(s.placement&&!s.toolOptions);
        s.fixed("tool:options",x+2*(bw+5),y,w-2*(bw+5),19,"Opções e filtros",()->{s.toolOptions=!s.toolOptions;s.refresh();}).selected(s.toolOptions);
        if(s.toolOptions){options(s);return;}
        var prefs=ClientState.profile.tools;var shapes=s.placement?ShapePlanner.PLACEMENT:ShapePlanner.MINING;
        int columns=w>=520?5:4,rows=(shapes.size()+columns-1)/columns,gap=4,cw=(w-(columns-1)*gap)/columns,ch=h<225?24:34;
        String chosen=s.placement?prefs.placementShape:prefs.miningShape;
        for(int i=0;i<shapes.size();i++){
            final int index=i;String shape=shapes.get(i);
            var b=s.fixed("shape:"+shape,x+i%columns*(cw+gap),y+27+i/columns*(ch+gap),cw,ch,ToolScreen.label(shape),
                ()->ClientState.send(Protocol.Op.TOOL_CONFIG,s.placement?"placement":"mining",index));
            b.selected(chosen.equals(shape)).accent(s.placement?SkillType.BUILDER.color:SkillType.MINER.color).shape(shape);
            b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.literal(hint(shape))));
        }
        int dimensionsY=y+27+rows*(ch+gap)+1;
        var keys=dimensions(chosen,s.placement);
        if(!keys.isEmpty()){
            int mw=(w-(keys.size()-1)*5)/keys.size();
            for(int i=0;i<keys.size();i++){
                String key=keys.get(i);int n=key.equals("size")?prefs.size:key.equals("depth")?prefs.depth:prefs.spacing;
                int max=key.equals("spacing")?4:20,xx=x+i*(mw+5);
                s.fixed("less:"+key,xx+mw-38,dimensionsY,17,19,"−",()->ClientState.send(Protocol.Op.TOOL_CONFIG,key,Math.max(1,n-1)));
                s.fixed("more:"+key,xx+mw-17,dimensionsY,17,19,"+",()->ClientState.send(Protocol.Op.TOOL_CONFIG,key,Math.min(max,n+1)));
            }
        }
        int foot=y+h-21,aw=(w-10)/3;
        s.fixed("tool:preview",x,foot,aw,20,"Ver prévia",()->{ClientState.send(Protocol.Op.PREVIEW,"",s.placement?1:0);s.onClose();});
        if(s.placement){
            s.fixed("tool:execute",x+aw+5,foot,aw,20,"Colocar",()->{ClientState.send(Protocol.Op.PLACE,"",0);s.onClose();}).active=ClientState.profile.unlocked(SkillType.BUILDER,15);
            s.fixed("tool:undo",x+2*(aw+5),foot,w-2*(aw+5),20,"Desfazer",()->ClientState.send(Protocol.Op.UNDO,"",0));
        }else{
            s.fixed("tool:radial",x+aw+5,foot,aw,20,"Abrir radial",()->s.minecraftClient().gui.setScreen(new UltmineRadialScreen(s,false)));
            s.fixed("tool:binding",x+2*(aw+5),foot,w-2*(aw+5),20,ClientPreferences.current.miningToggle?"Alternar":"Segurar",()->{ClientPreferences.current.miningToggle=!ClientPreferences.current.miningToggle;MiningInput.reset();ClientPreferences.save();s.refresh();});
        }
    }
    private static List<String> dimensions(String shape,boolean placement){return switch(shape){
        case "VEIN","SINGLE"->List.of();case "3X3","2X1"->List.of("depth");
        case "LINE"->placement?List.of("depth","spacing"):List.of("depth");
        case "TUNNEL"->List.of("depth","spacing");case "COLUMN"->List.of("size","spacing");
        case "PLANE","QUARRY","WALL","CIRCLE","SPHERE"->List.of("size","spacing");default->List.of("size","depth","spacing");};}
    static void render(ProfessionScreen s,GuiGraphicsExtractor g){
        if(s.toolOptions)return;
        int x=s.contentX(),y=s.contentY(),w=s.contentW(),h=s.contentH();
        var prefs=ClientState.profile.tools;var shapes=s.placement?ShapePlanner.PLACEMENT:ShapePlanner.MINING;
        int columns=w>=520?5:4,nrows=(shapes.size()+columns-1)/columns,ch=h<225?24:34;
        String chosen=s.placement?prefs.placementShape:prefs.miningShape;var keys=dimensions(chosen,s.placement);
        int dy=y+27+nrows*(ch+4)+1;
        if(!keys.isEmpty()){
            int mw=(w-(keys.size()-1)*5)/keys.size();
            for(int i=0;i<keys.size();i++){String key=keys.get(i);int n=key.equals("size")?prefs.size:key.equals("depth")?prefs.depth:prefs.spacing;
                String label=key.equals("size")?"Tam.":key.equals("depth")?"Prof.":"Esp.";g.text(s.uiFont(),label+" "+n,x+i*(mw+5)+2,dy+6,TEXT,false);}
        }
        var skill=s.placement?SkillType.BUILDER:SkillType.MINER;var summary=ClientState.summaries.get(skill);
        String limit=summary==null?"…":summary.effects().stream().filter(e->e.id().equals(s.placement?"area":"miner.area")).findFirst().map(e->e.value()).orElse("0");
        String info=ClientState.profile.unlocked(skill,15)?"Capacidade: "+limit+(ClientState.active(skill)?" · ativa em uso":""):"Requer "+skill.title+" escolhido e conquista 15";
        g.text(s.uiFont(),SkillsTheme.fit(s.uiFont(),info,w),x+2,Math.min(y+h-35,dy+26),ClientState.profile.unlocked(skill,15)?GOLD:MUTED,false);
    }
    private static String hint(String shape){return switch(shape){case "VEIN"->"Blocos conectados do mesmo tipo. Filtros em Opções e filtros.";case "3X3"->"Seção de 3 × 3 a partir da face atingida.";case "LINE"->"Linha na profundidade escolhida.";case "BOX"->"Volume com tamanho e profundidade configuráveis.";case "SPHERE"->"Esfera; habilite Oco nas opções para construir só a superfície.";default->"A prévia no mundo mostra o plano válido antes da execução.";};}
    private static void options(ProfessionScreen s){
        var r=s.rows;r.clear();var p=ClientState.profile.tools;
        r.add("filters","Filtros","Selecione os itens e recursos usados por suas ferramentas.",GOLD,
            new Action("filter:ore","Radar",()->s.minecraftClient().gui.setScreen(new FilterScreen(s,"ore")),true),
            new Action("filter:classic","Veio",()->s.minecraftClient().gui.setScreen(new FilterScreen(s,"classic")),true),
            new Action("filter:trash","Lixeira",()->s.minecraftClient().gui.setScreen(new FilterScreen(s,"trash")),true));
        option(s,"Drops no inventário","inventory",p.inventoryDrops,"Excedentes ficam no chão; a lixeira é aplicada primeiro.");
        option(s,"XP vanilla direto","direct_xp",p.directXp,"Recolhe os orbes gerados na operação.");
        option(s,"Somente minérios","ores",p.oresOnly,"Filtra os blocos válidos para o Ultmine.");
        option(s,"Formato oco","hollow",p.hollow,"Usa somente a superfície das formas compatíveis.");
        r.add("brush","Pincel construtor","Dois cantos no mundo, prévia e confirmação com UltPlace. Materiais insuficientes não alteram o desfazer anterior.",TEXT,
            new Action("brush:clear","Limpar marcações",()->ClientState.send(Protocol.Op.CLEAR_BRUSH,"",0),ClientState.profile.selected(SkillType.BUILDER)));
        s.layoutRows(s.contentY()+26,s.contentH()-26);
    }
    private static void option(ProfessionScreen s,String name,String key,boolean on,String detail){
        s.rows.add("option:"+key,name+" · "+(on?"ligado":"desligado"),detail,TEXT,new Action("toggle:option:"+key,on?"Desligar":"Ligar",()->ClientState.send(Protocol.Op.TOOL_CONFIG,key,on?0:1),true));
    }
}
