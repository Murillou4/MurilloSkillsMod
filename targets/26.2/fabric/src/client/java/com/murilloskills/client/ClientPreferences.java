package com.murilloskills.client;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
public final class ClientPreferences {
    public enum CardMode {
        ACTIVE_ONLY("Só durante a ativa"),ALWAYS("Sempre"),HIDDEN("Oculto");
        public final String label;
        CardMode(String label){this.label=label;}
        public CardMode next(){return values()[(ordinal()+1)%values().length];}
    }
    public static ClientPreferences current=new ClientPreferences();
    public CardMode equippedCard=CardMode.ACTIVE_ONLY;
    public boolean hud=true,xpNotices=true,milestoneSound=true,worldMarkers=true;
    public boolean miningToggle,placementContinuous;
    public boolean abilitySounds=true,abilityVisuals=true;
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("murilloskills-client-v2.json");
    public CardMode cardMode(){return equippedCard==null?CardMode.ACTIVE_ONLY:equippedCard;}
    public boolean showEquippedCard(boolean active){return cardMode()==CardMode.ALWAYS||cardMode()==CardMode.ACTIVE_ONLY&&active;}
    public static void load(){
        if(Files.exists(FILE))try{var value=new com.google.gson.Gson().fromJson(Files.readString(FILE),ClientPreferences.class);if(value!=null){value.equippedCard=value.cardMode();current=value;}}
        catch(Exception e){com.murilloskills.MurilloSkills.LOG.warn("Preferências do cliente inválidas; usando os padrões sem substituir o arquivo.",e);}
    }
    public static void save(){try{Files.createDirectories(FILE.getParent());Files.writeString(FILE,new GsonBuilder().setPrettyPrinting().create().toJson(current));}catch(Exception e){com.murilloskills.MurilloSkills.LOG.warn("Não foi possível salvar preferências do cliente.",e);}}
}
