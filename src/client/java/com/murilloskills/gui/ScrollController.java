package com.murilloskills.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Manages scrolling behavior and scrollbar rendering for the ModInfoScreen.
 * Encapsulates scroll offset tracking, bounds checking, scrollbar
 * visualization, and scrollbar drag support.
 */
public class ScrollController {
    private static final int SCROLL_SPEED = 15;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_MARGIN = 4;
    private static final int MIN_THUMB_HEIGHT = 20;

    private int scrollOffset = 0;
    private int maxScrollOffset = 0;

    // Drag state
    private boolean dragging = false;
    private double dragStartY = 0;
    private int dragStartOffset = 0;

    // Cached layout values (updated each render)
    private int cachedScrollbarX, cachedScrollbarY, cachedScrollbarHeight;
    private int cachedThumbY, cachedThumbHeight;
    private int cachedContentX, cachedContentWidth;

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
     * Handles mouse click on the scrollbar track/thumb.
     *
     * @return true if the click was on the scrollbar
     */
    public boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || maxScrollOffset <= 0) {
            return false;
        }

        // Check if click is on the scrollbar track area
        if (mouseX >= cachedScrollbarX && mouseX <= cachedScrollbarX + SCROLLBAR_WIDTH
                && mouseY >= cachedScrollbarY
                && mouseY <= cachedScrollbarY + cachedScrollbarHeight) {

            // Check if click is on the thumb
            if (mouseY >= cachedThumbY && mouseY <= cachedThumbY + cachedThumbHeight) {
                // Start dragging from thumb
                dragging = true;
                dragStartY = mouseY;
                dragStartOffset = scrollOffset;
            } else {
                // Click on track - jump to position
                float clickRatio = (float) (mouseY - cachedScrollbarY - cachedThumbHeight / 2.0)
                        / (cachedScrollbarHeight - cachedThumbHeight);
                clickRatio = Math.max(0, Math.min(1, clickRatio));
                scrollOffset = (int) (clickRatio * maxScrollOffset);
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));

                // Also start dragging so user can adjust after click
                dragging = true;
                dragStartY = mouseY;
                dragStartOffset = scrollOffset;
            }
            return true;
        }
        return false;
    }

    /**
     * Handles mouse drag while scrollbar thumb is being dragged.
     *
     * @return true if drag was handled
     */
    public boolean handleMouseDragged(double mouseY) {
        if (!dragging || maxScrollOffset <= 0) {
            return false;
        }

        int trackRange = cachedScrollbarHeight - cachedThumbHeight;
        if (trackRange <= 0) {
            return true;
        }

        double deltaY = mouseY - dragStartY;
        float scrollDelta = (float) deltaY / trackRange;
        scrollOffset = dragStartOffset + (int) (scrollDelta * maxScrollOffset);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
        return true;
    }

    /**
     * Handles mouse release - stops dragging.
     *
     * @return true if was dragging
     */
    public boolean handleMouseReleased() {
        if (dragging) {
            dragging = false;
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

        // Calculate thumb size and position
        float contentRatio = (float) contentHeight / (contentHeight + maxScrollOffset);
        int thumbHeight = Math.max(MIN_THUMB_HEIGHT, (int) (scrollbarHeight * contentRatio));
        float scrollRatio = maxScrollOffset > 0 ? (float) scrollOffset / maxScrollOffset : 0;
        int thumbY = scrollbarY + (int) ((scrollbarHeight - thumbHeight) * scrollRatio);

        // Cache layout for hit detection
        this.cachedScrollbarX = scrollbarX;
        this.cachedScrollbarY = scrollbarY;
        this.cachedScrollbarHeight = scrollbarHeight;
        this.cachedThumbY = thumbY;
        this.cachedThumbHeight = thumbHeight;
        this.cachedContentX = contentX;
        this.cachedContentWidth = contentWidth;

        // Track background
        context.fill(scrollbarX, scrollbarY,
                scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight,
                palette.scrollbarBg());

        // Render thumb (brighter when dragging)
        int thumbColor = dragging ? palette.scrollbarActive() : palette.scrollbarFg();
        context.fill(scrollbarX, thumbY,
                scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                thumbColor);
    }

    /**
     * Resets scroll position to top. Called when switching tabs.
     */
    public void reset() {
        this.scrollOffset = 0;
        this.dragging = false;
    }

    public boolean isDragging() {
        return dragging;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getMaxScrollOffset() {
        return maxScrollOffset;
    }
}
