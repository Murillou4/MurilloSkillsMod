package com.murilloskills.forge112.skills;

import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.forge112.api.SkillRegistry;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.reflect.Field;
import java.util.UUID;

import static com.murilloskills.forge112.MurilloSkillsForge112.BUILDER_REACH;
import static com.murilloskills.forge112.MurilloSkillsForge112.LOG;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.isSelected;
import static com.murilloskills.forge112.utils.Forge112SkillMath.getBuilderReachBonus;

public final class Forge112Passives {
    private Forge112Passives() {
    }

    public static void applyAllPassives(EntityPlayer player, PlayerSkillDataCore data, boolean forceLog) {
        SkillRegistry.applyPassives(player, data);
        if (forceLog) {
            LOG.info("[MurilloSkills][1.12.2][Passives] Applied passives for {} selected={}", player.getName(), data.getSelectedSkills());
        }
    }

    public static void applyAttribute(EntityPlayer player, IAttribute attribute, UUID id, String name, double amount, int operation) {
        IAttributeInstance instance = player.getEntityAttribute(attribute);
        if (instance == null) {
            LOG.warn("[MurilloSkills][1.12.2][Attribute] {} attribute is null for {}", name, player.getName());
            return;
        }
        AttributeModifier old = instance.getModifier(id);
        if (old != null) {
            instance.removeModifier(old);
        }
        if (amount != 0.0D) {
            instance.applyModifier(new AttributeModifier(id, name, amount, operation).setSaved(false));
        }
    }

    public static void applyReach(EntityPlayer player, PlayerSkillDataCore data) {
        IAttribute reach = reachAttribute();
        if (reach == null) {
            if (player.ticksExisted % 200 == 0) {
                LOG.warn("[MurilloSkills][1.12.2][Builder] Forge reach attribute is null; reach passive cannot be applied.");
            }
            return;
        }
        double amount = isSelected(data, SkillType.BUILDER)
                ? getBuilderReachBonus(data.getSkill(SkillType.BUILDER))
                : 0.0D;
        applyAttribute(player, reach, BUILDER_REACH, "MurilloSkills builder reach", amount, 0);
    }

    public static IAttribute reachAttribute() {
        try {
            Field field = EntityPlayer.class.getField("REACH_DISTANCE");
            Object value = field.get(null);
            return value instanceof IAttribute ? (IAttribute) value : null;
        } catch (Throwable error) {
            return null;
        }
    }
}
