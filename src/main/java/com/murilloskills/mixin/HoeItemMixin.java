package com.murilloskills.mixin;

import com.murilloskills.data.ModAttachments;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.impl.FarmerSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends Farmer area mode to hoe tilling.
 * When the mode is enabled, a hoe tills the entire selected square area.
 */
@Mixin(HoeItem.class)
public class HoeItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void onHoeUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (context.getSide() == Direction.DOWN) {
            return;
        }

        PlayerSkillData data = serverPlayer.getAttachedOrCreate(ModAttachments.PLAYER_SKILLS);
        if (!data.isSkillSelected(MurilloSkillsList.FARMER)) {
            return;
        }

        var stats = data.getSkill(MurilloSkillsList.FARMER);
        if (stats.level < SkillConfig.FARMER_AREA_PLANTING_LEVEL) {
            return;
        }

        int radius = FarmerSkill.getAreaPlantingRadius(serverPlayer.getUuid(), stats.level);
        if (radius <= 0 || !FarmerSkill.canUseAreaPlanting(serverPlayer.getUuid())) {
            return;
        }

        BlockPos origin = context.getBlockPos();
        ItemStack hoeStack = context.getStack();
        int tilled = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = origin.add(dx, 0, dz);
                if (tryTill(world, pos, serverPlayer)) {
                    tilled++;
                }
            }
        }

        if (tilled > 0) {
            world.playSound(null, origin, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
            hoeStack.damage(1, serverPlayer, context.getHand().getEquipmentSlot());
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    private static boolean tryTill(World world, BlockPos pos, PlayerEntity player) {
        BlockState state = world.getBlockState(pos);
        if (!world.getBlockState(pos.up()).isAir()) {
            return false;
        }

        BlockState result;
        if (state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.DIRT_PATH) || state.isOf(Blocks.PODZOL)) {
            result = Blocks.FARMLAND.getDefaultState();
        } else if (state.isOf(Blocks.COARSE_DIRT)) {
            result = Blocks.DIRT.getDefaultState();
        } else {
            return false;
        }

        world.setBlockState(pos, result, Block.NOTIFY_ALL_AND_REDRAW);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, result));
        return true;
    }
}
