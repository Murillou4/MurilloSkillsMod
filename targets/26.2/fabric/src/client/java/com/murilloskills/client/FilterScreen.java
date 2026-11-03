package com.murilloskills.client;
import com.murilloskills.network.Protocol;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import java.util.*;

public final class FilterScreen extends NotebookScreen {
    private final String kind;private String query="";private int page;private boolean inventoryOnly;
    private List<Item> filtered=List.of();
    public FilterScreen(Screen parent,String kind){super(kind.equals("trash")?"LIXEIRA / ITENS DESCARTADOS":kind.equals("classic")?"CLASSIC / FILTRO DE BLOCOS":"RADAR / FILTRO DE MINÉRIOS",parent);this.kind=kind;}
    private int rows(){return Math.max(2,(h-115)/23);}
    protected void init(){
        super.init();back();EditBox search=addRenderableWidget(new EditBox(font,x+12,y+39,w-24,18,net.minecraft.network.chat.Component.literal("Buscar item")));
        search.setMaxLength(100);search.setValue(query);search.setHint(net.minecraft.network.chat.Component.literal("Nome ou minecraft:id"));
        search.setResponder(value->{query=value;inventoryOnly=false;page=0;filter();rebuildRows();});
        button(12,h-45,35,"<",()->{page=Math.max(0,page-1);rebuildWidgets();});
        button(53,h-45,35,">",()->{page=Math.min(Math.max(0,(filtered.size()-1)/rows()),page+1);rebuildWidgets();});
        button(w-155,h-45,143,inventoryOnly?"Todos os itens":"Meu inventário",()->{query="";inventoryOnly=!inventoryOnly;page=0;rebuildWidgets();});
        filter();rebuildRows();
    }
    private final List<Button> rowButtons=new ArrayList<>();
    private void filter(){String q=query.toLowerCase(Locale.ROOT);var source=inventoryOnly?minecraft.player.getInventory().getNonEquipmentItems().stream().filter(s->!s.isEmpty()).map(ItemStack::getItem).distinct():BuiltInRegistries.ITEM.stream();filtered=source.filter(item->item!=Items.AIR&&(kind.equals("trash")||item instanceof BlockItem)).filter(item->BuiltInRegistries.ITEM.getKey(item).toString().contains(q)||item.getName(item.getDefaultInstance()).getString().toLowerCase(Locale.ROOT).contains(q)).toList();}
    private Set<String> selected(){return switch(kind){case "ore"->ClientState.profile.tools.oreFilter;case "classic"->ClientState.profile.tools.classicFilter;default->ClientState.profile.tools.trash;};}
    private void rebuildRows(){
        for(var b:rowButtons)removeWidget(b);rowButtons.clear();
        for(int i=0;i<rows()&&page*rows()+i<filtered.size();i++){
            Item item=filtered.get(page*rows()+i);String id=BuiltInRegistries.ITEM.getKey(item).toString();int yy=66+i*23;
            var b=button(40,yy,w-52,item.getName(item.getDefaultInstance()).getString(),()->ClientState.send(Protocol.Op.FILTER,kind+"|"+id,selected().contains(id)?0:1));rowButtons.add(b);
        }
    }
    public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractRenderState(g,mx,my,partial);
        for(int i=0;i<rows()&&page*rows()+i<filtered.size();i++){var item=filtered.get(page*rows()+i);g.item(item.getDefaultInstance(),x+14,y+68+i*23);String id=BuiltInRegistries.ITEM.getKey(item).toString();if(selected().contains(id))g.text(font,"✓",x+w-23,y+72+i*23,GOLD);}
        g.text(font,selected().size()+"/512 selecionados",x+100,y+h-39,MUTED);
    }
}

