package com.murilloskills.client;
import com.murilloskills.skills.SkillType;
import java.util.*;
/** Session-only navigation; never owns gameplay state. */
public final class WorkspaceMemory {
    public static SkillType skill=SkillType.MINER;
    public static ProfessionScreen.Tab tab=ProfessionScreen.Tab.SUMMARY;
    public static final Map<String,Integer> scrolls=new HashMap<>();
    private WorkspaceMemory(){}
    public static void reset(){skill=SkillType.MINER;tab=ProfessionScreen.Tab.SUMMARY;scrolls.clear();}
}
