package com.murilloskills.gui.renderer;

import net.minecraft.client.gui.DrawContext;

/**
 * Strategy interface for rendering different tabs in the ModInfoScreen.
 * 
 * Each tab type (Status, Synergies, Prestige, Perks) has its own
 * implementation of this interface, following the Strategy pattern
 * to eliminate large switch statements and improve cohesion.
 * 
 * Benefits:
 * - Single Responsibility: Each renderer only handles one tab type
 * - Open/Closed Principle: Easy to add new tabs without modifying existing code
 * - Testability: Each renderer can be tested independently
 */
@FunctionalInterface
public interface TabRenderer {
    /**
     * Renders the tab content to the screen.
     * 
     * @param context       The Minecraft DrawContext for rendering
     * @param renderContext Immutable context containing layout and styling info
     * @return The total height of the rendered content (for scroll calculations)
     */
    int render(DrawContext context, RenderContext renderContext);
}
