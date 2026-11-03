package com.murilloskills.client;
import java.util.Map;
/** Compatibility entry point: all tool settings now live in the profession workspace. */
public final class ToolScreen extends net.minecraft.client.gui.screens.Screen {
    public final boolean placement;
    private static final Map<String,String> LABELS=Map.ofEntries(Map.entry("VEIN","Veio"),Map.entry("3X3","3 × 3"),Map.entry("2X1","2 × 1"),Map.entry("LINE","Linha"),Map.entry("STAIRS","Escada"),Map.entry("QUARRY","Área"),Map.entry("PLANE","Plano"),Map.entry("BOX","Volume"),Map.entry("WALL","Parede"),Map.entry("COLUMN","Coluna"),Map.entry("TUNNEL","Túnel"),Map.entry("CIRCLE","Círculo"),Map.entry("SPHERE","Esfera"),Map.entry("SINGLE","Bloco"));
    public ToolScreen(boolean placement){super(net.minecraft.network.chat.Component.literal("Ferramentas"));this.placement=placement;}
    protected void init(){minecraft.gui.setScreen(ProfessionScreen.tools(placement));}
    public static String label(String shape){return LABELS.getOrDefault(shape,shape);}
}

