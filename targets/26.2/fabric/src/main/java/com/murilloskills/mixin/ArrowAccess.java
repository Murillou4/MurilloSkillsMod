package com.murilloskills.mixin;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(AbstractArrow.class)
public interface ArrowAccess {
    @Invoker("setPierceLevel") void murilloskills$pierce(byte level);
}
