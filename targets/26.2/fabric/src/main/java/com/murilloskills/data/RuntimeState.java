package com.murilloskills.data;
import com.murilloskills.skills.SkillType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class RuntimeState {
    public final EnumMap<SkillType,Long> activeUntil=new EnumMap<>(SkillType.class);
    public final EnumMap<SkillType,com.murilloskills.skills.ActionCombo> combos=new EnumMap<>(SkillType.class);
    public final Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>,com.murilloskills.utils.OwnedEffects.Lease> effects=new HashMap<>();
    public long combatUntil, nextToolTick, nextPreviewTick, lastMessageTick, lastActionTick,lastLeechTick;
    public int packetWindow, packetCount;
    public long session, lastRequest, lastStateRevision, importPreviewUntil;
    public LegacySource.Snapshot legacyPreview;
    public String summarySignature="";
    public long summaryRevision;
    public final Map<String,com.murilloskills.network.ActivityView> activities=new LinkedHashMap<>();
    public final Map<String,com.murilloskills.utils.GrowthService.Job> growth=new LinkedHashMap<>();
    public final Map<java.util.UUID,net.minecraft.world.phys.Vec3> impacts=new HashMap<>();
    public BlockPos lastRadarOre;
    public double lastRadarDistance=Double.MAX_VALUE;
    public boolean reflecting;
    public long nextRadarTick;
    public BlockPos brushFirst,brushSecond;
    public String brushDimension="";
    public boolean area;
    public boolean miningHeld;
    public BlockPos pendingMiningAnchor;
    public List<BlockPos> pendingMiningPlan=List.of(),queuedMiningPlan=List.of();
    public final Map<String,Double> batchRewards=new HashMap<>();
    public final Set<BlockPos> pendingCollections=new HashSet<>();
    public final Map<String,com.murilloskills.utils.ScanService.Job> scans=new HashMap<>();
    public Vec3 lastPosition;
    public String lastDimension="";
    public String dimension="";
    public double traveled;
    public final List<Placement> undo=new ArrayList<>();
    public long undoUntil;
    public record Placement(String dimension,BlockPos position,BlockState placed,ItemStack refund) {}
    public boolean active(SkillType type,long now){return activeUntil.getOrDefault(type,0L)>now;}
    public boolean locked(long now){return combatUntil>now||activeUntil.values().stream().anyMatch(end->end>now);}
}

