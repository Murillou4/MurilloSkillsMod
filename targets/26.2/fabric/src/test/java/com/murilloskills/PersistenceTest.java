package com.murilloskills;
import com.murilloskills.data.*;
import com.murilloskills.skills.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;

class PersistenceTest {
    @TempDir Path temp;
    @Test void fileRoundTripKeepsCooldownContractsBranchAndTools()throws Exception{
        var p=new PlayerProfile();p.selected.add(SkillType.MINER);p.stats(SkillType.MINER).branch=1;
        p.stats(SkillType.MINER).highestLevel=50;p.stats(SkillType.MINER).cooldownUntil=3000;p.seals=8;
        p.tools.trash.add("minecraft:dirt");ContractService.ensure(p);p.contracts.getFirst().progress=15;
        Path path=temp.resolve("players/user.json");ProfileCodec.write(path,p);var loaded=ProfileCodec.read(path);
        assertEquals(3000,loaded.stats(SkillType.MINER).cooldownUntil);assertEquals(15,loaded.contracts.getFirst().progress);assertEquals(8,loaded.seals);
        assertTrue(loaded.tools.trash.contains("minecraft:dirt"));assertFalse(Files.exists(path.resolveSibling("user.json.tmp")));
    }
    @Test void damagedOrFutureSaveIsRejectedInsteadOfResetting(){
        assertThrows(java.io.IOException.class,()->ProfileCodec.decode("{broken"));
        assertThrows(java.io.IOException.class,()->ProfileCodec.decode("{\"schemaVersion\":3}"));
    }
    @Test void importPreservesFractionAndPrestigeWithoutRetroactiveRewards()throws Exception{
        String old="{\"schemaVersion\":1,\"selectedSkills\":[\"MINER\"],\"skills\":{\"MINER\":{\"level\":50,\"xp\":2905,\"prestige\":2}}}";
        var p=LegacyImporter.convert(old);assertEquals(50,p.stats(SkillType.MINER).level);assertEquals(100,p.stats(SkillType.MINER).highestLevel);
        assertEquals(ProgressionCurve.required(SkillType.MINER,50)*0.5,p.stats(SkillType.MINER).xp,0.01);
        assertEquals(0,p.seals);assertEquals(0,p.achievements.size());assertEquals(0,p.stats(SkillType.MINER).branch);
    }
    @Test void importDoesNotModifyLegacyAndCannotRunTwice()throws Exception{
        Path source=temp.resolve("old.json"),dest=temp.resolve("new.json");
        String text="{\"schemaVersion\":1,\"selectedSkills\":[],\"skills\":{}}";Files.writeString(source,text);
        var p=LegacyImporter.importFile(source,dest,new PlayerProfile());assertEquals(text,Files.readString(source));
        assertThrows(java.io.IOException.class,()->LegacyImporter.importFile(source,dest,p));
        assertTrue(Files.list(temp).anyMatch(f->f.getFileName().toString().endsWith(".bak")));
    }
}

