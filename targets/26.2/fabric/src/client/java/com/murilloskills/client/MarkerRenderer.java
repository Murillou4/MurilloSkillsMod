package com.murilloskills.client;
import net.minecraft.gizmos.*;
public final class MarkerRenderer {
    private MarkerRenderer(){}
    public static void extract(){
        if(!ClientPreferences.current.worldMarkers)return;
        int count=0;
        for(var entry:ClientState.layers.entrySet()){
            if(entry.getValue().until()<=ClientState.clientTicks)continue;
            boolean mining=entry.getKey().equals("mine");
            int color=mining?0x80ffffff:entry.getKey().equals("place")?0xff88b98a:0xffd4b77a;
            // A dense mining preview stacks many faces: keep both the outline and fill subdued.
            int fill=mining?0x06ffffff:color&0xffffff|0x18000000;
            for(var pos:entry.getValue().positions()){Gizmos.cuboid(new net.minecraft.world.phys.AABB(pos).inflate(0.003),GizmoStyle.strokeAndFill(color,mining?1f:1.5f,fill)).setAlwaysOnTop();if(++count>=512)return;}
        }
    }
}

