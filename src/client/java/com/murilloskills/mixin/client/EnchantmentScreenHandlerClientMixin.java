package com.murilloskills.mixin.client;

import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side Blacksmith enchanting-table discount.
 *
 * The server reduces {@code enchantmentPower} and syncs it via screen
 * handler Properties, but the client re-runs {@code onContentChanged}
 * when slot syncs arrive and vanilla rewrites the array with the
 * non-discounted roll. This mixin reapplies the discount using
 * {@link ClientSkillData} so the displayed required level stays low.
 */
@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerClientMixin {

    @Shadow
    @Final
    private int[] enchantmentPower;

    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void murilloskills$applyClientEnchantingDiscount(Inventory inventory, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || !mc.player.getEntityWorld().isClient()) {
            return;
        }

        if (!ClientSkillData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        PlayerSkillData.SkillStats stats = ClientSkillData.get(MurilloSkillsList.BLACKSMITH);
        int level = stats != null ? stats.level : 0;
        if (level < SkillConfig.BLACKSMITH_EFFICIENT_ANVIL_LEVEL) {
            return;
        }

        for (int i = 0; i < this.enchantmentPower.length; i++) {
            int current = this.enchantmentPower[i];
            if (current <= 0) {
                continue;
            }
            int discounted = SkillConfig.getBlacksmithEnchantingTableRequirement(level, current);
            if (discounted != current) {
                this.enchantmentPower[i] = discounted;
            }
        }
    }
}
