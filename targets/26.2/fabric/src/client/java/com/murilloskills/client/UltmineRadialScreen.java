package com.murilloskills.client;

import com.murilloskills.network.Protocol;
import com.murilloskills.skills.ShapePlanner;
import com.murilloskills.skills.SkillType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import static com.murilloskills.client.SkillsTheme.*;

/** Six floating tool cards; the pointing gesture stays radial, without a rasterized wheel. */
public final class UltmineRadialScreen extends Screen {
    private final Screen parent;
    private final boolean heldOpen;
    private final List<Entry> entries=new ArrayList<>();
    private int cx,cy,spreadX,spreadY,cardWidth,hubWidth,hovered=-1;
    private static final int CARD_HEIGHT=40,HUB_HEIGHT=80;
    private double pointerX,pointerY;
    private boolean keyboardNavigation;
    private OptionsButton optionsButton;

    public UltmineRadialScreen(){this(null,true);}
    public UltmineRadialScreen(Screen parent,boolean heldOpen){
        super(Component.literal("Ultmine"));this.parent=parent;this.heldOpen=heldOpen;
    }
    @Override protected void init(){
        cx=width/2;cy=(height-30)/2;
        cardWidth=Math.min(96,Math.max(66,(width-24)/3));
        hubWidth=Math.min(100,Math.max(76,width/3-4));
        spreadX=Math.min(106,(width-cardWidth-16)/2);
        spreadY=Math.min(86,Math.max(60,(height-86)/2));
        pointerX=cx;pointerY=cy;hovered=-1;keyboardNavigation=false;entries.clear();
        int[][] offsets={{0,-spreadY},{spreadX,-spreadY/2},{spreadX,spreadY/2},
            {0,spreadY},{-spreadX,spreadY/2},{-spreadX,-spreadY/2}};
        for(int i=0;i<ShapePlanner.MINING.size();i++){
            var entry=new Entry(cx+offsets[i][0]-cardWidth/2,cy+offsets[i][1]-CARD_HEIGHT/2,i);
            entries.add(addRenderableWidget(entry));
        }
        optionsButton=addRenderableWidget(new OptionsButton(cx-hubWidth/2+6,cy+21,hubWidth-12));
    }

    /** Card corners are clickable too; the hub and the legend are safe areas to release/cancel. */
    private int pointed(double mx,double my){
        if(optionsButton!=null&&optionsButton.isMouseOver(mx,my))return -1;
        for(var entry:entries)if(entry.isMouseOver(mx,my))return entry.index;
        double dx=mx-cx,dy=my-cy;
        if(Math.abs(dx)<hubWidth/2d+5&&Math.abs(dy)<HUB_HEIGHT/2d+5)return -1;
        if(Math.abs(dx)>spreadX+cardWidth/2d+16||Math.abs(dy)>spreadY+CARD_HEIGHT/2d+4)return -1;
        double angle=(Math.atan2(dy/spreadY,dx/spreadX)+Math.PI/2+Math.PI/6+Math.PI*2)%(Math.PI*2);
        return (int)(angle/(Math.PI/3));
    }
    private int highlighted(){
        if(keyboardNavigation)return getFocused() instanceof Entry entry?entry.index:-1;
        return pointed(pointerX,pointerY);
    }
    private void choose(int index){ClientState.send(Protocol.Op.TOOL_CONFIG,"mining",index);onClose();}
    public void releaseAndClose(){int index=highlighted();if(index>=0)choose(index);else onClose();}
    private void options(){minecraft.gui.setScreen(ProfessionScreen.tools(false));}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}

    @Override public void mouseMoved(double mx,double my){
        pointerX=mx;pointerY=my;keyboardNavigation=false;super.mouseMoved(mx,my);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        pointerX=event.x();pointerY=event.y();keyboardNavigation=false;
        if(super.mouseClicked(event,doubleClick))return true;
        int index=pointed(pointerX,pointerY);
        if(event.button()==GLFW.GLFW_MOUSE_BUTTON_LEFT&&index>=0){choose(index);return true;}
        return false;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){
        pointerX=event.x();pointerY=event.y();keyboardNavigation=false;
        if(heldOpen&&MurilloSkillsClient.mineMenu.matchesMouse(event)){releaseAndClose();return true;}
        return super.mouseReleased(event);
    }
    @Override public boolean keyReleased(KeyEvent key){
        if(heldOpen&&MurilloSkillsClient.mineMenu.matches(key)){releaseAndClose();return true;}
        return super.keyReleased(key);
    }
    @Override public boolean keyPressed(KeyEvent key){
        if(MurilloSkillsClient.tools.matches(key)){options();return true;}
        if(key.key()==GLFW.GLFW_KEY_TAB||key.key()>=GLFW.GLFW_KEY_RIGHT&&key.key()<=GLFW.GLFW_KEY_UP)keyboardNavigation=true;
        return super.keyPressed(key);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(vertical==0)return false;
        pointerX=mx;pointerY=my;keyboardNavigation=false;int index=pointed(mx,my);if(index<0)return false;
        String shape=ShapePlanner.MINING.get(index);if(shape.equals("VEIN"))return true;
        String key=shape.equals("QUARRY")?"size":"depth";
        int value=key.equals("size")?ClientState.profile.tools.size:ClientState.profile.tools.depth;
        ClientState.send(Protocol.Op.TOOL_CONFIG,key,Math.clamp(value+(vertical>0?1:-1),1,20));return true;
    }

    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        pointerX=mx;pointerY=my;hovered=highlighted();
        g.fill(0,0,width,height,0xb81b1c1a);
        drawHub(g);
        String hint=heldOpen?"Solte ["+MurilloSkillsClient.mineMenu.getTranslatedKeyMessage().getString()+"] para escolher":"Clique para escolher";
        if(hovered>=0&&!ShapePlanner.MINING.get(hovered).equals("VEIN"))hint+=" · roda ajusta";
        hint+=heldOpen?" · Esc cancela":" · Esc volta";
        int legendWidth=Math.min(width-16,Math.max(240,font.width(hint)+20));
        int legendY=cy+spreadY+CARD_HEIGHT/2+9;
        panel(g,cx-legendWidth/2,legendY,legendWidth,20,BACKGROUND,BORDER);
        center(g,hint,legendY+6,MUTED,legendWidth-16);
        g.nextStratum();
        for(var child:children())if(child instanceof Renderable renderable)renderable.extractRenderState(g,mx,my,partial);
    }
    private void drawHub(GuiGraphicsExtractor g){
        int left=cx-hubWidth/2,top=cy-HUB_HEIGHT/2;
        panel(g,left,top,hubWidth,HUB_HEIGHT,BACKGROUND,BORDER);
        g.item(SkillsTheme.icon(SkillType.MINER),left+9,top+8);
        g.text(font,"Ultmine",left+30,top+12,TEXT,false);
        g.fill(left+8,top+30,left+hubWidth-8,top+31,BORDER);
        String blocked=MiningInput.blockedReason();
        if(blocked.isEmpty()){
            String shape=hovered>=0?ShapePlanner.MINING.get(hovered):ClientState.profile.tools.miningShape;
            center(g,ToolScreen.label(shape),top+38,hovered>=0?GOLD:TEXT,hubWidth-12);
            center(g,"Até "+capacityLabel(),top+51,MUTED,hubWidth-12);
        }else{
            center(g,ClientState.profile.selected(SkillType.MINER)?"Conquista 15":"Minerador",top+38,TEXT,hubWidth-12);
            center(g,"Necessário",top+51,MUTED,hubWidth-12);
        }
    }
    private void center(GuiGraphicsExtractor g,String text,int y,int color,int maxWidth){
        String fit=SkillsTheme.fit(font,text,maxWidth);g.text(font,fit,cx-font.width(fit)/2,y,color,false);
    }
    private String dimensions(String shape){
        var tools=ClientState.profile.tools;
        return switch(shape){
            case "VEIN"->"Mesmo tipo";
            case "QUARRY"->tools.size+" × "+tools.size;
            case "STAIRS"->tools.size+" × "+tools.depth;
            default->"Prof. "+tools.depth;
        };
    }
    public static String capacityLabel(){
        var summary=ClientState.summaries.get(SkillType.MINER);
        return summary==null?"Sincronizando…":summary.effects().stream().filter(e->e.id().equals("miner.area")).findFirst().map(e->e.value()).orElse("Sincronizando…");
    }
    private static int mix(int from,int to,double amount){
        int r=(int)(((from>>16)&255)*(1-amount)+((to>>16)&255)*amount);
        int g=(int)(((from>>8)&255)*(1-amount)+((to>>8)&255)*amount);
        int b=(int)((from&255)*(1-amount)+(to&255)*amount);
        return 0xff000000|r<<16|g<<8|b;
    }
    private final class Entry extends Button {
        final int index;
        private double emphasis;
        private long lastFrame;
        Entry(int x,int y,int index){
            super(x,y,cardWidth,CARD_HEIGHT,Component.literal(ToolScreen.label(ShapePlanner.MINING.get(index))),b->choose(index),DEFAULT_NARRATION);
            this.index=index;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial){
            String shape=ShapePlanner.MINING.get(index);
            boolean chosen=ClientState.profile.tools.miningShape.equals(shape),pointing=hovered==index;
            long now=System.nanoTime();double step=lastFrame==0?1:Math.min(1,(now-lastFrame)/110_000_000d);lastFrame=now;
            emphasis=pointing?Math.min(1,emphasis+step):Math.max(0,emphasis-step);
            int surface=mix(chosen?RAISED:PANEL,HOVER,emphasis);
            int border=chosen?GOLD:mix(BORDER,GOLD,emphasis);
            panel(g,getX(),getY(),getWidth(),getHeight(),surface,border);
            if(chosen)g.fill(getX()+2,getY()+7,getX()+4,getBottom()-7,GOLD);
            g.fill(getX()+8,getY()+8,getX()+30,getY()+32,BACKGROUND);
            ShapeChoice.glyph(g,shape,getX()+12,getY()+13,pointing?WHITE:chosen?GOLD:MUTED);
            int textX=getX()+36,textWidth=getWidth()-42;
            g.text(font,SkillsTheme.fit(font,ToolScreen.label(shape),textWidth),textX,getY()+10,chosen?GOLD:pointing?WHITE:TEXT,false);
            g.text(font,SkillsTheme.fit(font,dimensions(shape),textWidth),textX,getY()+24,MUTED,false);
        }
    }
    private final class OptionsButton extends Button {
        OptionsButton(int x,int y,int width){
            super(x,y,width,16,Component.literal("Configurar"),button->options(),DEFAULT_NARRATION);
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial){
            boolean focus=keyboardNavigation&&isFocused(),hover=isMouseOver(mx,my);
            if(focus||hover)g.fill(getX(),getY(),getRight(),getBottom(),RAISED);
            String label=SkillsTheme.fit(font,"Ajustes ["+MurilloSkillsClient.tools.getTranslatedKeyMessage().getString()+"]",getWidth()-6);
            g.text(font,label,getX()+(getWidth()-font.width(label))/2,getY()+4,focus||hover?WHITE:GOLD,false);
        }
    }
}
