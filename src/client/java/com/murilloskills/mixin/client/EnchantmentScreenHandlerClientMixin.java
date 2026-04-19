package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BlacksmithOverEnchanting;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Client-side Blacksmith enchanting-table discount.
 *
 * The server keeps the vanilla {@code enchantmentPower} values so the reroll on
 * click matches the previewed enchantment. This mixin only discounts the
 * client's local copy for rendering and button availability, while preserving
 * the original values for the screen overlay.
 *
 * It also stores the original (pre-discount) values so the
 * {@code EnchantmentScreen} mixin can render both numbers.
 */
@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerClientMixin implements BlacksmithCostAccessor {

    @Shadow
    @Final
    public int[] enchantmentPower;

    @Shadow
    @Final
    private Property seed;

    @Unique
    private final int[] murilloskills$originalEnchantmentPower = new int[3];

    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void murilloskills$applyClientEnchantingDiscount(Inventory inventory, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || !mc.player.getEntityWorld().isClient()) {
            return;
        }

        for (int i = 0; i < this.enchantmentPower.length && i < this.murilloskills$originalEnchantmentPower.length; i++) {
            this.murilloskills$originalEnchantmentPower[i] = this.enchantmentPower[i];
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

    @Inject(method = "generateEnchantments", at = @At("RETURN"), cancellable = true)
    private void murilloskills$previewDeterministicTableBonus(
            DynamicRegistryManager registryManager,
            ItemStack stack,
            int slot,
            int level,
            CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        if (!ClientSkillData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            return;
        }

        PlayerSkillData.SkillStats stats = ClientSkillData.get(MurilloSkillsList.BLACKSMITH);
        int blacksmithLevel = stats != null ? stats.level : 0;
        if (!BlacksmithOverEnchanting.isUnlocked(blacksmithLevel)) {
            return;
        }

        List<EnchantmentLevelEntry> generated = cir.getReturnValue();
        if (generated == null || generated.isEmpty()) {
            return;
        }

        cir.setReturnValue(BlacksmithOverEnchanting.applyDeterministicEnchantingTableBonus(
                generated,
                this.seed.get(),
                slot));
    }

    @Override
    public int murilloskills$getOriginalLevelCost() {
        return 0;
    }

    @Override
    public int[] murilloskills$getOriginalEnchantmentPower() {
        return this.murilloskills$originalEnchantmentPower;
    }
}
