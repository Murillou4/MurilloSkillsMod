package com.murilloskills.data;

import com.murilloskills.config.ModConfig;

public class XpCurveDefinition {
    public Formula formula = new Formula();

    public int getXpForLevel(int level) {
        return formula.base + (level * formula.multiplier) + (formula.exponent * level * level);
    }

    public static XpCurveDefinition defaultsFromConfig() {
        ModConfig.XpConfig xpConfig = ModConfig.get().xp;
        XpCurveDefinition curve = new XpCurveDefinition();
        curve.formula = new Formula(xpConfig.base, xpConfig.multiplier, xpConfig.exponent);
        return curve;
    }

    public static class Formula {
        public int base = 60;
        public int multiplier = 15;
        public int exponent = 2;

        public Formula() {
        }

        public Formula(int base, int multiplier, int exponent) {
            this.base = base;
            this.multiplier = multiplier;
            this.exponent = exponent;
        }
    }
}
