package com.murilloskills.runtime;

import com.murilloskills.core.config.SkillType;

import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;

public final class RuntimeSkillsScreenModern extends class_437 {
    public RuntimeSkillsScreenModern() {
        super(class_2561.method_30163("MurilloSkills"));
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        super.method_25394(context, mouseX, mouseY, delta);

        int panelWidth = 340;
        int panelHeight = 214;
        int left = (this.field_22789 - panelWidth) / 2;
        int top = (this.field_22790 - panelHeight) / 2;
        class_327 font = this.field_22793;

        context.method_25294(left, top, left + panelWidth, top + panelHeight, 0xDD111827);
        context.method_25294(left, top, left + panelWidth, top + 2, 0xFFFFC857);
        context.method_25300(font, "MurilloSkills", left + 16, top + 16, 0xFFFFF3C4);
        context.method_25300(font, "Runtime compatibility port", left + 16, top + 32, 0xFFC7D2FE);
        context.method_25300(font, "Use /murilloskills stats para ver progresso.", left + 16, top + 52, 0xFFE5E7EB);
        context.method_25300(font, "Use /murilloskills select <skill> para escolher skills.", left + 16, top + 66,
                0xFFE5E7EB);
        context.method_25300(font, "O menu completo existe no target nativo 1.21.10/fabric.", left + 16, top + 80,
                0xFFCBD5E1);

        int x = left + 16;
        int y = top + 108;
        int columnWidth = 154;
        int index = 0;
        for (SkillType skill : SkillType.values()) {
            int column = index / 4;
            int row = index % 4;
            int color = skill.isMasterClass() ? 0xFFFFD166 : 0xFF8DE1D1;
            context.method_25300(font, "- " + pretty(skill), x + column * columnWidth, y + row * 18, color);
            index++;
        }
    }

    @Override
    public boolean method_25422() {
        return false;
    }

    private static String pretty(SkillType skill) {
        String lower = skill.name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
