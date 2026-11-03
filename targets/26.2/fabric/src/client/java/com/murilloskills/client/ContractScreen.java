package com.murilloskills.client;
import net.minecraft.client.gui.screens.Screen;
/** Compatibility route; keeps old callers in the same workspace. */
public final class ContractScreen extends Screen {
    public ContractScreen(Screen parent){super(net.minecraft.network.chat.Component.literal("MurilloSkills"));}
    protected void init(){var panel=new ProfessionScreen();panel.page=ProfessionScreen.Page.CONTRACTS;minecraft.gui.setScreen(panel);}
}

