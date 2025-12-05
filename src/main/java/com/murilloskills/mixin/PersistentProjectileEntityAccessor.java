package com.murilloskills.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin para acessar campos/métodos privados de
 * PersistentProjectileEntity
 */
@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileEntityAccessor {

    @Accessor("damage")
    double getDamage();

    @Invoker("setPierceLevel")
    void invokeSetPierceLevel(byte level);
}
