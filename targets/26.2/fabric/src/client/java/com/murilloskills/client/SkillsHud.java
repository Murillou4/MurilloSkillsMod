package com.murilloskills.client;
import com.murilloskills.skills.*;
import com.murilloskills.network.ActivityView;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;

public final class SkillsHud {
    private SkillsHud(){}
    public static void extract(GuiGraphicsExtractor g,DeltaTracker delta){
        var mc=Minecraft.getInstance();if(!ClientState.available||mc.player==null||mc.gui.screen()!=null||!ClientPreferences.current.hud)return;
        var p=ClientState.profile;var font=mc.font;int x=9,y=9,w=224,maxY=mc.getWindow().getGuiScaledHeight()-44;
        if(p.equipped!=null&&ClientPreferences.current.showEquippedCard(ClientState.active(p.equipped))){
            var stats=p.stats(p.equipped);
            SkillsTheme.panel(g,x,y,w,34,0xd0252622,SkillsTheme.BORDER);g.item(SkillsTheme.icon(p.equipped),x+6,y+8);
            g.text(font,p.equipped.title+" · "+stats.level+(stats.prestige>0?" · P"+stats.prestige:""),x+29,y+6,p.equipped.color,false);
            g.text(font,SkillsTheme.fit(font,ProfessionScreen.abilityStatus(p.equipped)+(ClientState.active(p.equipped)?" · "+ProfessionScreen.key(MurilloSkillsClient.ability)+" encerra":""),w-36),x+29,y+19,SkillsTheme.TEXT,false);y+=39;
        }
        boolean others=ClientState.activeAbilities.entrySet().stream().anyMatch(e->e.getKey()!=p.equipped&&e.getValue()>ClientState.now());
        if(others){int xx=x;
            for(var e:ClientState.activeAbilities.entrySet())if(e.getKey()!=p.equipped&&e.getValue()>ClientState.now()){
                g.item(SkillsTheme.icon(e.getKey()),xx,y);g.text(font,""+((e.getValue()-ClientState.now()+19)/20),xx+18,y+5,e.getKey().color,false);xx+=48;
            }y+=22;
        }
        if(MiningInput.held()||PlacementInput.armed()){
            boolean mine=MiningInput.held();String blocked=mine?MiningInput.blockedReason():"";
            String title=mine?"Ultmine · "+ToolScreen.label(p.tools.miningShape):"UltPlace armado";
            String hint=!blocked.isEmpty()?blocked:mine?(ClientPreferences.current.miningToggle?"Aperte novamente para desligar":"Segure e quebre o primeiro bloco"):"Clique direito · "+ProfessionScreen.key(MurilloSkillsClient.place)+" desarma";
            SkillsTheme.panel(g,x,y,w,30,0xd0252622,blocked.isEmpty()?SkillsTheme.GOLD:0xffd87c70);
            g.text(font,title,x+7,y+5,SkillsTheme.GOLD,false);g.text(font,SkillsTheme.fit(font,hint,w-14),x+7,y+18,SkillsTheme.TEXT,false);y+=35;
        }
        if(ClientPreferences.current.abilityVisuals){
            ActivityView activity=ClientState.activities.values().stream().filter(a->a.until()>ClientState.now())
                .filter(a->!a.kind().startsWith("tool:")||MiningInput.held()||PlacementInput.armed())
                .max(java.util.Comparator.comparingLong(ActivityView::until)).orElse(null);
            if(activity!=null&&y+42<maxY){
                SkillsTheme.panel(g,x,y,w,activity.total()>0?40:32,0xd0252622,SkillsTheme.BORDER);
                g.text(font,SkillsTheme.fit(font,activity.title(),w-14),x+7,y+5,SkillsTheme.GOLD,false);
                g.text(font,SkillsTheme.fit(font,activity.detail(),w-14),x+7,y+18,SkillsTheme.TEXT,false);
                if(activity.total()>0)SkillsTheme.progress(g,x+7,y+32,w-14,4,activity.done()/(double)activity.total(),SkillsTheme.GREEN);
                y+=activity.total()>0?45:37;
            }
        }
        if(ClientState.xpUntil>ClientState.clientTicks&&ClientState.xpSkill!=null&&ClientPreferences.current.xpNotices&&y+17<maxY){
            g.text(font,"+"+SkillsTheme.number(ClientState.recentXp)+" XP · "+ClientState.xpSkill.title,x+3,y+3,ClientState.xpSkill.color,false);y+=18;
        }
        if(ClientState.milestoneUntil>ClientState.clientTicks&&ClientState.milestoneSkill!=null&&y+25<maxY){
            SkillsTheme.panel(g,x,y,w,23,0xe0252622,SkillsTheme.GOLD);
            g.text(font,SkillsTheme.fit(font,"Marco "+ClientState.milestoneLevel+" · "+ClientState.milestoneTitle,w-14),x+7,y+8,SkillsTheme.GOLD,false);
        }
        if(ClientState.messageUntil>ClientState.clientTicks&&!ClientState.message.isEmpty()){
            int yy=mc.getWindow().getGuiScaledHeight()-42;
            SkillsTheme.panel(g,x,yy,Math.min(mc.getWindow().getGuiScaledWidth()-18,Math.max(w,font.width(ClientState.message)+14)),16,0xdd252622,SkillsTheme.BORDER);
            g.text(font,SkillsTheme.fit(font,ClientState.message,mc.getWindow().getGuiScaledWidth()-34),x+7,yy+4,SkillsTheme.TEXT,false);
        }
        if(com.murilloskills.skills.effects.Feature.PATHFINDER.supplied(p,SkillType.EXPLORER)){
            var pos=mc.player.blockPosition();String direction=switch(mc.player.getDirection()){case NORTH->"N";case SOUTH->"S";case EAST->"L";case WEST->"O";default->"";};
            String text=pos.getX()+"  "+pos.getY()+"  "+pos.getZ()+" / "+direction;
            int yy=mc.getWindow().getGuiScaledHeight()-23;g.text(font,text,x+4,yy+3,SkillsTheme.MUTED,false);
        }
    }
}

