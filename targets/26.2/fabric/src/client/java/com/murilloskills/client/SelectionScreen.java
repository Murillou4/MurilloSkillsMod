package com.murilloskills.client;
import com.murilloskills.skills.SkillType;
import com.murilloskills.network.Protocol;
public final class SelectionScreen extends NotebookScreen {
    private final SkillType incoming;
    public SelectionScreen(net.minecraft.client.gui.screens.Screen parent,SkillType incoming){super("SUBSTITUIR PROFISSÃO · "+com.murilloskills.skills.ProgressionService.SKILL_SWAP_COST+" SELOS",parent);this.incoming=incoming;}
    protected void init(){super.init();back();int row=0;for(var old:ClientState.profile.selected)button(20,60+row++*30,w-40,old.title+" → "+incoming.title,()->minecraft.gui.setScreen(new DecisionScreen(parent,"Substituir profissão","Custa "+com.murilloskills.skills.ProgressionService.SKILL_SWAP_COST+" selos. Progresso e ramos de "+old.title+" serão guardados.",()->ClientState.send(Protocol.Op.SELECT,incoming.name()+","+old.name(),0))));}
}

