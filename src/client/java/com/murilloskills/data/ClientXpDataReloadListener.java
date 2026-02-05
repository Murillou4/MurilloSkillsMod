package com.murilloskills.data;

import com.murilloskills.MurilloSkills;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class ClientXpDataReloadListener implements SimpleSynchronousResourceReloadListener {
    private static final Identifier ID = Identifier.of(MurilloSkills.MOD_ID, "xp_data_client");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        XpDataManager.reload(manager);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.CLIENT_RESOURCES;
    }
}
