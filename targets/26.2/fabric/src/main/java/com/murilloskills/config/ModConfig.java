package com.murilloskills.config;
import com.google.gson.*;
import java.nio.file.*;
import java.io.*;

public final class ModConfig {
    public double xpMultiplier=1;
    public int maxAreaBlocks=256, maxScanBlocks=16000, autosaveSeconds=30;
    public double maxDamageBonus=0.4, maxSkillResistance=0.25, maxMoveBonus=0.25;
    public static ModConfig load(Path path) throws IOException {
        if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(new ModConfig()));}
        ModConfig c;
        try{c=new Gson().fromJson(Files.readString(path),ModConfig.class);}catch(JsonParseException e){throw new IOException("Config v2 inválida: "+path,e);}
        if(c==null||!Double.isFinite(c.xpMultiplier)||c.xpMultiplier<=0||c.xpMultiplier>100||c.maxAreaBlocks<1||c.maxAreaBlocks>512||c.maxScanBlocks<64||c.maxScanBlocks>65536||c.autosaveSeconds<5||c.autosaveSeconds>300
          ||!valid(c.maxDamageBonus,1)||!valid(c.maxSkillResistance,0.5)||!valid(c.maxMoveBonus,0.5))throw new IOException("Valores de configuração v2 fora dos limites.");
        return c;
    }
    private static boolean valid(double v,double max){return Double.isFinite(v)&&v>=0&&v<=max;}
}

