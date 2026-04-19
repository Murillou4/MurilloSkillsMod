package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overlays the original (pre-discount) enchanting requirement strikethrough
 * next to each enchanting slot when the Blacksmith discount changed the value.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends net.minecraft.client.gui.screen.ingame.HandledScreen<EnchantmentScreenHandler> {

    public EnchantmentScreenMixin(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
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
            // Draw strikethrough original to the left of the slot
            int sw = this.textRenderer.getWidth(strike);
            context.drawTextWithShadow(this.textRenderer, strike, baseX - sw - 2, rowY + 6, 0xFFAAAAAA);
        }
    }
}
