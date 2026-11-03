package com.murilloskills.client;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public abstract class NotebookScreen extends Screen {
    public static final int BG=SkillsTheme.BACKGROUND,PANEL=SkillsTheme.PANEL,TEXT=SkillsTheme.TEXT,MUTED=SkillsTheme.MUTED,GOLD=SkillsTheme.GOLD;
    protected int x,y,w,h;protected final Screen parent;
    protected NotebookScreen(String title,Screen parent){super(Component.literal(title));this.parent=parent;}
    protected void init(){w=Math.min(760,width-20);h=Math.min(430,height-20);x=(width-w)/2;y=(height-h)/2;}
    protected SkillsButton button(int dx,int dy,int width,String label,Runnable action){return button(dx,dy,width,20,label,action);}
    protected SkillsButton button(int dx,int dy,int width,int height,String label,Runnable action){return addRenderableWidget(new SkillsButton(x+dx,y+dy,width,height,label,action));}
    protected void back(){button(w-70,12,56,18,"Voltar",this::onClose);}
    protected void tabs(){
        int start=12;String[] labels={"Profissões","Contratos","Guia","Ferramentas"};
        Runnable[] actions={()->minecraft.gui.setScreen(new ProfessionScreen()),()->minecraft.gui.setScreen(new ContractScreen(this)),()->minecraft.gui.setScreen(new GuideScreen(this)),()->minecraft.gui.setScreen(new ToolScreen(false))};
        for(int i=0;i<labels.length;i++){int bw=i==0?86:i==1?80:i==2?50:86;button(start,38,bw,18,labels[i],actions[i]);start+=bw+5;}
    }
    public void onClose(){minecraft.gui.setScreen(parent);}public boolean isPauseScreen(){return false;}
    public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){}
    protected void background(GuiGraphicsExtractor g){g.fill(0,0,width,height,0xb812140e);SkillsTheme.panel(g,x,y,w,h,BG,SkillsTheme.BORDER);g.fill(x+1,y+1,x+w-1,y+33,0xff20221d);g.fill(x+12,y+33,x+w-12,y+34,SkillsTheme.BORDER);g.text(font,getTitle(),x+14,y+14,TEXT,false);}
    protected void widgets(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.nextStratum();
        for(var child:children())if(child instanceof Renderable renderable&&!(child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget.getY()<y+34))renderable.extractRenderState(g,mx,my,partial);
        g.nextStratum();
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget.getY()<y+34)widget.extractRenderState(g,mx,my,partial);
    }
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){background(g);widgets(g,mx,my,partial);notification(g);}
    protected void notification(GuiGraphicsExtractor g){if(ClientState.messageUntil>ClientState.clientTicks&&!ClientState.message.isEmpty()){SkillsTheme.panel(g,x+8,y+h-20,w-16,16,0xff272431,0xff665339);g.text(font,SkillsTheme.fit(font,ClientState.message,w-32),x+16,y+h-16,GOLD,false);}}
    protected int wrap(GuiGraphicsExtractor g,String text,int dx,int dy,int maxWidth,int color){for(var line:font.split(Component.literal(text),Math.max(20,maxWidth))){g.text(font,line,x+dx,y+dy,color,false);dy+=11;}return dy;}
    protected void bar(GuiGraphicsExtractor g,int dx,int dy,int width,double fraction,int color){SkillsTheme.progress(g,x+dx,y+dy,width,6,fraction,color);}
}

