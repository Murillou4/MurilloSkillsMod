package com.murilloskills.client;
import com.murilloskills.network.Protocol;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
public final class LegacyImportScreen extends NotebookScreen {
    private boolean requested;
    private SkillsButton confirm;
    public LegacyImportScreen(Screen parent){super("LEGADO / CONTINUE SUA HISTÓRIA",parent);}
    protected void init(){
        super.init();back();
        if(!requested){requested=true;ClientState.legacyReady=false;ClientState.legacyDescription="Consultando sua progressão antiga…";ClientState.send(Protocol.Op.IMPORT_PREVIEW,"",0);}
        button(14,h-43,(w-38)/2,20,"Cancelar",this::onClose);
        confirm=button(24+(w-38)/2,h-43,(w-38)/2,20,"Importar progresso",()->{ClientState.send(Protocol.Op.IMPORT_CONFIRM,"",0);onClose();}).primary();
        tick();
    }
    public void tick(){confirm.active=ClientState.legacyReady&&ClientState.clientTicks-ClientState.legacyReceivedAt<600&&ClientState.profile.fresh()&&!ClientState.profile.importedLegacy;}
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        background(g);SkillsTheme.panel(g,x+14,y+49,w-28,h-106,PANEL,SkillsTheme.BORDER);
        g.text(font,"PRÉVIA DA IMPORTAÇÃO",x+26,y+63,GOLD,false);
        int yy=wrap(g,ClientState.legacyDescription,26,86,w-52,TEXT);
        if(h>300)wrap(g,"Os novos ramos continuam disponíveis para você escolher. Selos e recompensas não serão concedidos retroativamente.",26,yy+18,w-52,MUTED);
        widgets(g,mx,my,partial);notification(g);
    }
}

