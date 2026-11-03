package com.murilloskills.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

/** A compact profession card measured before placement, including padding and screen margins. */
public final class ProfessionTooltip {
    public record Paragraph(String text,int color){}
    public record Bounds(int x,int y,int width,int height){}
    private record Line(FormattedCharSequence text,int color){}
    private ProfessionTooltip(){}
    public static Bounds render(GuiGraphicsExtractor g,Font font,List<Paragraph> paragraphs,int accent,int mx,int my){
        int margin=8,width=Math.min(g.guiWidth()-margin*2,Math.min(280,Math.max(200,g.guiWidth()/2)));
        int maxLines=Math.max(1,(g.guiHeight()-margin*2-20)/11);
        var lines=new ArrayList<Line>();
        for(var paragraph:paragraphs)for(var line:font.split(Component.literal(paragraph.text()),width-20))lines.add(new Line(line,paragraph.color()));
        if(lines.isEmpty())return null;
        if(lines.size()>maxLines){
            lines.subList(maxLines,lines.size()).clear();
            lines.set(maxLines-1,new Line(Component.literal("… Veja a trilha para os detalhes.").getVisualOrderText(),SkillsTheme.MUTED));
        }
        int height=20+lines.size()*11;
        int x=mx+12;if(x+width>g.guiWidth()-margin)x=mx-width-12;
        x=Math.clamp(x,margin,Math.max(margin,g.guiWidth()-width-margin));
        int y=Math.clamp(my+12,margin,Math.max(margin,g.guiHeight()-height-margin));
        g.nextStratum();
        SkillsTheme.panel(g,x,y,width,height,SkillsTheme.PANEL,SkillsTheme.BORDER);
        g.fill(x+1,y+4,x+3,y+height-4,accent);
        for(int i=0;i<lines.size();i++){var line=lines.get(i);g.text(font,line.text(),x+10,y+8+i*11+(i>0?4:0),line.color(),false);}
        return new Bounds(x,y,width,height);
    }
}
