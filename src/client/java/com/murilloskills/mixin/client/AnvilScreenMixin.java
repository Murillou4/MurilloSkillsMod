package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a stable Blacksmith dual-cost overlay after vanilla foreground render.
 *
 * We inject at RETURN and paint over the vanilla cost panel only when an actual
 * discount is present (original > displayed). This avoids replacing/canceling
 * the full foreground pass, which can conflict with other GUI changes.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends net.minecraft.client.gui.screen.ingame.HandledScreen<AnvilScreenHandler> {

    @Shadow
    @Final
    private PlayerEntity player;

    public AnvilScreenMixin(AnvilScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "drawForeground", at = @At("RETURN"))
    private void murilloskills$drawDualCost(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        AnvilScreenHandler h = this.handler;
        int displayed = h.getLevelCost();
        if (displayed <= 0 || displayed >= 40) {
            return;
        }
        if (!h.getSlot(2).hasStack()) {
            return;
        }

        int original = displayed;
        if (h instanceof BlacksmithCostAccessor accessor) {
            int syncedOriginal = accessor.murilloskills$getOriginalLevelCost();
            if (syncedOriginal > 0) {
                original = syncedOriginal;
            }
        }

        if (original <= displayed) {
            return;
        }

        boolean canTake = h.getSlot(2).canTakeItems(this.player);
        int discountedColor = canTake ? 0xFF6AFF8E : 0xFFFF6B6B;
        int savingsColor = canTake ? 0xFF66F5FF : 0xFFFFB0B0;

        int savingsPercent = Math.max(1,
                Math.round((1.0f - (float) displayed / (float) Math.max(1, original)) * 100.0f));

        String originalText = Integer.toString(original);
        String arrowText = "→";
        String discountedText = Integer.toString(displayed);
        String savingsText = " (" + savingsPercent + "%)";

        int labelWidth = this.textRenderer.getWidth(originalText)
                + this.textRenderer.getWidth(arrowText)
                + this.textRenderer.getWidth(discountedText)
                + this.textRenderer.getWidth(savingsText);

        int x = this.backgroundWidth - 8 - labelWidth - 2;
        int y = 58;
        int panelLeft = x - 4;
        int panelRight = this.backgroundWidth - 8;
        int panelTop = y - 3;
        int panelBottom = y + 11;
        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xD015171E);
        context.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0x66FFFFFF);
        context.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0x88000000);

        int drawX = x;
        context.drawTextWithShadow(this.textRenderer, Text.literal(originalText), drawX, y, 0xFFB7BEC9);
        int originalWidth = this.textRenderer.getWidth(originalText);
        int strikeY = y + 4;
        context.fill(drawX, strikeY, drawX + originalWidth, strikeY + 1, 0xFF7A8090);

        drawX += originalWidth;
        context.drawTextWithShadow(this.textRenderer, Text.literal(arrowText), drawX, y, 0xFF949AA7);
        drawX += this.textRenderer.getWidth(arrowText);

        context.drawTextWithShadow(this.textRenderer, Text.literal(discountedText), drawX, y, discountedColor);
        drawX += this.textRenderer.getWidth(discountedText);

        context.drawTextWithShadow(this.textRenderer, Text.literal(savingsText), drawX, y, savingsColor);
    }
}
