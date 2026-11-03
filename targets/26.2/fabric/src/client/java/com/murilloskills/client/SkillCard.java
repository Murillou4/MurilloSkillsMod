package com.murilloskills.client;
import com.murilloskills.skills.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class SkillCard extends Button {
    public final SkillType skill;
    public SkillCard(int x,int y,int w,int h,SkillType skill,Runnable open){super(x,y,w,h,Component.literal(skill.title),button->open.run(),DEFAULT_NARRATION);this.skill=skill;}
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial){
        var font=Minecraft.getInstance().font;var p=ClientState.profile;var s=p.stats(skill);
        boolean chosen=p.selected(skill),paragon=p.paragon==skill,hover=isHoveredOrFocused();int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        int accent=paragon?SkillsTheme.GOLD:chosen?skill.color:SkillsTheme.FAINT;
        SkillsTheme.panel(g,x,y,w,h,hover?SkillsTheme.HOVER:chosen?0xff1c2230:SkillsTheme.PANEL,hover?skill.color:paragon?SkillsTheme.GOLD:chosen?SkillsTheme.BORDER:0xff252839);
        g.fill(x+1,y+4,x+3,y+h-4,accent);
        if(h<52){
            g.item(SkillsTheme.icon(skill),x+8,y+6);g.text(font,SkillsTheme.fit(font,skill.title,h>=39?w-38:w-67),x+29,y+6,chosen?SkillsTheme.WHITE:skill.color,false);
            String level=""+s.level+(s.prestige>0?" P"+s.prestige:"");if(h<39)g.text(font,level,x+w-font.width(level)-8,y+6,accent,false);
            SkillsTheme.progress(g,x+29,y+20,w-38,5,s.level>=100?1:s.xp/ProgressionCurve.required(skill,s.level),accent);
            if(h>=39)g.text(font,SkillsTheme.fit(font,"Nível "+level,w-38),x+29,y+28,accent,false);
        }else{
            SkillsTheme.item(g,SkillsTheme.icon(skill),x+9,y+12,1.35f);g.text(font,SkillsTheme.fit(font,skill.title,w-44),x+36,y+12,chosen?SkillsTheme.WHITE:skill.color,false);
            g.text(font,"NV. "+s.level+(s.prestige>0?" · P"+s.prestige:""),x+36,y+25,accent,false);
            if(paragon)g.text(font,"✦",x+w-16,y+11,SkillsTheme.GOLD,false);else if(chosen)g.fill(x+w-12,y+13,x+w-8,y+17,skill.color);
            SkillsTheme.progress(g,x+10,y+43,w-20,6,s.level>=100?1:s.xp/ProgressionCurve.required(skill,s.level),accent);
            g.text(font,SkillsTheme.fit(font,chosen?(s.level==100?"Domínio conquistado":skill.branch(s.branch)):"Escolher profissão",w-20),x+10,y+54,chosen?SkillsTheme.TEXT:SkillsTheme.MUTED,false);
            if(h>=110){g.text(font,SkillsTheme.fit(font,skill.ability,w-20),x+10,y+73,SkillsTheme.MUTED,false);g.text(font,"Ativa · nível 20",x+10,y+85,SkillsTheme.FAINT,false);}
            if(h>=90){int step=Math.max(3,(w-20)/20);for(int i=0;i<20;i++)g.fill(x+10+i*step,y+h-21,x+12+i*step,y+h-17,s.unlockLevel()>=(i+1)*5?accent:SkillsTheme.BORDER);
                g.text(font,SkillsTheme.fit(font,s.level==100?"PRESTÍGIO DISPONÍVEL":"Próximo marco · "+MilestoneCatalog.next(s.level),w-20),x+10,y+h-12,SkillsTheme.MUTED,false);}
        }
        if(hover){List<Component> lines=new ArrayList<>();lines.add(Component.literal(skill.title+" · nível "+s.level).withColor(skill.color));
            lines.add(Component.literal(chosen?"Profissão escolhida · "+skill.branch(s.branch):"Escolha até quatro profissões para esta jornada."));
            lines.add(Component.literal(skill.ability+" · "+ProfessionScreen.abilityStatus(skill)));
            if(chosen&&s.level<100)lines.add(Component.literal(SkillsTheme.number(s.xp)+" / "+SkillsTheme.number(ProgressionCurve.required(skill,s.level))+" XP"));
            lines.add(Component.literal("Clique para equipar esta habilidade.").withColor(SkillsTheme.GOLD));g.setComponentTooltipForNextFrame(font,lines,mx,my);}
    }
}

