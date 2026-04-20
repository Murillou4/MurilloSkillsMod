package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
import com.murilloskills.data.ClientSkillData;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the Blacksmith enchanting-table requirement discount to the handler
 * at render time (when the synced values are guaranteed to be current) and
 * overlays the strikethrough original requirement next to each slot.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends net.minecraft.client.gui.screen.ingame.HandledScreen<EnchantmentScreenHandler> {

    @Unique
    private final int[] murilloskills$lastAppliedDiscount = new int[3];

    public EnchantmentScreenMixin(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void murilloskills$applyDiscountBeforeRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EnchantmentScreenHandler h = this.handler;
        if (!(h instanceof BlacksmithCostAccessor accessor)) {
            return;
        }
        int[] originals = accessor.murilloskills$getOriginalEnchantmentPower();
        if (originals == null) {
            return;
        }

        boolean blacksmithQualifies = false;
        int level = 0;
        if (ClientSkillData.isSkillSelected(MurilloSkillsList.BLACKSMITH)) {
            PlayerSkillData.SkillStats stats = ClientSkillData.get(MurilloSkillsList.BLACKSMITH);
            level = stats != null ? stats.level : 0;
            blacksmithQualifies = level >= SkillConfig.BLACKSMITH_EFFICIENT_ANVIL_LEVEL;
        }

        for (int i = 0; i < 3 && i < h.enchantmentPower.length && i < originals.length; i++) {
            int current = h.enchantmentPower[i];
            if (current <= 0) {
                originals[i] = 0;
                this.murilloskills$lastAppliedDiscount[i] = 0;
                continue;
            }
            // If the handler's current value still matches our last-applied
            // discount, the original is already cached — skip re-computation.
            if (current == this.murilloskills$lastAppliedDiscount[i] && originals[i] > 0) {
                continue;
            }
            // Fresh value from server sync: treat as original.
            originals[i] = current;
            this.murilloskills$lastAppliedDiscount[i] = 0;

            if (!blacksmithQualifies) {
                continue;
            }

            int discounted = SkillConfig.getBlacksmithEnchantingTableRequirement(level, current);
            if (discounted > 0 && discounted != current) {
                h.enchantmentPower[i] = discounted;
                this.murilloskills$lastAppliedDiscount[i] = discounted;
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void murilloskills$drawDualEnchantPower(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EnchantmentScreenHandler h = this.handler;
        if (!(h instanceof BlacksmithCostAccessor accessor)) {
            return;
        }
        int[] originals = accessor.murilloskills$getOriginalEnchantmentPower();
        if (originals == null) {
            return;
        }

        int baseX = this.x + 60;
        int baseY = this.y + 14;

        for (int i = 0; i < 3 && i < originals.length && i < h.enchantmentPower.length; i++) {
            int original = originals[i];
            int displayed = h.enchantmentPower[i];
            if (original <= 0 || displayed <= 0 || original == displayed) {
                continue;
            }
            int rowY = baseY + i * 19;
            Text strike = Text.literal(String.valueOf(original))
                    .formatted(Formatting.STRIKETHROUGH, Formatting.GRAY);
            int sw = this.textRenderer.getWidth(strike);
            context.drawTextWithShadow(this.textRenderer, strike, baseX - sw - 2, rowY + 6, 0xFFAAAAAA);
        }
    }
}
