package com.murilloskills.client;
import com.murilloskills.skills.SkillType;
import net.minecraft.client.gui.screens.Screen;
/** Compatibility route into the unified profession workspace. */
public final class MilestoneScreen extends Screen {
    public final SkillType skill;
    public MilestoneScreen(Screen parent,SkillType skill){super(net.minecraft.network.chat.Component.literal(skill.title));this.skill=skill;}
    protected void init(){minecraft.gui.setScreen(new ProfessionScreen(skill,ProfessionScreen.Tab.PROGRESSION));}
}

