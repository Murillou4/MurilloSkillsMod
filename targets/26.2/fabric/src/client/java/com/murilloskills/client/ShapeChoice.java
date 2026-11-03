package com.murilloskills.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class ShapeChoice extends SkillsButton {
    private final String shape,label;private final int color;private final boolean chosen;
    public ShapeChoice(int x,int y,int width,int height,String shape,String label,int color,boolean chosen,Runnable action){super(x,y,width,height,label,action);this.shape=shape;this.label=label;this.color=color;this.chosen=chosen;}
    protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial){
        SkillsTheme.panel(g,getX(),getY(),getWidth(),getHeight(),isHoveredOrFocused()?SkillsTheme.HOVER:chosen?0xff252b39:SkillsTheme.PANEL,chosen?color:SkillsTheme.BORDER);
        boolean compact=getWidth()<92;int sx=getX()+(compact?getWidth()/2-7:8),sy=getY()+5;
        glyph(g,shape,sx,sy,chosen?color:SkillsTheme.MUTED);
        var font=Minecraft.getInstance().font;String text=SkillsTheme.fit(font,label,getWidth()-(compact?6:34));
        g.text(font,text,compact?getX()+(getWidth()-font.width(text))/2:getX()+29,getY()+(compact?getHeight()-10:14),chosen?color:SkillsTheme.TEXT,false);
    }
    public static void glyph(GuiGraphicsExtractor g,String shape,int x,int y,int color){
        String pattern=switch(shape){
            case "VEIN"->"0110011000011000011000010";
            case "3X3"->"0000001110011100111000000";
            case "2X1"->"0000000000011000000000000";
            case "LINE"->"0000000000111110000000000";
            case "STAIRS"->"0000100011001110111111111";
            case "COLUMN"->"0010000100001000010000100";
            case "TUNNEL"->"1111110001100011000111111";
            case "CIRCLE"->"0111011111111111111101110";
            case "SPHERE"->"0111010001101011000101110";
            case "SINGLE"->"0000000000001000000000000";
            case "BOX"->"0111111001101011001111110";
            case "WALL"->"1111111111111111111111111";
            default->"0000001111111101111000000";
        };
        for(int i=0;i<25;i++)if(pattern.charAt(i)=='1')g.fill(x+i%5*3,y+i/5*3,x+i%5*3+2,y+i/5*3+2,color);
    }
}
