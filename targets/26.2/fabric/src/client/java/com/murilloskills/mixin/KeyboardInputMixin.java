package com.murilloskills.mixin;
import com.murilloskills.client.*;
import net.minecraft.client.*;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.2 processes global friends/fullscreen keys before normal key mapping queues. */
@Mixin(KeyboardHandler.class)
abstract class KeyboardInputMixin {
    @Inject(method="keyPress",at=@At("HEAD"),cancellable=true)
    private void professions(long window,int action,KeyEvent key,CallbackInfo ci){
        var client=Minecraft.getInstance();
        if(window!=client.getWindow().handle()||!ClientState.available||client.player==null||client.level==null)return;
        if(client.gui.screen() instanceof ProfessionScreen panel&&panel.capturingKey()){
            if(action==GLFW.GLFW_PRESS)panel.keyPressed(key);
            ci.cancel();return;
        }
        if(action!=GLFW.GLFW_PRESS&&action!=GLFW.GLFW_REPEAT)return;
        if(client.options.keyDebugModifier.isDown())return;
        if(MurilloSkillsClient.dispatchKey(key,action==GLFW.GLFW_PRESS))ci.cancel();
    }
}
