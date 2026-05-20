package com.murilloskills.forge112.network;

import com.murilloskills.forge112.MurilloSkillsForge112;
import com.murilloskills.core.config.SkillType;
import com.murilloskills.core.data.PlayerSkillDataCore;
import com.murilloskills.core.data.SkillStatsCore;
import com.murilloskills.forge112.api.SkillRegistry;
import com.murilloskills.forge112.client.config.ClientUltmineConfig;
import com.murilloskills.forge112.client.data.UltmineClientState112;
import com.murilloskills.forge112.client.gui.UltmineShape112;
import com.murilloskills.forge112.client.render.Forge112NotificationHud;
import com.murilloskills.forge112.utils.Forge112FirstTimeHints;
import com.murilloskills.forge112.utils.Forge112MiningTools;
import com.murilloskills.forge112.utils.Forge112Notifications;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static com.murilloskills.forge112.MurilloSkillsForge112.CONFIG;
import static com.murilloskills.forge112.MurilloSkillsForge112.LOG;
import static com.murilloskills.forge112.MurilloSkillsForge112.STORE;
import static com.murilloskills.forge112.skills.Forge112Abilities.triggerAbility;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.data;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.say;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.skillName;
import static com.murilloskills.forge112.utils.Forge112PlayerServices.toggle;

public final class ModNetwork112 {
    private static final int MAX_FIELDS = 16;
    private static final int MAX_ACTION_BYTES = 64;
    private static final int MAX_FIELD_BYTES = 1024;
    private static final int MAX_TEXT_BYTES = 32767;
    private static final int MAX_PREVIEW_BLOCKS = 1536;
    private static SimpleNetworkWrapper channel;
    private static int discriminator;

    private ModNetwork112() {
    }

    public static void register() {
        if (channel != null) {
            return;
        }
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(MurilloSkillsForge112.MOD_ID + "_112");
        channel.registerMessage(NotificationHandler.class, NotificationMessage.class, discriminator++, Side.CLIENT);
        channel.registerMessage(ClientActionHandler.class, ClientActionMessage.class, discriminator++, Side.SERVER);
        channel.registerMessage(UltminePreviewHandler.class, UltminePreviewMessage.class, discriminator++, Side.CLIENT);
        MurilloSkillsForge112.LOG.info("[MurilloSkills][1.12.2][Network] SimpleNetworkWrapper registered.");
    }

    public static boolean sendNotification(EntityPlayer player, String type, String... fields) {
        if (channel == null || !(player instanceof EntityPlayerMP)) {
            return false;
        }
        channel.sendTo(new NotificationMessage(type, fields), (EntityPlayerMP) player);
        return true;
    }

    public static void sendUltmineHeld(boolean held) {
        sendToServer(new ClientActionMessage("ultmine_hold", held ? "true" : "false"));
    }

    public static void sendUltmineSelection(UltmineShape112 shape, int depth, int length, int variant,
            int legacyMaxBlocks) {
        String safeShape = (shape == null ? UltmineShape112.S_3x3 : shape).name();
        sendToServer(new ClientActionMessage("ultmine_select",
                safeShape + ";" + depth + ";" + length + ";" + variant + ";" + legacyMaxBlocks));
    }

    public static void sendUltmineConfigToServer() {
        sendToServer(new ClientActionMessage("ultmine_config", ClientUltmineConfig.toNetworkJson()));
    }

    public static void sendSkillSelection(Collection<SkillType> skills) {
        if (skills == null || skills.isEmpty()) {
            return;
        }
        StringBuilder payload = new StringBuilder();
        for (SkillType skill : skills) {
            if (skill == null) {
                continue;
            }
            if (payload.length() > 0) {
                payload.append(',');
            }
            payload.append(skill.name());
        }
        if (payload.length() > 0) {
            sendToServer(new ClientActionMessage("skill_select", payload.toString()));
        }
    }

    public static void sendSkillParagon(SkillType skill) {
        sendSkillAction("skill_paragon", skill);
    }

    public static void sendSkillPrestige(SkillType skill) {
        sendSkillAction("skill_prestige", skill);
    }

    public static void sendSkillAbility(SkillType skill) {
        sendSkillAction("skill_ability", skill);
    }

    public static void sendSkillToggle(SkillType skill, String toggleName) {
        if (skill == null || toggleName == null || toggleName.trim().length() == 0) {
            return;
        }
        sendToServer(new ClientActionMessage("skill_toggle", skill.name() + ";" + clean(toggleName)));
    }

    public static void sendSkillResetAll() {
        sendToServer(new ClientActionMessage("skill_reset_all", ""));
    }

    private static void sendSkillAction(String action, SkillType skill) {
        sendToServer(new ClientActionMessage(action, skill == null ? "" : skill.name()));
    }

    public static void requestUltminePreview(BlockPos pos, EnumFacing face) {
        if (pos == null) {
            return;
        }
        int faceIndex = face == null ? -1 : face.getIndex();
        sendToServer(new ClientActionMessage("ultmine_preview",
                pos.getX() + ";" + pos.getY() + ";" + pos.getZ() + ";" + faceIndex));
    }

    private static void sendToServer(IMessage message) {
        if (channel != null && message != null) {
            channel.sendToServer(message);
        }
    }

    private static void sendPreview(EntityPlayerMP player, List<BlockPos> positions) {
        if (channel != null && player != null) {
            channel.sendTo(new UltminePreviewMessage(positions), player);
        }
    }

    private static void handleClientAction(EntityPlayerMP player, String action, String payload) {
        if (player == null || player.world == null || action == null) {
            return;
        }
        if ("ultmine_hold".equals(action)) {
            Forge112MiningTools.setUltmineHeld(player, "true".equalsIgnoreCase(payload));
            MurilloSkillsForge112.LOG.info("[MurilloSkills][1.12.2][Network] Ultmine hold {} from {}",
                    payload, player.getName());
            if (!"true".equalsIgnoreCase(payload)) {
                sendPreview(player, Collections.<BlockPos>emptyList());
            }
            return;
        }
        if ("ultmine_select".equals(action)) {
            applyUltmineSelection(player, payload);
            return;
        }
        if ("ultmine_config".equals(action)) {
            Forge112MiningTools.applyClientUltmineConfig(player, payload);
            return;
        }
        if ("ultmine_preview".equals(action)) {
            handlePreviewRequest(player, payload);
            return;
        }
        if ("skill_select".equals(action)) {
            handleSkillSelection(player, payload);
            return;
        }
        if ("skill_paragon".equals(action)) {
            handleSkillParagon(player, parseSkill(payload));
            return;
        }
        if ("skill_prestige".equals(action)) {
            handleSkillPrestige(player, parseSkill(payload));
            return;
        }
        if ("skill_ability".equals(action)) {
            triggerAbility(player, parseSkill(payload));
            return;
        }
        if ("skill_toggle".equals(action)) {
            handleSkillToggle(player, payload);
            return;
        }
        if ("skill_reset_all".equals(action)) {
            handleSkillResetAll(player);
        }
    }

    private static void handleSkillSelection(EntityPlayerMP player, String payload) {
        List<SkillType> requested = parseSkillList(payload);
        if (requested.isEmpty()) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        Set<SkillType> before = new LinkedHashSet<SkillType>(data.getSelectedSkills());
        if (!data.setSelectedSkills(requested, CONFIG)) {
            Forge112Notifications.notice(player, "MurilloSkills", "Selection failed",
                    "Limit " + CONFIG.getMaxSelectedSkills());
            LOG.warn("[MurilloSkills][1.12.2][Network] Rejected selection {} for {} existing={}",
                    requested, player.getName(), before);
            return;
        }
        persistSkillData(player, data);
        int added = 0;
        for (SkillType skill : requested) {
            if (!before.contains(skill) && data.isSkillSelected(skill)) {
                added++;
                Forge112Notifications.selection(player, skill);
                Forge112FirstTimeHints.onSelection(player, skill);
            }
        }
        LOG.info("[MurilloSkills][1.12.2][Network] {} selected {} skill(s): {}",
                player.getName(), added, data.getSelectedSkills());
    }

    private static void handleSkillParagon(EntityPlayerMP player, SkillType skill) {
        if (skill == null) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        data.normalizeParagonState();
        if (!data.isSkillSelected(skill) && !data.setSelectedSkills(Collections.singletonList(skill), CONFIG)) {
            Forge112Notifications.notice(player, "Paragon", skillName(skill), "Select limit reached");
            return;
        }
        if (!data.activateParagonSkill(skill)) {
            Forge112Notifications.notice(player, "Paragon", skillName(skill), "Cannot activate");
            return;
        }
        data.getSkill(skill).setLevel(Math.max(data.getSkill(skill).getLevel(), CONFIG.getMaxLevel()));
        persistSkillData(player, data);
        Forge112Notifications.paragon(player, skill);
        LOG.info("[MurilloSkills][1.12.2][Network] {} set paragon {}", player.getName(), skill);
    }

    private static void handleSkillPrestige(EntityPlayerMP player, SkillType skill) {
        if (skill == null) {
            return;
        }
        PlayerSkillDataCore data = data(player);
        SkillStatsCore stats = data.getSkill(skill);
        if (!data.isParagonSkill(skill)) {
            say(player, "MurilloSkills: apenas a skill Paragon pode prestigiar.");
            return;
        }
        if (stats.getLevel() < CONFIG.getMaxLevel()) {
            say(player, "MurilloSkills: precisa estar no nivel " + CONFIG.getMaxLevel() + " para prestigiar.");
            return;
        }
        if (stats.getPrestige() >= CONFIG.getMaxPrestigeLevel()) {
            say(player, "MurilloSkills: prestigio maximo ja alcancado.");
            return;
        }
        int nextPrestige = stats.getPrestige() + 1;
        stats.setPrestige(nextPrestige);
        stats.setLevel(0);
        stats.setXp(0.0D);
        stats.setLastAbilityUse(-1L);
        persistSkillData(player, data);
        Forge112Notifications.notice(player, "Prestige", skillName(skill), "Prestige " + nextPrestige);
        LOG.info("[MurilloSkills][1.12.2][Network] {} prestiged {} to {}", player.getName(), skill, nextPrestige);
    }

    private static void handleSkillToggle(EntityPlayerMP player, String payload) {
        String[] parts = payload == null ? new String[0] : payload.split(";", 2);
        if (parts.length < 2) {
            return;
        }
        SkillType skill = parseSkill(parts[0]);
        String toggleName = clean(parts[1]).toLowerCase(Locale.ROOT);
        if (skill == null || toggleName.length() == 0) {
            return;
        }
        toggle(player, skill, toggleName, false);
    }

    private static void handleSkillResetAll(EntityPlayerMP player) {
        PlayerSkillDataCore data = new PlayerSkillDataCore();
        STORE.cache.put(player.getUniqueID(), data);
        persistSkillData(player, data);
        Forge112Notifications.notice(player, "MurilloSkills", "Reset", "Skill data cleared");
        LOG.info("[MurilloSkills][1.12.2][Network] {} reset all skill data", player.getName());
    }

    private static void persistSkillData(EntityPlayer player, PlayerSkillDataCore data) {
        data.normalizeParagonState();
        SkillRegistry.applyPassives(player, data);
        STORE.save(player.getUniqueID());
    }

    private static void applyUltmineSelection(EntityPlayerMP player, String payload) {
        String[] parts = payload == null ? new String[0] : payload.split(";", -1);
        if (parts.length < 5) {
            return;
        }
        try {
            UltmineShape112 shape = UltmineShape112.valueOf(parts[0]);
            Forge112MiningTools.setUltmineSelection(player, shape, parseInt(parts[1], 1),
                    parseInt(parts[2], 1), parseInt(parts[3], 0), parseInt(parts[4], 1500));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static void handlePreviewRequest(EntityPlayerMP player, String payload) {
        String[] parts = payload == null ? new String[0] : payload.split(";", -1);
        if (parts.length < 4) {
            sendPreview(player, Collections.<BlockPos>emptyList());
            return;
        }
        try {
            Forge112MiningTools.setUltmineHeld(player, true);
            BlockPos pos = new BlockPos(parseInt(parts[0], 0), parseInt(parts[1], 0), parseInt(parts[2], 0));
            EnumFacing face = null;
            int faceIndex = parseInt(parts[3], -1);
            if (faceIndex >= 0 && faceIndex < EnumFacing.values().length) {
                face = EnumFacing.getFront(faceIndex);
            }
            Vec3d eyes = player.getPositionEyes(1.0F);
            if (eyes.squareDistanceTo(new Vec3d(pos).addVector(0.5D, 0.5D, 0.5D)) > 100.0D) {
                sendPreview(player, Collections.<BlockPos>emptyList());
                return;
            }
            if (!Forge112MiningTools.isLoadedBlock(player.world, pos)) {
                sendPreview(player, Collections.<BlockPos>emptyList());
                return;
            }
            IBlockState state = player.world.getBlockState(pos);
            if (state == null || state.getBlock() == Blocks.AIR) {
                sendPreview(player, Collections.<BlockPos>emptyList());
                return;
            }
            Forge112MiningTools.recordUltmineTargetFace(player, pos, face == null ? Forge112MiningTools.faceFromLook(player) : face);
            sendPreview(player, Forge112MiningTools.getValidatedUltminePreview(player, pos, face));
        } catch (Exception error) {
            MurilloSkillsForge112.LOG.warn("[MurilloSkills][1.12.2][Network] Ultmine preview failed: {}",
                    error.toString());
            sendPreview(player, Collections.<BlockPos>emptyList());
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static SkillType parseSkill(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            return SkillType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static List<SkillType> parseSkillList(String payload) {
        List<SkillType> out = new ArrayList<SkillType>();
        Set<SkillType> seen = new LinkedHashSet<SkillType>();
        String[] parts = payload == null ? new String[0] : payload.split("[,;]", -1);
        for (String part : parts) {
            SkillType skill = parseSkill(part);
            if (skill != null && seen.add(skill)) {
                out.add(skill);
            }
        }
        return out;
    }

    public static final class NotificationMessage implements IMessage {
        private String type = "";
        private String[] fields = new String[0];

        public NotificationMessage() {
        }

        public NotificationMessage(String type, String[] fields) {
            this.type = clean(type);
            if (fields == null) {
                this.fields = new String[0];
            } else {
                int count = Math.min(fields.length, MAX_FIELDS);
                this.fields = new String[count];
                for (int i = 0; i < count; i++) {
                    this.fields[i] = clean(fields[i]);
                }
            }
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            type = readString(buf, MAX_FIELD_BYTES);
            int rawCount = buf.readableBytes() > 0 ? buf.readUnsignedByte() : 0;
            int count = Math.max(0, Math.min(MAX_FIELDS, rawCount));
            fields = new String[count];
            for (int i = 0; i < rawCount; i++) {
                String value = readString(buf, MAX_FIELD_BYTES);
                if (i < count) {
                    fields[i] = value;
                }
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writeString(buf, clean(type), MAX_FIELD_BYTES);
            int count = fields == null ? 0 : Math.min(fields.length, MAX_FIELDS);
            buf.writeByte(count);
            for (int i = 0; i < count; i++) {
                writeString(buf, clean(fields[i]), MAX_FIELD_BYTES);
            }
        }
    }

    public static final class ClientActionMessage implements IMessage {
        private String action = "";
        private String payload = "";

        public ClientActionMessage() {
        }

        public ClientActionMessage(String action, String payload) {
            this.action = clean(action);
            this.payload = payload == null ? "" : payload;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            action = clean(readString(buf, MAX_ACTION_BYTES));
            payload = cleanPayload(readString(buf, MAX_TEXT_BYTES));
        }

        @Override
        public void toBytes(ByteBuf buf) {
            writeString(buf, clean(action), MAX_ACTION_BYTES);
            writeString(buf, cleanPayload(payload), MAX_TEXT_BYTES);
        }
    }

    public static final class UltminePreviewMessage implements IMessage {
        private List<BlockPos> positions = new ArrayList<BlockPos>();

        public UltminePreviewMessage() {
        }

        public UltminePreviewMessage(List<BlockPos> positions) {
            this.positions = positions == null ? new ArrayList<BlockPos>() : new ArrayList<BlockPos>(positions);
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            int rawCount = buf.readableBytes() >= 2 ? buf.readUnsignedShort() : 0;
            int count = Math.max(0, Math.min(MAX_PREVIEW_BLOCKS, rawCount));
            positions = new ArrayList<BlockPos>(count);
            for (int i = 0; i < rawCount && buf.readableBytes() >= 8; i++) {
                BlockPos pos = BlockPos.fromLong(buf.readLong()).toImmutable();
                if (i < count) {
                    positions.add(pos);
                }
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            int count = positions == null ? 0 : Math.min(positions.size(), MAX_PREVIEW_BLOCKS);
            buf.writeShort(count);
            for (int i = 0; i < count; i++) {
                BlockPos pos = positions.get(i);
                buf.writeLong(pos == null ? 0L : pos.toLong());
            }
        }
    }

    public static final class NotificationHandler implements IMessageHandler<NotificationMessage, IMessage> {
        @Override
        public IMessage onMessage(final NotificationMessage message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    Forge112NotificationHud.acceptNetwork(message.type, message.fields);
                }
            });
            return null;
        }
    }

    public static final class ClientActionHandler implements IMessageHandler<ClientActionMessage, IMessage> {
        @Override
        public IMessage onMessage(final ClientActionMessage message, MessageContext ctx) {
            if (ctx.getServerHandler() == null || ctx.getServerHandler().player == null) {
                return null;
            }
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    handleClientAction(player, message.action, message.payload);
                }
            });
            return null;
        }
    }

    public static final class UltminePreviewHandler implements IMessageHandler<UltminePreviewMessage, IMessage> {
        @Override
        public IMessage onMessage(final UltminePreviewMessage message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    UltmineClientState112.setPreview(message.positions);
                }
            });
            return null;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').replace('|', '/').trim();
    }

    private static String cleanPayload(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replace('\n', ' ').replace('\r', ' ').trim();
        return limitUtf8(cleaned, MAX_TEXT_BYTES);
    }

    private static String readString(ByteBuf buf, int maxBytes) {
        if (buf == null || buf.readableBytes() <= 0) {
            return "";
        }
        try {
            int length = ByteBufUtils.readVarInt(buf, 5);
            if (length < 0 || length > maxBytes || length > buf.readableBytes()) {
                buf.skipBytes(Math.min(Math.max(length, 0), buf.readableBytes()));
                return "";
            }
            String value = buf.toString(buf.readerIndex(), length, StandardCharsets.UTF_8);
            buf.readerIndex(buf.readerIndex() + length);
            return value;
        } catch (Exception ignored) {
            buf.readerIndex(buf.writerIndex());
            return "";
        }
    }

    private static void writeString(ByteBuf buf, String value, int maxBytes) {
        byte[] bytes = limitUtf8(value, maxBytes).getBytes(StandardCharsets.UTF_8);
        ByteBufUtils.writeVarInt(buf, bytes.length, 5);
        buf.writeBytes(bytes);
    }

    private static String limitUtf8(String value, int maxBytes) {
        if (value == null || value.length() == 0) {
            return "";
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return value;
        }
        int low = 0;
        int high = value.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (value.substring(0, mid).getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return value.substring(0, low);
    }
}
