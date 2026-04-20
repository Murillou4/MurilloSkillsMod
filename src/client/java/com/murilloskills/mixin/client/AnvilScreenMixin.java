package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.MutableText;
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
            MutableText dualLabel = Text.literal(String.valueOf(original))
                    .formatted(Formatting.STRIKETHROUGH, Formatting.GRAY)
                    .append(Text.literal(" "))
                    .append(Text.literal(String.valueOf(displayed)).formatted(discountedColor, Formatting.BOLD));
            label = dualLabel;
            color = 0xFFFFFFFF;
        }

        int x = this.backgroundWidth - 8 - this.textRenderer.getWidth(label) - 2;
        context.fill(x - 2, 67, this.backgroundWidth - 8, 79, 1325400064);
        context.drawTextWithShadow(this.textRenderer, label, x, 69, color);

        ci.cancel();
    }
}
