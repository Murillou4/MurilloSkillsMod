package com.murilloskills.validation;
import com.murilloskills.*;
import com.murilloskills.data.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.network.Wire;
import com.murilloskills.skills.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import static com.murilloskills.validation.Checks.check;

/** Fixtures prepare a wall; only the real client input may break it. */
public final class UltmineInputProbes {
    private BlockPos root;
    private List<BlockPos> wall;
    private boolean creative;
    public BlockPos root(){return root;}
    public void prepare(ServerPlayer player,boolean creative,int level){
        this.creative=creative;player.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);
        var c=GameplayEvents.context(player);var p=new PlayerProfile();p.selected.add(SkillType.MINER);p.equipped=SkillType.MINER;
        var stats=p.stats(SkillType.MINER);stats.level=level;stats.highestLevel=level;stats.claimedMilestone=level/5*5;
        p.tools.miningShape="3X3";p.tools.depth=1;p.tools.size=3;
        c.store().replace(player,p);
        c.runtime().nextToolTick=0;c.runtime().miningHeld=false;c.runtime().queuedMiningPlan=List.of();c.runtime().pendingMiningPlan=List.of();
        c.runtime().activeUntil.clear();c.runtime().packetCount=0;
        player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
        root=player.blockPosition().offset(0,1,3);wall=new ArrayList<>();
        for(var pos:BlockPos.betweenClosed(root.offset(-2,-1,-1),root.offset(2,2,1)))player.level().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        for(int yy=-1;yy<=1;yy++)for(int xx=-1;xx<=1;xx++){var pos=root.offset(xx,yy,0);wall.add(pos);player.level().setBlock(pos,Blocks.IRON_ORE.defaultBlockState(),3);}
        GameplayEvents.refresh(c);Wire.send(player,"");
    }
    public void verify(ServerPlayer player,int expected,String name){
        long broken=wall.stream().filter(pos->player.level().getBlockState(pos).isAir()).count();
        check(broken==expected,"input.ultmine."+name+".native_blocks_"+broken+"_expected_"+expected);
        int wear=player.getMainHandItem().getDamageValue();
        check(wear==(creative?0:expected),"input.ultmine."+name+".durability");
        long drops=player.getInventory().getNonEquipmentItems().stream().filter(s->s.is(Items.RAW_IRON)).mapToInt(ItemStack::getCount).sum();
        if(!creative&&expected==9)check(drops==9,"input.ultmine."+name+".real_drops_once");
    }
}
