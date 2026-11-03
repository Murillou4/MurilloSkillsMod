package com.murilloskills.client;
import net.minecraft.client.gui.screens.Screen;
public final class SkillPreferencesScreen extends Screen {
    public SkillPreferencesScreen(Screen parent){super(net.minecraft.network.chat.Component.literal("Controles"));}
    protected void init(){var panel=new ProfessionScreen();panel.tab=ProfessionScreen.Tab.CONTROLS;minecraft.gui.setScreen(panel);}
}
