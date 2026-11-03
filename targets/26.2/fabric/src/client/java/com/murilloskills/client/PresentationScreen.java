package com.murilloskills.client;
import net.minecraft.client.gui.screens.Screen;
/** Compatibility route; keeps old callers in the same workspace. */
public final class PresentationScreen extends Screen {
    public PresentationScreen(Screen parent){super(net.minecraft.network.chat.Component.literal("MurilloSkills"));}
    protected void init(){var panel=new ProfessionScreen();panel.page=ProfessionScreen.Page.HELP;minecraft.gui.setScreen(panel);}
}
