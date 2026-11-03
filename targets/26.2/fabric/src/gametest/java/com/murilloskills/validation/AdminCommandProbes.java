package com.murilloskills.validation;

import com.murilloskills.data.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.skills.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import static com.murilloskills.validation.Checks.check;

final class AdminCommandProbes {
    static void run(ServerPlayer player)throws Exception{
        var c=GameplayEvents.context(player);var p=new PlayerProfile();c.store().replace(player,p);
        p.selected.add(SkillType.MINER);p.selected.add(SkillType.BUILDER);p.seals=7;
        var miner=p.stats(SkillType.MINER);miner.level=34;miner.highestLevel=100;miner.prestige=2;miner.branch=1;
        var builder=p.stats(SkillType.BUILDER);builder.level=8;builder.xp=123;
        var inactive=p.stats(SkillType.ARCHER);inactive.level=19;inactive.highestLevel=19;inactive.prestige=3;inactive.xp=15;
        p.paragon=SkillType.MINER;p.hasChosenParagon=true;ContractService.ensure(p);p.contracts.getFirst().progress=3;
        var dispatcher=player.level().getServer().getCommands().getDispatcher();
        var self=player.createCommandSourceStack();boolean denied=false;
        try{dispatcher.execute("murilloskills admin maxselected",self.withPermission(PermissionSet.NO_PERMISSIONS));}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
        check(denied&&miner.level==34,"admin.maxselected.requires_gamemaster");
        check(dispatcher.execute("murilloskills admin maxselected",self.withPermission(PermissionSet.ALL_PERMISSIONS))==2,"admin.maxselected.self_command");
        check(miner.level==100&&builder.level==100&&miner.xp==0&&builder.xp==0&&builder.highestLevel==100,"admin.maxselected.levels_xp_unlocks");
        check(inactive.level==19&&inactive.xp==15&&inactive.prestige==3,"admin.maxselected.inactive_progress_unchanged");
        check(miner.prestige==2&&miner.branch==1&&p.seals==7&&p.paragon==SkillType.MINER&&p.contracts.getFirst().progress==3,"admin.maxselected.preserves_choices_and_rewards");
        check(miner.claimedMilestone==100&&builder.claimedMilestone==100,"admin.maxselected.no_repeated_first_rewards");
        check(ProfileCodec.read(c.store().path(player.getUUID())).stats(SkillType.BUILDER).level==100,"admin.maxselected.saved");
        builder.level=3;
        check(dispatcher.execute("murilloskills admin "+player.getName().getString()+" maxselected",player.level().getServer().createCommandSourceStack())==2&&builder.level==100,"admin.maxselected.target_command");
        p.selected.clear();
        check(dispatcher.execute("murilloskills admin "+player.getName().getString()+" maxselected",player.level().getServer().createCommandSourceStack())==0,"admin.maxselected.empty_selection_refused");
    }
}
