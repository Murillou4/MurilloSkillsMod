package com.murilloskills.client;
import com.murilloskills.network.Protocol;
import net.minecraft.client.gui.*;
public final class AbilityRadialScreen extends NotebookScreen {
    public AbilityRadialScreen(){super("HABILIDADES",null);}
    protected void init(){
        super.init();w=Math.min(430,width-20);h=Math.min(300,height-20);x=(width-w)/2;y=(height-h)/2;back();
        int bw=110,bh=47,cx=w/2,cy=(h+34)/2,spreadX=Math.min(w/2-63,120),spreadY=Math.min((h-88)/2,85);
        int[][] offsets={{0,-spreadY},{spreadX,0},{0,spreadY},{-spreadX,0}};int i=0;
        for(var type:ClientState.profile.selected){
            int xx=cx+offsets[i][0]-bw/2,yy=cy+offsets[i++][1]-bh/2;
            addRenderableWidget(new SkillCard(x+xx,y+yy,bw,bh,type,()->{ClientState.send(Protocol.Op.EQUIP,type.name(),0);onClose();}));
        }
    }
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        background(g);int cx=x+w/2,cy=y+(h+34)/2;
        g.fill(cx-1,y+63,cx+1,y+h-35,SkillsTheme.BORDER);g.fill(x+35,cy-1,x+w-35,cy+1,SkillsTheme.BORDER);
        SkillsTheme.panel(g,cx-26,cy-18,52,36,BG,SkillsTheme.BORDER);g.centeredText(font,"ATIVA",cx,cy-8,GOLD);g.centeredText(font,ProfessionScreen.key(MurilloSkillsClient.ability),cx,cy+5,MUTED);
        widgets(g,mx,my,partial);notification(g);
    }
}

