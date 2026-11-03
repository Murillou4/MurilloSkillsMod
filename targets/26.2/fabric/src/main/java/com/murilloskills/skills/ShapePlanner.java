package com.murilloskills.skills;
import net.minecraft.core.*;
import java.util.*;

public final class ShapePlanner {
    public static final List<String> MINING=List.of("VEIN","3X3","2X1","LINE","STAIRS","QUARRY");
    public static final List<String> PLACEMENT=List.of("PLANE","BOX","LINE","WALL","STAIRS","COLUMN","TUNNEL","CIRCLE","SPHERE","SINGLE");
    private ShapePlanner(){}
    public static List<BlockPos> plan(String shape,BlockPos start,Direction facing,int size,int depth,int spacing,boolean hollow,int limit){
        int n=shape.equals("TUNNEL")?3:Math.clamp(size,1,20),d=Math.clamp(depth,1,20),step=Math.clamp(spacing,1,4);
        Direction forward=facing.getAxis().isVertical()?Direction.NORTH:facing;
        Direction right=forward.getClockWise();LinkedHashSet<BlockPos> out=new LinkedHashSet<>();
        int half=n/2;
        if(shape.equals("SINGLE"))return List.of(start);
        for(int z=0;z<Math.max(n,d)&&out.size()<limit;z++)for(int y=0;y<n&&out.size()<limit;y++)for(int x=0;x<n&&out.size()<limit;x++){
            int across=x-half,vertical=y,along=z;boolean include=false;
            switch(shape){
                case "LINE" -> {include=x==0&&y==0&&z<d;across=0;}
                case "COLUMN" -> {include=x==0&&z==0;across=0;}
                case "3X3" -> {include=x<3&&y<3&&z<d;across=x-1;vertical=y-1;}
                case "2X1" -> {include=x<2&&y==0&&z<d;across=x;}
                case "STAIRS" -> {include=y==0&&z<d;vertical=z;}
                case "WALL" -> include=z==0;
                case "PLANE","QUARRY" -> include=y==0&&z<n;
                case "BOX" -> include=z<d&&(!hollow||x==0||x==n-1||y==0||y==n-1||z==0||z==d-1);
                case "TUNNEL" -> {include=x<3&&y<3&&z<d&&(x==0||x==2||y==0||y==2);across=x-1;}
                case "CIRCLE" -> {int zz=z-half;include=y==0&&across*across+zz*zz<=half*half&&(!hollow||across*across+zz*zz>=(half-1)*(half-1));along=zz;}
                case "SPHERE" -> {int yy=y-half,zz=z-half;int q=across*across+yy*yy+zz*zz;include=q<=half*half&&(!hollow||q>=(half-1)*(half-1));vertical=yy;along=zz;}
                default -> {}
            }
            if(include)out.add(start.relative(right,across*step).above(vertical*step).relative(forward,along*step));
        }
        return List.copyOf(out);
    }
    public static List<BlockPos> mining(String shape,BlockPos root,Direction face,Direction facing,int size,int depth,int spacing,int limit){
        if(shape.equals("LINE")){
            List<BlockPos> line=new ArrayList<>();for(int i=0;i<Math.clamp(depth,1,20)&&line.size()<limit;i++)line.add(root.relative(face.getOpposite(),i));return List.copyOf(line);
        }
        if(shape.equals("STAIRS")){
            List<BlockPos> stairs=new ArrayList<>();int sign=face==Direction.UP?-1:1;
            for(int z=0;z<Math.clamp(depth,1,20)&&stairs.size()<limit;z++)for(int xx=0;xx<Math.clamp(size,1,20)&&stairs.size()<limit;xx++)
                stairs.add(root.relative(facing,z*spacing).relative(facing.getClockWise(),(xx-size/2)*spacing).above(sign*z*spacing));return List.copyOf(stairs);
        }
        if(!shape.equals("3X3")&&!shape.equals("2X1"))return plan(shape,root,facing,size,depth,spacing,false,limit);
        Direction normal=face.getOpposite(),up=normal.getAxis().isVertical()?facing:Direction.UP;
        Direction right=normal.getAxis().isVertical()?facing.getClockWise():normal.getClockWise();
        int width=shape.equals("3X3")?3:2,height=shape.equals("3X3")?3:1;List<BlockPos> result=new ArrayList<>();
        for(int z=0;z<Math.clamp(depth,1,20)&&result.size()<limit;z++)for(int y=0;y<height&&result.size()<limit;y++)for(int x=0;x<width&&result.size()<limit;x++)
            result.add(root.relative(right,x-(width==3?1:0)).relative(up,y-(height==3?1:0)).relative(normal,z));
        return List.copyOf(result);
    }
}

