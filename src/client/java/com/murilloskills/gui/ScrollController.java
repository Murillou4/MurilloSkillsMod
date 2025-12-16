package com.murilloskills.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Manages scrolling behavior and scrollbar rendering for the ModInfoScreen.
 * Encapsulates scroll offset tracking, bounds checking, and scrollbar
 * visualization.
 * 
 * Following Single Responsibility Principle - this class only handles
 * scrolling.
 */
public class ScrollController {
    private static final int SCROLL_SPEED = 15;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_MARGIN = 4;
    private static final int MIN_THUMB_HEIGHT = 20;

    private int scrollOffset = 0;
    private int maxScrollOffset = 0;

    /**
     * Updates the maximum scroll offset based on content height.
     * Should be called whenever the content changes or tab switches.
     */
    public void updateMaxScroll(int contentHeight, int viewportHeight) {
        this.maxScrollOffset = Math.max(0, contentHeight - viewportHeight + 30);
        // Clamp current scroll to new bounds
        this.scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
    }

    /**
     * Handles mouse scroll input.
     * 
     * @return true if the scroll was handled, false otherwise
     */
    public boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount,
            int contentX, int contentY, int contentWidth, int contentHeight) {
        if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
                mouseY >= contentY && mouseY <= contentY + contentHeight) {
            scrollOffset -= (int) (verticalAmount * SCROLL_SPEED);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            return true;
        }
        return false;
    }

    /**
     * Renders the scrollbar if content is scrollable.
     */
    public void renderScrollbar(DrawContext context, int contentX, int contentY,
            int contentWidth, int contentHeight, ColorPalette palette) {
        if (maxScrollOffset <= 0) {
            return; // No scrolling needed
        }

        int scrollbarX = contentX + contentWidth - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
        int scrollbarY = contentY + SCROLLBAR_MARGIN;
        int scrollbarHeight = contentHeight - (SCROLLBAR_MARGIN * 2);

        // Track background
        context.fill(scrollbarX, scrollbarY,
                scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight,
                palette.scrollbarBg());

        // Calculate thumb size and position
        float contentRatio = (float) contentHeight / (contentHeight + maxScrollOffset);
        int thumbHeight = Math.max(MIN_THUMB_HEIGHT, (int) (scrollbarHeight * contentRatio));
        float scrollRatio = maxScrollOffset > 0 ? (float) scrollOffset / maxScrollOffset : 0;
        int thumbY = scrollbarY + (int) ((scrollbarHeight - thumbHeight) * scrollRatio);

        // Render thumb
        context.fill(scrollbarX, thumbY,
                scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                palette.scrollbarFg());
    }

    /**
     * Resets scroll position to top. Called when switching tabs.
     */
    public void reset() {
        this.scrollOffset = 0;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getMaxScrollOffset() {
        return maxScrollOffset;
    }
}
