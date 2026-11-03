package com.murilloskills.data;
import com.google.gson.*;
import com.murilloskills.skills.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class ProfileCodec {
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private ProfileCodec(){}
    public static PlayerProfile decode(String json) throws IOException {
        PlayerProfile p;
        try{p=JSON.fromJson(json,PlayerProfile.class);}catch(RuntimeException e){throw new IOException("Progressão inválida",e);}
        if(p!=null&&p.toggles!=null&&!JsonParser.parseString(json).getAsJsonObject().has("featureSettings"))com.murilloskills.skills.effects.Feature.migrate(p);
        if(p==null||p.schemaVersion!=2||p.skills==null||p.skills.size()!=SkillType.values().length||p.selected==null||p.contracts==null||p.selected.size()>4||new HashSet<>(p.selected).size()!=p.selected.size()||p.selected.contains(null)||p.contracts.size()>3||p.seals<0||p.globalCooldownUntil<0||p.completedSets<0)
            throw new IOException("Estrutura de progressão v2 inválida.");
        for(var type:SkillType.values()){
            var s=p.skills.get(type);if(s==null)throw new IOException("Profissão ausente: "+type);
            if(s.level<0||s.level>100||s.highestLevel<s.level||s.highestLevel>100||s.prestige<0||s.prestige>10||s.branch<0||s.branch>2||!Double.isFinite(s.xp)||s.xp<0||s.cooldownUntil<0||s.claimedMilestone<0||s.claimedMilestone>100
                ||(s.level<100&&s.xp>=ProgressionCurve.required(type,s.level)))throw new IOException("Estado inválido: "+type);
        }
        for(var c:p.contracts)if(c==null||c.skill==null||c.activity==null||!Double.isFinite(c.progress)||!Double.isFinite(c.target)||c.progress<0||c.target<=0||c.progress>c.target)throw new IOException("Contrato inválido.");
        if(p.tools==null||p.toggles==null||p.featureSettings==null||p.featureSettings.size()>64||p.featureSettings.containsValue(null)||p.achievements==null||p.discoveries==null||p.tools.trash==null||p.tools.oreFilter==null||p.tools.classicFilter==null)throw new IOException("Preferências inválidas.");
        if(p.tools.trash.size()>512||p.tools.oreFilter.size()>512||p.tools.classicFilter.size()>512||p.tools.size<1||p.tools.size>20||p.tools.depth<1||p.tools.depth>20||p.tools.spacing<1||p.tools.spacing>4)throw new IOException("Limites de ferramentas inválidos.");
        if(p.paragon!=null&&!p.selected(p.paragon))throw new IOException("Paragon fora das profissões.");
        if(p.equipped!=null&&!p.selected(p.equipped))p.equipped=p.selected.isEmpty()?null:p.selected.getFirst();
        return p;
    }
    public static void write(Path path,PlayerProfile p) throws IOException { atomicWrite(path,JSON.toJson(p)); }
    public static void atomicWrite(Path path,String json) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp=path.resolveSibling(path.getFileName()+".tmp");
        Files.writeString(temp,json);
        try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
    public static PlayerProfile read(Path path) throws IOException {
        String json=Files.readString(path);var p=decode(json);
        if(!JsonParser.parseString(json).getAsJsonObject().has("featureSettings")){
            var backup=path.resolveSibling(path.getFileName()+".pre-2.1.bak");if(!Files.exists(backup))Files.copy(path,backup);
        }
        return p;
    }
}

