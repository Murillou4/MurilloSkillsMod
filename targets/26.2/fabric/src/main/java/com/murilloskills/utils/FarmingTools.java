package com.murilloskills.utils;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.impl.FarmerSkill;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.block.CropBlock;
public final class FarmingTools {
    private FarmingTools(){}
    public static int plant(SkillContext c,BlockPos center,BlockItem seed){
        if(c.runtime().area||c.now()<c.runtime().nextToolTick)return 0;
        c.runtime().nextToolTick=c.now()+10;c.runtime().area=true;int planted=0;
        int radius=(int)FarmerSkill.PLANT.value(c,SkillType.FARMER);
        try{for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            var pos=center.offset(x,0,z);var stack=c.player().getMainHandItem();
            if(stack.isEmpty())return planted;
            if(!AreaTools.valid(c,pos)||!c.player().level().getBlockState(pos).isAir())continue;
            var context=new BlockPlaceContext(c.player(),InteractionHand.MAIN_HAND,stack,new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false));
            if(seed.place(context).consumesAction())planted++;
        }}finally{SkillExperience.flushArea(c);com.murilloskills.network.Wire.notice(c,"farm:plant","result","Plantio em área",planted+" cultivos plantados",planted,40);}return planted;
    }
    public static int boneMeal(SkillContext c,BlockPos center){
        if(c.runtime().area||c.now()<c.runtime().nextToolTick)return 0;
        int radius=(int)FarmerSkill.MEAL.value(c,SkillType.FARMER);if(radius<=0)return 0;
        c.runtime().area=true;c.runtime().nextToolTick=c.now()+10;int used=0;
        try{for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            var pos=center.offset(x,0,z);var stack=c.player().getMainHandItem();
            if(!stack.is(Items.BONE_MEAL))return used;
            if(!AreaTools.valid(c,pos))continue;
            var state=c.player().level().getBlockState(pos);
            if(state.getBlock() instanceof CropBlock crop&&!crop.isMaxAge(state)&&BoneMealItem.growCrop(stack,c.player().level(),pos)){c.player().level().levelEvent(1505,pos,0);used++;}
        }}finally{SkillExperience.flushArea(c);com.murilloskills.network.Wire.notice(c,"farm:meal","result","Bone meal em área",used+" aplicações vanilla bem-sucedidas",used,40);}return used;
    }
    public static int harvest(SkillContext c,BlockPos center){
        if(c.runtime().area)return 0;
        c.runtime().area=true;int harvested=0,radius=(int)FarmerSkill.HARVEST.value(c,SkillType.FARMER);
        try{for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(x==0&&z==0)continue;var pos=center.offset(x,0,z);
            if(!AreaTools.valid(c,pos))continue;var state=c.player().level().getBlockState(pos);
            if(state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state)&&c.player().gameMode.destroyBlock(pos))harvested++;
        }}finally{SkillExperience.flushArea(c);com.murilloskills.network.Wire.notice(c,"farm:harvest","result","Colheita adjacente",harvested+" cultivos maduros colhidos",harvested,40);}return harvested;
    }
}

