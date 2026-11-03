package com.murilloskills.skills;
import com.murilloskills.data.PlayerProfile;
import java.util.*;
/** Atomic free initial choices / completion of free slots; replacements use the paid SELECT action. */
public final class InitialSelection {
    private InitialSelection(){}
    public static String apply(PlayerProfile p,List<SkillType> requested,boolean locked){
        if(locked)return "Aguarde o fim do combate e das habilidades.";
        if(requested.isEmpty()||requested.size()>4||requested.stream().anyMatch(Objects::isNull)||new HashSet<>(requested).size()!=requested.size())return "Escolha de uma a quatro profissões diferentes.";
        if(!requested.containsAll(p.selected))return "Substituições exigem "+ProgressionService.SKILL_SWAP_COST+" selos e confirmação.";
        var next=new ArrayList<>(p.selected);for(var type:requested)if(!next.contains(type))next.add(type);
        p.selected.clear();p.selected.addAll(next);
        if(p.equipped==null)p.equipped=p.selected.getFirst();
        if(p.completedSets==0&&p.contracts.stream().allMatch(c->c.progress==0))p.contracts.clear();
        ContractService.ensure(p);return "";
    }
}
