package com.murilloskills.client;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
public final class DecisionScreen extends NotebookScreen {
    private final String detail;private final Runnable confirm;
    public DecisionScreen(Screen parent,String title,String detail,Runnable confirm){super(title,parent);this.detail=detail;this.confirm=confirm;}
    protected void init(){
        super.init();w=Math.min(430,width-32);h=Math.min(height-30,Math.max(140,font.split(net.minecraft.network.chat.Component.literal(detail),w-40).size()*11+102));x=(width-w)/2;y=(height-h)/2;
        button(18,h-38,(w-46)/2,22,"Cancelar",this::onClose);
        button(28+(w-46)/2,h-38,(w-46)/2,22,"Confirmar",()->{confirm.run();onClose();}).primary();
    }
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        if(parent!=null)parent.extractRenderState(g,-1,-1,partial);g.nextStratum();g.fill(0,0,width,height,0xb8000008);
        SkillsTheme.panel(g,x,y,w,h,BG,SkillsTheme.GOLD);g.text(font,getTitle(),x+18,y+18,GOLD,false);g.fill(x+18,y+35,x+w-18,y+36,SkillsTheme.BORDER);
        wrap(g,detail,18,50,w-36,TEXT);widgets(g,mx,my,partial);
    }
}

