package com.murilloskills.mixin;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends Farmer area mode to bone meal usage.
 * When the mode is enabled, bone meal is applied in the selected square area.
 */
@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void onUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        if (world.isClient() || !(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        PlayerSkillData playerData = serverPlayer
                .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        if (!playerData.isSkillSelected(MurilloSkillsList.FARMER)) {
            return;
        }

        int farmerLevel = playerData.getSkill(MurilloSkillsList.FARMER).level;
        if (farmerLevel < SkillConfig.FARMER_AREA_PLANTING_LEVEL) {
            return;
        }

        int radius = FarmerSkill.getAreaPlantingRadius(serverPlayer.getUuid(), farmerLevel);
        if (radius <= 0 || !FarmerSkill.canUseAreaPlanting(serverPlayer.getUuid())) {
            return;
        }

        ItemStack stack = context.getStack();
        BlockPos clickedPos = context.getBlockPos();
        int applications = 0;

        for (int dx = -radius; dx <= radius && !stack.isEmpty(); dx++) {
            for (int dz = -radius; dz <= radius && !stack.isEmpty(); dz++) {
                BlockPos targetPos = clickedPos.add(dx, 0, dz);
                if (BoneMealItem.useOnFertilizable(stack, world, targetPos)) {
                    world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, targetPos, 15);
                    applications++;
                }
            }
        }

        if (applications > 0) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
