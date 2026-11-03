package com.murilloskills.client;
import com.murilloskills.skills.*;
import com.murilloskills.network.Protocol;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Catalog is always all eight enum entries; a draft is committed atomically. */
final class ProfessionSelection {
    private final List<Choice> choices=new ArrayList<>();
    final LinkedHashSet<SkillType> draft=new LinkedHashSet<>();
    SkillType incoming;long pending;boolean dirty;
    void build(ProfessionScreen s){
        choices.clear();
        if(!dirty&&pending==0){draft.clear();draft.addAll(ClientState.profile.selected);}
        if(incoming!=null){replacement(s);return;}
        int gap=5,cw=(s.contentW()-3*gap)/4,gh=s.contentH()-42,ch=(gh-gap)/2;
        int i=0;for(var type:SkillType.values()){
            int xx=s.contentX()+i%4*(cw+gap),yy=s.contentY()+15+i/4*(ch+gap);i++;
            var tile=new Choice(xx,yy,cw,ch,type,draft.contains(type),()->{
                if(pending!=0)return;
                if(ClientState.profile.selected.contains(type)){s.detail(type,ProfessionScreen.Tab.SUMMARY);return;}
                if(draft.remove(type))dirty=true;
                else if(draft.size()<4){draft.add(type);dirty=true;}
                else if(ClientState.profile.selected.size()==4){incoming=type;}
                else ClientState.message("Desmarque uma escolha para adicionar outra.");
                s.refresh();
            });
            tile.active=pending==0;choices.add(tile);s.addFixed(tile);
        }
        int foot=s.contentY()+s.contentH()-21,bw=(s.contentW()-6)/2;
        s.fixed("selection:later",s.contentX(),foot,bw,20,"Escolher depois",s::onClose).active=pending==0;
        var confirm=s.fixed("selection:commit",s.contentX()+bw+6,foot,s.contentW()-bw-6,20,pending!=0?"Confirmando…":"Começar ("+draft.size()+"/4)",()->{
            pending=ClientState.send(Protocol.Op.SELECT_BATCH,String.join(",",draft.stream().map(Enum::name).toList()),0);s.refresh();
        });
        confirm.active=ClientState.ready()&&pending==0&&!draft.isEmpty()&&!new ArrayList<>(draft).equals(ClientState.profile.selected);
    }
    private void replacement(ProfessionScreen s){
        int gap=6,cw=(s.contentW()-gap)/2,ch=(s.contentH()-40-gap)/2;
        int i=0;for(var old:ClientState.profile.selected){
            int xx=s.contentX()+i%2*(cw+gap),yy=s.contentY()+15+i/2*(ch+gap);i++;
            var button=s.fixed("replace:"+old,xx,yy,cw,ch,old.title+" → "+incoming.title,()->s.confirm("Trocar profissão","Custa "+ProgressionService.SKILL_SWAP_COST+" selos. O progresso de "+old.title+" fica guardado.",()->{pending=ClientState.send(Protocol.Op.SELECT,incoming.name()+","+old.name(),0);}));
            button.active=pending==0&&ClientState.profile.seals>=ProgressionService.SKILL_SWAP_COST;
        }
        s.fixed("replace:cancel",s.contentX(),s.contentY()+s.contentH()-21,s.contentW(),20,"Voltar ao catálogo",()->{incoming=null;s.refresh();});
    }
    void tick(ProfessionScreen s){
        if(pending==0)return;var reply=ClientState.replies.get(pending);if(reply==null)return;
        if(reply.code()==Protocol.ResultCode.OK&&(!ClientState.ready()||(incoming==null?!ClientState.profile.selected.containsAll(draft):!ClientState.profile.selected.contains(incoming))))return;
        pending=0;
        if(reply.code()==Protocol.ResultCode.OK){dirty=false;incoming=null;s.home();}
        else{s.refresh();}
    }
    void render(ProfessionScreen s,GuiGraphicsExtractor g){
        String line=incoming==null?(ClientState.ready()?"Escolha de uma a quatro profissões · primeira escolha gratuita":"Sincronizando escolhas e descrições…"):"Substituir por "+incoming.title+" · "+ProgressionService.SKILL_SWAP_COST+" selos · escolha qual retirar";
        g.text(s.uiFont(),SkillsTheme.fit(s.uiFont(),line,s.contentW()),s.contentX(),s.contentY()+1,SkillsTheme.MUTED,false);
    }
    ProfessionTooltip.Bounds renderTooltip(ProfessionScreen s,GuiGraphicsExtractor g,int mx,int my){
        var hovered=choices.stream().filter(c->c.isMouseOver(mx,my)).findFirst().orElse(null);
        if(hovered==null&&s.minecraftClient().getLastInputType().isKeyboard())hovered=choices.stream().filter(Choice::isFocused).findFirst().orElse(null);
        if(hovered==null)return null;
        return ProfessionTooltip.render(g,s.uiFont(),hovered.description,hovered.type.color,mx,my);
    }
    private static final class Choice extends Button {
        final SkillType type;final boolean chosen;
        final List<ProfessionTooltip.Paragraph> description=new ArrayList<>();
        Choice(int x,int y,int w,int h,SkillType type,boolean chosen,Runnable click){
            super(x,y,w,h,Component.literal(type.title),b->click.run(),DEFAULT_NARRATION);this.type=type;this.chosen=chosen;
            description.add(new ProfessionTooltip.Paragraph(type.title,type.color));
            description.add(new ProfessionTooltip.Paragraph(ProfessionInfo.role(type),SkillsTheme.MUTED));
            var summary=ClientState.summaries.get(type);
            if(summary!=null){
                String first=summary.milestones().getFirst().benefits().getFirst().split("\\. ",2)[0];
                description.add(new ProfessionTooltip.Paragraph("Nv. 5 · "+first,SkillsTheme.TEXT));
                String active=summary.milestones().get(3).benefits().stream().filter(b->b.startsWith(type.ability+" ·")).findFirst().orElse(type.ability);
                description.add(new ProfessionTooltip.Paragraph("Nv. 20 · "+active,SkillsTheme.TEXT));
            }
            description.add(new ProfessionTooltip.Paragraph("Ramos no 25 · "+type.branchA+" / "+type.branchB,SkillsTheme.MUTED));
        }
        protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial){
            var font=net.minecraft.client.Minecraft.getInstance().font;
            SkillsTheme.panel(g,getX(),getY(),getWidth(),getHeight(),chosen?0xff343c2d:SkillsTheme.PANEL,chosen||isHoveredOrFocused()?type.color:SkillsTheme.BORDER);
            g.item(SkillsTheme.icon(type),getX()+(getWidth()-16)/2,getY()+5);
            g.centeredText(font,SkillsTheme.fit(font,type.title,getWidth()-6),getX()+getWidth()/2,getY()+25,type.color);
            if(getHeight()>46)g.centeredText(font,chosen?"Escolhida":ClientState.profile.selected.size()==4?"Substituir":"Selecionar",getX()+getWidth()/2,getY()+39,chosen?SkillsTheme.GREEN:SkillsTheme.MUTED);
            if(getHeight()>90){
                var lines=font.split(Component.literal(ProfessionInfo.role(type)),getWidth()-16);
                for(int i=0;i<Math.min(3,lines.size());i++)g.text(font,lines.get(i),getX()+8,getY()+60+i*11,SkillsTheme.MUTED,false);
            }
        }
    }
}
