package com.murilloskills.utils;
import com.murilloskills.api.*;
import com.murilloskills.impl.BlacksmithSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
public final class BlacksmithCrafting {
    private BlacksmithCrafting(){}
    public static ItemStack enhance(SkillContext c,ItemStack left,ItemStack right,ItemStack vanilla){
        int bonus=((BlacksmithSkill)SkillRegistry.get(SkillType.BLACKSMITH)).enchantLimit(c);
        if(bonus==0||left.isEmpty()||right.isEmpty()||!(right.is(Items.ENCHANTED_BOOK)||left.is(right.getItem())))return vanilla;
        var a=EnchantmentHelper.getEnchantmentsForCrafting(left);var b=EnchantmentHelper.getEnchantmentsForCrafting(right);
        ItemStack result=vanilla.isEmpty()?left.copyWithCount(1):vanilla.copy();boolean changed=false;
        for(var enchant:a.keySet()){
            int level=a.getLevel(enchant),other=b.getLevel(enchant),max=enchant.value().getMaxLevel();
            if(max>1&&level==other&&level>=max&&level<max+bonus){
                final int next=level+1;EnchantmentHelper.updateEnchantments(result,mutable->mutable.set(enchant,next));changed=true;
            }
        }
        return changed?result:vanilla;
    }
}

