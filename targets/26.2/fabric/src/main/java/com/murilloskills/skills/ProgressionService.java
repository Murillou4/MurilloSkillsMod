package com.murilloskills.skills;

import com.murilloskills.data.*;
import java.util.*;

/** Pure server rules: also used by deterministic progression tests. */
public final class ProgressionService {
    public static final int BRANCH_SWAP_COST=1,SKILL_SWAP_COST=3,PARAGON_SWAP_COST=3,MAX_PRESTIGE=10;
    private ProgressionService() {}
    public record Gain(int from, int to, double amount, List<Integer> firstMilestones) {}
    public static Gain addXp(PlayerProfile p, SkillType type, double base, double multiplier) {
        SkillStats s=p.stats(type); int old=s.level;
        if(!p.selected(type)||!Double.isFinite(base)||base<=0||!Double.isFinite(multiplier)||multiplier<=0||s.level>=100)
            return new Gain(old,old,0,List.of());
        double amount=base*multiplier*ProgressionCurve.xpBonus(s.prestige);
        if(!Double.isFinite(amount)) return new Gain(old,old,0,List.of());
        s.xp+=amount;
        while(s.level<100 && s.xp>=ProgressionCurve.required(type,s.level)) {
            s.xp-=ProgressionCurve.required(type,s.level); s.level++;
        }
        if(s.level==100) s.xp=0;
        s.highestLevel=Math.max(s.highestLevel,s.level);
        List<Integer> crossed=new ArrayList<>();
        for(int l=5;l<=s.level;l+=5) if(l>s.claimedMilestone) crossed.add(l);
        s.claimedMilestone=Math.max(s.claimedMilestone,s.level/5*5);
        for(int l:crossed) p.achievements.add(type.name()+":"+l);
        return new Gain(old,s.level,amount,List.copyOf(crossed));
    }
    public static String select(PlayerProfile p, SkillType incoming, SkillType outgoing, boolean locked) {
        if(locked) return "Aguarde o fim do combate e da habilidade.";
        if(p.selected(incoming)) return "Essa profissão já está escolhida.";
        if(outgoing!=null && !p.selected(outgoing)) return "Profissão de origem inválida.";
        if(outgoing==null && p.selected.size()>=4) return "Escolha qual profissão substituir.";
        int cost=outgoing==null?0:SKILL_SWAP_COST;
        if(p.seals<cost) return "São necessários "+cost+" selos.";
        p.seals-=cost;
        if(outgoing!=null) { p.selected.remove(outgoing); if(p.paragon==outgoing)p.paragon=null; }
        p.selected.add(incoming);
        if(p.equipped==null||p.equipped==outgoing)p.equipped=incoming;
        return "";
    }
    public static String branch(PlayerProfile p, SkillType type, int choice, boolean locked) {
        var s=p.stats(type);
        if(locked) return "Aguarde o fim do combate e da habilidade.";
        if(!p.unlocked(type,25)||choice<1||choice>2) return "Ramo disponível a partir do nível 25.";
        if(s.branch==choice)return "";
        int cost=s.branch==0?0:BRANCH_SWAP_COST;
        if(p.seals<cost)return "É necessário um selo.";
        p.seals-=cost;s.branch=choice;return "";
    }
    public static String paragon(PlayerProfile p, SkillType type, boolean locked) {
        if(locked)return "Aguarde o fim do combate e da habilidade.";
        if(!p.unlocked(type,100))return "Domine a profissão antes de escolher a Paragon.";
        if(p.paragon==type)return "";
        int cost=p.hasChosenParagon?PARAGON_SWAP_COST:0;
        if(p.seals<cost)return "São necessários "+cost+" selos.";
        p.seals-=cost;p.paragon=type;p.hasChosenParagon=true;return "";
    }
    public static String prestige(PlayerProfile p, SkillType type, boolean locked) {
        var s=p.stats(type);
        if(locked)return "Aguarde o fim do combate e da habilidade.";
        if(!p.selected(type)||s.level<100||s.prestige>=MAX_PRESTIGE)return "Prestígio exige nível 100 e tem limite de "+MAX_PRESTIGE+".";
        s.highestLevel=100;s.prestige++;s.level=0;s.xp=0;return "";
    }
    public static boolean activate(PlayerProfile p, SkillType type, long now, int cooldownTicks) {
        var s=p.stats(type);
        if(!p.unlocked(type,20)||s.cooldownUntil>now||p.globalCooldownUntil>now) return false;
        s.cooldownUntil=now+cooldownTicks;p.globalCooldownUntil=now+100;p.equipped=type;return true;
    }
}

