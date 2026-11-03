package com.murilloskills.client;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.network.Protocol;
import com.murilloskills.data.ProfileCodec;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import org.lwjgl.glfw.GLFW;

/** Workspace shell. Fixed navigation, selection, home, tool grid and scroll content have separate owners. */
public final class ProfessionScreen extends NotebookScreen {
    public enum Page { HOME,CATALOG,PROFESSIONS,TOOLS,CONTRACTS,HELP }
    public enum Tab { SUMMARY,PROGRESSION,BRANCH,CONTROLS }
    public SkillType skill=WorkspaceMemory.skill;
    public Tab tab=WorkspaceMemory.tab;
    public Page page;
    public boolean placement,toolOptions;
    public int branchTier=1;
    public SkillSummary.Group effectGroup=SkillSummary.Group.PASSIVE;
    final ProfessionRows rows=new ProfessionRows();
    private final ProfessionSelection selection=new ProfessionSelection();
    private final Map<AbstractWidget,String> ids=new IdentityHashMap<>();
    private int cx,cy,cw,ch,baseY;
    private KeyMapping capture;
    private String signature="";
    private List<ProfessionTooltip.Paragraph> pendingTooltip;
    public ProfessionTooltip.Bounds tooltipBounds;
    public ProfessionScreen(){super("MurilloSkills",null);page=ClientState.profile.selected.isEmpty()?Page.CATALOG:Page.HOME;}
    public ProfessionScreen(SkillType skill,Tab tab){this();page=Page.PROFESSIONS;this.skill=skill;this.tab=tab;branchTier=currentTier(skill);}
    public static ProfessionScreen tools(boolean place){var s=new ProfessionScreen();s.page=Page.TOOLS;s.placement=place;return s;}
    public Font uiFont(){return font;}public Minecraft minecraftClient(){return minecraft;}
    public int contentX(){return cx;}public int contentY(){return cy;}public int contentW(){return cw;}public int contentH(){return ch;}
    public AbstractWidget action(String id){return ids.entrySet().stream().filter(e->e.getValue().equals(id)).map(Map.Entry::getKey).findFirst().orElse(null);}
    public int professionNavigationCount(){return (int)ids.values().stream().filter(id->id.startsWith("profession:")).count();}
    String focusId(){return getFocused() instanceof AbstractWidget widget?ids.get(widget):null;}
    void restoreFocus(String id){if(id!=null&&action(id)!=null)setFocused(action(id));}
    public boolean capturingKey(){return capture!=null;}
    private static int currentTier(SkillType type){int highest=ClientState.profile.stats(type).unlockLevel();return highest>=75?3:highest>=50?2:1;}
    private String scrollKey(){return page+":"+skill+":"+tab+":"+effectGroup+":"+branchTier;}
    private void remember(){WorkspaceMemory.skill=skill;WorkspaceMemory.tab=tab;WorkspaceMemory.scrolls.put(scrollKey(),rows.scroll);}
    public void refresh(){
        String focus=getFocused() instanceof AbstractWidget widget?ids.get(widget):null;
        rebuildWidgets();
        if(focus!=null)ids.entrySet().stream().filter(e->e.getValue().equals(focus)).findFirst().ifPresent(e->setFocused(e.getKey()));
    }
    public void home(){remember();page=ClientState.profile.selected.isEmpty()?Page.CATALOG:Page.HOME;refresh();}
    public void catalog(){remember();page=Page.CATALOG;refresh();}
    public void detail(SkillType type,Tab next){remember();skill=type;tab=next;page=Page.PROFESSIONS;branchTier=currentTier(type);rows.scroll=WorkspaceMemory.scrolls.getOrDefault(scrollKey(),0);refresh();}
    public void tab(Tab next){remember();tab=next;rows.scroll=WorkspaceMemory.scrolls.getOrDefault(scrollKey(),0);WorkspaceMemory.tab=next;refresh();}
    public void group(SkillSummary.Group next){remember();effectGroup=next;rows.scroll=WorkspaceMemory.scrolls.getOrDefault(scrollKey(),0);refresh();}
    public void tier(int rank){remember();branchTier=rank;rows.scroll=0;refresh();}
    public void tool(boolean place){placement=place;toolOptions=false;refresh();}
    private void page(Page next){remember();page=next;rows.scroll=WorkspaceMemory.scrolls.getOrDefault(scrollKey(),0);refresh();}
    public void confirm(String title,String detail,Runnable action){minecraft.gui.setScreen(new DecisionScreen(this,title,detail,action));}
    public ProfessionRows.Action bind(String id,KeyMapping key){return new ProfessionRows.Action(id,"Tecla: "+key(key),()->{capture=key;refresh();},true);}
    public static String key(KeyMapping mapping){return mapping==null||mapping.isUnbound()?"sem tecla":mapping.getTranslatedKeyMessage().getString();}
    public <T extends AbstractWidget> T addFixed(T widget){return addRenderableWidget(widget);}
    public SkillsButton fixed(String id,int xx,int yy,int width,int height,String title,Runnable action){
        var button=addRenderableWidget(new SkillsButton(xx,yy,width,height,title,action));ids.put(button,id);return button;
    }
    public SkillsButton body(String id,int xx,int yy,int width,int height,String title,Runnable action){return fixed(id,xx,yy,width,height,title,action);}
    public void removeBody(AbstractWidget widget){if(getFocused()==widget)setFocused(null);ids.remove(widget);removeWidget(widget);}
    public void tooltip(GuiGraphicsExtractor g,String text,int mx,int my){pendingTooltip=Arrays.stream(text.split("\n")).map(line->new ProfessionTooltip.Paragraph(line,TEXT)).toList();}
    protected void init(){
        super.init();w=Math.min(940,width-16);h=Math.min(540,height-16);x=(width-w)/2;y=(height-h)/2;
        ids.clear();rows.clear();cx=x+14;cw=w-28;baseY=y+(h<300?59:68);cy=baseY;ch=y+h-25-cy;
        fixed("close",x+w-40,y+9,27,19,"×",this::onClose);
        int nx=x+14,ny=y+(h<300?34:41);String[] labels={"Profissões","Ferramentas","Contratos","Ajuda"};
        Page[] targets={Page.HOME,Page.TOOLS,Page.CONTRACTS,Page.HELP};
        for(int i=0;i<labels.length;i++){final Page next=targets[i];int bw=font.width(labels[i])+15;
            fixed("nav:"+i,nx,ny,bw,18,labels[i],()->{if(next==Page.HOME)home();else page(next);}).selected(i==0&&(page==Page.HOME||page==Page.CATALOG||page==Page.PROFESSIONS)||page==next);nx+=bw+4;
        }
        switch(page){
            case HOME->ProfessionHome.build(this);
            case CATALOG->selection.build(this);
            case TOOLS->ProfessionTools.build(this);
            case PROFESSIONS->{
                var chosen=ClientState.profile.selected;int i=0;
                for(var type:chosen){fixed("profession:"+type,cx+i++*27,cy,23,21,"",()->detail(type,tab)).icon(SkillsTheme.icon(type)).accent(type.color).selected(type==skill)
                    .setTooltip(Tooltip.create(Component.literal(type.title)));}
                int tabsY=cy+(h<300?25:35),tw=(cw-15)/4;String[] names={"Resumo","Progressão","Especialização","Controles"};
                for(int j=0;j<4;j++){final Tab next=Tab.values()[j];fixed("tab:"+next,cx+j*(tw+5),tabsY,tw,18,names[j],()->tab(next)).selected(tab==next).accent(skill.color);}
                cy+=h<300?47:59;ch=y+h-25-cy;int offset=ProfessionDetails.build(this,rows);layoutRows(cy+offset,ch-offset);
            }
            case CONTRACTS->{ProfessionExtras.contracts(this);layoutRows(cy,ch);}
            case HELP->{ProfessionExtras.help(this);layoutRows(cy,ch);}
        }
        signature=signature();
    }
    void layoutRows(int rowY,int rowH){rows.layout(this,cx,rowY,cw,Math.max(20,rowH));}
    private String signature(){
        return ClientState.presentationRevision+":"+ClientState.summaryRevision+":"+ClientState.profile.selected+":"+ClientState.profile.equipped+":"+ClientState.profile.paragon+":"+ClientState.profile.seals+":"+ClientState.activeAbilities+":"+ProfileCodec.JSON.toJson(ClientState.profile.tools);
    }
    public void tick(){
        if(page==Page.CATALOG)selection.tick(this);
        if(!signature.equals(signature())&&capture==null)refresh();
    }
    private boolean hasRows(){return page==Page.PROFESSIONS||page==Page.CONTRACTS||page==Page.HELP||page==Page.TOOLS&&toolOptions;}
    public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(hasRows()&&rows.scroll(this,mx,my,vertical)){remember();return true;}return false;
    }
    private void assign(InputConstants.Key key){capture.setKey(key);KeyMapping.resetMapping();minecraft.options.save();capture=null;refresh();}
    public boolean keyPressed(KeyEvent key){
        if(capture!=null){assign(key.key()==GLFW.GLFW_KEY_ESCAPE?InputConstants.UNKNOWN:InputConstants.getKey(key));return true;}
        if(hasRows()&&(key.key()==GLFW.GLFW_KEY_PAGE_DOWN||key.key()==GLFW.GLFW_KEY_PAGE_UP)){rows.page(this,key.key()==GLFW.GLFW_KEY_PAGE_DOWN?1:-1);remember();return true;}
        return super.keyPressed(key);
    }
    public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(capture!=null){assign(InputConstants.Type.MOUSE.getOrCreate(event.button()));return true;}return super.mouseClicked(event,doubleClick);
    }
    public void onClose(){remember();ClientState.needsSelection=false;super.onClose();}
    public static String abilityStatus(SkillType type){
        var stats=ClientState.profile.stats(type);
        if(ClientState.active(type))return "Em uso · "+((ClientState.activeAbilities.get(type)-ClientState.now()+19)/20)+" s";
        if(stats.activeTier()==0)return "Ativa na conquista 20";
        long cooldown=Math.max(stats.cooldownUntil,ClientState.profile.globalCooldownUntil)-ClientState.now();
        return cooldown>0?"Recarga · "+((cooldown+19)/20)+" s":"Pronta";
    }
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        pendingTooltip=null;tooltipBounds=null;
        background(g);g.text(font,com.murilloskills.MurilloSkills.VERSION,x+font.width("MurilloSkills")+25,y+14,MUTED,false);
        String status=ClientState.profile.selected.size()+"/4 · "+ClientState.profile.seals+" selos";g.text(font,status,x+w-55-font.width(status),y+14,MUTED,false);
        if(page==Page.CATALOG)selection.render(this,g);
        if(page==Page.TOOLS)ProfessionTools.render(this,g);
        if(page==Page.PROFESSIONS){
            var stats=ClientState.profile.stats(skill);int titleX=cx+Math.max(1,ClientState.profile.selected.size())*27+6;
            g.text(font,SkillsTheme.fit(font,skill.title+" · Nv. "+stats.level+(stats.prestige>0?" · P"+stats.prestige:""),cx+cw-titleX),titleX,baseY+2,skill.color,false);
            SkillsTheme.progress(g,titleX,baseY+16,Math.max(20,cx+cw-titleX),4,stats.level==100?1:stats.xp/ProgressionCurve.required(skill,stats.level),skill.color);
            String xp=stats.level==100?"Domínio":SkillsTheme.number(stats.xp)+" / "+SkillsTheme.number(ProgressionCurve.required(skill,stats.level))+" XP";
            if(h>=300)g.text(font,xp,titleX,baseY+23,MUTED,false);
        }
        if(hasRows())rows.render(this,g,mx,my);
        if(page==Page.PROFESSIONS)ProfessionDetails.render(this,g);
        widgets(g,mx,my,partial);
        String footer=capture!=null?"Pressione uma tecla ou botão · Esc remove o atalho":ClientState.messageUntil>ClientState.clientTicks?ClientState.message:page==Page.CATALOG?"Oito profissões disponíveis · escolha até quatro":"Esc / "+key(MurilloSkillsClient.guide)+" fecha · passe o mouse para detalhes";
        g.text(font,SkillsTheme.fit(font,footer,w-28-(page==Page.HOME?116:0)),x+14,y+h-16,capture!=null?GOLD:MUTED,false);
        if(page==Page.CATALOG)tooltipBounds=selection.renderTooltip(this,g,mx,my);
        else if(pendingTooltip!=null)tooltipBounds=ProfessionTooltip.render(g,font,pendingTooltip,skill.color,mx,my);
    }
}

