package com.murilloskills.client;
import com.mojang.blaze3d.platform.InputConstants;
import com.murilloskills.network.Protocol;
import com.murilloskills.skills.SkillType;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.*;
import org.lwjgl.glfw.GLFW;

/** Central input path runs before vanilla starts breaking, including creative's first break. */
public final class MiningInput {
    private static boolean held,latched;
    private MiningInput(){}
    public static boolean held(){return held;}
    public static void reset(){if(held&&ClientState.available&&net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(Protocol.Action.TYPE))ClientState.send(Protocol.Op.MINING_HOLD,"",0);held=false;latched=false;}
    public static boolean down(Minecraft client,KeyMapping mapping){
        if(mapping.isDown())return true;var key=KeyMappingHelper.getBoundKeyOf(mapping);
        if(key.getType()==InputConstants.Type.KEYSYM&&key.getValue()>=0)return InputConstants.isKeyDown(client.getWindow(),key.getValue());
        if(key.getType()==InputConstants.Type.MOUSE)return GLFW.glfwGetMouseButton(client.getWindow().handle(),key.getValue())==GLFW.GLFW_PRESS;
        return false;
    }
    public static void sync(Minecraft client,boolean beforeAttack){
        boolean allowed=ClientState.available&&client.player!=null&&client.gui.screen()==null;
        while(MurilloSkillsClient.mine.consumeClick())if(allowed){MurilloSkillsClient.drainConflicts(MurilloSkillsClient.mine);if(ClientPreferences.current.miningToggle)latched=!latched;ClientState.send(Protocol.Op.PREVIEW,"",0);}
        if(client.player==null)latched=false;
        boolean next=allowed&&(ClientPreferences.current.miningToggle?latched:down(client,MurilloSkillsClient.mine));
        if(next!=held||beforeAttack&&next){held=next;ClientState.send(Protocol.Op.MINING_HOLD,"",next?1:0);}
    }
    public static String blockedReason(){
        var p=ClientState.profile;if(!p.selected(SkillType.MINER))return "Escolha a profissão Minerador.";
        if(!p.unlocked(SkillType.MINER,15))return "Ultmine libera no nível 15 de Minerador.";return "";
    }
}
