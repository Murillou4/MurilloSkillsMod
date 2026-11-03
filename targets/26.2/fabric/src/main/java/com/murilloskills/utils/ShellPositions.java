package com.murilloskills.utils;
import net.minecraft.core.BlockPos;
/** Nearest-first complete cube, bounded by callers per tick. */
public final class ShellPositions extends com.google.common.collect.AbstractIterator<BlockPos> {
    private final BlockPos center;private final int maximum;private int shell,x,y,z;
    public ShellPositions(BlockPos center,int radius){this.center=center.immutable();maximum=radius;}
    protected BlockPos computeNext(){
        while(shell<=maximum){
            int xx=x,yy=y,zz=z,edge=shell;
            if(++x>shell){x=-shell;if(++y>shell){y=-shell;if(++z>shell){shell++;x=y=z=-shell;}}}
            if(Math.max(Math.max(Math.abs(xx),Math.abs(yy)),Math.abs(zz))==edge)return center.offset(xx,yy,zz);
        }return endOfData();
    }
}
