package com.murilloskills.skills;
import com.murilloskills.api.*;
import com.murilloskills.data.RuntimeState;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.network.Wire;
import com.murilloskills.utils.WorldTools;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import java.util.*;

/** Server plans previews and execution from the same bounded candidate list. */
public final class AreaTools {
    private AreaTools(){}
    public static com.murilloskills.network.Protocol.ResultCode failureCode(SkillContext c,boolean place,List<BlockPos> plan){
        var type=skillFor(place);
        if(!c.profile().selected(type))return com.murilloskills.network.Protocol.ResultCode.NOT_SELECTED;
        if(!c.has(type,15))return com.murilloskills.network.Protocol.ResultCode.LEVEL;
        if(c.runtime().area||c.now()<c.runtime().nextToolTick)return com.murilloskills.network.Protocol.ResultCode.BUSY;
        if(c.player().getMainHandItem().isEmpty()||place&&!(c.player().getMainHandItem().getItem() instanceof BlockItem))return com.murilloskills.network.Protocol.ResultCode.WRONG_TOOL;
        return plan.isEmpty()?com.murilloskills.network.Protocol.ResultCode.NO_TARGET:com.murilloskills.network.Protocol.ResultCode.REJECTED;
    }
    public static String blocked(SkillContext c,boolean place,List<BlockPos> plan){
        SkillType type=place?SkillType.BUILDER:SkillType.MINER;
        if(!c.profile().selected(type))return "Escolha a profissão "+type.title+" primeiro.";
        if(!c.has(type,15))return (place?"UltPlace":"Ultmine")+" libera no nível 15.";
        if(c.player().isSpectator())return "Ferramentas indisponíveis no modo espectador.";
        if(c.runtime().area||c.now()<c.runtime().nextToolTick)return "Aguarde a operação atual terminar.";
        var item=c.player().getMainHandItem();
        if(item.isEmpty()||place&&!(item.getItem() instanceof BlockItem))return place?"Segure blocos na mão.":"Segure uma ferramenta adequada.";
        if(plan.isEmpty())return place?"Aponte para uma superfície com espaço livre; confira colisões e permissões.":"Nenhum bloco válido: confira ferramenta, filtro, alvo e permissões.";
        return "";
    }
    public static List<BlockPos> candidates(SkillContext c,boolean place,BlockPos root,Direction face){
        var prefs=c.profile().tools;var skill=place?SkillType.BUILDER:SkillType.MINER;
        if(!c.has(skill,15))return List.of();
        int bonus=c.branch(skill,2);
        int limit=Math.min(c.store().config.maxAreaBlocks,ProgressionCurve.toolLimit(c.stats(skill).unlockLevel(),bonus)*(c.active(skill)?2:1));
        String shape=place?prefs.placementShape:prefs.miningShape;
        if(!place&&shape.equals("VEIN")){
            LinkedHashSet<BlockPos> seen=new LinkedHashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(root);
            var source=c.player().level().getBlockState(root).getBlock();
            while(!queue.isEmpty()&&seen.size()<limit){
                var pos=queue.removeFirst();if(seen.contains(pos)||pos.distSqr(root)>256||!c.player().level().hasChunkAt(pos))continue;
                var state=c.player().level().getBlockState(pos);String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                if(state.getBlock()!=source&&!(prefs.oresOnly&&WorldTools.ore(state)))continue;
                if(!prefs.classicFilter.isEmpty()&&!prefs.classicFilter.contains(id))continue;
                seen.add(pos);for(var dir:Direction.values())queue.add(pos.relative(dir));
            }return List.copyOf(seen);
        }
        return place?ShapePlanner.plan(shape,root,c.player().getDirection(),prefs.size,prefs.depth,prefs.spacing,prefs.hollow,limit):
            ShapePlanner.mining(shape,root,face,c.player().getDirection(),prefs.size,prefs.depth,prefs.spacing,limit);
    }
    public static List<BlockPos> preview(SkillContext c,boolean place){
        HitResult hit=c.player().pick(c.player().blockInteractionRange(),0,false);
        if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK){
            Wire.notice(c,place?"tool:place":"tool:mine","blocked",place?"UltPlace":"Ultmine","Aponte para um bloco dentro do alcance.",0,35);return List.of();
        }
        BlockPos anchor=place?block.getBlockPos().relative(block.getDirection()):block.getBlockPos();
        var list=candidates(c,place,anchor,block.getDirection()).stream().filter(p->place?placeable(c,p):mineable(c,p)).toList();
        int capacity=place?(int)BuilderSkill.AREA.value(c,skillFor(true)):(int)com.murilloskills.impl.MinerSkill.AREA.value(c,skillFor(false));
        int available=place?c.player().getMainHandItem().getCount():0;
        Wire.markers(c.player(),list,place?"place":"mine",30);
        String detail=place?list.size()+" blocos previstos · "+available+" materiais na mão · limite "+capacity:list.size()+" blocos previstos · limite "+capacity+" · "+(c.profile().tools.oresOnly?"somente minérios":"blocos permitidos");
        Wire.notice(c,place?"tool:place":"tool:mine","preview",place?"UltPlace":"Ultmine",detail,list.size(),35);
        return list;
    }
    private static SkillType skillFor(boolean place){return place?SkillType.BUILDER:SkillType.MINER;}
    public static boolean valid(SkillContext c,BlockPos pos){
        return c.player().level().hasChunkAt(pos)&&c.player().level().isInWorldBounds(pos)&&c.player().level().getWorldBorder().isWithinBounds(pos)
            &&c.player().mayInteract(c.player().level(),pos)&&c.player().mayUseItemAt(pos,Direction.UP,c.player().getMainHandItem());
    }
    public static boolean mineable(SkillContext c,BlockPos pos){
        if(!valid(c,pos))return false;var state=c.player().level().getBlockState(pos);var tool=c.player().getMainHandItem();
        return !tool.isEmpty()&&!state.isAir()&&!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock)&&state.getDestroySpeed(c.player().level(),pos)>=0&&c.player().level().getBlockEntity(pos)==null
            &&(!state.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(state))&&(!c.profile().tools.oresOnly||WorldTools.ore(state));
    }
    public static boolean placeable(SkillContext c,BlockPos pos){
        var stack=c.player().getMainHandItem();
        if(!valid(c,pos)||!c.player().level().getBlockState(pos).isAir()||stack.isEmpty()||!(stack.getItem() instanceof BlockItem item)||stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA))return false;
        var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false);
        var context=item.updatePlacementContext(new BlockPlaceContext(c.player(),InteractionHand.MAIN_HAND,stack,hit));
        if(context==null)return false;var access=(com.murilloskills.mixin.BlockItemAccess)item;var state=access.murilloskills$state(context);
        return state!=null&&context.canPlace()&&access.murilloskills$canPlace(context,state);
    }
    public static int mine(SkillContext c,List<BlockPos> planned){
        if(!c.has(SkillType.MINER,15)||c.runtime().area||c.now()<c.runtime().nextToolTick||c.player().isSpectator())return 0;
        c.runtime().nextToolTick=c.now()+10;c.runtime().area=true;int broken=0;
        try{
            var tool=c.player().getMainHandItem();int max=Math.min(c.store().config.maxAreaBlocks,ProgressionCurve.toolLimit(c.stats(SkillType.MINER).unlockLevel(),c.branch(SkillType.MINER,2))*(c.active(SkillType.MINER)?2:1));
            for(var pos:planned){
                if(broken>=max||tool.isEmpty())break;
                if(!mineable(c,pos))continue;
                if(c.player().gameMode.destroyBlock(pos)){broken++;collect(c,pos);c.runtime().pendingCollections.remove(pos);}
            }
        }finally{SkillExperience.flushArea(c);}
        c.runtime().nextToolTick=c.now()+10+broken*2;
        return broken;
    }
    public static int place(SkillContext c,List<BlockPos> planned){
        if(!c.has(SkillType.BUILDER,15)||c.runtime().area||c.now()<c.runtime().nextToolTick||c.player().isSpectator())return 0;
        var stack=c.player().getMainHandItem();if(!(stack.getItem() instanceof BlockItem item)||stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA))return 0;
        var usable=planned.stream().filter(pos->placeable(c,pos)).toList();if(usable.isEmpty())return 0;
        var batch=new ArrayList<RuntimeState.Placement>();
        c.runtime().nextToolTick=c.now()+10;c.runtime().area=true;int count=0;
        int max=Math.min(c.store().config.maxAreaBlocks,ProgressionCurve.toolLimit(c.stats(SkillType.BUILDER).unlockLevel(),c.branch(SkillType.BUILDER,2))*(c.active(SkillType.BUILDER)?2:1));
        try{
            for(var pos:usable){
                if(stack.isEmpty()||count>=max)break;
                if(!placeable(c,pos))continue;
                int before=stack.getCount();var refund=stack.copyWithCount(1);
                var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false);
                var result=item.place(new BlockPlaceContext(c.player(),InteractionHand.MAIN_HAND,stack,hit));
                if(result.consumesAction()&&!c.player().level().getBlockState(pos).isAir()){
                    // Native BlockItem.place consumes the actual stack and validates survival/collision.
                    var placed=c.player().level().getBlockState(pos);
                    batch.add(new RuntimeState.Placement(c.player().level().dimension().identifier().toString(),pos.immutable(),placed,before>stack.getCount()?refund:ItemStack.EMPTY));
                    count++;
                }
            }
        }finally{SkillExperience.flushArea(c);}
        c.runtime().nextToolTick=c.now()+10+count*2;
        if(count>0){c.runtime().undo.clear();c.runtime().undo.addAll(batch);c.runtime().undoUntil=c.now()+(long)(BuilderSkill.UNDO.value(c,SkillType.BUILDER)*20);}
        return count;
    }
    public static int undo(SkillContext c){
        if(c.runtime().combatUntil>c.now()||c.runtime().undoUntil<c.now()||!c.profile().selected(SkillType.BUILDER))return 0;
        int removed=0;
        for(var entry:List.copyOf(c.runtime().undo)){
            if(!entry.dimension().equals(c.player().level().dimension().identifier().toString())||!valid(c,entry.position()))continue;
            var state=c.player().level().getBlockState(entry.position());
            if(state!=entry.placed()||c.player().level().getBlockEntity(entry.position())!=null)continue;
            if(c.player().level().removeBlock(entry.position(),false)){c.give(entry.refund().copy());c.store().removed(c.player(),entry.position());removed++;}
        }
        c.runtime().undo.clear();return removed;
    }
    public static void collect(SkillContext c,BlockPos pos){
        if(c.profile().tools.directXp){
            for(var orb:c.player().level().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,new AABB(pos).inflate(1.25))){
                c.player().giveExperiencePoints(orb.getValue()*((com.murilloskills.mixin.ExperienceOrbAccess)orb).murilloskills$count());orb.discard();
            }
        }
        if(!c.profile().tools.inventoryDrops&&c.profile().tools.trash.isEmpty())return;
        var items=c.player().level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(1.5));
        for(var entity:items){
            var stack=entity.getItem();String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if(c.profile().tools.trash.contains(id)){entity.discard();continue;}
            if(c.profile().tools.inventoryDrops){c.player().getInventory().add(stack);if(stack.isEmpty())entity.discard();else entity.setItem(stack);}
        }
    }
}

