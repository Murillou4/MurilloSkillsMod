package com.murilloskills.client;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import com.murilloskills.skills.SkillType;
import net.minecraft.world.item.*;
public final class SkillsTheme {
    public static final int BACKGROUND=0xff1b1c1a,PANEL=0xff252622,RAISED=0xff30312b,BORDER=0xff484940,HOVER=0xff3b3e33;
    public static final int TEXT=0xffedeadd,WHITE=0xfffff9e9,MUTED=0xffb1b2a4,FAINT=0xff73766a,GOLD=0xffe1bd77,GREEN=0xffa5c68e;
    private SkillsTheme(){}
    public static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h,int background,int border){
        if(w<3||h<3)return;
        g.fill(x+1,y,x+w-1,y+h,background);g.fill(x,y+1,x+w,y+h-1,background);
        g.fill(x+1,y,x+w-1,y+1,border);g.fill(x+1,y+h-1,x+w-1,y+h,border);
        g.fill(x,y+1,x+1,y+h-1,border);g.fill(x+w-1,y+1,x+w,y+h-1,border);
        g.fill(x+2,y+1,x+w-2,y+2,0x10ffffff);
    }
    public static void progress(GuiGraphicsExtractor g,int x,int y,int width,int height,double fraction,int color){
        panel(g,x,y,width,height,0xff10131e,BORDER);int fill=(int)((width-2)*Math.clamp(fraction,0,1));
        if(fill>0){g.fill(x+1,y+1,x+1+fill,y+height-1,color);g.fill(x+1,y+1,x+1+fill,y+2,0x35ffffff);}
    }
    public static ItemStack icon(SkillType type){return switch(type){
        case MINER->new ItemStack(Items.DIAMOND_PICKAXE);case WARRIOR->new ItemStack(Items.IRON_SWORD);case ARCHER->new ItemStack(Items.BOW);case FARMER->new ItemStack(Items.WHEAT);
        case FISHER->new ItemStack(Items.FISHING_ROD);case BLACKSMITH->new ItemStack(Items.ANVIL);case BUILDER->new ItemStack(Items.BRICKS);case EXPLORER->new ItemStack(Items.COMPASS);};}
    public static void item(GuiGraphicsExtractor g,ItemStack stack,int x,int y,float scale){g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);g.item(stack,0,0);g.pose().popMatrix();}
    public static String fit(Font font,String text,int width){return font.width(text)<=width?text:font.plainSubstrByWidth(text,Math.max(0,width-7))+"…";}
    public static String number(double value){return value>=1000000?String.format(java.util.Locale.ROOT,"%.1fM",value/1000000):value>=10000?String.format(java.util.Locale.ROOT,"%.1fk",value/1000):com.murilloskills.skills.effects.EffectDefinition.number(value);}
}

