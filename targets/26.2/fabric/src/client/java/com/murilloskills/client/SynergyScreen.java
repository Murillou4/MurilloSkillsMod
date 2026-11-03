package com.murilloskills.client;
import com.murilloskills.skills.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** Shared synergy definitions, including their real activation requirements. */
public final class SynergyScreen extends NotebookScreen {
    private int page;
    public SynergyScreen(Screen parent){super("SINERGIAS / PROFISSÕES QUE SE COMPLETAM",parent);}
    private int rows(){return Math.max(2,(h-115)/47);}
    protected void init(){
        super.init();back();
        button(14,h-43,30,18,"<",()->page=Math.max(0,page-1));
        button(w-44,h-43,30,18,">",()->page=Math.min((SynergyService.ALL.size()-1)/rows(),page+1));
    }
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        background(g);var p=ClientState.profile;var active=SynergyService.active(p);
        g.text(font,active.size()+" combinações ativas · as duas profissões precisam do nível 10",x+15,y+44,MUTED,false);
        for(int i=0;i<rows()&&page*rows()+i<SynergyService.ALL.size();i++){
            var s=SynergyService.ALL.get(page*rows()+i);int yy=65+i*47;boolean enabled=active.contains(s);
            SkillsTheme.panel(g,x+14,y+yy,w-28,42,PANEL,enabled?SkillsTheme.GREEN:SkillsTheme.BORDER);
            g.item(SkillsTheme.icon(s.a()),x+22,y+yy+6);g.item(SkillsTheme.icon(s.b()),x+43,y+yy+6);
            g.text(font,SkillsTheme.fit(font,s.name(),w-107),x+68,y+yy+6,enabled?SkillsTheme.GREEN:TEXT,false);
            String effect=switch(s.effect()){case DEFENSE->"redução de dano";case DAMAGE->"dano";case ORE->"chance de minérios extras";case DROPS->"chance de colheita e pesca extra";case ECONOMY->"economia de construção";case SPEED->"velocidade";case FINDS->"alcance dos radares";};
            g.text(font,SkillsTheme.fit(font,"+"+(int)Math.round(s.amount()*100)+"% "+effect,w-85),x+68,y+yy+20,MUTED,false);
            g.text(font,SkillsTheme.fit(font,s.a().title+" + "+s.b().title,w-85),x+68,y+yy+31,SkillsTheme.FAINT,false);
        }
        g.centeredText(font,"Página "+(page+1)+" / "+(1+(SynergyService.ALL.size()-1)/rows())+" · limite +"+com.murilloskills.skills.effects.EffectDefinition.number(SynergyService.MAX_BONUS*100)+"% por efeito",x+w/2,y+h-36,MUTED);
        widgets(g,mx,my,partial);notification(g);
    }
}
