package com.murilloskills.client;
import com.murilloskills.skills.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Four stable character slots; essential actions are always visible. */
final class ProfessionHome {
    private ProfessionHome(){}
    static void build(ProfessionScreen s){
        s.fixed("home:manage",s.contentX()+s.contentW()-110,s.y+s.h-20,110,16,"Trocar profissões",s::catalog);
        int gap=8,cw=(s.contentW()-gap)/2,ch=(s.contentH()-gap)/2;
        for(int i=0;i<4;i++){
            int xx=s.contentX()+i%2*(cw+gap),yy=s.contentY()+i/2*(ch+gap);
            if(i>=ClientState.profile.selected.size()){
                s.fixed("slot:"+i,xx,yy,cw,ch,"+ Escolher profissão",s::catalog);continue;
            }
            var type=ClientState.profile.selected.get(i);
            s.addFixed(new Tile(xx,yy,cw,ch-24,type,()->s.detail(type,WorkspaceMemory.tab)));
            int bw=(cw-5)/2;
            var equip=s.fixed("equip:"+type,xx,yy+ch-21,bw,20,ClientState.profile.equipped==type?"Equipada":"Equipar",()->ClientState.send(com.murilloskills.network.Protocol.Op.EQUIP,type.name(),0));
            equip.selected(ClientState.profile.equipped==type).accent(type.color);
            var use=s.fixed("use:"+type,xx+bw+5,yy+ch-21,cw-bw-5,20,ClientState.active(type)?"Encerrar":ClientState.profile.unlocked(type,20)?"Ativar":"Ativa no 20",()->ClientState.use(type));
            use.active=ClientState.ready()&&ClientState.profile.unlocked(type,20);use.accent(type.color);
        }
    }
    private static final class Tile extends Button {
        final SkillType type;
        Tile(int x,int y,int w,int h,SkillType type,Runnable action){super(x,y,w,h,Component.literal(type.title),b->action.run(),DEFAULT_NARRATION);this.type=type;}
        protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial){
            var font=net.minecraft.client.Minecraft.getInstance().font;var stats=ClientState.profile.stats(type);
            SkillsTheme.panel(g,getX(),getY(),getWidth(),getHeight(),SkillsTheme.PANEL,isHoveredOrFocused()?type.color:SkillsTheme.BORDER);
            g.item(SkillsTheme.icon(type),getX()+7,getY()+7);
            g.text(font,type.title,getX()+29,getY()+7,type.color,false);
            String level="Nv. "+stats.level+(stats.prestige>0?" · P"+stats.prestige:"")+" · "+SkillsTheme.number(stats.xp)+" XP";
            g.text(font,level,getX()+29,getY()+18,SkillsTheme.TEXT,false);
            SkillsTheme.progress(g,getX()+8,getY()+(getHeight()<50?27:31),getWidth()-16,getHeight()<50?3:4,stats.level==100?1:stats.xp/ProgressionCurve.required(type,stats.level),type.color);
            var summary=ClientState.summaries.get(type);
            var next=summary==null?null:summary.milestones().stream().filter(m->!m.unlocked()).findFirst().orElse(null);
            String full=summary==null?"Sincronizando":next==null?"Marcos de domínio conquistados":"Próximo: "+next.level()+" · "+next.title();
            String state=ProfessionScreen.abilityStatus(type);
            String line=getHeight()>=67?full:(next==null?"Domínio":"Marco "+next.level())+" · "+state;
            g.text(font,SkillsTheme.fit(font,line,getWidth()-16),getX()+8,getY()+(getHeight()<50?33:40),SkillsTheme.MUTED,false);
            if(getHeight()>=67)g.text(font,SkillsTheme.fit(font,state,getWidth()-16),getX()+8,getY()+54,SkillsTheme.TEXT,false);
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(full+"\n"+state+" · "+type.ability)));
        }
    }
}
