package com.murilloskills.validation;

import com.murilloskills.*;
import com.murilloskills.data.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.skills.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import static com.murilloskills.validation.Checks.check;

/** Real vanilla operations through the mod's mixins; no fixtures enter the release JAR. */
public final class NativeFeatureProbes {
    private BlockPos furnace;
    private ItemStack repair;
    private long started;

    public void begin(ServerPlayer player)throws Exception {
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var c=GameplayEvents.context(player);var p=new PlayerProfile();c.store().replace(player,p);
        // The 1.21.10 Fabric source is an attachment, not the reflective runtime's JSON schema.
        String legacy="{\"selectedSkills\":[\"MINER\"],\"paragonSkill\":\"MINER\",\"skills\":{\"MINER\":{\"level\":50,\"xp\":2905,\"prestige\":2}}}";
        var json=com.google.gson.JsonParser.parseString(legacy);
        player.setAttached(LegacySource.ATTACHMENT,new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE,json));
        var snapshot=LegacySource.locate(c);
        check(LegacyImporter.convert(snapshot.json()).stats(SkillType.MINER).prestige==2,"import.native_fabric_attachment_preview");
        var nbt=new net.minecraft.nbt.CompoundTag();var attachments=new net.minecraft.nbt.CompoundTag();
        attachments.put("murilloskills:player_skills",com.mojang.serialization.JsonOps.INSTANCE.convertTo(net.minecraft.nbt.NbtOps.INSTANCE,json));nbt.put("fabric:attachments",attachments);
        var source=c.store().directory.resolve("native-legacy-fixture.dat");java.nio.file.Files.createDirectories(source.getParent());net.minecraft.nbt.NbtIo.writeCompressed(nbt,source);
        byte[] original=java.nio.file.Files.readAllBytes(source);
        var imported=LegacyImporter.importFile(source,c.store().directory.resolve("native-import-fixture.json"),new PlayerProfile(),LegacySource.read(source));
        check(imported.stats(SkillType.MINER).level==50&&imported.stats(SkillType.MINER).prestige==2&&imported.seals==0,"import.native_nbt_conversion");
        check(java.util.Arrays.equals(original,java.nio.file.Files.readAllBytes(source)),"import.native_nbt_original_unchanged");
        p.selected.addAll(java.util.List.of(SkillType.ARCHER,SkillType.WARRIOR,SkillType.FISHER,SkillType.BLACKSMITH));
        p.featureSettings.put("repair_aura",true);
        for(var type:p.selected){var s=p.stats(type);s.level=99;s.highestLevel=100;s.branch=1;s.claimedMilestone=100;}
        GameplayEvents.refresh(c);

        var arrow=new Arrow(player.level(),player,new ItemStack(Items.ARROW),new ItemStack(Items.BOW));
        arrow.shoot(1,0,0,2,0);
        check(arrow.getDeltaMovement().length()>2.1,"native.ARCHER.projectile_velocity");
        var victim=new Zombie(player.level());victim.setPos(player.getX()+3,player.getY(),player.getZ());
        victim.setNoAi(true);player.level().addFreshEntity(victim);
        float before=victim.getHealth();victim.hurtServer(player.level(),player.damageSources().arrow(arrow,player),4);
        check(before-victim.getHealth()>4,"native.ARCHER.ranged_damage");
        victim.discard();arrow.discard();

        var rod=new ItemStack(Items.FISHING_ROD);player.setItemInHand(InteractionHand.MAIN_HAND,rod);
        var hook=new FishingHook(player,player.level(),0,0);
        hook.setPos(player.getX()+2,player.getY(),player.getZ());player.level().addFreshEntity(hook);
        var luck=FishingHook.class.getDeclaredField("luck");luck.setAccessible(true);
        check(luck.getInt(hook)>0,"native.FISHER.hook_luck");
        var nibble=FishingHook.class.getDeclaredField("nibble");nibble.setAccessible(true);nibble.setInt(hook,20);
        double xp=p.stats(SkillType.FISHER).xp;
        hook.retrieve(rod);
        check(p.stats(SkillType.FISHER).xp>xp,"native.FISHER.retrieved_loot_awards_xp");

        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(500);sword.set(DataComponents.REPAIR_COST,20);
        p.selected.remove(SkillType.BLACKSMITH);
        var ordinary=new AnvilMenu(41,player.getInventory(),ContainerLevelAccess.NULL);
        ordinary.getSlot(0).set(sword.copy());ordinary.getSlot(1).set(new ItemStack(Items.DIAMOND));ordinary.createResult();
        int normalCost=ordinary.getCost();p.selected.add(SkillType.BLACKSMITH);
        var skilled=new AnvilMenu(42,player.getInventory(),ContainerLevelAccess.NULL);
        skilled.getSlot(0).set(sword.copy());skilled.getSlot(1).set(new ItemStack(Items.DIAMOND));skilled.createResult();
        check(skilled.getCost()>0&&skilled.getCost()<normalCost,"native.BLACKSMITH.anvil_discount");
        var sharpness=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        var a=new ItemStack(Items.DIAMOND_SWORD);a.enchant(sharpness,5);
        var b=new ItemStack(Items.DIAMOND_SWORD);b.enchant(sharpness,5);
        var forge=new AnvilMenu(43,player.getInventory(),ContainerLevelAccess.NULL);
        forge.getSlot(0).set(a);forge.getSlot(1).set(b);forge.createResult();
        check(EnchantmentHelper.getEnchantmentsForCrafting(forge.getSlot(2).getItem()).getLevel(sharpness)==6,"native.BLACKSMITH.overenchant_output");
        player.experienceLevel=100;int cost=forge.getCost();var result=forge.getSlot(2).safeTake(1,1,player);
        check(!result.isEmpty()&&forge.getSlot(0).getItem().isEmpty()&&forge.getSlot(1).getItem().isEmpty()&&player.experienceLevel==100-cost,"native.BLACKSMITH.anvil_consumes_inputs_and_levels");
        player.experienceLevel=30;player.onEnchantmentPerformed(new ItemStack(Items.DIAMOND_PICKAXE),3);
        check(player.experienceLevel==29,"native.BLACKSMITH.table_level_discount");

        p.stats(SkillType.BLACKSMITH).branch=2;
        furnace=player.blockPosition().offset(2,0,2);
        player.level().setBlock(furnace,Blocks.FURNACE.defaultBlockState(),3);
        var block=(AbstractFurnaceBlockEntity)player.level().getBlockEntity(furnace);
        block.setItem(0,new ItemStack(Items.IRON_ORE));block.setItem(1,new ItemStack(Items.COAL));
        repair=new ItemStack(Items.DIAMOND_SWORD);repair.setDamageValue(30);player.setItemInHand(InteractionHand.MAIN_HAND,repair);
        started=c.now();c.runtime().combatUntil=0;c.runtime().activeUntil.clear();
    }
    public boolean ready(ServerPlayer player){
        var block=(AbstractFurnaceBlockEntity)player.level().getBlockEntity(furnace);
        return block!=null&&!block.getItem(2).isEmpty()&&repair.getDamageValue()<30;
    }
    public void finish(ServerPlayer player){
        check(ready(player),"native.BLACKSMITH.timed_furnace_and_repair");
        check(GameplayEvents.context(player).now()-started<=220,"native.BLACKSMITH.accelerated_smelting");
        player.level().setBlock(furnace,Blocks.AIR.defaultBlockState(),3);
    }
}

