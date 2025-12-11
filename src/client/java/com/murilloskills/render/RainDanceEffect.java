package com.murilloskills.render;

import net.minecraft.client.MinecraftClient;

/**
 * Client-side state tracker for Rain Dance effect.
 * Manages the visual rain effect duration.
 */
public class RainDanceEffect {

    private static boolean active = false;
    private static int remainingTicks = 0;

    /**
     * Starts the Rain Dance visual effect.
     */
    public static void start(int durationTicks) {
        active = true;
        remainingTicks = durationTicks;
    }

    /**
     * Stops the Rain Dance visual effect.
     */
    public static void stop() {
        active = false;
        remainingTicks = 0;
    }

    /**
     * Called every client tick to update the effect state.
     */
    public static void tick(MinecraftClient client) {
        if (!active)
            return;

        if (remainingTicks > 0) {
            remainingTicks--;

            // Force rain rendering when effect is active
            if (client.world != null) {
                // Set rain strength to create visual rain effect
                // This makes the client think it's raining even if the server says it isn't
                client.world.setRainGradient(1.0f);
            }
        } else {
            // Effect ended
            stop();
        }
    }

    /**
     * Checks if Rain Dance effect is currently active.
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * Gets remaining duration in ticks.
     */
    public static int getRemainingTicks() {
        return remainingTicks;
    }

    /**
     * Gets remaining duration formatted as "Xm Xs".
     */
    public static String getRemainingTimeFormatted() {
        int seconds = remainingTicks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return minutes + net.minecraft.text.Text.translatable("murilloskills.gui.time.minutes").getString() + " " + seconds + net.minecraft.text.Text.translatable("murilloskills.gui.time.seconds").getString();
        }
        return seconds + net.minecraft.text.Text.translatable("murilloskills.gui.time.seconds").getString();
    }
}
