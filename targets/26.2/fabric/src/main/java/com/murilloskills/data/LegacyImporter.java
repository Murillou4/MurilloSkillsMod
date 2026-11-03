package com.murilloskills.data;
import com.google.gson.*;
import com.murilloskills.skills.*;
import java.nio.file.*;
import java.io.*;

public final class LegacyImporter {
    private LegacyImporter(){}
    public static PlayerProfile convert(String json) throws IOException {
        try {
            JsonObject root=JsonParser.parseString(json).getAsJsonObject();
            if(!root.has("skills")||(root.has("schemaVersion")?root.get("schemaVersion").getAsInt()!=1:!root.has("selectedSkills")))throw new IOException("Arquivo legado sem schema suportado.");
            PlayerProfile p=new PlayerProfile();
            for(var entry:root.getAsJsonObject("skills").entrySet()){
                SkillType type;try{type=SkillType.valueOf(entry.getKey());}catch(IllegalArgumentException e){continue;}
                var old=entry.getValue().getAsJsonObject();var s=p.stats(type);
                s.level=Math.clamp(old.get("level").getAsInt(),0,100);
                s.prestige=old.has("prestige")?Math.clamp(old.get("prestige").getAsInt(),0,10):0;
                s.highestLevel=s.prestige>0?100:s.level;s.claimedMilestone=s.highestLevel/5*5;
                double xp=old.has("xp")?old.get("xp").getAsDouble():0;
                if(!Double.isFinite(xp)||xp<0)throw new IOException("XP legado inválido.");
                s.xp=s.level==100?0:Math.min(0.999999,xp/(60+15*s.level+2.0*s.level*s.level))*ProgressionCurve.required(type,s.level);
            }
            if(root.has("selectedSkills"))for(var e:root.getAsJsonArray("selectedSkills")){
                try{var type=SkillType.valueOf(e.getAsString());if(p.selected.size()<4&&!p.selected(type))p.selected.add(type);}catch(IllegalArgumentException ignored){}
            }
            String paragonKey=root.has("activeParagonSkill")?"activeParagonSkill":"paragonSkill";
            if(root.has(paragonKey)&&!root.get(paragonKey).isJsonNull()){
                try{var t=SkillType.valueOf(root.get(paragonKey).getAsString());if(p.unlocked(t,100)){p.paragon=t;p.hasChosenParagon=true;}}catch(IllegalArgumentException ignored){}
            }
            if(!p.selected.isEmpty())p.equipped=p.selected.getFirst();
            p.importedLegacy=true;ContractService.ensure(p);return p;
        }catch(JsonParseException|IllegalStateException|NullPointerException e){throw new IOException("Arquivo legado inválido.",e);}
    }
    public static PlayerProfile importFile(Path source,Path destination,PlayerProfile current) throws IOException {
        return importFile(source,destination,current,Files.readString(source));
    }
    public static PlayerProfile importFile(Path source,Path destination,PlayerProfile current,String snapshot) throws IOException {
        if(current.importedLegacy||!current.fresh())throw new IOException("Importação permitida uma vez e somente antes de evoluir.");
        PlayerProfile incoming=convert(snapshot); // Validate before touching either destination.
        if(Files.exists(destination))Files.copy(destination,destination.resolveSibling(destination.getFileName()+".pre-import-"+System.currentTimeMillis()+".bak"));
        Files.copy(source,source.resolveSibling(source.getFileName()+".v2-import-"+System.currentTimeMillis()+".bak"));
        ProfileCodec.write(destination,incoming);return incoming;
    }
}

