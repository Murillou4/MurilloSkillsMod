package com.murilloskills.utils;
import com.murilloskills.api.SkillContext;
import com.murilloskills.skills.SkillType;
import com.murilloskills.skills.effects.Feature;
import com.murilloskills.network.Wire;
import net.minecraft.world.level.block.CropBlock;
import java.util.*;

/** Same radius, stages, chances and per-cycle caps as 2.1; no omitted outer shells. */
public final class GrowthService {
    public static final class Job {
        final Iterator<net.minecraft.core.BlockPos> cursor;final boolean strong;final int limit,stages;int visited,grown;
        Job(SkillContext c,int radius,boolean strong){cursor=new ShellPositions(c.player().blockPosition(),Math.min(radius,8));this.strong=strong;limit=strong?(c.paragon(SkillType.FARMER)?48:32):com.murilloskills.impl.FarmerSkill.GROW_LIMIT;stages=strong?(c.paragon(SkillType.FARMER)?3:2):1;}
    }
    private GrowthService(){}
    public static void start(SkillContext c,int radius,boolean strong){
        String key=strong?"surge":"growth";if(c.runtime().growth.containsKey(key))return;
        var job=new Job(c,radius,strong);c.runtime().growth.put(key,job);work(c,key,job,3000);
    }
    public static void tick(SkillContext c){for(var e:List.copyOf(c.runtime().growth.entrySet()))work(c,e.getKey(),e.getValue(),1024);}
    private static void work(SkillContext c,String key,Job job,int budget){
        if(!c.profile().selected(SkillType.FARMER)||job.strong&&!c.active(SkillType.FARMER)||!job.strong&&!c.feature(Feature.GROWTH)){c.runtime().growth.remove(key);return;}
        while(job.cursor.hasNext()&&job.grown<job.limit&&budget-->0){
            var pos=job.cursor.next();job.visited++;
            if(!c.player().level().hasChunkAt(pos)||!c.player().level().isInWorldBounds(pos)||!c.player().mayInteract(c.player().level(),pos))continue;
            var state=c.player().level().getBlockState(pos);
            if(state.getBlock() instanceof CropBlock crop&&!crop.isMaxAge(state)&&c.player().getRandom().nextDouble()<(job.strong?1:com.murilloskills.impl.FarmerSkill.GROW_CHANCE)){
                c.player().level().setBlockAndUpdate(pos,crop.getStateForAge(Math.min(crop.getMaxAge(),crop.getAge(state)+job.stages)));job.grown++;
            }
        }
        if(!job.cursor.hasNext()||job.grown>=job.limit){
            c.runtime().growth.remove(key);
            if(job.strong||job.grown>0)Wire.notice(c,"farm:"+key,"result",job.strong?"Surto de crescimento":"Solo fértil",job.grown>0?job.grown+" cultivos desenvolvidos · "+job.stages+" estágio(s) por cultivo":"Nenhum cultivo imaturo disponível na área",job.grown,60);
        }
    }
    public static void clear(SkillContext c){c.runtime().growth.clear();}
}
