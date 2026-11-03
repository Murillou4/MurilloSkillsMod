package com.murilloskills.utils;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import java.util.*;

/** Full radius across bounded ticks; no chunk loading. */
public final class ScanService {
    public static final class Job {
        final boolean treasure;final BlockPos center;final int radius,total;final Iterator<BlockPos> cursor;final List<BlockPos> results=new ArrayList<>();
        int lastSent,visited;
        Job(boolean treasure,BlockPos center,int radius){this.treasure=treasure;this.center=center;this.radius=radius;total=(int)Math.pow(radius*2+1,3);cursor=new ShellPositions(center,radius);}
    }
    private ScanService(){}
    public static void cancel(SkillContext c,String kind){c.runtime().scans.remove(kind);c.runtime().activities.remove("scan:"+kind);Wire.markers(c.player(),List.of(),kind,1);}
    public static void scan(SkillContext c,boolean treasure,int radius){
        radius=Math.clamp(radius,1,24);String key=treasure?"treasure":"ore";var current=c.runtime().scans.get(key);
        if(current!=null&&current.radius==radius&&current.center.distSqr(c.player().blockPosition())<16)return;
        var job=new Job(treasure,c.player().blockPosition().immutable(),radius);c.runtime().scans.put(key,job);status(c,key,job,false);
    }
    public static void tick(SkillContext c){
        int budget=Math.min(4096,c.store().config.maxScanBlocks);
        for(var entry:List.copyOf(c.runtime().scans.entrySet())){
            var job=entry.getValue();var skill=job.treasure?SkillType.EXPLORER:SkillType.MINER;
            if(!c.profile().selected(skill)){cancel(c,entry.getKey());continue;}
            int work=0;
            while(job.cursor.hasNext()&&job.results.size()<256&&work++<budget){
                var pos=job.cursor.next();job.visited++;if(!c.player().level().hasChunkAt(pos)||!c.player().level().isInWorldBounds(pos))continue;
                var state=c.player().level().getBlockState(pos);
                boolean found=job.treasure?c.player().level().getBlockEntity(pos) instanceof RandomizableContainerBlockEntity
                    ||c.paragon(SkillType.EXPLORER)&&(state.is(Blocks.SPAWNER)||state.is(Blocks.TRIAL_SPAWNER)||state.is(Blocks.VAULT)):WorldTools.ore(state);
                if(!found)continue;
                String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                if(!job.treasure&&c.perk(SkillPerk.ORE_FILTER)&&!c.profile().tools.oreFilter.isEmpty()&&!c.profile().tools.oreFilter.contains(id))continue;
                job.results.add(pos.immutable());
            }
            boolean done=!job.cursor.hasNext()||job.results.size()>=256;
            if(done||job.lastSent!=job.results.size()&&c.now()%5==0){Wire.markers(c.player(),List.copyOf(job.results),entry.getKey(),200);job.lastSent=job.results.size();}
            if(done||c.now()%5==0)status(c,entry.getKey(),job,done);
            if(done)c.runtime().scans.remove(entry.getKey());
        }
    }
    private static void status(SkillContext c,String key,Job job,boolean done){
        String filter=!job.treasure&&c.perk(SkillPerk.ORE_FILTER)&&!c.profile().tools.oreFilter.isEmpty()?" · filtro ativo":"";
        var near=job.results.stream().min(Comparator.comparingDouble(p->p.distSqr(c.player().blockPosition()))).orElse(null);
        String bearing="";
        if(near!=null){var from=c.player().blockPosition();int dx=near.getX()-from.getX(),dy=near.getY()-from.getY(),dz=near.getZ()-from.getZ();String direction=dx==0&&dz==0?dy>0?"acima":dy<0?"abaixo":"aqui":(Math.abs(dx)>Math.abs(dz)?dx>0?"L":"O":dz>0?"S":"N")+(dy>2?" ↑":dy< -2?" ↓":"");bearing=" · mais próximo: "+Math.round(Math.sqrt(near.distSqr(from)))+" m "+direction;}
        String detail=job.results.isEmpty()?(done?"Nenhum sinal no alcance carregado":"Procurando nos chunks carregados"):job.results.size()+" sinais"+bearing;
        c.runtime().activities.put("scan:"+key,new ActivityView("scan:"+key,done?"result":"scanning",job.treasure?"Busca de tesouros":"Pulso de prospecção",detail+filter+(job.results.size()>=256?" · limite de sinais":""),job.visited,job.total,job.results.size(),c.now()+(done?200:80)));
    }
}

