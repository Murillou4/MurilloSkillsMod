package com.murilloskills.mixin;

import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenHandler.class)
public interface ScreenHandlerInvoker {

    @Invoker("addProperty")
    Property murilloskills$invokeAddProperty(Property property);
}

