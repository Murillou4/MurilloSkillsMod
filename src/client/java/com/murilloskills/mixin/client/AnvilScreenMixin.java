package com.murilloskills.mixin.client;

import com.murilloskills.client.ui.BlacksmithCostAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overlays the Blacksmith dual cost label ({@code <strike>original</strike> → discounted})
 * on top of the vanilla anvil cost label only when the final cost is actually lower.
 * If over-enchanting or another modifier pushes the total cost above the base value,
 * we leave the vanilla label untouched to avoid presenting an increase as a discount.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends net.minecraft.client.gui.screen.ingame.HandledScreen<AnvilScreenHandler> {

    public AnvilScreenMixin(AnvilScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void murilloskills$drawDualCost(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        AnvilScreenHandler h = this.handler;
        if (!(h instanceof BlacksmithCostAccessor accessor)) {
            return;
        }

        int original = accessor.murilloskills$getOriginalLevelCost();
        int displayed = h.getLevelCost();
        if (original <= 0 || displayed <= 0 || original <= displayed) {
            return;
        }

        MutableText label = Text.literal(String.valueOf(original))
                .formatted(Formatting.STRIKETHROUGH, Formatting.GRAY)
                .append(Text.literal(" → " + displayed).formatted(Formatting.GREEN, Formatting.BOLD));

        Text vanillaLabel = Text.translatable("container.repair.cost", displayed);

        int textWidth = this.textRenderer.getWidth(label);
        int x = this.backgroundWidth - 8 - textWidth - 2;
        int y = 69;

        int vanillaX = this.backgroundWidth - 8 - this.textRenderer.getWidth(vanillaLabel) - 2;
        int coverLeft = Math.max(8, Math.min(x, vanillaX) - 2);
        context.fill(coverLeft, 67, this.backgroundWidth - 8, 79, 0xCF000000);
        context.drawTextWithShadow(this.textRenderer, label, x, y, 0xFFFFFFFF);
    }
}
