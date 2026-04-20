package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla anvil cost draw with the Blacksmith dual-cost label
 * ({@code <strike>X</strike> Y}) when the final cost is actually lower. The
 * label uses a compact format so it does not overlap the {@code Inventory}
 * header and keeps the "can't afford" red color even when a discount is
 * applied, so the player still sees the indicator instead of plain vanilla
 * text.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends net.minecraft.client.gui.screen.ingame.HandledScreen<AnvilScreenHandler> {

    @Shadow
    @Final
    private PlayerEntity player;

    public AnvilScreenMixin(AnvilScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
    private void murilloskills$drawDualCost(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        super.drawForeground(context, mouseX, mouseY);

        AnvilScreenHandler h = this.handler;
        int displayed = h.getLevelCost();
        if (displayed <= 0) {
            ci.cancel();
            return;
        }

        boolean creativeMode = this.client.player != null && this.client.player.isInCreativeMode();
        int color = -8323296;
        Text label = null;

        if (displayed >= 40 && !creativeMode) {
            label = Text.translatable("container.repair.expensive");
            color = -40864;
        } else if (h.getSlot(2).hasStack()) {
            label = Text.translatable("container.repair.cost", displayed);
            if (!h.getSlot(2).canTakeItems(this.player)) {
                color = -40864;
            }
        }

        if (label == null) {
            ci.cancel();
            return;
        }

        int original = 0;
        if (h instanceof BlacksmithCostAccessor accessor) {
            original = accessor.murilloskills$getOriginalLevelCost();
        }

        boolean hasDiscount = original > displayed && displayed < 40;
        if (hasDiscount) {
            Formatting discountedColor = (color == -40864) ? Formatting.RED : Formatting.GREEN;
            int savingsPercent = Math.max(1,
                    Math.round((1.0f - (float) displayed / (float) Math.max(1, original)) * 100.0f));

            MutableText dualLabel = Text.empty()
                    .append(Text.literal(String.valueOf(original))
                            .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY, Formatting.STRIKETHROUGH)))
                    .append(Text.literal(" -> ").setStyle(Style.EMPTY.withColor(0xFF868686)))
                    .append(Text.literal(String.valueOf(displayed))
                            .setStyle(Style.EMPTY.withFormatting(discountedColor, Formatting.BOLD)))
                    .append(Text.literal(" (-" + savingsPercent + "%)")
                            .setStyle(Style.EMPTY.withColor(color == -40864 ? 0xFFFF8A8A : 0xFF55FFFF)));

            int maxLabelWidth = this.backgroundWidth - 16;
            if (this.textRenderer.getWidth(dualLabel) > maxLabelWidth) {
                dualLabel = Text.empty()
                        .append(Text.literal(String.valueOf(original))
                                .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY, Formatting.STRIKETHROUGH)))
                        .append(Text.literal(" -> ").setStyle(Style.EMPTY.withColor(0xFF868686)))
                        .append(Text.literal(String.valueOf(displayed))
                                .setStyle(Style.EMPTY.withFormatting(discountedColor, Formatting.BOLD)));
            }

            label = dualLabel;
            color = 0xFFFFFFFF;
        }

        int x = this.backgroundWidth - 8 - this.textRenderer.getWidth(label) - 2;
        int panelLeft = x - 3;
        int panelRight = this.backgroundWidth - 8;
        context.fill(panelLeft, 66, panelRight, 80, 0xAA101014);
        context.fill(panelLeft, 66, panelRight, 67, 0x55FFFFFF);
        context.fill(panelLeft, 79, panelRight, 80, 0x66000000);
        context.drawTextWithShadow(this.textRenderer, label, x, 69, color);

        ci.cancel();
    }
}
