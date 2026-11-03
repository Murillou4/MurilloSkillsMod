package com.murilloskills.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
/** Native input and narration, with compact profession UI styling. */
public class SkillsButton extends Button {
    private int accent=SkillsTheme.GOLD;private boolean selected,primary;
    private net.minecraft.world.item.ItemStack itemIcon;
    private String shape;
    public SkillsButton(int x,int y,int width,int height,String text,Runnable action){super(x,y,width,height,Component.literal(text),button->action.run(),DEFAULT_NARRATION);}
    public SkillsButton accent(int color){accent=color;return this;}
    public SkillsButton selected(boolean value){selected=value;return this;}
    public SkillsButton primary(){primary=true;return this;}
    public SkillsButton icon(net.minecraft.world.item.ItemStack value){itemIcon=value;return this;}
    public SkillsButton shape(String value){shape=value;return this;}
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial){
        var font=Minecraft.getInstance().font;boolean hover=isHoveredOrFocused();
        int border=active&&(hover||selected||primary)?accent:SkillsTheme.BORDER;
        int background=!active?SkillsTheme.BACKGROUND:selected||primary?0xff3a3b30:hover?SkillsTheme.HOVER:SkillsTheme.RAISED;
        SkillsTheme.panel(g,getX(),getY(),getWidth(),getHeight(),background,border);
        if(selected)g.fill(getX()+2,getY()+3,getX()+4,getBottom()-3,accent);
        int reserved=shape==null?0:23;
        String text=SkillsTheme.fit(font,getMessage().getString(),getWidth()-(getWidth()<30?2:10)-reserved);
        g.text(font,text,getX()+reserved+(getWidth()-reserved-font.width(text))/2,getY()+(getHeight()-8)/2,active?(primary||selected?accent:SkillsTheme.TEXT):SkillsTheme.FAINT,false);
        if(shape!=null)drawShape(g,getX()+7,getY()+(getHeight()-14)/2,active?accent:SkillsTheme.FAINT);
        if(itemIcon!=null)g.item(itemIcon,getX()+(getWidth()-16)/2,getY()+(getHeight()-16)/2);
    }
    private void drawShape(GuiGraphicsExtractor g,int x,int y,int color){
        ShapeChoice.glyph(g,shape,x,y,color);
    }
}

