package com.murilloskills.client;
import net.minecraft.client.gui.screens.Screen;
/** Compatibility route; keeps old callers in the same workspace. */
public final class ToolOptionsScreen extends Screen {
    public ToolOptionsScreen(Screen parent){super(net.minecraft.network.chat.Component.literal("MurilloSkills"));}
    protected void init(){var panel=new ProfessionScreen();panel.page=ProfessionScreen.Page.TOOLS;minecraft.gui.setScreen(panel);}
}

