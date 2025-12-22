package com.murilloskills.data;

import com.murilloskills.MurilloSkills;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;

@SuppressWarnings("UnstableApiUsage")
public class ModAttachments {

    // Register the attachment for PlayerSkillData using the builder pattern.
    // - initializer: () -> new PlayerSkillData() provides the default value for new
    // players
    // - persistent: with CODEC for saving/loading
    // - copyOnDeath: ensures data persists when player dies
    public static final AttachmentType<PlayerSkillData> PLAYER_SKILLS = AttachmentRegistry
            .<PlayerSkillData>builder()
            .initializer(PlayerSkillData::new)
            .persistent(PlayerSkillData.CODEC)
            .copyOnDeath()
            .buildAndRegister(Identifier.of(MurilloSkills.MOD_ID, "player_skills"));

    public static void register() {
        // Just referencing the static field to ensure class loading/registration
        // happens
        // but explicit registration method is good practice for clarity in Main class
    }
}
