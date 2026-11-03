package com.murilloskills.client;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

/** Scroll content owns its widgets explicitly; navigation can never be removed by its coordinates. */
final class ProfessionRows {
    record Action(String id,String text,Runnable run,boolean enabled){}
    record Row(String id,String title,String detail,String tooltip,int color,List<Action> actions,String left,String right,DoubleSupplier progress,Supplier<String> liveTitle,boolean expanded){}
    record Positioned(Row row,int top,int height){}
    final List<Row> rows=new ArrayList<>();
    private final List<AbstractWidget> widgets=new ArrayList<>();
    private final List<Positioned> layout=new ArrayList<>();
    int scroll,total;private int x,y,w,h;
    void clear(){rows.clear();}
    void add(String id,String title,String detail,int color,Action... actions){rows.add(new Row(id,title,detail,detail,color,List.of(actions),null,null,null,null,false));}
    void live(String id,String title,String detail,int color,Supplier<String> live,Action... actions){rows.add(new Row(id,title,detail,detail,color,List.of(actions),null,null,null,live,false));}
    void effect(String id,String title,String detail,String tooltip,int color){rows.add(new Row(id,title,detail,tooltip,color,List.of(),null,null,null,null,false));}
    void milestone(String id,String title,String detail,int color){rows.add(new Row(id,title,detail,"",color,List.of(),null,null,null,null,true));}
    void compare(String id,String title,String left,String right,String tooltip){rows.add(new Row(id,title,"",tooltip,SkillsTheme.TEXT,List.of(),left,right,null,null,false));}
    void progress(String id,String title,String detail,int color,DoubleSupplier progress,Supplier<String> live){rows.add(new Row(id,title,detail,detail,color,List.of(),null,null,progress,live,false));}
    void layout(ProfessionScreen s,int x,int y,int w,int h){
        String focus=s.focusId();
        for(var widget:widgets)s.removeBody(widget);widgets.clear();layout.clear();this.x=x;this.y=y;this.w=w;this.h=h;
        int top=0;
        for(var row:rows){
            int titleLines=Math.min(2,s.uiFont().split(Component.literal(row.title),w-18).size());
            int availableLines=s.uiFont().split(Component.literal(row.detail),w-18).size();
            int detailLines=row.detail.isEmpty()?0:row.expanded?availableLines:Math.min(2,availableLines);
            int height=titleLines*11+detailLines*11+10+(row.actions.isEmpty()?0:24)+(row.left!=null?19:0)+(row.progress!=null?10:0);
            layout.add(new Positioned(row,top,height));top+=height+5;
        }
        total=Math.max(0,top-5);scroll=Math.clamp(scroll,0,Math.max(0,total-h));
        for(var placed:layout){
            var row=placed.row;int buttonY=y+placed.top-scroll+placed.height-25;
            if(row.actions.isEmpty()||buttonY<y||buttonY+19>y+h)continue;
            int n=row.actions.size(),bw=(w-16-5*(n-1))/n;
            for(int i=0;i<n;i++){
                var action=row.actions.get(i);var button=s.body(action.id,x+8+i*(bw+5),buttonY,bw,19,action.text,action.run);
                button.active=action.enabled;widgets.add(button);
            }
        }
        s.restoreFocus(focus);
    }
    boolean scroll(ProfessionScreen s,double mx,double my,double amount){
        if(mx<x||mx>x+w||my<y||my>y+h||amount==0)return false;
        scroll=Math.clamp(scroll-(int)(amount*30),0,Math.max(0,total-h));layout(s,x,y,w,h);return true;
    }
    void page(ProfessionScreen s,int direction){scroll=Math.clamp(scroll+direction*Math.max(30,h-20),0,Math.max(0,total-h));layout(s,x,y,w,h);}
    void render(ProfessionScreen s,GuiGraphicsExtractor g,int mx,int my){
        g.enableScissor(x,y,x+w,y+h);
        for(var placed:layout){
            var row=placed.row;int yy=y+placed.top-scroll;if(yy+placed.height<y||yy>y+h)continue;
            if(!row.actions.isEmpty()||row.left!=null)SkillsTheme.panel(g,x,yy,w,placed.height,SkillsTheme.PANEL,SkillsTheme.BORDER);
            else g.fill(x+5,yy+placed.height-1,x+w-5,yy+placed.height,0x55484940);
            String title=row.liveTitle==null?row.title:row.liveTitle.get();
            int ty=yy+6;
            var titleLines=s.uiFont().split(Component.literal(title),w-18);
            for(int i=0;i<Math.min(2,titleLines.size());i++){g.text(s.uiFont(),titleLines.get(i),x+8,ty,row.color,false);ty+=11;}
            if(!row.detail.isEmpty()){
                var details=s.uiFont().split(Component.literal(row.detail),w-18);
                for(int i=0;i<(row.expanded?details.size():Math.min(2,details.size()));i++){g.text(s.uiFont(),details.get(i),x+8,ty,SkillsTheme.MUTED,false);ty+=11;}
            }
            if(row.left!=null){
                g.text(s.uiFont(),SkillsTheme.fit(s.uiFont(),row.left,(w-24)/2),x+8,ty+3,SkillsTheme.GOLD,false);
                g.text(s.uiFont(),SkillsTheme.fit(s.uiFont(),row.right,(w-24)/2),x+w/2+5,ty+3,SkillsTheme.GREEN,false);
            }
            if(row.progress!=null)SkillsTheme.progress(g,x+8,yy+placed.height-9,w-16,4,row.progress.getAsDouble(),row.color);
            if(mx>=x&&mx<x+w&&my>=Math.max(y,yy)&&my<Math.min(y+h,yy+placed.height)&&!row.tooltip.isEmpty())
                s.tooltip(g,row.tooltip,mx,my);
        }
        g.disableScissor();
        if(total>h){int thumb=Math.max(16,h*h/total),offset=(int)((h-thumb)*(scroll/(double)Math.max(1,total-h)));g.fill(x+w+3,y,x+w+5,y+h,SkillsTheme.BORDER);g.fill(x+w+3,y+offset,x+w+5,y+offset+thumb,SkillsTheme.MUTED);}
    }
}
