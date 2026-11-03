package com.murilloskills.utils;
import com.murilloskills.api.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.*;

public final class WorldTools {
    private WorldTools(){}
    public static boolean fortuneResource(BlockState s){return ore(s)||s.is(Blocks.GLOWSTONE)||s.is(Blocks.CLAY)||s.is(Blocks.AMETHYST_CLUSTER);}
    public static Item seedFor(CropBlock crop){return crop==Blocks.CARROTS?Items.CARROT:crop==Blocks.POTATOES?Items.POTATO:crop==Blocks.BEETROOTS?Items.BEETROOT_SEEDS:Items.WHEAT_SEEDS;}
    public static boolean economicBlock(BlockState s){
        return s.is(BlockTags.PLANKS)||s.is(Blocks.STONE)||s.is(Blocks.COBBLESTONE)||s.is(Blocks.COBBLED_DEEPSLATE)||s.is(Blocks.BRICKS)||s.is(Blocks.STONE_BRICKS)||s.is(Blocks.GLASS)||s.is(Blocks.TERRACOTTA)||s.is(Blocks.SANDSTONE);
    }
    public static boolean ore(BlockState s){return s.is(BlockTags.IRON_ORES)||s.is(BlockTags.COPPER_ORES)||s.is(BlockTags.GOLD_ORES)||rareOre(s)||s.is(Blocks.COAL_ORE)||s.is(Blocks.DEEPSLATE_COAL_ORE)||s.is(Blocks.LAPIS_ORE)||s.is(Blocks.DEEPSLATE_LAPIS_ORE)||s.is(Blocks.REDSTONE_ORE)||s.is(Blocks.DEEPSLATE_REDSTONE_ORE)||s.is(Blocks.NETHER_QUARTZ_ORE);}
    public static boolean rareOre(BlockState s){return s.is(Blocks.DIAMOND_ORE)||s.is(Blocks.DEEPSLATE_DIAMOND_ORE)||s.is(Blocks.EMERALD_ORE)||s.is(Blocks.DEEPSLATE_EMERALD_ORE)||s.is(Blocks.ANCIENT_DEBRIS);}
    public static List<BlockPos> loadedBox(SkillContext c,BlockPos center,int radius,int limit){
        List<BlockPos> out=new ArrayList<>();var level=c.player().level();
        // Nearest shells first: bounded scans must not miss the player's immediate surroundings.
        for(int shell=0;shell<=radius&&out.size()<limit;shell++)for(int y=-shell;y<=shell&&out.size()<limit;y++)for(int x=-shell;x<=shell&&out.size()<limit;x++)for(int z=-shell;z<=shell&&out.size()<limit;z++){
            if(Math.max(Math.max(Math.abs(x),Math.abs(y)),Math.abs(z))!=shell)continue;
            var p=center.offset(x,y,z);if(level.hasChunkAt(p)&&level.isInWorldBounds(p))out.add(p);
        }return out;
    }
    public static void autoTorch(SkillContext c){
        BlockPos at=c.player().blockPosition();
        if(c.player().level().getMaxLocalRawBrightness(at)>=7||!c.player().level().getBlockState(at).isAir())return;
        for(var stack:c.player().getInventory().getNonEquipmentItems())if(stack.is(Items.TORCH)){
            if(!c.player().mayInteract(c.player().level(),at)||!c.player().mayUseItemAt(at,Direction.UP,stack))return;
            ((BlockItem)Items.TORCH).place(new BlockPlaceContext(c.player(),InteractionHand.MAIN_HAND,stack,new BlockHitResult(Vec3.atCenterOf(at.below()),Direction.UP,at.below(),false)));return;
        }
    }
    public static void grow(SkillContext c,int radius,boolean strong){GrowthService.start(c,radius,strong);}
    public static void replant(SkillContext c,BlockPos pos,CropBlock crop){
        if(!c.player().level().getBlockState(pos).isAir()||!c.player().mayInteract(c.player().level(),pos))return;
        Item seed=crop==Blocks.CARROTS?Items.CARROT:crop==Blocks.POTATOES?Items.POTATO:crop==Blocks.BEETROOTS?Items.BEETROOT_SEEDS:Items.WHEAT_SEEDS;
        for(var stack:c.player().getInventory().getNonEquipmentItems())if(stack.is(seed)){
            var context=new BlockPlaceContext(c.player(),InteractionHand.MAIN_HAND,stack,new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false));
            if(seed instanceof BlockItem item)item.place(context);return;
        }
    }
    public static void particles(SkillContext c,BlockPos pos){c.player().level().sendParticles(c.player(),net.minecraft.core.particles.ParticleTypes.END_ROD,false,false,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,1,0,0,0,0);}
}

