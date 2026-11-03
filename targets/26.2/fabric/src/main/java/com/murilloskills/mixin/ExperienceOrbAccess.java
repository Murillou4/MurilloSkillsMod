package com.murilloskills.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.entity.ExperienceOrb;
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccess { @Accessor("count") int murilloskills$count(); }
