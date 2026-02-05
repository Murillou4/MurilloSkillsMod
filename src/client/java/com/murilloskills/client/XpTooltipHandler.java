package com.murilloskills.client;

import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.utils.FarmerXpGetter;
import com.murilloskills.utils.MinerXpGetter;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class XpTooltipHandler {
    private XpTooltipHandler() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register(XpTooltipHandler::appendXpInfo);
    }

    private static void appendXpInfo(ItemStack stack, net.minecraft.client.item.TooltipContext context,
            java.util.List<Text> lines) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        var block = blockItem.getBlock();
        SkillReceptorResult minerXp = MinerXpGetter.isMinerXpBlock(block, false, false);
        if (minerXp.didGainXp()) {
            lines.add(Text.translatable("murilloskills.tooltip.xp_block",
                    Text.translatable("murilloskills.skill.name.miner"), minerXp.getXpAmount())
                    .formatted(Formatting.GRAY));
        }

        SkillReceptorResult farmerXp = FarmerXpGetter.getCropHarvestXp(block, true);
        if (farmerXp.didGainXp()) {
            lines.add(Text.translatable("murilloskills.tooltip.xp_block",
                    Text.translatable("murilloskills.skill.name.farmer"), farmerXp.getXpAmount())
                    .formatted(Formatting.GRAY));
        }
    }
}
